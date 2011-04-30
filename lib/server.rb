require 'bundler/setup'
require 'fileutils'
require 'sinatra'
require 'json'
require 'active_record'
require 'haml'
require 'sass'
require 'rack-flash'
require 'sinatra/respond_to'
require 'lib/configuration'
require 'lib/models/product'
require 'lib/seed_data'
require 'builder'
require 'maruku'

puts "Starting QEDServer #{QED_SERVER_VERSION}..."

# Sinatra setup
Sinatra::Application.register Sinatra::RespondTo
enable :sessions
use Rack::Flash
set :public, PUBLIC_PATH

set :markdown, :layout_engine => :haml, :layout => :layout


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

# Display all products.
# Responds with HTML, JSON, or XML
# Renders products.html.haml
get "/products" do
  
  @products = if params[:q]
    Product.with_keyword(params[:q])
  else
    Product.all
  end
  
  respond_to do |format|
    format.html do
      @product = Product.new  
      haml :products
    end
    format.json  { @products.to_json }
    format.xml { @products.to_xml }  
    format.rss do
      builder do |xml|
         xml.instruct! :xml, :version => '1.0'
         xml.rss :version => "2.0" do
           xml.channel do
             xml.title "QED Products"
             xml.description "Products"
             xml.link "http://localhost:#{Sinatra::Application.port}"

             @products.each do |product|
               xml.item do
                 xml.title product.name
                 xml.link "http://localhost:#{Sinatra::Application.port }/products/#{product.id}"
                 xml.description product.description
                 xml.pubDate Time.parse(product.created_at.to_s).rfc822()
                 xml.guid "http://localhost:#{Sinatra::Application.port }/products/#{product.id}"
               end
             end
           end
         end
       end
      
    end
  end
  
end

# Display a product as html, json, or xml.
# Renders product.html.haml
get "/products/:id" do
  @product = Product.find(params[:id])
  respond_to do |format|
    format.html {haml :product }
    format.json  { @product.to_json }
    format.xml { @product.to_xml }  
  end
end



# Create a new product.
# Respond with HTML or JSON.
post "/products" do
  @product = Product.new(params[:product])
  if @product.save
    message = "Created #{@product.name}"
    respond_to do |format|
      format.html do
        flash[:notice] = message
        redirect "/products"
      end
      format.json { {:success => true, :message => message}.to_json }
    end
  else
    message = "The product was not saved."
    respond_to do |format|
      format.html do
        @products = Product.all
        @message = message
        haml :products
      end
      format.json { {:success => false, :message => message}.to_json }
    end
  end
end

# Remove a product. 
# Respond with HTML or JSON
delete "/products/:id" do
  @product = Product.find(params[:id])
  @product.destroy
  message = "#{@product.name} was deleted."
  respond_to do |format|
    format.html do
      flash[:notice] = message
      redirect "/products"
    end
    format.json { {:success => true, :message => message}.to_json }
  end
end