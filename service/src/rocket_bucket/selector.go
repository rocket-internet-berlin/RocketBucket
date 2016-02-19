package rocket_bucket

import (
	"encoding/json"
)

type Selector struct {
	Experiments Experiments
}

type SelectedBucket struct {
	Name string           `json:"name"`
	Data *json.RawMessage `json:"data,omitempty"`
}

func (s *Selector) AssignBuckets(token string) map[string]SelectedBucket {
	selectedBuckets := make(map[string]SelectedBucket)
	tokenHash := hash(token)

	for _, experiment := range s.Experiments {
        comparableHash := uint64(tokenHash + hash(experiment.Name))
		for _, bucket := range experiment.Buckets {
			if uint64(bucket.CumulativeProbability) > (comparableHash % 100) {
				selectedBucket := SelectedBucket{}

				selectedBucket.Name = bucket.Name

				if bucket.Data != nil {
					selectedBucket.Data = &bucket.Data
				}

				selectedBuckets[experiment.Name] = selectedBucket

				break
			}
		}
	}

	return selectedBuckets
}

// THEN READ http://stackoverflow.com/questions/4463561/weighted-random-selection-from-array
