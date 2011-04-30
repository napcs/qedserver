require 'rubygems'
require 'lib/server'

  set :run,         false
  set :environment, :production
  
  run Sinatra::Application



