* Building The Source RPM

- Build The source rpm (at this point it is packed as a tar file), execute the command below to generate the source rpm
mvn install

- the command  above produces a tar file target/jetty-source-rpm.tar
- extract the tar file ( target/jetty-source-rpm.tar ) at your home directory using a none root account

tar -xzvf target/jetty-source-rpm.tar.gz -C ~



* Building The Binary RPMs

- at your home directory, execute the command below, you may need to install a list of required see `Installing Required Packages`
rpmbuild -ba rpm/SPECS/jetty6.spec



* Installing Required Packages

The build process requires several packages to be installed from
jpackage.  consult the jpackage documentation to install:

  java-devel >= 1.5.0
  jpackage-utils >= 0:1.7.2
  ant >= 0:1.6
  ant-junit >= 0:1.6
  junit >= 0:3.8.1
  maven2 >= 2.0.4-10jpp
  maven2-plugin-compiler
  maven2-plugin-install
  maven2-plugin-jar
  maven2-plugin-javadoc
  maven2-plugin-resources
  maven2-plugin-surefire
  maven2-plugin-antrun
  maven2-plugin-war
  derby
  sun-mail
  sun-jaf
  jakarta-commons-el
  ecj
  geronimo-specs-poms

* Installing the rpm
cd rpm/RPMS/noarch/
rpm -ivh jetty6-servlet-2.5-api-${jetty-version}-1jpp.noarch.rpm
rpm -ivh jetty6-core-${jetty-version}-1jpp.noarch.rpm
rpm -ivh jetty6-${jetty-version}-1jpp.noarch.rpm
rpm -ivh jetty6-plus-${jetty-version}-1jpp.noarch.rpm
rpm -ivh jett6-jsp-2.1-${jetty-version}-1jpp.noarch.rpm
#optional
rpm -ivh jetty6-javadoc-${jetty-version}-1jpp.noarch.rpm
rpm -ivh jetty6-demos-6.1.12-1jpp.noarch.rpm

