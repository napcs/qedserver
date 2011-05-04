QED_SERVER_VERSION="0.3.1"
DBFILE =  File.expand_path(".", ENV_JAVA['user.dir']) + "/products.sqlite3"
PUBLIC_PATH = File.expand_path(".", ENV_JAVA['user.dir']) + ("/public")

# Public Folder setup
FileUtils.mkdir_p PUBLIC_PATH

# Database Configuration and setup
ActiveRecord::Base.establish_connection(
:adapter => "jdbcsqlite3",
:database => DBFILE
)

# Create the product table, but only if
# there's no existing database file.
if File.exist?(DBFILE)
  puts "Using existing DB at #{DBFILE}..."
else
  ActiveRecord::Schema.define do
    create_table :products, :force => true do |t|
      t.string :name
      t.text :description
      t.decimal :price
      t.string :image_url
      t.timestamps
    end
  end
  puts "Loading sample data...."  
end