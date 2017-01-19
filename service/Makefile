.PHONY: build fmt run test

# Prepend our _vendor directory to the system GOPATH
# so that import path resolution will prioritize
# our third party snapshots.
GOPATH := ${PWD}
export GOPATH

default: build

build:
	go get github.com/prometheus/client_golang/prometheus ; \
	go build -v -o ${PWD}/bin/bucket_server ./src/bucket_server.go

fmt:
	go fmt ${PWD}/src/**/*.go

run: build
	./bin/bucket_server config.json.example

test:
	go test -v ${PWD}/src/**/*.go 2>&1 | grep -vE ' (Info|Fatal|Error): '

test_verbose:
	go test -v ${PWD}/src/**/*.go

