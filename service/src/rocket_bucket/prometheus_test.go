package rocket_bucket

import (
	"github.com/prometheus/client_golang/prometheus"
	"net/http"
	"net/http/httptest"
	"testing"

	"strings"
)

func TestCreateExperimentMetrics(t *testing.T) {
	experiments := getMockedExperiments()
	r := createExperimentsMetrics(experiments)
	if len(r) != len(experiments) {
		t.Errorf("Expected %d prometheus metrics was %d", len(experiments), len(r))
	}
}

func TestCreateMetricLabel(t *testing.T) {
	input := "www www"
	expected := "www_www"
	r := createMetricLabel(input)

	if r != expected {
		t.Errorf("The label is invalid. Expected %s", expected)
	}
}

func TestServingMetrics(t *testing.T) {
	experiments := getMockedExperiments()
	m := createExperimentsMetrics(experiments)
	registerExperimentMetrics(m)
	incMetricMockedExperiments(m)

	recorder := httptest.NewRecorder()
	h := getPrometheusHandler()

	req1, _ := http.NewRequest("GET", "http://localhost:3000/metrics", nil)
	h.ServeHTTP(recorder, req1)
	body := recorder.Body.String()

	expected := []string{"bucket_experiment_wear_style",
		"bucket_experiment_talking_attitude"}

	for idx := range expected {
		e := expected[idx]
		if !strings.Contains(body, e) {
			t.Errorf("body does not contain request total entry '%s'", e)
		}
	}
}

func getMockedExperiments() []Experiment {
	e1 := Experiment{}
	e1.Name = "talking attitude"
	e1.Buckets = make([]Bucket, 2)
	e1.Buckets[0].Name = "very calm"
	e1.Buckets[1].Name = "all capslock"

	e2 := Experiment{}
	e2.Name = "wear style"
	e2.Buckets = make([]Bucket, 2)
	e2.Buckets[0].Name = "causal"
	e2.Buckets[1].Name = "formal"
	return []Experiment{e1, e2}
}

func incMetricMockedExperiments(m map[string]*prometheus.CounterVec) {
	m["talking attitude"].WithLabelValues("very_calm").Inc()
	m["talking attitude"].WithLabelValues("all_capslock").Inc()
	m["wear style"].WithLabelValues("causal").Inc()
	m["wear style"].WithLabelValues("formal").Inc()
}
