# Display all products.
# Responds with HTML, JSON, or XML
# Renders products.html.haml
get "/categories/:id/products" do
  @category = Category.find(params[:id])
  @per_page = 10
  @products_count = @category.products.count
  @products = if params[:q]
    @category.products.with_keyword(params[:q])
  else
    @category.products.order("created_at desc")
  end
  
  page = params[:page] || 1
  
  @products = @products.paginate(page, @per_page) 
  
  respond_to do |format|
    format.html do
      haml :category_products
    end
    format.json do
      params[:callback] ? @products.to_json(:callback => params[:callback]) : @products.to_json
    end
    format.xml { @products.to_xml }  
    format.rss do
      builder do |xml|
         xml.instruct! :xml, :version => '1.0'
         xml.rss :version => "2.0" do
           xml.channel do
             xml.title "QED Products in #{@category.name}"
             xml.description "Products within #{@category.name}"
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

get "/categories/:category_id/products/:id" do
  @category = Category.find(params[:category_id])
  @product = @category.products.find(params[:id])
  respond_to do |format|
    format.html {haml :product }
    format.json  do 
      params[:callback] ? @product.to_json(:callback => params[:callback]) : @product.to_json
    end
    format.xml { @product.to_xml }  
  end
end