#!/bin/sh
echo Starting QED server...
rm products.sqlite3
java -server -Xms512m -Xmx512M -jar qedserver.war --debug=1 --httpPort=8080