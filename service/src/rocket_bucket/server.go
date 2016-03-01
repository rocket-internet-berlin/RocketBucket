package rocket_bucket

import (
	"fmt"
	"net/http"
	"time"
)

type Server struct {
	Config   *Config
	Selector *Selector
}

func (s *Server) HandleRequest(w http.ResponseWriter, r *http.Request) {
	// handle when url is "/" but request is "/something"
	defer func() {
		if err := recover(); err != nil {
			Fatal("%v", err)
		}
	}()

	session := Session{}
	wasProcessedOk := session.Process(r, s.Selector, s.Config)

	// set response headers
	w.Header().Set("Content-Type", "application/json")

	logString := fmt.Sprintf("processing_time=%.6f, response_code=%d, response_body=`%s`, remote_address=`%s`, user_id=`%s`, x_api_key=`%s`, log_only_response=`%s`",
		session.EndTime.Sub(session.StartTime).Seconds(), session.ResponseCode, session.ResponseBody, session.RemoteAddr, session.UserID, session.APIKey, session.PrivateLoggedResponseString)

	if wasProcessedOk {
		w.Header().Set("Last-Modified", s.Config.LastParsed.Format(time.RFC1123))

		if s.Config.Server.CacheMaxAge > 0 {
			w.Header().Set("Cache-Control",
				fmt.Sprintf("public, max-age=%d, must-revalidate", s.Config.Server.CacheMaxAge))
		}

		Info(logString)
	} else {
		Error(logString)
	}

	w.WriteHeader(session.ResponseCode)
	w.Write(session.ResponseBody)
}

func (s *Server) Run() {
	Info("listening: url=`%s`, port=`%d`", s.Config.Server.URL, s.Config.Server.Port)
	http.HandleFunc(s.Config.Server.URL, s.HandleRequest)
	http.ListenAndServe(fmt.Sprintf(":%d", s.Config.Server.Port), nil)
}
