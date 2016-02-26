.PHONY: build fmt run test

# Prepend our _vendor directory to the system GOPATH
# so that import path resolution will prioritize
# our third party snapshots.
GOPATH := ${PWD}:${GOPATH}
export GOPATH

default: build

build:
	go build -v -o ./bin/bucket_server ./src/bucket_server.go

fmt:
	go fmt ./src/...

run: build
	./bin/bucket_server config.json.example

test:
	go test -v ./src/...

