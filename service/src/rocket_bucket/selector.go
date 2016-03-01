package rocket_bucket

import (
	"encoding/json"
)

type Selector struct {
	Experiments *Experiments
}

type SelectedExperiment struct {
    Name string `json:"name"`
    Bucket SelectedBucket `json:"bucket"`
}

type SelectedBucket struct {
	Name string           `json:"name"`
	Data *json.RawMessage `json:"data,omitempty"`
}

func (s *Selector) AssignBuckets(userID string) []SelectedExperiment {
	var selectedExperiments = make([]SelectedExperiment, len(*s.Experiments))
	userIDHash := hash(userID)

	for i, experiment := range *s.Experiments {
		comparableHash := uint64(userIDHash + hash(experiment.Name))
		for _, bucket := range experiment.Buckets {
			if uint64(bucket.CumulativeProbability) > (comparableHash % 100) {
				selectedBucket := SelectedBucket{}

				selectedBucket.Name = bucket.Name

				if bucket.Data != nil {
					selectedBucket.Data = &bucket.Data
				}

				selectedExperiments[i] = SelectedExperiment{Name: experiment.Name, Bucket:selectedBucket}

				break
			}
		}
	}

	return selectedExperiments
}
