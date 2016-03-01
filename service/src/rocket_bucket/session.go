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

func (s *Session) Process(request *http.Request, selector *Selector, config *Config) bool {
	s.StartTime = time.Now()
	s.RemoteAddr = request.RemoteAddr
	s.UserID = request.URL.Query().Get("user_id")
	s.APIKey = request.Header.Get("X-Api-Key")

	s.selector = selector
	s.config = config
	s.request = request

	wasProcessedOk := s.process()

	s.EndTime = time.Now()

	return wasProcessedOk
}

func (s *Session) process() bool {
	if s.validateAPIKey() && s.validateRequestPath() && s.validateUserID() {
		if !s.isModified() || s.assignBucket() {
			return true
		}
	}

	s.ResponseBody, _ = json.Marshal(map[string]interface{}{"error": string(s.ResponseBody)})

	return false
}

func (s *Session) assignBucket() bool {
	selectedExperiments := s.selector.AssignBuckets(s.UserID)

	jsonBytes, err := json.Marshal(map[string]interface{}{"experiments": selectedExperiments})

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

func (s *Session) validateRequestPath() bool {
	if !s.config.DoesURLMatch(s.request.URL.Path) {
		s.ResponseCode = http.StatusNotFound
		s.ResponseBody = []byte(fmt.Sprintf("Unknown path %s", s.request.URL.Path))
		return false
	}

	return true
}

func (s *Session) validateAPIKey() bool {
	if !s.config.IsAPIKeyMandatory() {
		return true
	}

	if s.APIKey == "" || !s.config.IsValidAPIKey(s.APIKey) {
		s.ResponseCode = http.StatusForbidden
		s.ResponseBody = []byte("Invalid value for X-Api-Key header.")
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

	if !s.config.LastParsed.After(ifModifiedSince) {
		s.ResponseCode = http.StatusNotModified
		return false
	}

	return true
}
