Spring EJB3 (Pitchfork) Demo Webapp
-----------------------------------

This is a very simple webapp that provides a stateless session bean which
will return the date when when called, a POJO to access the session bean,
and a JSP for presentation.

The stateless session bean and the POJO are marked up with JEE annotations.

To run this webapp, you'll need to:

1. build it (mvn package)
2. copy the built webapp in target to your $jetty.home/webapps directory
3. make sure you've built $jetty.home/extras/spring and copied the jar to 
   $jetty.home/lib/ext/spring
3. make sure you have downloaded pitchfork and copied all of its jars into
   $jetty.home/lib/ext/spring


There is more information on the Jetty wiki at: 
  http://docs.codehaus.org/display/JETTY/Jetty+and+Spring+EJB3+%28Pitchfork%29
