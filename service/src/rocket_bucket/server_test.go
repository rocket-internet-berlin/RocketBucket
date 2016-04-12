package rocket_bucket

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"
	"time"
)

const apiKey = `01234567890123456789012345678912`

func buildRequest(header http.Header) (Server, *httptest.ResponseRecorder, *http.Request) {
	config := Config{}
	config.Parse([]byte(fmt.Sprintf(`{
        "server":{
            "port":8080,
            "url":"/",
            "cache_max_age": 3600,
            "api_keys": [
                "%s"
            ]
        },
        "experiments":[
            {
                "name":"experiment",
                "enabled":true,
                "buckets":[
                    {
                        "name": "bucket 1",
                        "percent":50,
                        "data":[{"name":"some name","value":"some value"}]
                    },
                    {
                        "name": "bucket 2",
                        "percent":50,
                        "data":[{"name":"some other name","value":"some other value"}]
                    }
                ]
            }
        ]
    }`, apiKey)))

	selector := Selector{Experiments: &config.Experiments}

	server := Server{Config: &config, Selector: &selector}

	response := httptest.NewRecorder()
	request := &http.Request{
		Method: "GET",
		Header: header,
	}

	return server, response, request
}

func requestAssignBuckets(header http.Header, userID string) *httptest.ResponseRecorder {
	server, response, request := buildRequest(header)
	request.URL = &url.URL{RawQuery: fmt.Sprintf("user_id=%s", userID)}
	server.HandleBucketAssignment(response, request)
	return response
}

func requestDumpBuckets(header http.Header) *httptest.ResponseRecorder {
	server, response, request := buildRequest(header)
	server.HandleBucketDump(response, request)
	return response
}

func TestMissingUserID(t *testing.T) {
	response := requestAssignBuckets(http.Header{"X-Api-Key": {apiKey}}, "")

	if response.Code != 400 {
		t.Errorf("expected 400 response code, got %d", response.Code)
	}
}

func TestMissingAPIKey(t *testing.T) {
	response := requestAssignBuckets(http.Header{}, "123")

	if response.Code != 403 {
		t.Errorf("expected 403 response code, got %d", response.Code)
	}
}

func TestHeaders(t *testing.T) {
	response := requestAssignBuckets(http.Header{"X-Api-Key": {apiKey}}, "123")
	headers := response.Header()

	if headers["Cache-Control"][0] != "public, max-age=3600, must-revalidate" {
		t.Errorf("unexpected Cache-Control header %s", headers["Cache-Control"][0])
	}

	if headers["Content-Type"][0] != "application/json" {
		t.Errorf("unexpected Content-Type header %s", headers["Content-Type"][0])
	}

	if strings.Count(headers["Last-Modified"][0], ":") != 2 {
		t.Errorf("Last-Modified doesn't look like time...? %s", headers["Last-Modified"][0])
	}
}

func TestNotModified(t *testing.T) {
	response := requestAssignBuckets(http.Header{"If-Modified-Since": {time.Now().Format(time.RFC1123)}, "X-Api-Key": {apiKey}}, "123")

	if response.Code != 304 {
		t.Errorf("expected 304 response code, got %d", response.Code)
	}

	// just make sure it's modified since the past
	response = requestAssignBuckets(http.Header{"If-Modified-Since": {time.Now().Add(-10 * time.Minute).Format(time.RFC1123)}, "X-Api-Key": {apiKey}}, "123")

	if response.Code != 200 {
		t.Errorf("expected 200 response code, got %d", response.Code)
	}
}

func TestValidResponse(t *testing.T) {
	response := requestAssignBuckets(http.Header{"X-Api-Key": {apiKey}}, "123")

	if response.Code != 200 {
		t.Errorf("expected 200 response code, got %d", response.Code)
	}

	var decoded map[string]json.RawMessage

	json.Unmarshal(response.Body.Bytes(), &decoded)

	experiments := decoded["experiments"]

	if experiments == nil {
		t.Errorf("missing all experiments from response")
	}

	var decodedExperiments []SelectedExperiment

	json.Unmarshal(experiments, &decodedExperiments)

	experiment := decodedExperiments[0]

	if experiment.Name != "experiment" {
		t.Errorf("missing experiment from response")
	}

	if experiment.Bucket.Name != "bucket 1" {
		t.Errorf("missing experiment bucket from response")
	}

	if experiment.Bucket.Data == nil {
		t.Errorf("missing experiment bucket data from response")
	}

	if experiment.Bucket.Data[0].Name != "some name" || experiment.Bucket.Data[0].Value != "some value" {
		t.Errorf("corrupt experiment bucket data value from response")
	}
}

func TestDumpAllVariants(t *testing.T) {
	response := requestDumpBuckets(http.Header{"X-Api-Key": {apiKey}})

	var decoded map[string]json.RawMessage

	json.Unmarshal(response.Body.Bytes(), &decoded)

	experiments := decoded["experiments"]

	if experiments == nil {
		t.Errorf("missing all experiments from response")
	}

	var decodedExperiments Experiments

	json.Unmarshal(experiments, &decodedExperiments)

	experiment := decodedExperiments[0]

	// ASSERT EXPERIMENT CONTENT
	if experiment.Name != "experiment" {
		t.Errorf("returned experiment name is not correct")
	}

	if experiment.Buckets[0].Name != "bucket 1" {
		t.Errorf("first bucket name is incorrect")
	}

	if experiment.Buckets[0].Percent != 50 {
		t.Errorf("first bucket name is incorrect")
	}

	if experiment.Buckets[0].Data[0].Name != "some name" {
		t.Errorf("first bucket data does not exist")
	}

	if experiment.Buckets[0].CumulativeProbability != 0 {
		t.Errorf("Bucket.CumulativeProbability should not be exported")
	}

	if experiment.Hash != 0 {
		t.Errorf("experiment.Hash should not be exported")
	}

	if experiment.Buckets[1].Name != "bucket 2" {
		t.Errorf("second bucket name is incorrect")
	}

	if experiment.Buckets[1].Percent != 50 {
		t.Errorf("second bucket name is incorrect")
	}

	if experiment.Buckets[1].Data[0].Name != "some other name" {
		t.Errorf("second bucket data does not exist")
	}

	fmt.Printf("%v\n", decodedExperiments[0].Name)
}
