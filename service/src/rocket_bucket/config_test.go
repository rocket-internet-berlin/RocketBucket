package rocket_bucket

import (
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
			t.Fatalf("Expected exception containing `%s`. Got `%s`.", expectedException, err)
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
        "experiments":[
            {
                "name":"experiment 1",
                "enabled":true,
                "buckets":[
                    {
                        "name": "bucket 1",
                        "percent":20,
                        "data":[{"name":"some data","value":"some value"}]
                    },
                    {
                        "name":"bucket 2",
                        "percent":50,
                        "data":[{"name":"some other data","value":"some other value"}]
                    },
                    {
                        "name":"bucket 3",
                        "percent":30
                    }
                ]
            },
            {
                "name":"disabled experiment",
                "enabled":false,
                "buckets":[{"name":"everyone","percent":100}]
            }
        ]
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

	if config.Experiments[0].Buckets[0].Data[0].Name != "some data" || config.Experiments[0].Buckets[0].Data[0].Value != "some value" {
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

func TestExperimentExceptionWithDuplicateExperimentNames(t *testing.T) {
	assertException(t, "experiment name `duplicate` is not unique", func() {
		config.Parse([]byte(`{
            "server":{"port":8080},
            "experiments":[
                {
                    "name":"duplicate",
                    "enabled":false,
                    "buckets":[{"name":"everyone","percent":100}]
                },
                {
                    "name":"duplicate",
                    "enabled":false,
                    "buckets":[{"name":"everyone","percent":100}]
                }
            ]
        }`))
	})
}

func TestExperimentExceptionWithMissingName(t *testing.T) {
	assertException(t, "experiment has missing name", func() {
		config.Parse([]byte(`{
            "server":{"port":8080},
            "experiments":[
                {
                    "enabled":false,
                    "buckets":[{"name":"everyone","percent":100}]
                }
            ]
        }`))
	})
}
func TestExperimentWithMissingEnabledFlagIsDisabled(t *testing.T) {
	config.Parse([]byte(`{
        "server":{"port":8080},
        "experiments":[
            {
                "name":"blah",
                "buckets":[{"name":"everyone","percent":100}]
            }
        ]
    }`))

	if len(config.Experiments) != 0 {
		t.Error("Experiments should be disabled/omitted by default")
	}
}
func TestExperimentExceptionWithMissingBuckets(t *testing.T) {
	assertException(t, "experiment `bucketless` has no buckets defined", func() {
		config.Parse([]byte(`{
            "server":{"port":8080},
            "experiments":[
                {
                    "name":"bucketless"
                }
            ]
        }`))
	})
}

func TestExperimentBuckedExceptionWithDuplicateNames(t *testing.T) {
	assertException(t, "experiment `bad buckets` has duplicate buckets named `duplicate`", func() {
		config.Parse([]byte(`{
            "server":{"port":8080},
            "experiments":[
                {
                    "name":"bad buckets",
                    "enabled":true,
                    "buckets":[
                        {
                            "name":"duplicate",
                            "percent":50
                        },
                        {
                            "name":"duplicate",
                            "percent":50
                        }
                    ]
                }
            ]
        }`))
	})
}
func TestExperimentBucketExceptionWithMissingName(t *testing.T) {
	assertException(t, "experiment `missing bucket name` bucket with missing name", func() {
		config.Parse([]byte(`{
            "server":{"port":8080},
            "experiments":[
                {
                    "name":"missing bucket name",
                    "enabled":false,
                    "buckets":[{"percent":100}]
                }
            ]
        }`))
	})
}

func TestExperimentBucketsExceptionWithNot100PercentCoverage(t *testing.T) {
	assertException(t, `do not total 100% (actual: 66%)`, func() {
		config.Parse([]byte(`{
            "server":{"port":8080},
            "experiments":[
                {
                    "name":"not complete percentage",
                    "enabled":true,
                    "buckets":[
                        {
                            "name": "bucket 1",
                            "percent":33
                        },
                        {
                            "name":"bucket 2",
                            "percent":33
                        }
                    ]
                }
            ]
        }`))
	})
}
