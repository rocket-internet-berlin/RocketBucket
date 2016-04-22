package rocket_bucket

type Selector struct {
	Experiments *Experiments
}

type SelectedExperiment struct {
	Name        string         `json:"name"`
	Description string         `json:"description"`
	Bucket      SelectedBucket `json:"bucket"`
}

type SelectedBucket struct {
	Name string             `json:"name"`
	Data []ConfigBucketData `json:"data,omitempty"`
}

func (s *Selector) AssignBuckets(userID string) []SelectedExperiment {
	var selectedExperiments = make([]SelectedExperiment, len(*s.Experiments))
	userIDHash := hash(userID)

	for i, experiment := range *s.Experiments {
		// Stupid or smart...
		// Overflow a uint32 to generate a pseudo-random number based on the
		// user id and the experiment name. After much testing this seems to better distribute
		// users into buckets by reducing overlap where experiment.Name%100 is the same
		// for multiple experiments.
		// This overflow behavior is specified here: https://golang.org/ref/spec#Integer_overflow
		comparableHash := userIDHash * hash(experiment.Name)
		for _, bucket := range experiment.Buckets {
			if bucket.CumulativeProbability > (comparableHash % 100) {
				selectedBucket := SelectedBucket{Name: bucket.Name, Data: bucket.Data}
				selectedExperiments[i] = SelectedExperiment{Name: experiment.Name, Description: experiment.Description, Bucket: selectedBucket}
				break
			}
		}
	}

	return selectedExperiments
}
