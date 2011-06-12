Building
========
1. Ensure you have a recent release of Openspaces/Gigaspaces installed.
2. Run the installmavenrep script or bat file in $GIGA-HOME/tools/maven.
3. mvn clean install in this directory

Using Openspaces for Session Clustering
=======================================
A. Preparing the DataGrid
----------------------
1. Ensure that you have a recent release of Openspaces and Gigaspaces installed.
2. Start a gsm instance
3. Start as many gsc instances as you need
4. Start a grid-ui instance 
5. Using the grid-ui, create an Enterprise Grid called "jettyGrid", clustering
   according to your preference.


B. Preparing Jetty
---------------
1. mkdir $JETTY-HOME/lib/ext/openspaces

2. Copy the following files from your gigaspaces lib directory to 
   $JETTY-HOME/lib/ext/openspaces, maintaining the directory hierarchy:
   
   $GIGA-HOME/lib/JSpaces.jar
   $GIGA-HOME/lib/common/commons-logging.jar 
   $GIGA-HOME/lib/jini/jsk-lib.jar
   $GIGA-HOME/lib/jini/jsk-platform.jar
   $GIGA-HOME/lib/jini/reggie.jar
   $GIGA-HOME/lib/jini/start.jar
   $GIGA-HOME/lib/openspaces/openspaces.jar
   $GIGA-HOME/lib/ServiceGrid/gs-boot.jar
   $GIGA-HOME/lib/ServiceGrid/gs-lib.jar
   $GIGA-HOME/lib/spring/spring.jar
   $GIGA-HOME/lib/spring/cglib-nodep-2.1_3.jar

3. Edit your $JETTY-HOME/etc/jetty.xml file and configure a
   org.mortbay.jetty.openspaces.GigaSessionIdManager:

   <Set name="sessionIdManager">
    <New id="gigaidmgr" class="org.mortbay.jetty.openspaces.GigaSessionIdManager">
      <Arg><Ref id="Server"/></Arg>
      <Set name="workerName">fred</Set>
      <Set name="spaceUrl"><SystemProperty name="space.url"/></Set>
    </New>
   </Set>
   <Call name="setAttribute">
    <Arg>gigaIdMgr</Arg>
    <Arg><Ref id="gigaIdMgr"/></Arg>
   </Call>


4. For each webapp you want to use with clustered sessions, create a context
   xml configuration file for it and add the following lines:
   
<Configure class="org.mortbay.jetty.webapp.WebAppContext">
  <Call id= "server" name="getServer">
    <Call id="gigaIdMgr" name="getAttribute">
      <Arg>gigaIdMgr</Arg>
    </Call>
  </Call>
  <Set name="sessionHandler">
    <New class="org.mortbay.jetty.servlet.SessionHandler">
      <Arg>
        <New id="gigamgr" class="org.mortbay.jetty.openspaces.GigaSessionManager">
          <Set name="idManager">          
            <Ref id="gigaIdMgr"/>           
          </Set>
          <Set name="spaceUrl"><SystemProperty name="space.url"/></Set>
          <Set name="scavengePeriod">60</Set>
        </New>
      </Arg>
    </New>
  </Set> 
</Configure>

5. Start jetty from the command line, passing in the name of the 
   Enterprise Data Grid you created in the step A, and the location of 
   your gigaspaces installation:

   java -Dext,default -Dspace.url="jini://*/*/jettyGrid?useLocalCache&updateMode=2" \
        -Dcom.gs.home=/usr/local/java/gigaspaces-xap-6.5-rc1 \
        -Djava.security.policy=/usr/local/java/gigaspaces-xap-6.5-rc1/policy/policy.all \
        -jar start.jar etc/jetty.xml




NOTE:  Openspaces uses spring so if you have issues with deploying your webapp into this setup, check if the webapp is using
spring, you might need to do some exclusions or scope settings on your webapp dependencies.  For example with the async-webapp
which uses cxf which also uses spring 2.0.8 in many of its artifacts.  These need to be excluded so that your webapp doesn't
package another version of spring which will conflict with the one in Step 2 above.
