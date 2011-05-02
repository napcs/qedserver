QEDServer
============

You've just come across a great new client-side library that makes building richer client-side apps a breeze. It has support for client-side storage, but you're really interested in using this with an existing server-side web application, so you decide to try it out. Then you realize that to test everything, you need a backend. That means you need to write some server side code, set up a database, and you need to deploy the thing on a web server so that all the AJAX requests work right. And of course, you need to populate that database with dummy data. That's a lot of work just to try out a new framework!

QEDServer solves those pesky problems for you so you can focus on sharpening your front-end skills. In one small package, you get a simple web server that hosts a small "product catalog" web application and a database full of existing products. You can immediately start writing code against its RESTLike API that responds with JSON and XML.

You put your files in the "public" folder that QEDServer creates, visit http://localhost:8080/index.html in your browser, and you can start coding against this simple backend without worrying about server setup or same-origin-policy issues.

Additionally, QEDServer provides a web interface of its own that you can use to manage the stock data and add your own records. 

And if you need a fresh start, just delete the `products.sqlite3` file and restart QEDServer. The database will be recreated
so you'll have a clean environment again.

Setup
--------
You'll need the Java Runtime Environment version 1.6. 
    
On Windows, you can run 

    server.bat
    
And on Mac OSX and Linux, you can run

    sh server.sh
    
to start the server.

You can also run it directly with Java by doing

    java -jar webserver.war
    
Finally, you can use `fresh_server.bat` on Windows and `fresh_server.sh` on Linux or OSX to delete the database file before starting the server so that you'll have a fresh environment when you star the server again.


Usage
-------

When QEDServer starts,  a database file and a `public` folder will appear in the folder where you ran the server. 
Any HTML files you place in the public folder will be served out of this folder. This is where you'll
work on your HTML and JavaScript files. By placing files in this folder and allowing QEDServer
to serve them, you avoid AJAX "same origin policy" issues.

If you visit  http://localhost:8080/help you'll see the current API as well as a brief tutorial.

### Managing the database

Point your browser to http://localhost:8080/products to manage the database.



