require File.dirname(__FILE__) + '/spec_helper'
describe Category do
  
    it "requires the name to be filled in" do
      c = Category.create
      c.errors[:name].should include("can't be blank")
    end
    
    it "requires category names to be unique" do
      Category.create! :name => "foo"
      c = Category.create :name => "foo"
      c.errors[:name].should include("has already been taken")
    end

end