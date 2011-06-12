Before building, the ff should already be installed:
fakeroot
debhelper
cdbs

---------------------------------------

Building/Generating the debs:
$ mvn install (will produce a debian tree under target/jetty6-${version})

---------------------------------------

Installing the debs:
$ sudo dpkg -i libjetty6-java_6.1.${version}_all.deb
$ sudo dpkg -i jetty6_6.1.${version}_all.deb

#optional
$ sudo dpkg -i libjetty6-jsp-java_6.1.${version}_all.deb (requires ant)
$ sudo dpkg -i libjetty6-extra-java_6.1.${version}_all.deb (required libgnumail, libgnujaf)
$ sudo dpkg -i jetty6-examples_6.1.${version}_all.deb

---------------------------------------

In order to start jetty, edit /etc/default/jetty and set:
NO_START=0

---------------------------------------

Starting:
$ sudo service jetty6 start

---------------------------------------

Stopping:
$ sudo service jetty6 stop
