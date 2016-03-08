#!/usr/bin/env ruby

# this runs user ids from a file against the service to ensure even bucketing
require "uri"
require "json"
require "optparse"
require "net/http"
require "benchmark"

options = {}
experiments = {}

option_parser = OptionParser.new do |opts|
  opts.banner = "Usage: test_service.rb --server-config=<server config file> --user-ids=<file of user ids> [--server-host=<hostname>]"

  opts.on("-c", "--server-config [filename]", "Server config file") do |v|
    options[:server_config_file] = v
  end
  
  opts.on("-u", "--user-ids [filename]", "File of user IDs") do |v|
    options[:user_id_file] = v
  end
  
  opts.on("-h", "--server-host [hostname]", "Optional host of the bucketing server (default: localhost)") do |v|
    options[:server_host] = v
  end
    
  opts.on("-h", "--help", "Help") do |v|
    puts opts
    exit
  end
end

option_parser.parse!

if !options[:server_config_file] || !options[:user_id_file]
  puts option_parser
  exit
end

server_config = JSON.parse(File.read(options[:server_config_file]))
user_ids = File.read(options[:user_id_file]).split("\n").reject {|user_id| user_id.length == 0}
user_id_count = user_ids.length
success_count = 0
benchmark_total = 0.0

server = URI.parse(options[:server_host] || 'localhost')
server_url = server_config["server"]["url"] || '/'
api_key = (server_config["server"]["api_keys"] || []).first

http = Net::HTTP.new(server.host, server.port)
http.use_ssl = server.is_a?(URI::HTTPS)

headers = {}
headers['X-Api-Key'] = api_key if api_key

longest_bucket_name = 0

(server_config["experiments"] || []).each do |experiment|
  experiments[experiment["name"]] ||= {expected: {}, got: {}, total: 0}
  
  experiment["buckets"].each do |bucket|
    experiments[experiment["name"]][:expected][bucket["name"]] = bucket["percent"]
  end
end

puts "Test parameters:"
puts "  Server config:   #{options[:server_config_file]}"
puts "  User ID file:    #{options[:user_id_file]}"
puts "  User IDs to run: #{user_id_count}"
puts "  Host:            #{server.host}"
puts "  Port:            #{server.port}"
puts "  URL:             #{server_url}"
puts "  X-Api-Key:       #{api_key || none}"
puts "  Start time:      #{Time.now}"
puts "Running..."

user_ids.each_with_index do |user_id, i|
  benchmark_total += Benchmark.measure {
    @response = http.request_get(server_url + "?#{URI.encode_www_form(user_id: user_id)}", headers)
  }.real
  
  puts "#{Time.now} (#{i+1}/#{user_id_count}) user_id=`#{user_id}` response_code=#{@response.code}"
  
  if @response.code.to_i == 200
    success_count += 1
    JSON.parse(@response.body)['experiments'].each do |experiment|
      experiment_name = experiment['name']
      bucket = experiment['bucket']
      longest_bucket_name = bucket["name"].length if bucket["name"].length > longest_bucket_name

      experiments[experiment_name][:total] += 1
      experiments[experiment_name][:got][bucket['name']] ||= 0
      experiments[experiment_name][:got][bucket['name']] += 1
    end
  else
    raise("Error: `#{@response.body}`")
  end
end

puts "\nReport:"
puts "  Total HTTP time:   #{benchmark_total}"
puts "  Average HTTP time: #{(benchmark_total.to_f/user_id_count).round(3)}"
puts "  Records bucketed:  #{success_count}/#{user_id_count}"
puts "  Experiments:"

experiments.each do |name, buckets|
  puts "    `#{name}`"
  
  buckets[:expected].each do |name, percent|
    got_percent = "%0.2f" % ((buckets[:got][name].to_f / buckets[:total]) * 100)
    diff = (got_percent.to_f - buckets[:expected][name]).round(2)
    aligned_name = "`#{name}`".ljust(longest_bucket_name+3)
    puts "      #{aligned_name} got:#{got_percent} (#{buckets[:got][name]}) expected:#{buckets[:expected][name]} diff:#{(diff >= 0 ? '+' : '')+diff.to_s}"
  end
end