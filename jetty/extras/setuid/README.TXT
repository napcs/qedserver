
Build and Install
-----------------

On Linux or Unix with gcc installed, do:

  mvn install


On other systems, you may need to manually generate the lib. Here are
some guidelines to get you started, but you may need to vary the arguments
to the gcc command depending on your environment: 

+  run mvn install at the top level of this project
+  mkdir -p modules/native/target/generated
+  cp modules/native/src/main/native/org_mortbay_setuid_SetUID.c modules/native/target/generated  
+  javah -d modules/native/target/generated -classpath modules/java/target/jetty-setuid-java-6.1.26.jar org.mortbay.setuid.SetUID
+  gcc -I$JAVA_HOME/include/ -I$JAVA_HOME/include/linux/  \
       -shared modules/native/target/generated/org_mortbay_setuid_SetUID.c -o \
       ../../lib/ext/libsetuid.so



Configuration
-------------

Change etc/jetty-setuid.xml to use the userid or username, and/or group id or group name you want.


Running
-------
In the top level jetty directory do:
  
+  if your libsetuid.so is located at <jetty-root>/lib/ext (the default location):
       sudo java -jar start.jar etc/jetty-setuid.xml etc/jetty.xml

+  or set the System property java.library.path to the directory where your libsetuid.so is located:
       sudo java -Djava.library.path=/directory/path/to/your/jetty/lib -jar start.jar etc/jetty-setuid.xml etc/jetty.xml

+ alternatively, set the System property jetty.libsetuid.path to the location of your
  libsetuid.so file:
       sudo java -Djetty.libsetuid.path=/directory/path/to/your/jetty/lib/libsetuid.so -jar start.jar etc/jetty-setuid.xml etc/jetty.xml

