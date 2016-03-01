# RocketBucket

Service to split audiences into buckets based on an arbitrary "user_id".

Source code for the service is all under "./service".

## Running the Rocket Bucket Service

I've included a handy Makefile and an example configuration. `cd service` and run `make run`.

If Go (compatible with 1.4.2) is installed, that would have worked and you can point your browser
at http://localhost:8080/split?user_id=someuserid to see the results.
If you change the value of the "user_id" parameter a few times, you should see different results.

## Configuration

Everything is set up in 1 JSON configuration file. It has two top level sections:

* `server` (object): this provides setup for the running bucket_server.
  * `port` (int): the port the bucket_server should listen on for HTTP connections.
  * `url` (string, optional, default '/'): the url/path the bucket_server should respond to.
  * `api_keys` (array of 32 byte strings, optional): if set, bucket_server will require the "X-Api-Key" header to be set in the HTTP request and only the keys listed in this section will be considered valid. All must be 32 characters long.
* `experiments` (array of objects): this contains an array of the following objects
  * _experiment_ (object): an experiment.
    * `name` (string): the name of the experiment. Once set and being used, it should not be changed as it, along with the user_id in the URL is used to bucket a user.
    * `enabled` (bool, optional, default *false*): whether this experiment is enabled. If disabled, the bucket_server will not list it when requested. Keeping disabled experiments may be useful for historic purposes.
    * `buckets` (array of objects): These are the buckets into which users will be arbitrarily (but consistently) placed. The contain pertinent details about the bucket size & other data.
      * `name` (string): the name of the bucket.
      * `percent` (int): what percentage of users will be put in this bucket. The total for all buckets must equal 100.
      * `data` (array of objects, optional): data that you'd like to pass to the client. As the client may be a statically typed language, this is a fixed format of an array of objects, with each object containing a string value for `name` and a string value for `value`. For example: `"data":[{"name":"firstname","value":"barry"},{"name":"lastname","value":"white"}]`.

A valid example config file is provided in service/config.json.example.

## Making Requests

todo

## Development Guide

`make test` and all sorts of other gems.

## TODO
... Client libraries, more documentation... Etc.

... Test coverage around various configuration scenarios.
