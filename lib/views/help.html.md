Quick Tutorial
-------
Let's make a simple page that fetches the products from our database using jQuery. Create a file called product_list.html in the "public" folder with this content:

    <!DOCTYPE html>
    <html lang='en'>
      <head>
        <meta content='text/html; charset=utf-8' http-equiv='Content-Type'>
        <title>Simple Product List</title>
        <script src='http://ajax.googleapis.com/ajax/libs/jquery/1.4.0/jquery.min.js' type='text/javascript'></script>
      </head>
      <body>
        <div id="products">
          <p>Click to load products</p>
        </div>
      </body>
    </html>

If we visit http://localhost:8080/products.json we'll get a JSON representation of 
the products in our database.

    [
      {
        "created_at":"2011-05-23T09:10:57-05:00",
        "description":"Description of Camera",
        "id":1,
        "image_url":null,
        "name":"Nikon D90 Digial Camera",
        "price":"500.0",
        "updated_at":"2011-05-23T09:10:57-05:00"
      },
      {
        "created_at":"2011-05-23T09:10:57-05:00",
        "description":"Description of the Macbook 13",
        "id":2,
        "image_url":null,
        "name":"Macbook 13 inch",
        "price":"999.0",
        "updated_at":"2011-05-23T09:10:57-05:00"
      }
    ]

Within the `head` tags of our page, let's add another script block that fetches 
products into the page using the JSON feed we just inspected. We'll need to add a click event
to our products region that triggers the AJAX request.

    <script>
    $(function(){
      $("#products").click(function(e){
      $.get("/products.json",
         function(data){
           var products = data;
           var ul = $("<ul></ul>"); // create a new unordered list
         
           $.each(products, function(index, product){
             var li = $("<li>" + product["name"] + "</li>");
             ul.append(li);  // add the listitem to the unordered list
           });
         
           $("#products").html(ul); // replace the products div with the ul
         }, "json");
      });
    });
    </script>
  
We fetch the data using the `.ajax()` method in jQuery, which takes the URL, a function, and
the content type we expect to get back. 

In our function, we construct an unordered list, iterate over the results we get back,
append each result to the unordered list as a list item, and then we replace the contents of our projects div with the unordered list.

If you visit http://localhost:8080/product_list.html, you'll see our demo, which will pull records from our database.

That was just a short demo. You can use the other pieces of the API to add and delete records, or even perform searches against the data, which is perfect for testing out those fun auto-complete scripts everyone wants you to write.

The API
---------

The API that's currently implemented is:

### Products
* `GET` http://localhost:8080/products.json fetches a JSON feed of the products
* `GET` http://localhost:8080/products.xml fetches an XML feed of the products
* `GET` http://localhost:8080/product/1.json fetches the product with the ID of 1 as JSON
* `GET` http://localhost:8080/product/1.xml fetches the product with the ID of 1 as XML
* `POST` http://localhost:8080/products.json creates a new product. The fields need to be nested if you form-encode the data, so name your form fields `product[name]`, etc. No nesting is required if you send the JSON in the request body.
* `PUT` http://localhost:8080/products/1.json modifies the existing product with the id of 1. If you form-encode the data, you'll need to post data in the nested format `product[name]`, etc. If you send JSON in the request body, no nesting is required.
* `DELETE` http://localhost:8080/products/1.json deletes the product with the ID of 1.
* `GET` http://localhost:8080/products.rss fetches an RSS 2.0 feed of the products

#### Creating and updating products
To create products, send a `POST` request to http://localhost:8080/products. The
web server expects to find the parameters nested. 

    <input type="text" name="product[name]">
    
No nesting is required if you send the JSON in the request body.

When a product is created successfully via JSON, you'll get this response:

    {success: true, message: "Created Camera."}

#### Deleting products
You can delete products by constructing a `DELETE` request to  http://localhost:8080/products/1.json where `1` is the ID of the product you want to remove. You'll get a JSON response back that looks like this:

    {success: true, message: "Camera was deleted."}
    
#### Searching for products

Searching for products is quite easy. You simply send a GET request to http://localhost:8080/products.json with a query paramter called `q` 
like this:

    http://localhost:8080/products.json?q=camera

You'll get back only products that have "camera" in the name or description.

#### Pagination

Results come back 10 pages at a time. Use the `page` query parameter to get additional results.

    `http://localhost:8080/products.json?page=2
    
This works with RSS, XML, and JSON, with or without keywords.

### Categories

* `GET` http://localhost:8080/categories.json fetches a JSON feed of the categories
* `GET` http://localhost:8080/categories.xml fetches an XML feed of the categories
* `GET` http://localhost:8080/categories/1.json fetches the categoy with the ID of 1 as JSON
* `GET` http://localhost:8080/categories/1.xml fetches the category with the ID of 1 as XML
* `POST` http://localhost:8080/categories.json creates a new category. The fields need to be nested if you form-encode the data, so name your form fields `category[name]`, etc. No nesting is required if you send the JSON in the request body.
* `PUT` http://localhost:8080/categories/1.json modifies the existing category with the id of 1. If you form-encode the data, you'll need to post data in the nested format `category[name]`, etc. If you send JSON in the request body, no nesting is required.
* `DELETE` http://localhost:8080/categories/1.json deletes the category with the ID of 1.
* `GET` http://localhost:8080/categories.rss fetches an RSS 2.0 feed of the categories
* `GET` http://localhost:8080/categories/1/products.json fetches products in the category with the ID of 1
* `GET` http://localhost:8080/categories/1/products.xml fetches products in the category with the ID of 1

Creating, updating, deleting, pagination, and searching work the same as with Products.

#### Pagination and Searching

When looking at products within categories, you can apply the same pagination and querying as you would with products.

* `http://localhost:8080/categories/1/products.json?page=2`
* `http://localhost:8080/categories/1/products.json?page=2&q=iPad`
    
This works with RSS, XML, and JSON, with or without keywords.

### JSON-P Support

You can get the JSON results back as JSON-P if you append a callback query parameter to the request. The result will be wrapped by the callback function you specify.

Right now we only support this for the following API endpoints.

* `GET` http://localhost:8080/products.json?callback=jsonpcallback
* `GET` http://localhost:8080/products/1.json?callback=jsonpcallback
* `GET` http://localhost:8080/categories.json?callback=jsonpcallback
* `GET` http://localhost:8080/categories/1/products.json?callback=jsonpcallback
* `GET` http://localhost:8080/categories/1/products/1.json?callback=jsonpcallback
