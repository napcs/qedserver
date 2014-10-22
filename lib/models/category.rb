class Category < ActiveRecord::Base
  has_many :product_categories
  has_many :products, :through => :product_categories
  
  validates_uniqueness_of :name
  validates_presence_of :name
  
  scope :with_keyword, lambda{|word|
    term = "%#{word}%"
    where(["name like ?", term]) 
  }
  
  scope :paginate, 
    lambda{ |page, per_page| 
      limit(per_page.to_i).offset((page.to_i-1) * per_page.to_i)
    }
    
    
  
end
