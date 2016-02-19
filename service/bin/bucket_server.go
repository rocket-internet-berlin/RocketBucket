package main

import (
    "os"
    "fmt"
    "log"
    "time"
    "net/http"
    "io/ioutil"
    "rocket_bucket"
    "encoding/json"
)

var (
    config rocket_bucket.Config
    selector rocket_bucket.Selector
    startupTime time.Time
    startupTimeString string
)

func permissionDenied(w http.ResponseWriter) {
        w.WriteHeader(http.StatusForbidden)
        w.Write([]byte("Valid API key required."))
}

type Response struct {
    StartTime time.Time
    EndTime time.Time
    ClientIP string
    ResponseCode int
    ResponseString string
    UserID string
}

func handler(w http.ResponseWriter, r *http.Request) {
    // handle when url is "/" but request is "/something"
    // write solid access logs

    var userId string = r.URL.Query().Get("user_id")

    if userId == "" {
        w.WriteHeader(http.StatusBadRequest)
        w.Write([]byte("user_id must be set"))
        return
    }

    w.Header().Set("Content-Type", "application/json")
    w.Header().Set("Last-Modified", startupTimeString)
    
    if r.Header.Get("If-Modified-Since") != "" {
        ifModifiedSince, err := time.Parse(time.RFC1123, r.Header.Get("If-Modified-Since"))
        
        if err != nil {
            log.Panic(err.Error())
            http.Error(w, err.Error(), http.StatusInternalServerError)
        }

        if !startupTime.After(ifModifiedSince) {
            w.WriteHeader(http.StatusNotModified)
            return
        }
    }
        
    if config.Server.CacheMaxAge > 0 {
        w.Header().Set("Cache-Control",
            fmt.Sprintf("public, max-age=%d, must-revalidate", config.Server.CacheMaxAge))
    }
    
    if config.IsAPIKeyMandatory() {
        var apiKey string = r.URL.Query().Get("api_key")
        
        if (apiKey == "" || !config.IsValidAPIKey(apiKey)) {
            permissionDenied(w)
            return
        }
    }
        
    selectedBuckets := selector.AssignBuckets(userId)
    
	jsonBytes, err := json.Marshal(selectedBuckets)
    
    if err == nil {
        w.Write(jsonBytes)
    } else {
        log.Panic(err.Error())
        http.Error(w, err.Error(), http.StatusInternalServerError)
    }
}

func readConfig() []byte {
    if len(os.Args) == 1 {
        log.Panicln("Usage: bucket_server configuration_file_path")
    }
    
    configFile := os.Args[1]
    log.Printf("Using config file %s", configFile)
    
    configData, err := ioutil.ReadFile(configFile)
    
    if (err != nil) {
        log.Panicln(err)
    }
    
    return configData
}

func main() {
    // doing it this way round to remove microseconds from time.Now() (made testing 304 hard)
    startupTimeString = time.Now().Format(time.RFC1123) 
    startupTime, _ = time.Parse(time.RFC1123, startupTimeString)
    
    log.Printf("%v\n", startupTime)
    
    config = rocket_bucket.Config{}
    config.Parse(readConfig())

    selector = rocket_bucket.Selector{Experiments: config.Experiments}

    log.Printf("Listening: url: `%s`, port: `%d`", config.Server.URL, config.Server.Port)

    http.HandleFunc(config.Server.URL, handler)    
    
    http.ListenAndServe(fmt.Sprintf(":%d", config.Server.Port), nil)
}

