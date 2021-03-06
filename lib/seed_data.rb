# Create some default categories and products in the database
# so the user doesn't have to do anything.

laptops = Category.where(name: "Laptops").first_or_create
cameras = Category.where(name: "Cameras").first_or_create
accessories = Category.where(name: "Accessories").first_or_create
desktops = Category.where(name: "Desktops").first_or_create
tablets = Category.where(name: "Tablets").first_or_create
music = Category.where(name: "Music Players").first_or_create

unless Product.any?
  cameras.products.create :name => "Nikon Digial Camera", :price => 600.00, :description => "Description of Nikon Digital Camera"
  cameras.products.create :name => "Canon Digial Camera", :price => 600.00, :description => "Description of Canon Digital Camera"
  laptops.products.create :name => "Macbook Air 11 inch", :price => 999.00, :description => "Description of Macbook Air 11 inch"
  laptops.products.create :name => "Macbook Air 13 inch", :price => 1299.00, :description => "Description of Macbook Air 13 inch"
  laptops.products.create :name => "Macbook Pro 13 inch", :price => 1299.00, :description => "Description of Macbook Pro 13 inch"
  laptops.products.create :name => "Macbook Pro 15 inch", :price => 1799.00, :description => "Description of the Macbook Pro 15 inch"
  desktops.products.create :name => "iMac 21.5 inch", :price => 1299.00, :description => "Description of the iMac 21.5 inch"
  desktops.products.create :name => "iMac 27 inch", :price => 1799.00, :description => "Description of the iMac 27 inch"
  tablets.products.create :name => "iPad 64GB Wifi+3G", :price => 829.99, :description => "Description of the iPad"
  tablets.products.create :name => "Amazon Kindle Fire", :price => 139.99, :description => "Description of Kindle Fire"
  tablets.products.create :name => "Amazon Paper White", :price => 119.99, :description => "Description of Amazon Paper White"
  tablets.products.create :name => "Google Nexux 7", :price => 199.99, :description => "Description of Google Nexus 7"
  music.products.create :name => "iPod Touch 64GB", :price => 829.99, :description => "Description of the iPod Touch"
  music.products.create :name => "iPod Shuffle", :price => 49.99, :description => "Description of the iPod Shuffle"
  music.products.create :name => "Apple TV", :price => 99.99, :description => "Description of the Apple TV"
  accessories.products.create :name => "iPad Smart Cover", :price => 29.99, :description => "Description of iPad Smart Cover"
  accessories.products.create :name => "iPad Smart Cover - Leather", :price => 59.99, :description => "Description of iPad Smart Cover - Leather"
  accessories.products.create :name => "Apple Magic Mouse", :price => 69.00, :description => "Description of Apple Magic Mouse"
  accessories.products.create :name => "Apple Keyboard with Numeric Keypad", :price => 49.00, :description => "Description of Apple Keyboard with Numeric Keypad"
  accessories.products.create :name => "Apple Magic Trackpad", :price => 69.00, :description => "Description of Apple Magic Trackpad"
  accessories.products.create :name => "Airport Extreme Base Station", :price => 179.00, :description => "Description of Airport Extreme Base Station"
  accessories.products.create :name => "Apple Wireless Keyboard", :price => 69.00, :description => "Description of Apple Wireless Keyboard"
  accessories.products.create :name => "Mini DisplayPort to VGA Adapter", :price => 29.00, :description => "Description of Mini DisplayPort to VGA Adapter"
  accessories.products.create :name => "Mini DisplayPort to DVI Adapter", :price => 29.00, :description => "Description of Mini DisplayPort to DVI Adapter"
  accessories.products.create :name => "Mini DVI to VGA Adapter", :price => 29.00, :description => "Description of Mini DVI to VGA Adapter"
  accessories.products.create :name => "DVI to VGA Adapter", :price => 29.00, :description => "Description of DVI to VGA Adapter"
  accessories.products.create :name => "AirPort Express", :price => 99.00, :description => "Description of AirPort Express"
end
