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
	Port        int      `json:"port"`
	URL         string   `json:"url"`
	CacheMaxAge int      `json:"cache_max_age"`
	APIKeys     []string `json:"api_keys"`
	APIKeyMap   map[string]bool
}

type Bucket struct {
	Name                  string          `json:"name"`
	Percent               int             `json:"percent"`
	Data                  json.RawMessage `json:"data"`
	CumulativeProbability int
}

type Buckets []Bucket
type Experiments []Experiment

type Experiment struct {
	Name      string  `json:"name"`
	IsEnabled bool    `json:"enabled"`
	Buckets   Buckets `json:"buckets"`
	Hash      uint32
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
	return slice[j].Percent > slice[i].Percent
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

func (c *Config) DoesURLMatch(requestedURL string) bool {
	return c.tidyURL(requestedURL) == c.Server.URL
}

func (c *Config) tidyExperiments() {
	var enabledOnlyExperiments []Experiment
	for _, experiment := range c.TemporaryExperiments {
		Info("parsing experiment `%s`...", experiment.Name)

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

func (c *Config) tidyBucketsFor(experiment Experiment) {
	cumulativePercent := 0

	sort.Sort(experiment.Buckets)

	for i, bucket := range experiment.Buckets {
		Info("using bucket for experiment `%s`: name=`%s`, percent=%d, data=`%s`",
			experiment.Name, bucket.Name, bucket.Percent, string(bucket.Data))
		cumulativePercent += bucket.Percent
		experiment.Buckets[i].CumulativeProbability = cumulativePercent
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

	if len(c.TemporaryServer.URL) == 0 {
		c.TemporaryServer.URL = "/"
	} else {
		c.TemporaryServer.URL = c.tidyURL(c.TemporaryServer.URL)
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
