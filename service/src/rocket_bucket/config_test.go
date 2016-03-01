package rocket_bucket

import (
	// "fmt"
	"strings"
	"testing"
	"time"
)

var config Config

func init() {
	config = Config{}
}

func assertException(t *testing.T, expectedException string, f func()) {
	defer func() {
		err := recover()
		if err == nil || !strings.Contains(err.(string), expectedException) {
			t.Errorf("Expected exception containing `%s`. Got `%s`.", expectedException, err)
		}
	}()
	f()
}

func TestServerConfig(t *testing.T) {
	config.Parse([]byte(`{"server":{"port":8080,"url":"/blah"}}`))

	if config.Server.Port != 8080 {
		t.Error("port is missing")
	}

	if config.Server.URL != "/blah" {
		t.Error("url is missing")
	}

	if config.IsAPIKeyMandatory() {
		t.Error("API key marked as mandatory eventhough none are set")
	}
}

func TestServerDefaultURL(t *testing.T) {
	config.Parse([]byte(`{"server":{"port":8080}}`))

	if config.Server.URL != "/" {
		t.Error("Default url not set")
	}
}

func TestServerStripTrailingSlashesFromURL(t *testing.T) {
	config.Parse([]byte(`{"server":{"port":8080, "url":"/blah//"}}`))

	if strings.HasSuffix(config.Server.URL, "/") {
		t.Errorf("Trailing slash not stripped from %s", config.Server.URL)
	}
}

func TestDoesURLMatch(t *testing.T) {
	config.Parse([]byte(`{"server":{"port":8080, "url":"/blah"}}`))

	if !config.DoesURLMatch("/blah/") {
		t.Error("cleaned up urls should match")
	}

	if config.DoesURLMatch("/wrong/") {
		t.Error("unmatching urls should not match")
	}
}

func TestServerInvalidURL(t *testing.T) {}

func TestServerCacheMaxAge(t *testing.T) {
	config.Parse([]byte(`{"server":{"port":8080,"cache_max_age":3600}}`))

	if config.Server.CacheMaxAge != 3600 {
		t.Error("Cache max age not set")
	}
}

func TestServerApiKeys(t *testing.T) {
	config.Parse([]byte(`{
        "server":{
            "port":8080,
            "api_keys": [
                "12345678901234567890123456789012"
            ]
        }
    }`))

	if !config.IsAPIKeyMandatory() {
		t.Error("API key should be mandatory when set in config")
	}

	if !config.IsValidAPIKey("12345678901234567890123456789012") {
		t.Error("cannot lookup VALID API key")
	}

	if config.IsValidAPIKey("hey!") {
		t.Error("cannot lookup INVALID API key")
	}
}

func TestLastParsedDate(t *testing.T) {
	oldenTimes := time.Now().Add(-10 * time.Minute)
	config.LastParsed = oldenTimes
	config.Parse([]byte(`{"server":{"port":8080}}`))

	if !config.LastParsed.After(oldenTimes) {
		t.Errorf("LastParsed not set %v", config.LastParsed)
	}
}

func TestShortApiKey(t *testing.T) {
	assertException(t, "API key `abc` too short", func() {
		config.Parse([]byte(`{
            "server":{
                "port":8080,
                "api_keys": [
                    "abc"
                ]
            }
        }`))
	})
}

func TestExceptionOnMissingServerPort(t *testing.T) {
	assertException(t, `no server port set`, func() {
		config := Config{}
		config.Parse([]byte(`{"server":{}}`))
	})
}

func TestExperimentFullExperimentData(t *testing.T) {
	config.Parse([]byte(`{
        "server":{"port":8080},
        "experiments":{
            "experiment 1":{
                "enabled":true,
                "buckets":[
                    {
                        "name": "bucket 1",
                        "percent":20,
                        "data":{"some data":123}
                    },
                    {
                        "name":"bucket 2",
                        "percent":50,
                        "data":"some other data"
                    },
                    {
                        "name":"bucket 3",
                        "percent":30
                    }
                ]
            },
            "disabled experiment":{
                "enabled":false
            }
        }
    }`))

	if config.Experiments[0].Name != "experiment 1" {
		t.Error("experiment name unmatched")
	}

	if len(config.Experiments[0].Buckets) != 3 {
		t.Error("experiment should have 3 buckets")
	}

	if config.Experiments[0].Buckets[0].Name != "bucket 1" {
		t.Error("first bucket name should be smallest bucket")
	}

	if config.Experiments[0].Buckets[0].CumulativeProbability != 20 {
		t.Errorf("first bucket has lowest cumumulative probability (expected 20, got %d)", config.Experiments[0].Buckets[0].CumulativeProbability)
	}

	if config.Experiments[0].Buckets[1].CumulativeProbability != 50 {
		t.Errorf("middle bucket has mid cumumulative probability (expected 50, got %d)", config.Experiments[0].Buckets[1].CumulativeProbability)
	}

	if config.Experiments[0].Buckets[2].CumulativeProbability != 100 {
		t.Errorf("last bucket has highest cumumulative probability (expected 100, got %d)", config.Experiments[0].Buckets[2].CumulativeProbability)
	}

	if config.Experiments[0].Buckets[0].Percent == 50 {
		t.Error("first bucket has highest percentage")
	}

	if config.Experiments[0].Buckets[2].Percent == 20 {
		t.Error("last bucket has lowest percentage")
	}

	if string(config.Experiments[0].Buckets[0].Data) != `{"some data":123}` {
		t.Error("bucket data is incorrect")
	}

	// that big number is apparently the hash value for the experiment name
	var expectedHash uint32 = 435230149
	if config.Experiments[0].Hash != expectedHash {
		t.Errorf("bucket hash not calculated (expected %d, got %d)", expectedHash, config.Experiments[0].Hash)
	}

	if len(config.Experiments) > 1 {
		t.Error("disabled experiment is in list")
	}
}

func TestExperimentExceptionWithMissingPercent(t *testing.T)           {}
func TestExperimentExceptionWithMissingName(t *testing.T)              {}
func TestExperimentExceptionWithMissingEnabledFlag(t *testing.T)       {}
func TestExperimentExceptionWithMissingBuckets(t *testing.T)           {}

func TestExperimentBuckedExceptionWithDuplicateNames(t *testing.T)  {}
func TestExperimentBucketExceptionWithMissingName(t *testing.T)     {}
func TestExperimentBucketExceptionWithMissingAudience(t *testing.T) {}

func TestExperimentBucketsExceptionWithNot100PercentCoverage(t *testing.T) {
	assertException(t, `do not total 100% (actual: 66%)`, func() {
		config.Parse([]byte(`{
            "server":{"port":8080},
            "experiments":{
                "not complete percentage":{
                    "enabled":true,
                    "buckets":[
                        {
                            "name": "bucket 1",
                            "percent":33,
                            "data":{"some data":123}
                        },
                        {
                            "name":"bucket 2",
                            "percent":33,
                            "data":"some other data"
                        }
                    ]
                }
            }
        }`))
	})
}
