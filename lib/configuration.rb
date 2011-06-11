require 'lib/version'
puts "Starting QEDServer #{QED_SERVER_VERSION}..."

ENV['RACK_ENV'] ||= "development"

puts "Using #{ENV['RACK_ENV']} environment"

DBFILE =  File.expand_path(".", ENV_JAVA['user.dir'] + "/../products.sqlite3")
PUBLIC_PATH = File.expand_path(".", ENV_JAVA['user.dir'] + ("/../public"))

# Public Folder setup
FileUtils.mkdir_p PUBLIC_PATH

puts "Serving files from #{PUBLIC_PATH}"

configure(:development) do |c|
  require "sinatra/reloader"
  c.also_reload "*.rb"
end

# Database Configuration and setup
ActiveRecord::Base.establish_connection(
:adapter => "jdbcsqlite3",
:database => DBFILE
)

puts "Using database file at #{DBFILE}"

ActiveRecord::Base.include_root_in_json = false

# Create the product table, but only if
# there's no existing database file.
if File.exist?(DBFILE)
  puts "Using existing DB at #{DBFILE}..."
end

  ActiveRecord::Schema.define do
    
    unless Product.table_exists?
      create_table :products do |t|
        t.string :name
        t.text :description
        t.decimal :price
        t.string :image_url
        t.timestamps
      end
    end
    
    unless Category.table_exists?
      create_table :categories do |t|
        t.string :name
        t.timestamps
      end
    end
    
    unless ProductCategory.table_exists?
      create_table :product_categories do |t|
        t.integer :category_id
        t.integer :product_id
      end
    end
  
  end