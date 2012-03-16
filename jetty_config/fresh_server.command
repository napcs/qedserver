#!/bin/sh
cd "`dirname "$0"`"
rm products.sqlite3
cd qedserver 
java -jar start.jar
cd ..