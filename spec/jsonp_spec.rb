require File.dirname(__FILE__) + '/spec_helper'
describe "the JSONP extension" do
  
  describe "when working with Arrays" do
    it "wraps an array with the callback foo" do
      [1,2,3].to_json(:callback => "foo").should == "foo([1,2,3])"
    end
  end
  
  describe "when working with Hashes" do
    it "wraps the hash with the callback foo" do
     {:one => "two", :two => "three"}.to_json(:callback => "foo").should == "foo(#{{:one => "two", :two => "three"}.to_json})"
    end
  end
  
  describe "When working with an Active Record relation" do
    it "wraps the result with the callback foo" do
      Category.delete_all
      Category.create! :name => "foo"
      Category.create! :name => "bar"
      categories = Category.limit(2)
      result = "foo(#{categories.to_json})"
      categories.to_json(:callback => "foo").should == result
    end
  end
  
  describe "When working with an Active Record model" do
    it "wraps the result with the callback foo" do
      Category.delete_all
      category = Category.create! :name => "foo"
      result = "foo(#{category.to_json})"
      category.to_json(:callback => "foo").should == result
    end
  end
  
end