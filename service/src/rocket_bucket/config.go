package rocket_bucket

import (
    "log"
    "sort"
    "strings"
    "encoding/json"
)

const minimumAPIKeyLength = 32

type ServerConfig struct {
    Port int `json:"port"`
    URL  string `json:"url"`
    CacheMaxAge int `json:"cache_max_age"`
    APIKeys []string `json:"api_keys"`
    APIKeyMap map[string]bool
}

type Bucket struct {
    Name    string          `json:"name"`
    Percent int             `json:"percent"`
    Data    json.RawMessage `json:"data"`
    CumulativeProbability int
}

type Buckets [] Bucket
type Experiments [] Experiment

type Experiment struct {
    Name      string   `json:"name"`
    IsEnabled bool     `json:"enabled"`
    Buckets   Buckets `json:"buckets"`
    Hash uint32
}

type Config struct {
    Server      ServerConfig "json:server"
    Experiments Experiments "json:experiment"
}

func (slice Buckets) Len() int {
    return len(slice)
}

func (slice Buckets) Less(i, j int) bool {
    return slice[j].Percent > slice[i].Percent
}

func (slice Buckets) Swap(i, j int) {
    slice[i], slice[j] = slice[j], slice[i]
}

func (c *Config) Parse(rawJson []byte) {
    err := json.Unmarshal(rawJson, &c)

    if err != nil {
        log.Panic(err)
    }

    c.tidyExperiments()
    c.tidyServerConfig()
}

func (c *Config) IsAPIKeyMandatory() bool {
    return len(c.Server.APIKeys) > 0
}

func (c *Config) IsValidAPIKey(possibleKey string) bool {
    return c.Server.APIKeyMap[possibleKey]
}

func (c *Config) tidyExperiments() {
    var enabledOnlyExperiments []Experiment
    for _, experiment := range c.Experiments {
        log.Printf("Parsing experiment `%s`...", experiment.Name)
        if experiment.IsEnabled {
            experiment.Hash = hash(experiment.Name)
            c.tidyBucketsFor(experiment)
            enabledOnlyExperiments = append(enabledOnlyExperiments, experiment)
        } else {
            log.Printf("Experiment `%s` disabled. Skipping.", experiment.Name)
        }
    }
    c.Experiments = enabledOnlyExperiments
}

func (c *Config) tidyBucketsFor(experiment Experiment) {
    cumulativePercent := 0
    
    sort.Sort(experiment.Buckets)
    
    for i, bucket := range experiment.Buckets {
        log.Printf("Bucket for experiment `%s`: name: `%s`, percent: %d, data: `%s`",
        experiment.Name, bucket.Name, bucket.Percent, string(bucket.Data))
        cumulativePercent += bucket.Percent
        experiment.Buckets[i].CumulativeProbability = cumulativePercent
    }
        
    if cumulativePercent != 100 {
        log.Panicf("Bucket percentages for experiment `%s` do not total 100%% (actual: %d%%)",
        experiment.Name, cumulativePercent)
    }
}
    
func (c *Config) tidyServerConfig() {
    if c.Server.Port == 0 {
        log.Panic("No server port set")
    }
    
    if len(c.Server.URL) == 0 {
        c.Server.URL = "/"
    } else if strings.HasSuffix(c.Server.URL, "/") && len(c.Server.URL) > 1 {
        c.Server.URL = strings.TrimRight(c.Server.URL, "/")
    }
    
    if len(c.Server.APIKeys) == 0 {
        log.Println("No API keys are set (api_keys in config). Anybody with access to this service can query it!")
    } else {
        c.Server.APIKeyMap = make(map[string]bool, len(c.Server.APIKeys))
        for _, key := range c.Server.APIKeys {
            if (len(key) < minimumAPIKeyLength) {
                log.Panicf("API key `%s` too short (minimum %d characters)", key, minimumAPIKeyLength)
            }
            
            log.Printf("Using API key: `%s`", key)
            c.Server.APIKeyMap[key] = true
        }
    }
}
