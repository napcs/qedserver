helpers do
  
  def error_messages_for(object)
    if object.errors.any?
      output = "<div id='error_messages'><ul>"
      object.errors.full_messages.each do |m|
        output << "<li>#{m}</li>"
      end
      output << "</ul></div>"  
    end
  end
  
  def pagination_links(count, per_page, url)
    pages = (count / per_page)
    pages+= 1 if count % per_page != 0
    
    output = "<div class='pagination_links'>"
    pages.times do |page|
      css_class = "class='current'" if (page + 1) == params[:page].to_i
      output << "<a #{css_class} href='#{url}?page=#{page + 1}'>#{page + 1}</a>"
    end
    
    output << "</div>"
  end
end