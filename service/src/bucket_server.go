package main

import (
	"fmt"
	"io/ioutil"
	"os"
	"os/signal"
	"rocket_bucket"
	"syscall"
)

var (
	config *rocket_bucket.Config
	server rocket_bucket.Server
)

func loadConfig() {
	if len(os.Args) == 1 {
		fmt.Println("Usage: bucket_server configuration_file_path")
		os.Exit(1)
	}

	rocket_bucket.Info("loading config file %s", os.Args[1])

	configData, err := ioutil.ReadFile(os.Args[1])

	if err != nil {
		rocket_bucket.Fatal("%v", err)
	}

	// prevent invalid config loading
	defer func() {
		if r := recover(); r != nil {
			rocket_bucket.Error("%v", r)
		}
	}()

	config.Parse(configData)
}

func setupSIGHUPHandler() {
	c := make(chan os.Signal, 1)
	signal.Notify(c, syscall.SIGHUP)

	go func() {
		for range c {
			rocket_bucket.Info("received SIGHUP")
			loadConfig()
		}
	}()
}

func main() {
	config = &rocket_bucket.Config{}
	selector := &rocket_bucket.Selector{Experiments: &config.Experiments}

	loadConfig()
	setupSIGHUPHandler()

	metrics := rocket_bucket.GetMetrics(config.Experiments)
	rocket_bucket.RegisterMetrics(metrics)

	server = rocket_bucket.Server{Config: config, Selector: selector, Metrics: metrics}
	server.Run()
}
