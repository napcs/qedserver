echo Starting QED server...
@del products.sqlite3
@cd qedserver
@java -jar start.jar
@cd ..