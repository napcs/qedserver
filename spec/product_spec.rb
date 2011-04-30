require File.dirname(__FILE__) + '/spec_helper'
describe Product do
  
  describe "when searching by keyword" do
    
    before(:each) do
      Product.create :name => "Camera and film"
      Product.create :name => "SLR camera"
      Product.create :name => "Macbook Pro", :description => "Has a camera"
      Product.create :name => "iPod Touch", :description => "foo"
    end
    
    it "finds Camera in the name or in the description" do
      Product.with_keyword("camera").length.should == 3
    end
    
    it "finds 3 records on page 1 and 1 record on page 2 for 4 records" do
      @products = Product.paginate(1,3)
      @products.length.should == 3
      @products = Product.paginate(2,3)
      @products.length.should == 1
    end
              
  end
  
end