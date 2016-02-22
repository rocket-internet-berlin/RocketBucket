package rocket_bucket

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

type Session struct {
	StartTime                   time.Time
	EndTime                     time.Time
	RemoteAddr                  string
	ResponseCode                int
	ResponseBody                []byte
	PrivateLoggedResponseString string
	UserID                      string
	APIKey                      string
	serverStartupTime           time.Time
	request                     *http.Request
	config                      *Config
	selector                    *Selector
}

func (s *Session) Process(request *http.Request, selector *Selector, config *Config, serverStartupTime time.Time) bool {
	s.StartTime = time.Now()
	s.RemoteAddr = request.RemoteAddr
	s.UserID = request.URL.Query().Get("user_id")
	s.serverStartupTime = serverStartupTime
	s.APIKey = request.URL.Query().Get("api_key")

	s.selector = selector
	s.config = config
	s.request = request

	wasProcessedOk := s.process()

	s.EndTime = time.Now()

	return wasProcessedOk
}

func (s *Session) process() bool {
	if !s.validateAPIKey() {
		return false
	}

	if !s.validateUserID() {
		return false
	}

	if !s.isModified() {
		return true
	}

	return s.assignBucket()
}

func (s *Session) assignBucket() bool {
	selectedBuckets := s.selector.AssignBuckets(s.UserID)

	jsonBytes, err := json.Marshal(selectedBuckets)

	if err != nil {
		s.ResponseCode = http.StatusInternalServerError
		s.ResponseBody = []byte("Something went wrong. Oopsy!")
		s.PrivateLoggedResponseString = err.Error()
		return false
	}

	s.ResponseBody = jsonBytes
	s.ResponseCode = http.StatusOK

	return true
}

func (s *Session) validateAPIKey() bool {
	if !s.config.IsAPIKeyMandatory() {
		return true
	}

	if s.APIKey == "" || !s.config.IsValidAPIKey(s.APIKey) {
		s.ResponseCode = http.StatusForbidden
		s.ResponseBody = []byte("Valid API key required.")
		return false
	}

	return true
}

func (s *Session) validateUserID() bool {
	if s.UserID != "" {
		return true
	}

	s.ResponseCode = http.StatusBadRequest
	s.ResponseBody = []byte("user_id must be set")
	return false
}

func (s *Session) isModified() bool {
	if s.request.Header.Get("If-Modified-Since") == "" {
		return true
	}

	ifModifiedSince, err := time.Parse(time.RFC1123, s.request.Header.Get("If-Modified-Since"))

	if err != nil {
		s.PrivateLoggedResponseString = fmt.Sprintf("Cannot parse If-Modified-Since: %s", err)
		return true
	}

	if !s.serverStartupTime.After(ifModifiedSince) {
		s.ResponseCode = http.StatusNotModified
		return false
	}

	return true
}
