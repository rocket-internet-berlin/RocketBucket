package rocket_bucket

import (
	"net/http"
)

type AssignBucketsRequestHandler struct {
	UserID   string
	Selector *Selector
	Metrics  *Metrics
}

func (p *AssignBucketsRequestHandler) Handle() (map[string]interface{}, int) {
	if p.UserID == "" {
		return map[string]interface{}{"error": "user_id must be set"}, http.StatusBadRequest
	}
	r := p.Selector.AssignBuckets(p.UserID)
	for idx := range r {
		p.Metrics.incForBucket(r[idx].Name, r[idx].Bucket.Name)
	}
	return map[string]interface{}{"experiments": r}, http.StatusOK
}
