Deploying
---------
Copy or move the $JETTY-HOME/contexts-available/test-annotations.d
directory to $JETTY-HOME/contexts, and copy or move the
$JETTY-HOME/contexts-available/test-annotations.xml file to
$JETTY-HOME/contexts.

Edit $JETTY-HOME/contexts/test-annotations.xml and uncomment
the setup for the transaction manager of your choice. The demo
works with either JOTM (http://jotm.objectweb.org) or Atomikos
(http://www.atomikos.com).

Download and copy the jar files necessary for your transaction 
manager to $JETTY-HOME/lib/ext. See the jetty wiki pages for
JOTM(http://docs.codehaus.org/display/JETTY/JOTM)  and 
Atomikos (http://docs.codehaus.org/display/JETTY/Atomikos)
for more info.

The example uses the Derby database, so also download the
derby.jar and derbytools.jar file from the Derby site 
(http://db.apache.org/derby) and put them in $JETTY-HOME/lib/ext.



Running the Demo
----------------
You run the demo like so:
   
   java -DOPTIONS=plus,ext,annotations,default -jar start.jar 


Adding Support for a Different Transaction Manager
--------------------------------------------------

1. Edit the filter.properties file in 
   $JETTY-HOME/modules/examples/jetty-annotation-example-webapp/src/etc/templates
   and add a new set of token and replacement strings following the
   pattern established for ATOMIKOS and JOTM.

2. Edit the jetty-env.xml file in
   $JETTY-HOME/modules/examples/jetty-annotation-example-webapp/src/etc/templates
   and add configuration for new transaction manager following the
   pattern established for the other transaction managers.

3. Edit the annotations-context.xml file in
   $JETTY-HOME/modules/examples/jetty-annotation-example-webapp/src/etc/templates
   and add configuration for the new transaction manager following
   the pattern established for the other transaction managers.

4. Rebuild $JETTY-HOME/modules/examples/jetty-annotation-example-webapp (mvn clean install).
