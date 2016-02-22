package rocket_bucket

import (
	// "fmt"
	"math"
	"math/rand"
	"testing"
)

const letterBytes = `0123456789!@#$%^&*()_+-={}[]:"|;\<>?/.,'~abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ`

func randStringBytes(n int) string {
	b := make([]byte, n)
	for i := range b {
		b[i] = letterBytes[rand.Intn(len(letterBytes))]
	}
	return string(b)
}

func assertAudienceSizeWithinDeviation(t *testing.T, actual int, expected int) {
	expectedFloat := float64(expected)
	maxExpected := int(math.Ceil(expectedFloat + (expectedFloat * 0.05)))
	minExpected := int(math.Floor(expectedFloat - (expectedFloat * 0.05)))

	if actual < minExpected || actual > maxExpected {
		t.Errorf("expected %d-%d, got %d", minExpected, maxExpected, actual)
	}
}

func TestBucketing(t *testing.T) {
	config := Config{}

	config.Parse([]byte(`{
        "server":{"port":8080},
        "experiments":[
            {
                "name":"experiment 1",
                "enabled":true,
                "buckets":[
                    {
                        "name": "bucket 1",
                        "percent":50
                    },
                    {
                        "name":"bucket 2",
                        "percent":35
                    },
                    {
                        "name":"bucket 3",
                        "percent":15
                    }
                ]
            },
            {
                "name":"experiment 2",
                "enabled":true,
                "buckets":[
                    {
                        "name": "bucket 1",
                        "percent":15
                    },
                    {
                        "name":"bucket 2",
                        "percent":35
                    },
                    {
                        "name":"bucket 3",
                        "percent":50
                    }
                ]
            }
        ]
    }`))

	selector := Selector{Experiments: &config.Experiments}

	bucketCounter := map[string]map[string]int{}

	for i := 0; i < 100; i++ {
		someUserID := randStringBytes(15)
		selectedBuckets := selector.AssignBuckets(someUserID)
		for experimentName, bucket := range selectedBuckets {
			if bucketCounter[experimentName] == nil {
				bucketCounter[experimentName] = map[string]int{}
			}

			bucketCounter[experimentName][bucket.Name] += 1
		}
	}

	assertAudienceSizeWithinDeviation(t, bucketCounter["experiment 1"]["bucket 1"], 50)
	assertAudienceSizeWithinDeviation(t, bucketCounter["experiment 1"]["bucket 2"], 35)
	assertAudienceSizeWithinDeviation(t, bucketCounter["experiment 1"]["bucket 3"], 15)

	assertAudienceSizeWithinDeviation(t, bucketCounter["experiment 2"]["bucket 1"], 15)
	assertAudienceSizeWithinDeviation(t, bucketCounter["experiment 2"]["bucket 2"], 35)
	assertAudienceSizeWithinDeviation(t, bucketCounter["experiment 2"]["bucket 3"], 50)
}

func BenchmarkBucketingfunc(b *testing.B) {
	config := Config{}

	config.Parse([]byte(`{
        "server":{"port":8080},
        "experiments":[
            {
                "name":"experiment 1",
                "enabled":true,
                "buckets":[
                    {
                        "name": "bucket 1",
                        "percent":50
                    },
                    {
                        "name":"bucket 2",
                        "percent":35
                    },
                    {
                        "name":"bucket 3",
                        "percent":15
                    }
                ]
            },
            {
                "name":"experiment 2",
                "enabled":true,
                "buckets":[
                    {
                        "name": "bucket 1",
                        "percent":15
                    },
                    {
                        "name":"bucket 2",
                        "percent":35
                    },
                    {
                        "name":"bucket 3",
                        "percent":50
                    }
                ]
            }
        ]
    }`))

	selector := Selector{Experiments: &config.Experiments}

	var tokens [1000000]string

	for i, _ := range tokens {
		tokens[i] = randStringBytes(15)
	}

	b.ResetTimer()

	for _, token := range tokens {
		selector.AssignBuckets(token)
	}
}
