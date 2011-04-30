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
              
  end
  
end