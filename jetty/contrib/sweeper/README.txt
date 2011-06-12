The job of the sweeper is to delete temp dirs created by jetty that have not been deleted
due to a sun bug - http://bugs.sun.com/view_bug.do?bug_id=4950148

Building:
mvn install

Usage:
java -jar sweeper-6.1.26.jar

To run once, add the system property:
 -Dinterval=0

Generally, you would setup a batch file to execute this jar on startup.

