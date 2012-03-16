#!/bin/sh
rm products.sqlite3
cd qedserver 
java -jar start.jar
cd ..