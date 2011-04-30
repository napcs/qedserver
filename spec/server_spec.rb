require File.dirname(__FILE__) + '/spec_helper'

describe "QED Server" do
  include Rack::Test::Methods

  def app
    @app ||= Sinatra::Application
  end

  it "responds to / with the index page" do
    get '/'
    last_response.should be_ok
  end
  
  describe "when working with products" do
    before(:each) do
     Product.create! :name => "iMac", :price => 1500.00, :description => "iMac description"
     @product = Product.create! :name => "Camera", :price => 50.00, :description => "Camera description"
     
    end
    it "displays the products page at /products" do
      get "/products"
      last_response.body.should include("Products")
    end
    
    it "displays the camera but not the iMac when we go to /products?q=camera" do
      get "/products", {:q => "camera"}
      last_response.body.should include("Camera")
      last_response.body.should_not include("iMac")
    end
  
    it "displays the camera but not the iMac when we go to /products.json?q=camera" do
      get "/products.json", {:q => "camera"}
      last_response.body.should include("Camera")
      last_response.body.should_not include("iMac")
    end
  
    it "shows the products as JSON at /products.json" do
      get "/products.json"
      last_response.body.should == Product.all.to_json
    end
    
    it "displays the products rss feed at /products.rss" do
      get "/products.rss"
      last_response.body.should include("QED Products")
    end
    
    describe "and creating a product" do
      
      describe "with HTML" do
        it "displays the success message" do
          post "/products", {:product => {:name => "Foo"}}
          follow_redirect!
          last_response.body.should include("Created Foo")
        end

        it "displays the error message when not created" do
          post "/products"
          last_response.body.should include("The product was not saved.")
        end
      end
      
      describe "with JSON" do
        it "displays the success message" do
          post "/products.json", {:product => {:name => "Foo"}}
          last_response.body.should == {:success => true, :message => "Created Foo"}.to_json
        end

        it "displays the error message when not created" do
          post "/products.json"
          last_response.body.should == {:success => false, :message => "The product was not saved."}.to_json

        end
      end
      
    end
    
    describe "When working with a product" do
 
     
      it "should show the product specified in the URL at /products/1" do
        get "/products/#{@product.id}"
        last_response.body.should include(@product.description)
      end
      
      it "shows the product as JSON at /products/1.json" do
        get "/products/#{@product.id}.json"
        last_response.body.should == @product.to_json
      end
      
      it "shows the product as XML at /products/1.xml" do
        get "/products/#{@product.id}.xml"
        last_response.body.should == @product.to_xml
      end
      
      it "deletes the product when we delete to /products/1" do
        delete "/products/#{@product.id}"
        follow_redirect!
        last_response.body.should include("deleted")
        Product.find_by_id(@product.id).should be_nil
      end
    end
  
  end
  
  describe "when working with the public folder" do
    it "shows the default page when we don't use the public folder and go to /index.html" do
      get "/index.html"
      last_response.body.should include("You're seeing the default page.")
    end

    it "shows our custom page when we do use the public folder and go to /index.html" do
      File.open("public/index.html", "w") do |f|
        f << "Hello World"
      end
      get "/index.html"
      last_response.body.should_not include("You're seeing the default page.")
      last_response.body.should include("Hello World")
      FileUtils.rm "public/index.html"
    end
  end
  
  

  
  
  
  
end