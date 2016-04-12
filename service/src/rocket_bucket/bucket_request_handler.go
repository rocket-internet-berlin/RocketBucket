package rocket_bucket

type BucketRequestHandler interface {
	Handle() (map[string]interface{}, int)
}
