# A Product represents a product in our database.
# It has a name field and that's it. 
# The name field must be unique
class Product < ActiveRecord::Base
  validates_presence_of :name
  validates_uniqueness_of :name
  scope :with_keyword, lambda{|word|
    term = "%#{word}%"
    where(["name like ? or description like ?", term, term]) 
  }
  
  scope :paginate, 
    lambda{ |page, per_page| 
      limit(per_page.to_i).offset((page.to_i-1) * per_page.to_i)
    }
  
  
end