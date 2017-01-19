package rocket_bucket

import (
	"github.com/prometheus/client_golang/prometheus"
	"net/http"
	"regexp"
	"strconv"
	"strings"
)

type Metrics struct {
	Experiments    map[string]*prometheus.CounterVec
	RequestResults *prometheus.CounterVec
}

func (m *Metrics) incForHttpCode(httpCode int) {
	m.RequestResults.WithLabelValues(strconv.Itoa(httpCode)).Inc()
}

func (m *Metrics) incForBucket(experimentName string, bucketName string) {
	l := createMetricLabel(bucketName)
	m.Experiments[experimentName].WithLabelValues(l).Inc()
}

func getPrometheusHandler() http.Handler {
	return prometheus.Handler()
}

func GetMetrics(experiments []Experiment) *Metrics {
	return &Metrics{createExperimentsMetrics(experiments),
		getResultCodeMetrics()}
}

func RegisterMetrics(m *Metrics) {
	prometheus.MustRegister(m.RequestResults)
	registerExperimentMetrics(m.Experiments)
}

func getResultCodeMetrics() *prometheus.CounterVec {
	return prometheus.NewCounterVec(
		prometheus.CounterOpts{Name: "bucket_result_code", Help: "http code"},
		[]string{"http_code"})
}

func registerExperimentMetrics(m map[string]*prometheus.CounterVec) {
	for _, v := range m {
		prometheus.MustRegister(v)
	}
}

func createExperimentsMetrics(experiments []Experiment) map[string]*prometheus.CounterVec {
	result := make(map[string]*prometheus.CounterVec)

	for idx := range experiments {
		r := createExperimentMetric(experiments[idx])
		result[experiments[idx].Name] = r
	}
	return result
}

func createExperimentMetric(experiment Experiment) *prometheus.CounterVec {
	exLabel := "bucket_experiment_" + createMetricLabel(experiment.Name)
	help := "Experiment: " + experiment.Name
	return prometheus.NewCounterVec(
		prometheus.CounterOpts{Name: exLabel, Help: help},
		[]string{"bucket"})
}

func createMetricLabel(s string) string {
	reg, err := regexp.Compile("[^A-Za-z0-9]+")
	if err != nil {
		Fatal("Fatal error. The Regex is incorrect!", err)
	}
	safe := reg.ReplaceAllString(s, "_")
	return strings.ToLower(strings.Trim(safe, "_"))
}
