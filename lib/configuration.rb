QED_SERVER_VERSION="0.2.0"
DBFILE =  File.expand_path(".", ENV_JAVA['user.dir']) + "/products.sqlite3"
PUBLIC_PATH = File.expand_path(".", ENV_JAVA['user.dir']) + ("/public")

# Public Folder setup
FileUtils.mkdir_p PUBLIC_PATH

# Database Configuration and setup
ActiveRecord::Base.establish_connection(
:adapter => "jdbcsqlite3",
:database => DBFILE
)

# Create the product table, removing it if it
# already exists. This gives us a fresh environment
# every time we start.
ActiveRecord::Schema.define do
  create_table :products, :force => true do |t|
    t.string :name
    t.text :description
    t.decimal :price
    t.timestamps
  end
end