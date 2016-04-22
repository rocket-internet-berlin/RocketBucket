package rocket_bucket

import (
	"encoding/json"
	"log"
	"sort"
	"strings"
	"time"
)

const minimumAPIKeyLength = 32

type ServerConfig struct {
	Port          int      `json:"port"`
	URL           string   `json:"url"`
	CacheMaxAge   int      `json:"cache_max_age"`
	APIKeys       []string `json:"api_keys"`
	APIKeyMap     map[string]bool
	BucketDumpURL string
}

type ConfigBucketData struct {
	Name  string `json:"name"`
	Value string `json:"value"`
}

type Bucket struct {
	Name                  string             `json:"name"`
	Percent               int                `json:"percent"`
	Data                  []ConfigBucketData `json:"data,omitempty"`
	CumulativeProbability uint32             `json:"-"`
}

type Buckets []Bucket
type Experiments []Experiment

type Experiment struct {
	Name      string  `json:"name"`
	IsEnabled bool    `json:"enabled"`
	Buckets   Buckets `json:"buckets"`
	Hash      uint32  `json:"-"`
}

type Config struct {
	Server               ServerConfig
	Experiments          Experiments
	TemporaryServer      ServerConfig `json:"server"`
	TemporaryExperiments Experiments  `json:"experiments"`
	LastParsed           time.Time
}

func (slice Buckets) Len() int {
	return len(slice)
}

func (slice Buckets) Less(i, j int) bool {
	return slice[j].Name > slice[i].Name
}

func (slice Buckets) Swap(i, j int) {
	slice[i], slice[j] = slice[j], slice[i]
}

func (c *Config) Parse(rawJson []byte) {
	// incase a previous config had something set and a new version does not
	c.TemporaryServer = ServerConfig{}
	c.TemporaryExperiments = Experiments{}

	err := json.Unmarshal(rawJson, &c)

	if err != nil {
		log.Panic(err)
	}

	c.tidyExperiments()
	c.tidyServerConfig()

	// instant switch over incase this is happening live
	c.Server = c.TemporaryServer
	c.Experiments = c.TemporaryExperiments
	// hack to remove microseconds which make testing 304 impossible
	c.LastParsed, _ = time.Parse(time.RFC1123, time.Now().Format(time.RFC1123))
}

func (c *Config) IsAPIKeyMandatory() bool {
	return len(c.Server.APIKeys) > 0
}

func (c *Config) IsValidAPIKey(possibleKey string) bool {
	return c.Server.APIKeyMap[possibleKey]
}

func (c *Config) tidyExperiments() {
	var enabledOnlyExperiments []Experiment
	definedExperimentNames := map[string]bool{}

	for _, experiment := range c.TemporaryExperiments {
		Info("parsing experiment `%s`...", experiment.Name)

		if experiment.Name == "" {
			Fatal("experiment has missing name")
		}

		if definedExperimentNames[experiment.Name] {
			Fatal("experiment name `%s` is not unique", experiment.Name)
		} else {
			definedExperimentNames[experiment.Name] = true
		}

		c.validateBucketsFor(experiment)

		if experiment.IsEnabled {
			experiment.Hash = hash(experiment.Name)
			c.tidyBucketsFor(experiment)
			enabledOnlyExperiments = append(enabledOnlyExperiments, experiment)
		} else {
			Info("experiment `%s` disabled. Skipping.", experiment.Name)
		}
	}
	c.TemporaryExperiments = enabledOnlyExperiments
}

func (c *Config) validateBucketsFor(experiment Experiment) {
	definedBucketNames := map[string]bool{}

	if len(experiment.Buckets) == 0 {
		Fatal("experiment `%s` has no buckets defined", experiment.Name)
	}

	for _, bucket := range experiment.Buckets {
		if len(bucket.Name) == 0 {
			Fatal("experiment `%s` bucket with missing name", experiment.Name)
		}

		if definedBucketNames[bucket.Name] {
			Fatal("experiment `%s` has duplicate buckets named `%s`", experiment.Name, bucket.Name)
		} else {
			definedBucketNames[bucket.Name] = true
		}
	}
}

func (c *Config) tidyBucketsFor(experiment Experiment) {
	cumulativePercent := 0

	sort.Sort(experiment.Buckets)

	for i, bucket := range experiment.Buckets {
		Info("using bucket for experiment `%s`: name=`%s`, percent=%d",
			experiment.Name, bucket.Name, bucket.Percent)

		cumulativePercent += bucket.Percent
		experiment.Buckets[i].CumulativeProbability = uint32(cumulativePercent)
	}

	if cumulativePercent != 100 {
		Fatal("bucket percentages for experiment `%s` do not total 100%% (actual: %d%%)",
			experiment.Name, cumulativePercent)
	}
}

func (c *Config) tidyURL(url string) string {
	if strings.HasSuffix(url, "/") && len(url) > 1 {
		url = strings.TrimRight(url, "/")
	}

	return url
}

func (c *Config) tidyServerConfig() {
	if c.TemporaryServer.Port == 0 {
		Fatal("no server port set")
	}

	if len(c.TemporaryServer.URL) == 0 || c.TemporaryServer.URL == "/" {
		c.TemporaryServer.URL = "/"
		c.TemporaryServer.BucketDumpURL = "/all"
	} else {
		c.TemporaryServer.URL = c.tidyURL(c.TemporaryServer.URL)
		c.TemporaryServer.BucketDumpURL = c.TemporaryServer.URL + "/all"
	}

	if len(c.TemporaryServer.APIKeys) == 0 {
		Info("no API keys are set (api_keys in config). Anybody with access to this service can query it!")
	} else {
		c.TemporaryServer.APIKeyMap = make(map[string]bool, len(c.TemporaryServer.APIKeys))
		for _, key := range c.TemporaryServer.APIKeys {
			if len(key) < minimumAPIKeyLength {
				Fatal("API key `%s` too short (minimum %d characters)", key, minimumAPIKeyLength)
			}

			Info("using API key: `%s`", key)
			c.TemporaryServer.APIKeyMap[key] = true
		}
	}
}
