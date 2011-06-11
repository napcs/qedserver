#!/bin/sh
rm products.sqlite3
cd webserver 
java -jar start.jar
cd ..