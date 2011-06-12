
For full information to build jetty jboss, see http://docs.codehaus.org/display/JETTY/JBoss


To build:
--------
mvn -P[jdk1.4|jdk1.5] -Djboss.version=4.0.x -Djboss.home=/path/to/jboss-4.0.x clean install

Note: you must select either the jdk1.4 or the jdk1.5 profile. This will build with either 
jsp-2.0 or jsp-2.1 respectively.

To install:
----------
   1. delete $JBOSS-HOME/server/default/deploy/jbossweb-tomcat55.sar (or from whichever deploy directory you
      are using)
   2. ensure you have built the Jetty JBoss module in $jetty.home/extras/jboss
   3. copy the $jetty.home/extras/jboss/target/jetty-JETTY-VERSION-jboss-JBOSS-VERSION.sar to your JBoss
      deploy directory (where JETTY-VERSION is the version of jetty you are using and JBOSS-VERSION is the
      version of JBoss).
   4. edit $JBOSS-HOME/server/default/deploy/management/console-mgr.sar/web-console.war/WEB-INF/web.xml
      and remove servlet and servlet-mapping for Status Servlet (tomcat specific)


