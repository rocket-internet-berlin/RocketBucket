package rocket_bucket

import (
	"fmt"
	"math"
	"math/rand"
	"reflect"
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
	maxExpected := int(math.Ceil(expectedFloat + (expectedFloat * 0.1)))
	minExpected := int(math.Floor(expectedFloat - (expectedFloat * 0.1)))

	if actual < minExpected || actual > maxExpected {
		t.Errorf("expected %d-%d, got %d", minExpected, maxExpected, actual)
	}
}

func assertUsersAreBucketed(t *testing.T, percentOne int, percentTwo int, expectedOne []int, expectedTwo []int) {
	config := Config{}

	config.Parse([]byte(fmt.Sprintf(`{
        "server":{"port":8080},
        "experiments":[
            {
                "name":"experiment",
                "enabled":true,
                "buckets":[
                    {
                        "name": "1",
                        "percent":%d
                    },
                    {
                        "name":"2",
                        "percent":%d
                    }
                ]
            }
        ]
    }`, percentOne, percentTwo)))

	selector := Selector{Experiments: &config.Experiments}

	gotBucket := make(map[string][]int)

	for i := 0; i < 10; i++ {
		selectedExperiments := selector.AssignBuckets(string(i))
		gotBucket[selectedExperiments[0].Bucket.Name] = append(gotBucket[selectedExperiments[0].Bucket.Name], i)
	}

	if len(expectedOne) > 0 && !reflect.DeepEqual(expectedOne, gotBucket["1"]) {
		t.Errorf("Bucket 1 does not match. Expected: %v, got: %v", expectedOne, gotBucket["1"])
	}

	if len(expectedTwo) > 0 && !reflect.DeepEqual(expectedTwo, gotBucket["2"]) {
		t.Errorf("Bucket 2 does not match. Expected: %v, got: %v", expectedTwo, gotBucket["2"])
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

	for i := 0; i < 10000; i++ {
		someUserID := randStringBytes(15)
		selectedExperiments := selector.AssignBuckets(someUserID)
		for _, experiment := range selectedExperiments {
			if bucketCounter[experiment.Name] == nil {
				bucketCounter[experiment.Name] = map[string]int{}
			}

			bucketCounter[experiment.Name][experiment.Bucket.Name] += 1
		}
	}

	assertAudienceSizeWithinDeviation(t, bucketCounter["experiment 1"]["bucket 1"], 5000)
	assertAudienceSizeWithinDeviation(t, bucketCounter["experiment 1"]["bucket 2"], 3500)
	assertAudienceSizeWithinDeviation(t, bucketCounter["experiment 1"]["bucket 3"], 1500)

	assertAudienceSizeWithinDeviation(t, bucketCounter["experiment 2"]["bucket 1"], 1500)
	assertAudienceSizeWithinDeviation(t, bucketCounter["experiment 2"]["bucket 2"], 3500)
	assertAudienceSizeWithinDeviation(t, bucketCounter["experiment 2"]["bucket 3"], 5000)
}

func TestUsersNeverMoveOutOfGrowingBucket(t *testing.T) {
	assertUsersAreBucketed(t, 0, 100, []int{}, []int{0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
	assertUsersAreBucketed(t, 25, 75, []int{0, 3, 9}, []int{1, 2, 4, 5, 6, 7, 8})
	assertUsersAreBucketed(t, 50, 50, []int{0, 1, 3, 9}, []int{2, 4, 5, 6, 7, 8})
	assertUsersAreBucketed(t, 75, 25, []int{0, 1, 3, 6, 7, 9}, []int{2, 4, 5, 8})
	assertUsersAreBucketed(t, 25, 75, []int{0, 3, 9}, []int{1, 2, 4, 5, 6, 7, 8})
	assertUsersAreBucketed(t, 100, 0, []int{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, []int{})

}

func TestOverflowStillConsistent(t *testing.T) {
	// this test is to try and ensure that whatever version of go we use, the overflow
	// behavior remains unchanged. sort of silly to write a test for go but this is
	// central to how bucketing works so ensuring it remains persistent is important.
	x, y := uint32(3576803822), uint32(1808322892)
	if x*y != 1492394152 {
		t.Errorf("expected %d*%d == %d", x, y, 1492394152)
	}
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
