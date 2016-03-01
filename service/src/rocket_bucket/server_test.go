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

func makeRequest(userID string, path string, header http.Header) httptest.ResponseRecorder {
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
        "experiments":{
            "experiment":{
                "enabled":true,
                "buckets":[
                    {
                        "name": "bucket",
                        "percent":100,
                        "data":{"some data key":"some data value"}
                    }
                ]
            }
        }
    }`, apiKey)))

	selector := Selector{Experiments: &config.Experiments}

	server := Server{Config: &config, Selector: &selector}

	response := httptest.NewRecorder()
	req := &http.Request{
		Method: "GET",
		URL:    &url.URL{Path: path, RawQuery: fmt.Sprintf("user_id=%s", userID)},
		Header: header,
	}

	server.HandleRequest(response, req)

	return *response
}

func TestMissingUserID(t *testing.T) {
	response := makeRequest("", "/", http.Header{"X-Api-Key": {apiKey}})

	if response.Code != 400 {
		t.Errorf("expected 400 response code, got %d", response.Code)
	}
}

func TestMissingAPIKey(t *testing.T) {
	response := makeRequest("123", "/", http.Header{})

	if response.Code != 403 {
		t.Errorf("expected 403 response code, got %d", response.Code)
	}
}

func Test404SuperfluousPath(t *testing.T) {
	response := makeRequest("123", "/should404", http.Header{"X-Api-Key": {apiKey}})

	if response.Code != 404 {
		t.Errorf("expected 404 response code, got %d", response.Code)
	}
}

func TestHeaders(t *testing.T) {
	response := makeRequest("123", "/", http.Header{"X-Api-Key": {apiKey}})
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
	response := makeRequest("123", "/", http.Header{"If-Modified-Since": {time.Now().Format(time.RFC1123)}, "X-Api-Key": {apiKey}})

	if response.Code != 304 {
		t.Errorf("expected 304 response code, got %d", response.Code)
	}

	// just make sure it's modified since the past
	response = makeRequest("123", "/", http.Header{"If-Modified-Since": {time.Now().Add(-10 * time.Minute).Format(time.RFC1123)}, "X-Api-Key": {apiKey}})

	if response.Code != 200 {
		t.Errorf("expected 200 response code, got %d", response.Code)
	}
}

func TestValidResponse(t *testing.T) {
	response := makeRequest("123", "/", http.Header{"X-Api-Key": {apiKey}})

	if response.Code != 200 {
		t.Errorf("expected 200 response code, got %d", response.Code)
	}

	var decoded map[string]interface{}

	json.Unmarshal(response.Body.Bytes(), &decoded)

	experiments := decoded["experiments"]

	if experiments == nil {
		t.Errorf("missing all experiments from response")
	}

	experiment := experiments.(map[string]interface{})["experiment"]

	if experiment == nil {
		t.Errorf("missing experiment from response")
	}

	if experiment.(map[string]interface{})["name"] != "bucket" {
		t.Errorf("missing experiment bucket from response")
	}

	experimentData := experiment.(map[string]interface{})["data"]

	if experimentData == nil {
		t.Errorf("missing experiment bucket data from response")
	}

	if experimentData.(map[string]interface{})["some data key"] != "some data value" {
		t.Errorf("missing experiment bucket data value from response")
	}
}
