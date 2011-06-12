//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

/*
 * Generated file - Do not edit!
 */
package org.jboss.jetty;

/**
 * MBean interface.
 * TODO - use JMXDoclet to autogenerate
 */
public interface JettyServiceMBean extends org.jboss.web.AbstractWebContainerMBean 
{
   //default object name
   public static final javax.management.ObjectName OBJECT_NAME = org.jboss.mx.util.ObjectNameFactory.create("jboss.jetty:service=Jetty");

  void setJava2ClassLoadingCompliance(boolean loaderCompliance) ;

  boolean getJava2ClassLoadingCompliance() ;

  boolean getUnpackWars() ;

  void setUnpackWars(boolean unpackWars) ;

  boolean getSupportJSR77() ;

  void setSupportJSR77(boolean supportJSR77) ;

  java.lang.String getWebDefaultResource() ;

  void setWebDefaultResource(java.lang.String webDefaultResource) ;

   /**
    * Get the extended Jetty configuration XML fragment
    * @return Jetty XML fragment embedded in jboss-service.xml    */
  org.w3c.dom.Element getConfig() ;

   /**
    * Configure Jetty
    * @param configElement XML fragment from jboss-service.xml
    */
  void setConfigurationElement(org.w3c.dom.Element configElement) ;

  java.lang.String getSubjectAttributeName() ;

  void setSubjectAttributeName(java.lang.String subjectAttributeName) ;
}
