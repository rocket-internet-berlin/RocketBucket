package main

import (
    "os"
    "fmt"
    "log"
    "time"
    "syscall"
    "net/http"
    "io/ioutil"
    "os/signal"
    "rocket_bucket"
)

var (
    config rocket_bucket.Config
    selector rocket_bucket.Selector
    startupTime time.Time
)

func handler(w http.ResponseWriter, r *http.Request) {
    // handle when url is "/" but request is "/something"
    
    defer func() {
        if err := recover(); err != nil {
            rocket_bucket.Fatal("%v", err)
        }
    }()
    
    session := rocket_bucket.Session{}
    wasProcessedOk := session.Process(r, &selector, &config, startupTime)

    // set response headers
    w.Header().Set("Content-Type", "application/json")
    w.Header().Set("Last-Modified", startupTime.Format(time.RFC1123))
    
    if config.Server.CacheMaxAge > 0 {
        w.Header().Set("Cache-Control",
            fmt.Sprintf("public, max-age=%d, must-revalidate", config.Server.CacheMaxAge))
    }

    logString := fmt.Sprintf("processing_time=%.6f, response_code=%d, response_body=`%s`, remote_address=`%s`, user_id=`%s`, api_key=`%s`, log_only_response=`%s`",
    session.EndTime.Sub(session.StartTime).Seconds(), session.ResponseCode, session.ResponseBody, session.RemoteAddr, session.UserID, session.APIKey, session.PrivateLoggedResponseString)

    if wasProcessedOk {
        w.WriteHeader(session.ResponseCode)
        w.Write(session.ResponseBody)
        rocket_bucket.Info(logString)
    } else {
        http.Error(w, string(session.ResponseBody), session.ResponseCode)
        rocket_bucket.Error(logString)
    }    
}

func readConfig() {
    if len(os.Args) == 1 {
        log.Panicln("Usage: bucket_server configuration_file_path")
    }
    
    rocket_bucket.Info("loading config file %s", os.Args[1])
    
    configData, err := ioutil.ReadFile(os.Args[1])
    
    if (err != nil) {
        rocket_bucket.Fatal("%v", err)
    }
    
    // prevent invalid config loading
    defer func() {
        if r := recover(); r != nil {
            rocket_bucket.Error("%v", r)
        }
    }()
    
    newConfig := rocket_bucket.Config{}
    newConfig.Parse(configData)
    
    config = newConfig
    
    // doing it like this to remove microseconds from time.Now() (made testing http 304 hard)
    startupTime, _ = time.Parse(time.RFC1123, time.Now().Format(time.RFC1123) )
}

func setupSIGHUPHandler() {
    c := make(chan os.Signal, 1)
  	signal.Notify(c, syscall.SIGHUP)

  	go func(){
  		for sig := range c {
            println(sig)
            rocket_bucket.Info("received SIGHUP")
            readConfig()
  		}
  	}()
}

func main() {
    readConfig()
    setupSIGHUPHandler()
    
    selector = rocket_bucket.Selector{Experiments: &config.Experiments}

    rocket_bucket.Info("listening: url=`%s`, port=`%d`", config.Server.URL, config.Server.Port)

    http.HandleFunc(config.Server.URL, handler)    
    
    http.ListenAndServe(fmt.Sprintf(":%d", config.Server.Port), nil)    
}

