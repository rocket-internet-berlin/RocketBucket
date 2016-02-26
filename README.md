# RocketBucket

Service to split audiences into buckets based on an arbitrary "user_id".

Source code for the service is all under "./service"

## Running the Rocket Bucket Service

I've included a handy Makefile and an example configuration. `cd service` and run `make run`.

If Go (compatible with 1.4.2) is installed, that would have worked and you can point your browser
at http://localhost:8080/split?user_id=someuserid to see the results.
If you change the value of the "user_id" parameter a few times, you should see different results.

## Configuration

## Development Guide

`make test` and all sorts of other gems.

## TODO
... Client libraries, more documentation... Etc.

... Test coverage around various configuration scenarios.
