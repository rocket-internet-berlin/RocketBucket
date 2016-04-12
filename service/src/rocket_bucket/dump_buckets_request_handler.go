package rocket_bucket

import (
	"net/http"
)

type DumpBucketsRequestHandler struct {
	Config *Config
}

func (p *DumpBucketsRequestHandler) Handle() (map[string]interface{}, int) {
	return map[string]interface{}{"experiments": p.Config.Experiments}, http.StatusOK
}
