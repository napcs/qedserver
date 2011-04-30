require File.join(File.dirname(__FILE__), '..', 'lib/server.rb')

require 'rubygems'
require 'sinatra'
require 'rack/test'
require 'rspec'
require 'database_cleaner'

# set test environment
set :environment, :test
set :run, false
set :raise_errors, true
set :logging, false

RSpec.configure do |config|

  config.before(:suite) do
    DatabaseCleaner.strategy = :transaction
    DatabaseCleaner.clean_with(:truncation)
  end

  config.before(:each) do
    DatabaseCleaner.start
  end

  config.after(:each) do
    DatabaseCleaner.clean
  end

end