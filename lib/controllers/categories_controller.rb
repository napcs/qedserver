###### categories

# Display all categories.
# Responds with HTML, JSON, or XML
# Renders categories.html.haml
get "/categories" do
  @per_page = 10
  @categories_count = Category.count
  @categories = if params[:q]
    Category.with_keyword(params[:q])
  else
    Category.order("created_at desc")
  end
  
  page = params[:page] || 1
  
  @categories = @categories.paginate(page, @per_page) 
  
  respond_to do |format|
    format.html do
      @category = Category.new  
      haml :categories
    end
    format.json do
      params[:callback] ? @categories.to_json(:callback => params[:callback]) : @categories.to_json
    end
    format.xml { @categories.to_xml }  
    format.rss do
      builder do |xml|
         xml.instruct! :xml, :version => '1.0'
         xml.rss :version => "2.0" do
           xml.channel do
             xml.title "QED Categories"
             xml.description "Categories"
             xml.link "http://localhost:#{Sinatra::Application.port}/categories.rss"

             @categories.each do |category|
               xml.item do
                 xml.title category.name
                 xml.link "http://localhost:#{Sinatra::Application.port }/categories/#{category.id}"
                 xml.pubDate Time.parse(category.created_at.to_s).rfc822()
                 xml.guid "http://localhost:#{Sinatra::Application.port }/categories/#{category.id}"
               end
             end
           end
         end
       end
      
    end
  end
  
end

#edit a category
get "/categories/:id/edit" do
  @category = Category.find(params[:id])
  respond_to do |format|
    format.html {haml :category_edit }
    format.json  { @category.to_json }
    format.xml { @category.to_xml }  
  end
end

# Update a category.
# Respond with HTML or JSON.
put "/categories/:id/update" do
  data = params[:category] || JSON.parse(request.body.read)   
  @category = Category.find(params[:id])
  @category.update_attributes(data)
  if @category.save
    message = "Updated #{@category.name}"
    respond_to do |format|
      format.html do
        flash[:notice] = message
        redirect "/categories"
      end
      format.json { {:success => true, :message => message}.to_json }
    end
  else
    message = "The category was not saved."
    respond_to do |format|
      format.html do
        @message = message
        haml :category_edit
      end
      format.json do
        status "500"
        {:success => false, :message => message, :errors => @category.errors}.to_json
      end
    end
  end
end

# Display a category as html, json, or xml.
# Renders category.html.haml
get "/categories/:id" do
  @category = Category.find(params[:id])
  respond_to do |format|
    format.html {haml :category }
    format.json do
      params[:callback] ? @category.to_json(:callback => params[:callback]) : @category.to_json
    end
    format.xml { @category.to_xml }  
  end
end

get "/categories/:id/products" do
  
end

# Create a new product.
# Respond with HTML or JSON.
post "/categories" do
  puts params[:category]
  
  data = params[:category] || JSON.parse(request.body.read) rescue {}
  @category = Category.new(data)
  if @category.save
    message = "Created #{@category.name}"
    respond_to do |format|
      format.html do
        flash[:notice] = message
        redirect "/categories"
      end
      format.json { {:success => true, :message => message}.to_json }
    end
  else
    message = "The category was not saved."
    respond_to do |format|
      format.html do
        @message = message
        @per_page = 10
        @categories_count = Product.count
        @categories = Category.paginate(1,10).order("created_at desc")
        
        haml :categories
      end
      format.json do
        status "500"
        {:success => false, :message => message, :errors => @category.errors}.to_json
      end
    end
  end
end

# Remove a category. 
# Respond with HTML or JSON
delete "/categories/:id" do
  @category = Category.find(params[:id])
  @category.destroy
  message = "#{@category.name} was deleted."
  respond_to do |format|
    format.html do
      flash[:notice] = message
      redirect "/categories"
    end
    format.json { {:success => true, :message => message}.to_json }
  end
end