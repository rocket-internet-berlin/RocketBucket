package rocket_bucket

import (
	"net/http"
)

type AssignBucketsRequestHandler struct {
	UserID   string
	Selector *Selector
}

func (p *AssignBucketsRequestHandler) Handle() (map[string]interface{}, int) {
	if p.UserID == "" {
		return map[string]interface{}{"error": "user_id must be set"}, http.StatusBadRequest
	}

	return map[string]interface{}{"experiments": p.Selector.AssignBuckets(p.UserID)}, http.StatusOK
}
