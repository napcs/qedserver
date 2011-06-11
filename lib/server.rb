require 'bundler/setup'
require 'fileutils'
require 'sinatra'
require 'json'
require 'active_record'
require 'haml'
require 'sass'
require 'rack-flash'
require 'sinatra/respond_to'
require 'builder'
require 'maruku'

# QEDServer requires
require 'lib/models/product'
require 'lib/models/category'
require 'lib/models/product_category'
require 'lib/configuration'
require 'lib/seed_data'
require 'lib/helpers'
require 'lib/extensions/jsonp'

# Sinatra setup
Sinatra::Application.register Sinatra::RespondTo
enable :sessions
use Rack::Flash
set :public, PUBLIC_PATH
set :markdown, :layout_engine => :haml, :layout => :layout

puts "Server started. Press CTRL+C to stop."

# The home page with information about the server itself
# renders index.html.haml
get "/" do
  haml :index
end

# Tutorial page
get "/help" do
  markdown :help
end

# using :provides to get around the respond_to plugin
# per README for the gem.
# https://github.com/cehoffman/sinatra-respond_to
get '/style' do
  sass :style
end

# The path to the public page.
# Responds to /index.html and renders default.html.haml
# when no public/index.html page exists.
get "/index" do
  haml :default
end

get "/env" do
  @qed_env = ENV.keys.collect{|a| a + " : " + ENV[a]}.compact
  @qed_java_env = ENV_JAVA.keys.collect{|a| a + " : " + ENV_JAVA[a]}.compact
  erb :env
end

require 'lib/controllers/categories_products_controller'
require 'lib/controllers/products_controller'
require 'lib/controllers/categories_controller'
