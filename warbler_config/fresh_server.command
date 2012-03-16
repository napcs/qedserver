#!/bin/sh
cd "`dirname "$0"`"
echo Starting QED server...
rm products.sqlite3
java -Xmx256M -jar webserver.war --debug=1 --httpPort=8080
