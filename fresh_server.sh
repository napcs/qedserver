#!/bin/sh
echo Starting QED server...
rm products.sqlite3
java -Xmx256M -jar webserver.war --debug=1 --httpPort=8080
