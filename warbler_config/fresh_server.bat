echo Starting QED server...
@del products.sqlite3
@java -server -Xms512m -Xmx512M -jar qedserver.war --debug=1 --httpPort=8080

