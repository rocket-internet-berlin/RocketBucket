package rocket_bucket

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

type Session struct {
	StartTime                   time.Time
	RemoteAddr                  string
	ResponseCode                int
	ResponseBody                []byte
	PrivateLoggedResponseString string
	APIKey                      string
	serverStartupTime           time.Time
	writer                      http.ResponseWriter
	request                     *http.Request
	config                      *Config
	metrics                     *Metrics
	bucketHandler               BucketRequestHandler
}

func (s *Session) Process(bucketHandler BucketRequestHandler, writer http.ResponseWriter, request *http.Request,
	config *Config, metrics *Metrics) {
	s.StartTime = time.Now()
	s.RemoteAddr = request.RemoteAddr
	s.APIKey = request.Header.Get("X-Api-Key")

	s.config = config
	s.request = request
	s.writer = writer
	s.bucketHandler = bucketHandler
	s.metrics = metrics

	s.writeResponse(s.process())
}

func (s *Session) writeResponse(wasProcessedOk bool) {
	s.writer.Header().Set("Content-Type", "application/json")

	logString := fmt.Sprintf("processing_time=%.6f, response_code=%d, response_body=`%s`, remote_address=`%s`, query_string=`%s`, x_api_key=`%s`, log_only_response=`%s`",
		time.Now().Sub(s.StartTime).Seconds(), s.ResponseCode, s.ResponseBody, s.RemoteAddr, s.request.URL, s.APIKey, s.PrivateLoggedResponseString)

	if wasProcessedOk {
		s.writer.Header().Set("Last-Modified", s.config.LastParsed.Format(time.RFC1123))

		if s.config.Server.CacheMaxAge > 0 {
			s.writer.Header().Set("Cache-Control", fmt.Sprintf("public, max-age=%d, must-revalidate", s.config.Server.CacheMaxAge))
		}

		Info(logString)
	} else {
		Error(logString)
	}

	s.writer.WriteHeader(s.ResponseCode)
	s.writer.Write(s.ResponseBody)
}

func (s *Session) process() bool {
	if !s.validateAPIKey() {
		s.ResponseCode = http.StatusForbidden
		s.ResponseBody, _ = json.Marshal(map[string]interface{}{"error": "Invalid value for X-Api-Key header."})
		s.metrics.incForHttpCode(http.StatusForbidden)
		return false
	} else if !s.isModified() {
		s.metrics.incForHttpCode(http.StatusNotModified)
		return true
	} else {
		responseBody, statusCode := s.bucketHandler.Handle()

		jsonBytes, err := json.Marshal(responseBody)

		if err != nil {
			s.ResponseCode = http.StatusInternalServerError
			s.ResponseBody = []byte("Something went wrong. Oopsy!")
			s.PrivateLoggedResponseString = err.Error()
			s.metrics.incForHttpCode(http.StatusInternalServerError)
			return false
		}

		s.ResponseBody = jsonBytes
		s.ResponseCode = statusCode
		s.metrics.incForHttpCode(statusCode)
		return true
	}
}

func (s *Session) validateAPIKey() bool {
	if !s.config.IsAPIKeyMandatory() {
		return true
	}
	return s.config.IsValidAPIKey(s.APIKey)
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
