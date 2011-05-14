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
      @product = Product.create! :name => "Camera", :price => 50.00, :description => "Camera description"
      
     Product.create! :name => "iMac", :price => 1500.00, :description => "iMac description"
     
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
  
    it "displays only the first 10 records" do
      Product.delete_all
      Product.create! :name => "Camera", :price => 50.00, :description => "Camera description"
      Product.create! :name => "iMac", :price => 1500.00, :description => "iMac description"
      Product.create :name => "iMac 27 inch", :price => 1799.00, :description => "Description of the iMac 27 inch"
      Product.create :name => "iPad 2 64GB Wifi+3G", :price => 829.99, :description => "Description of the iPad 2"
      Product.create :name => "iPod Touch 64GB", :price => 829.99, :description => "Description of the iPod Touch"
      Product.create :name => "iPod Shuffle", :price => 49.99, :description => "Description of the iPod Shuffle"
      Product.create :name => "Apple TV", :price => 99.99, :description => "Description of the Apple TV"
      Product.create :name => "iPad 2 Smart Cover", :price => 29.99, :description => "Description of iPad Smart Cover"
      Product.create :name => "iPad 2 Smart Cover - Leather", :price => 59.99, :description => "Description of iPad Smart Cover - Leather"
      Product.create :name => "Amazon Kindle Wi-Fi Only", :price => 139.99, :description => "Description of Kindle Wi-Fi Version"
      Product.create :name => "Amazon Kindle 3G", :price => 139.99, :description => "Description of Kindle 3G Version"
      get "/products.json"
      last_response.body.should_not include("Camera")
      last_response.body.should include("Amazon Kindle 3G")
    end
    
    it "displays the last 2 records on page 2" do
      Product.delete_all
      Product.create! :name => "Camera", :price => 50.00, :description => "Camera description"
      Product.create! :name => "iMac", :price => 1500.00, :description => "iMac description"
      Product.create :name => "iMac 27 inch", :price => 1799.00, :description => "Description of the iMac 27 inch"
      Product.create :name => "iPad 2 64GB Wifi+3G", :price => 829.99, :description => "Description of the iPad 2"
      Product.create :name => "iPod Touch 64GB", :price => 829.99, :description => "Description of the iPod Touch"
      Product.create :name => "iPod Shuffle", :price => 49.99, :description => "Description of the iPod Shuffle"
      Product.create :name => "Apple TV", :price => 99.99, :description => "Description of the Apple TV"
      Product.create :name => "iPad 2 Smart Cover", :price => 29.99, :description => "Description of iPad Smart Cover"
      Product.create :name => "iPad 2 Smart Cover - Leather", :price => 59.99, :description => "Description of iPad Smart Cover - Leather"
      Product.create :name => "Amazon Kindle Wi-Fi Only", :price => 139.99, :description => "Description of Kindle Wi-Fi Version"
      Product.create :name => "Amazon Kindle 3G", :price => 139.99, :description => "Description of Kindle 3G Version"
      get "/products.json?page=2"
      last_response.body.should_not include("Amazon Kindle Wi-Fi Only")
      last_response.body.should include("Camera")
    end
    
  
    it "shows the products as JSON at /products.json" do
      get "/products.json"
      last_response.body.should == Product.order("created_at desc").to_json
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
    
    describe "when editing a product" do
      it "displays the edit page" do
        get "/products/#{@product.id}/edit"
        last_response.body.should include("Edit Product")
      end
      
      describe "with HTML" do
        it "displays the success message" do
          put "/products/#{@product.id}/update", {:product => {:name => "Bar"}}
          follow_redirect!
          last_response.body.should include("Updated Bar")
        end

        it "displays the error message when not created" do
          put "/products/#{@product.id}/update", {:product => {:name => ""}}  
          last_response.body.should include("The product was not saved.")
        end
      end
      
      describe "with JSON" do
        it "displays the success message" do
          put "/products/#{@product.id}/update.json", {:product => {:name => "Bar"}}
          last_response.body.should == {:success => true, :message => "Updated Bar"}.to_json
        end

        it "displays the error message when not created" do
          put "/products/#{@product.id}/update.json", {:product => {:name => ""}} 
          last_response.body.should == {:success => false, :message => "The product was not saved."}.to_json

        end
      end

    end
  
  end
  
  describe "When working with categories" do
    
    before(:each) do
      @category = Category.create :name => "Testing Stuff"
    end
    
    it "displays the categories page at /categories" do
      get "/categories"
      last_response.body.should include("Categories")
    end    
    
    it "displays the categories page as JSON" do
      get "/categories.json"
      last_response.body.should == Category.order("created_at desc").to_json
    end
    
    it "displays the categories rss feed at /categories.rss" do
      get "/categories.rss"
      last_response.body.should include("QED Categories")
    end
    
    it "displays the Foo category but not the Bar category when we go to /categories?q=Foo" do
      
      Category.create! :name => "Foo"
      Category.create! :name => "Bar"
      get "/categories", {:q => "Foo"}
      last_response.body.should include("Foo")
      last_response.body.should_not include("Bar")
    end
    
    it "displays the Foo category but not the Bar category when we go to /categories.json?q=Foo" do
      Category.create! :name => "Foo"
      Category.create! :name => "Bar"
      get "/categories.json", {:q => "Foo"}
      last_response.body.should include("Foo")
      last_response.body.should_not include("Bar")
    end
  
    it "displays only the first 10 records" do
      Category.delete_all
      Category.create! :name => "One"
      Category.create! :name => "Two"
      Category.create! :name => "Three"
      Category.create! :name => "Four"
      Category.create! :name => "Five"
      Category.create! :name => "Six"
      Category.create! :name => "Seven"
      Category.create! :name => "Eight"
      Category.create! :name => "Nine"
      Category.create! :name => "Ten"
      Category.create! :name => "Eleven"
      Category.create! :name => "Twelve"
      get "/categories.json"
      
      last_response.body.should_not include("One")
      last_response.body.should_not include("Two")
      last_response.body.should include("Eleven")
      last_response.body.should include("Twelve")
      

      
    end
    
    it "displays the last 2 records on page 2" do
      Category.delete_all
      Category.create! :name => "One"
      Category.create! :name => "Two"
      Category.create! :name => "Three"
      Category.create! :name => "Four"
      Category.create! :name => "Five"
      Category.create! :name => "Six"
      Category.create! :name => "Seven"
      Category.create! :name => "Eight"
      Category.create! :name => "Nine"
      Category.create! :name => "Ten"
      Category.create! :name => "Eleven"
      Category.create! :name => "Twelve"
      get "/categories.json?page=2"
      last_response.body.should include("One")
      last_response.body.should include("Two")
      last_response.body.should_not include("Eleven")
      last_response.body.should_not include("Twelve")
    end
    
    
    
    describe "and creating a category" do
      
      describe "with HTML" do
        it "displays the success message" do
          post "/categories", {:category => {:name => "Foo"}}
          follow_redirect!
          last_response.body.should include("Created Foo")
        end

        it "displays the error message when not created" do
          post "/categories"
          last_response.body.should include("The category was not saved.")
        end
      end
      
      describe "with JSON" do
        it "displays the success message" do
          post "/categories.json", {:category => {:name => "Foo"}}
          last_response.body.should == {:success => true, :message => "Created Foo"}.to_json
        end

        it "displays the error message when not created" do
          post "/categories.json"
          last_response.body.should == {:success => false, :message => "The category was not saved."}.to_json

        end
      end
      
    end
    
    describe "When working with a category" do
      
     
      it "should show the category specified in the URL at /categories/1" do
        get "/categories/#{@category.id}"
        last_response.body.should include(@category.name)
      end
      
      it "shows the category as JSON at /categories/1.json" do
        get "/categories/#{@category.id}.json"
        last_response.body.should == @category.to_json
      end
      
      it "shows the category as XML at /categories/1.xml" do
        get "/categories/#{@category.id}.xml"
        last_response.body.should == @category.to_xml
      end
      
      it "deletes the categoriy when we delete to /categories/1" do
        delete "/categories/#{@category.id}"
        follow_redirect!
        last_response.body.should include("deleted")
        Category.find_by_id(@category.id).should be_nil
      end
    end
    
    describe "when editing a category" do
      it "displays the edit page" do
        get "/categories/#{@category.id}/edit"
        last_response.body.should include("Edit Category")
      end
      
      describe "with HTML" do
        it "displays the success message" do
          put "/categories/#{@category.id}/update", {:category => {:name => "Bar"}}
          follow_redirect!
          last_response.body.should include("Updated Bar")
        end

        it "displays the error message when not created" do
          put "/categories/#{@category.id}/update", {:category => {:name => ""}}  
          last_response.body.should include("The category was not saved.")
        end
      end
      
      describe "with JSON" do
        it "displays the success message" do
          put "/categories/#{@category.id}/update.json", {:category => {:name => "Bar"}}
          last_response.body.should == {:success => true, :message => "Updated Bar"}.to_json
        end

        it "displays the error message when not created" do
          put "/categories/#{@category.id}/update.json", {:category => {:name => ""}} 
          last_response.body.should == {:success => false, :message => "The category was not saved."}.to_json

        end
      end

    end
    
  end
  
  describe "When working with products in a category" do
    before(:each) do
      Category.delete_all
      @category = Category.create :name => "Test!"
    end
      
    it "displays the products page at /categories/1/products" do
      @product = @category.products.create! :name => "Camera", :price => 50.00, :description => "Camera description"
      @category.products.create! :name => "iMac", :price => 1500.00, :description => "iMac description"
      get "/categories/#{@category.id}/products"
      last_response.body.should include("Products")
    end
    
    it "displays the products for categories rss feed at /categories/1/products.rss" do
      get "/categories/#{@category.id}/products.rss"
      last_response.body.should include("QED Products in #{@category.name}")
    end
    
    it "displays the camera but not the iMac when we go to /categories/1/products?q=camera" do
      @product = @category.products.create! :name => "Camera", :price => 50.00, :description => "Camera description"
      @category.products.create! :name => "iMac", :price => 1500.00, :description => "iMac description"
      get "/categories/#{@category.id}/products", {:q => "camera"}
      last_response.body.should include("Camera")
      last_response.body.should_not include("iMac")
    end
    
    it "displays the camera but not the iMac when we go to /categories/1/products.json?q=camera" do
      @product = @category.products.create! :name => "Camera", :price => 50.00, :description => "Camera description"
      @category.products.create! :name => "iMac", :price => 1500.00, :description => "iMac description"
      
      get "/categories/#{@category.id}/products.json", {:q => "camera"}
      last_response.body.should include("Camera")
      last_response.body.should_not include("iMac")
    end
  
    it "displays only the first 10 records at /categories/1/products" do
      Product.delete_all
      @category.products.create! :name => "Camera", :price => 50.00, :description => "Camera description"
      @category.products.create! :name => "iMac", :price => 1500.00, :description => "iMac description"
      @category.products.create! :name => "iMac 27 inch", :price => 1799.00, :description => "Description of the iMac 27 inch"
      @category.products.create! :name => "iPad 2 64GB Wifi+3G", :price => 829.99, :description => "Description of the iPad 2"
      @category.products.create! :name => "iPod Touch 64GB", :price => 829.99, :description => "Description of the iPod Touch"
      @category.products.create! :name => "iPod Shuffle", :price => 49.99, :description => "Description of the iPod Shuffle"
      @category.products.create! :name => "Apple TV", :price => 99.99, :description => "Description of the Apple TV"
      @category.products.create! :name => "iPad 2 Smart Cover", :price => 29.99, :description => "Description of iPad Smart Cover"
      @category.products.create! :name => "iPad 2 Smart Cover - Leather", :price => 59.99, :description => "Description of iPad Smart Cover - Leather"
      @category.products.create! :name => "Amazon Kindle Wi-Fi Only", :price => 139.99, :description => "Description of Kindle Wi-Fi Version"
      @category.products.create! :name => "Amazon Kindle 3G", :price => 139.99, :description => "Description of Kindle 3G Version"
      get "/categories/#{@category.id}/products.json"
      last_response.body.should_not include("Camera")
      last_response.body.should include("Amazon Kindle 3G")
    end
    
    it "displays the last 2 records on page 2 of /categories/1/products?page=2" do
      Product.delete_all
      Category.delete_all
      @category = Category.create :name => "Test!"
      @category.products.create! :name => "Camera", :price => 50.00, :description => "Camera description"
      @category.products.create! :name => "iMac", :price => 1500.00, :description => "iMac description"
      @category.products.create! :name => "iMac 27 inch", :price => 1799.00, :description => "Description of the iMac 27 inch"
      @category.products.create! :name => "iPad 2 64GB Wifi+3G", :price => 829.99, :description => "Description of the iPad 2"
      @category.products.create! :name => "iPod Touch 64GB", :price => 829.99, :description => "Description of the iPod Touch"
      @category.products.create! :name => "iPod Shuffle", :price => 49.99, :description => "Description of the iPod Shuffle"
      @category.products.create! :name => "Apple TV", :price => 99.99, :description => "Description of the Apple TV"
      @category.products.create! :name => "iPad 2 Smart Cover", :price => 29.99, :description => "Description of iPad Smart Cover"
      @category.products.create! :name => "iPad 2 Smart Cover - Leather", :price => 59.99, :description => "Description of iPad Smart Cover - Leather"
      @category.products.create! :name => "Amazon Kindle Wi-Fi Only", :price => 139.99, :description => "Description of Kindle Wi-Fi Version"
      @category.products.create! :name => "Amazon Kindle 3G", :price => 139.99, :description => "Description of Kindle 3G Version"
      get "/categories/#{@category.id}/products.json?page=2"
      last_response.body.should_not include("Amazon Kindle Wi-Fi Only")
      last_response.body.should include("Camera")
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