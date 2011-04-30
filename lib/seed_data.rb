# Create some default products in the database
# so the user doesn't have to do anything.
unless Product.any?
  Product.create :name => "Nikon D90 Digial Camera", :price => 500.00, :description => "Description of Camera"
  Product.create :name => "Macbook 13 inch", :price => 999.00, :description => "Description of the Macbook 13"
  Product.create :name => "Macbook Air 11 inch", :price => 999.00, :description => "Description of the Macbook Air 11"
  Product.create :name => "Macbook Air 13 inch", :price => 1299.00, :description => "Description of the Macbook Air 13"
  Product.create :name => "Macbook Pro 13 inch", :price => 1299.00, :description => "Description of the Macbook Pro 13"
  Product.create :name => "Macbook Pro 15 inch", :price => 1799.00, :description => "Description of the Macbook Pro 15"
  Product.create :name => "iMac 21.5 inch", :price => 1299.00, :description => "Description of the iMac 21.5 inch"
  Product.create :name => "iMac 27 inch", :price => 1799.00, :description => "Description of the iMac 27 inch"
  Product.create :name => "iPad 2 64GB Wifi+3G", :price => 829.99, :description => "Description of the iPad 2"
  Product.create :name => "iPod Touch 64GB", :price => 829.99, :description => "Description of the iPod Touch"
  Product.create :name => "iPod Shuffle", :price => 49.99, :description => "Description of the iPod Shuffle"
  Product.create :name => "Apple TV", :price => 99.99, :description => "Description of the Apple TV"
  Product.create :name => "iPad 2 Smart Cover", :price => 29.99, :description => "Description of iPad Smart Cover"
  Product.create :name => "iPad 2 Smart Cover - Leather", :price => 59.99, :description => "Description of iPad Smart Cover - Leather"
  Product.create :name => "Amazon Kindle Wi-Fi Only", :price => 139.99, :description => "Description of Kindle Wi-Fi Version"
  Product.create :name => "Amazon Kindle 3G", :price => 139.99, :description => "Description of Kindle 3G Version"
  
  
end
