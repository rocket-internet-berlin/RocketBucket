package rocket_bucket

import (
	"encoding/json"
)

type Selector struct {
	Experiments *Experiments
}

type SelectedBucket struct {
	Name string           `json:"name"`
	Data *json.RawMessage `json:"data,omitempty"`
}

func (s *Selector) AssignBuckets(userID string) map[string]SelectedBucket {
	selectedBuckets := make(map[string]SelectedBucket)
	userIDHash := hash(userID)

	for _, experiment := range *s.Experiments {
		comparableHash := uint64(userIDHash + hash(experiment.Name))
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
