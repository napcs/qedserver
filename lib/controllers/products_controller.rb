# Display all products.
# Responds with HTML, JSON, or XML
# Renders products.html.haml
get "/products" do
  @per_page = 10
  @products_count = Product.count
  @products = if params[:q]
    Product.with_keyword(params[:q])
  else
    Product.order("created_at desc")
  end
  
  page = params[:page] || 1
  
  @products = @products.paginate(page, @per_page) 
  
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

#edit a product
get "/products/:id/edit" do
  @product = Product.find(params[:id])
  respond_to do |format|
    format.html {haml :product_edit }
    format.json  { @product.to_json }
    format.xml { @product.to_xml }  
  end
end

# Update a product.
# Respond with HTML or JSON.
put "/products/:id/update" do
  @product = Product.find(params[:id])
  @product.update_attributes(params[:product])
  if @product.save
    message = "Updated #{@product.name}"
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
        @message = message
        haml :product_edit
      end
      format.json { {:success => false, :message => message}.to_json }
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
        @per_page = 10
        @products_count = Product.count
        @products = Product.paginate(1,10).order("created_at desc")
        
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