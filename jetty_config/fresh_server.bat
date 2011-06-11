echo Starting QED server...
@del products.sqlite3
@cd webserver
@java -jar start.jar
@cd ..