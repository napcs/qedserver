#!/bin/sh
cd "`dirname "$0"`"
rm products.sqlite3
cd webserver 
java -jar start.jar
cd ..