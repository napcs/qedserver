#!/bin/sh
cd "`dirname "$0"`"
java -server -Xms512m -Xmx512M -jar qedserver.war --debug=2 --httpPort=8080
