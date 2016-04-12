package rocket_bucket

import (
	"fmt"
	"net/http"
)

type Server struct {
	Config   *Config
	Selector *Selector
}

func (s *Server) HandleBucketDump(w http.ResponseWriter, r *http.Request) {
	s.handleSession(&DumpBucketsRequestHandler{Config: s.Config}, w, r)
}

func (s *Server) HandleBucketAssignment(w http.ResponseWriter, r *http.Request) {
	s.handleSession(&AssignBucketsRequestHandler{UserID: r.URL.Query().Get("user_id"), Selector: s.Selector}, w, r)
}

func (s *Server) handleSession(handler BucketRequestHandler, w http.ResponseWriter, r *http.Request) {
	session := Session{}
	session.Process(handler, w, r, s.Config)
}

func (s *Server) Run() {
	Info("listening: assignment_url=`%s`, dump_url=`%s`, port=`%d`",
		s.Config.Server.URL, s.Config.Server.BucketDumpURL, s.Config.Server.Port)

	http.HandleFunc(s.Config.Server.URL, s.HandleBucketAssignment)
	http.HandleFunc(s.Config.Server.BucketDumpURL, s.HandleBucketDump)

	http.ListenAndServe(fmt.Sprintf(":%d", s.Config.Server.Port), nil)
}
