# RocketBucket

Service and clients to split audiences into buckets based on an arbitrary "user_id".

Source code for the service is all under "./service".

## Running the Rocket Bucket Service

The service requires Go 1.4 or 1.6.

I've included a Makefile and an example configuration (config.json.example). To use this and get everything running quickly, `cd service` and execute `make run`.

If you wish to run the service by hand (e.g. with your own config file), run `make build` and execute `./bin/bucket_server <<path to json config file>>`.

To test the service, run `curl -H "X-Api-Key: 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ" localhost:8080/split?user_id=123` to see the results. If you change the value of the "user_id" parameter a few times, you should see different results.

## Service Configuration

Everything is set up in 1 JSON configuration file. It has the following structure:

* `server` (object): this provides setup for the running bucket_server.
  * `port` (int): the port the bucket_server should listen on for HTTP connections.
  * `url` (string, optional, default '/'): the url/path the bucket_server should respond to.
  * `cache_max_age` (int, optional, default 0): the number of seconds the bucket_server recommends to clients for caching responses. It will appear in the Cache-Control HTTP header.
  * `api_keys` (array of 32 byte strings, optional): if set, bucket_server will require the "X-Api-Key" header to be set in the HTTP request and only the keys listed in this section will be considered valid. All must be 32 characters long.
* `experiments` (array of objects): this contains an array of the following objects
  * _experiment_ (object): an experiment.
    * `name` (string): the name of the experiment. Once set and being used, it should not be changed as it, along with the user_id in the URL, is used to bucket a user.
    * `enabled` (bool, optional, default *false*): whether this experiment is enabled. If disabled, the bucket_server will not list it when requested. Keeping disabled experiments may be useful for historic purposes.
    * `buckets` (array of objects): These are the buckets into which users will be arbitrarily (but consistently) placed. The contain pertinent details about the bucket size & other data.
      * `name` (string): the name of the bucket.
      * `percent` (int): what percentage of users will be put in this bucket. The total for all buckets must equal 100.
      * `data` (array of objects, optional): data that you'd like to pass to the client. As the client may be a statically typed language, this is a fixed format of an array of objects, with each object containing a string value for `name` and a string value for `value`. For example: `"data":[{"name":"firstname","value":"barry"},{"name":"lastname","value":"white"}]`.

A valid example config file is provided in service/config.json.example.

## Making Service Requests

The service has two functions: to assign buckets/variants for a given user ID and to list the available experiments and buckets.

Examples below assumes the following configuration:
 * An X-Api-Key header is required.
 * The base URL is "/split".
 * The service is running on localhost port 8080.
 * You're familiar with [curl](https://curl.haxx.se/docs/manpage.html).
 
Example responses have been formatted for readability.

### Bucket Assignment

This call can be made for each user. It takes a "user_id" CGI parameter (to dictate buckets for the user). It responds with a JSON string containing each experiment and the buckets (with associated data) to which the user has been assigned.

#### Example request:
`curl -H "X-Api-Key: 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ" localhost:8080/split?user_id=123`

#### Example response:
```json
{
   "experiments":[
      {
         "name":"checkout button colors",
         "bucket":{
            "name":"control group (green button)",
            "data":[
               {
                  "name":"color",
                  "value":"#00FF00"
               }
            ]
         }
      },
      {
         "name":"search call to action",
         "bucket":{
            "name":"all caps craziness",
            "data":[
               {
                  "name":"call to action string",
                  "value":"CLICK HERE!!!"
               }
            ]
         }
      }
   ]
}
```

### Dumping All Buckets

It can be helpful to know what experiments are currently running in order to provide manual override functionality or to help debug, for example. This can be achieved by adding "/all" to the base URL. It is not per-user, it is global.

#### Example request:
`curl -H "X-Api-Key: 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ" localhost:8080/split/all`

#### Example response:
```json
{
   "experiments":[
      {
         "name":"checkout button colors",
         "enabled":true,
         "buckets":[
            {
               "name":"variant (red button)",
               "percent":25,
               "data":[
                  {
                     "name":"color",
                     "value":"#FF0000"
                  }
               ]
            },
            {
               "name":"variant (blue button)",
               "percent":25,
               "data":[
                  {
                     "name":"color",
                     "value":"#0000FF"
                  }
               ]
            },
            {
               "name":"control group (green button)",
               "percent":50,
               "data":[
                  {
                     "name":"color",
                     "value":"#00FF00"
                  }
               ]
            }
         ]
      },
      {
         "name":"search call to action",
         "enabled":true,
         "buckets":[
            {
               "name":"control group (current)",
               "percent":50
            },
            {
               "name":"all caps craziness",
               "percent":50,
               "data":[
                  {
                     "name":"call to action string",
                     "value":"CLICK HERE!!!"
                  }
               ]
            }
         ]
      }
   ]
}
```

Note that empty fields (e.g. undefined "data") and disabled (enabled:false) experiments will be omitted.

## How The Service Works

The bucket_server is stateless. It stores no data nor does it access any whilst running. It uses an algorithm to calculate it's results based on the user_id and names of available experiments. The result of this is returned as JSON containing all the experiments and buckets a user is eligible for. This can then be cached by the client.

During the bucket_server startup, it parses the experiment data from the config file and calculates a hash of the experiment name, resulting in a 32 bit number. It orders the buckets by smallest to largest and accumulates the percentages. All this is kept in memory and used at the time of an HTTP request.

When an HTTP request is made to the bucket_server, a user_id parameter must be passed from the client. Like each experiment name, a hash is taken of the user_id and the experiment name hash and user_id hash are added together (`hash(user_id) + hash(experiment name)`). Each experiment and each of it's buckets are then looped through and user/experiment name combination are assigned to a bucket.

It's very important to keep in mind that the user_id and experiment name are used in combination. If you change the user_id OR the experiment name, the results from the bucket_server may change. This is intentional. While using the user_id for bucketing is obvious, the reason for using the experiment name is more subtle. If you imagine the follwing experiment setup:

1. An experiment for checkout button color:
  1. 50% of users see a RED button.
  1. 50% of users see a BLUE button.
1. An experiment for checkout button text:
  1. 50% of users see "Checkout now!"
  1. 50% of users see "Buy!"

If we only use the user_id, users will only ever see a RED checkout button with "Checkout now!" or a BLUE checkout button with "Buy!". If more people press the checkout button, we will be unsure whether the color or the text was more or less important. We therefore include the experiment name in order to decrease the overlap in such cases. It's also worth noting that, without using the experiment name to assist in bucketing, some users will always be experimented on.

The guts of this algorithm can be found in service/src/rocket_bucket/selector.go in the AssignBuckets method.

## Development Guide

`make test` and all sorts of other gems.

## TODO
... Client libraries, more documentation... Etc.

... Test coverage around various configuration scenarios.
