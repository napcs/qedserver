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

//========================================================================
//$Id:  $
//JBoss Jetty Integration
//------------------------------------------------------------------------
//Licensed under LGPL.
//See license terms at http://www.gnu.org/licenses/lgpl.html
//========================================================================
package org.jboss.jetty;


import java.lang.reflect.Method;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.deployment.DeploymentException;
import org.jboss.deployment.DeploymentInfo;
import org.jboss.deployment.SubDeployerExt;
import org.jboss.mx.util.MBeanProxyExt;
import org.jboss.system.ServiceControllerMBean;
import org.jboss.web.AbstractWebContainer;
import org.jboss.web.AbstractWebDeployer;
import org.jboss.web.WebApplication;
import org.w3c.dom.Element;

//------------------------------------------------------------------------------
/**
 * JettyService
 * A service to launch jetty as the webserver for JBoss.
 *
 *
 * @jmx:mbean name="jboss.jetty:service=Jetty"
 *            extends="org.jboss.web.AbstractWebContainerMBean"
 *
 * @todo convert to use JMXDoclet...
 * 
 */

public class JettyService extends AbstractWebContainer implements JettyServiceMBean, MBeanRegistration
{
    public static final String NAME = "Jetty";

    protected MBeanServer _server = null;
    protected Jetty _jetty = null;
    protected Element _jettyConfig = null;
    protected boolean _supportJSR77;
    protected String _webDefaultResource;
    protected SubDeployerExt subDeployerProxy = null;
    
    /**
     * ConfigurationData
     *
     * Holds info that the jboss API sets on the
     * AbstractWebContainer but is needed by the
     * AbstractWebDeployer.
     */
    public static class ConfigurationData
    {
        private boolean _loaderCompliance;
        private boolean _unpackWars;
        private boolean _lenientEjbLink;
        private String _subjectAttributeName;
        private String _defaultSecurityDomain;
        private boolean _acceptNonWarDirs;
        private String _webDefaultResource;
        private boolean _supportJSR77;
        private String _mbeanDomain;
        
        /**
         * @return the _webDefaultResource
         */
        public String getWebDefaultResource()
        {
            return _webDefaultResource;
        }

        /**
         * @param defaultResource the _webDefaultResource to set
         */
        public void setWebDefaultResource(String defaultResource)
        {
            _webDefaultResource = defaultResource;
        }

        public void setJava2ClassLoadingCompliance(boolean loaderCompliance)
        {
           _loaderCompliance=loaderCompliance;
        }

        public boolean getJava2ClassLoadingCompliance()
        {
            return _loaderCompliance;
        }
       
        public boolean getUnpackWars()
        {
            return _unpackWars;
        }

        public void setUnpackWars(boolean unpackWars)
        {
            _unpackWars=unpackWars;
        }
        
        public void setLenientEjbLink (boolean lenientEjbLink)
        {
            _lenientEjbLink=lenientEjbLink;
        }
        
        public boolean getLenientEjbLink()
        {
            return _lenientEjbLink;
        }

        public String getSubjectAttributeName()
        {
            return _subjectAttributeName;
        }

        /**
         * @jmx:managed-attribute
         */
        public void setSubjectAttributeName(String subjectAttributeName)
        {
            _subjectAttributeName=subjectAttributeName;
        }

        /**
         * @return the _defaultSecurityDomain
         */
        public String getDefaultSecurityDomain()
        {
            return _defaultSecurityDomain;
        }

        /**
         * @param securityDomain the _defaultSecurityDomain to set
         */
        public void setDefaultSecurityDomain(String securityDomain)
        {
            _defaultSecurityDomain = securityDomain;
        }

        /**
         * @return the _acceptNonWarDirs
         */
        public boolean getAcceptNonWarDirs()
        {
            return _acceptNonWarDirs;
        }

        /**
         * @param nonWarDirs the _acceptNonWarDirs to set
         */
        public void setAcceptNonWarDirs(boolean nonWarDirs)
        {
            _acceptNonWarDirs = nonWarDirs;
        }

        /**
         * @return the _supportJSR77
         */
        public boolean getSupportJSR77()
        {
            return _supportJSR77;
        }

        /**
         * @param _supportjsr77 the _supportJSR77 to set
         */
        public void setSupportJSR77(boolean _supportjsr77)
        {
            _supportJSR77 = _supportjsr77;
        }

        /**
         * @return the _mbeanDomain
         */
        public String getMBeanDomain()
        {
            return _mbeanDomain;
        }

        /**
         * @param domain the _mbeanDomain to set
         */
        public void setMBeanDomain(String domain)
        {
            _mbeanDomain = domain;
        }
    }

    
    
    /** 
     * Constructor
     */
    public JettyService()
    {
        super();
        _jetty = new Jetty(this);
    }



    /** 
     * Listen for our registration as an mbean and remember our name.
     * @see org.jboss.system.ServiceMBeanSupport#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
     */
    public ObjectName preRegister(MBeanServer server, ObjectName name)
            throws Exception
    {
        super.preRegister(server, name);
        name = getObjectName(server, name);
        _server = server;
        return name;
    }

    
    /** 
     * Listen for post-mbean registration and set up the jetty
     * mbean infrastructure so it can generate mbeans according
     * to the elements contained in the <configuration> element
     * of the jboss-service.xml file.
     * @see org.jboss.system.ServiceMBeanSupport#postRegister(java.lang.Boolean)
     */
    public void postRegister(Boolean done)
    {
        super.postRegister(done);
        try
        {
            log.debug("Setting up mbeanlistener on Jetty");
            _jetty.getContainer().addEventListener(new JBossMBeanContainer(_server));
        }
        catch (Throwable e)
        {
            log.error("could not create MBean peers", e);
        }
    }


    /** 
     * @see org.jboss.system.ServiceMBeanSupport#getName()
     */
    public String getName()
    {
        return NAME;
    }


    /** 
     * @see org.jboss.deployment.SubDeployerSupport#createService()
     */
    public void createService() throws Exception
    {
        super.createService();
        if (_jettyConfig != null) _jetty.setConfigurationElement(_jettyConfig);
    }

    /** 
     * Start up the jetty service. Also, as we need to be able
     * to have interceptors injected into us to support jboss.ws:service=WebService,
     * we need to create a proxy to ourselves and register that proxy with the
     * mainDeployer.
     * See <a href="http://wiki.jboss.org/wiki/Wiki.jsp?page=SubDeployerInterceptorSupport">SubDeployerInterceptorSupport</a>
     * @see org.jboss.web.AbstractWebContainer#startService()
     */
    public void startService() throws Exception
    {
        //do what AbstractWebContainer.startService() would have done
        serviceController = (ServiceControllerMBean)
        MBeanProxyExt.create(ServiceControllerMBean.class,
                             ServiceControllerMBean.OBJECT_NAME,
                             server);

        //instead of calling mainDeployer.addDeployer(this) as SubDeployerSupport super class does,
        //we register instead a proxy to oursevles so we can support dynamic addition of interceptors
        subDeployerProxy = (SubDeployerExt)MBeanProxyExt.create(SubDeployerExt.class, super.getServiceName(), super.getServer());
        mainDeployer.addDeployer(subDeployerProxy);
        _jetty.start();
    }

    public void stopService() throws Exception
    {
        mainDeployer.removeDeployer(subDeployerProxy);
        _jetty.stop();
    }

    public void destroyService() throws Exception
    {
        super.destroyService();
        _jetty.stop();
        _jetty = null;
    }

    /**
     * @jmx:managed-attribute
     */
    public boolean getSupportJSR77()
    {
        return _supportJSR77;
    }

    /**
     * @jmx:managed-attribute
     */
    public void setSupportJSR77(boolean supportJSR77)
    {
        if (log.isDebugEnabled())
            log.debug("set SupportJSR77 to " + supportJSR77);

        _supportJSR77=supportJSR77;
    }

    /**
     * Get the custom webdefault.xml file.
     * @jmx:managed-attribute
     */
    public String getWebDefaultResource()
    {
        return _webDefaultResource;
    }

    /**
     * Set a custom webdefault.xml file.
     * @jmx:managed-attribute
     */
    public void setWebDefaultResource(String webDefaultResource)
    {
        if (log.isDebugEnabled())
            log.debug("set WebDefaultResource to " + webDefaultResource);

        _webDefaultResource=webDefaultResource;
    }


    /**
     * Get the extended Jetty configuration XML fragment
     * 
     * @jmx:managed-attribute
     * @return Jetty XML fragment embedded in jboss-service.xml
     */

    public Element getConfigurationElement()
    {
        return _jettyConfig;
    }

    /**
     * Configure Jetty
     * 
     * @param configElement XML fragment from jboss-service.xml
     * @jmx:managed-attribute
     */
    public void setConfigurationElement(Element configElement)
    {
        log.debug("Saving Configuration to xml fragment");
        this._jettyConfig = configElement;
    }


    /**
     * Old deployment method from AbstractWebContainer.
     * 
     * TODO remove this?
     * @param webApp
     * @param warUrl
     * @param parser
     * @throws DeploymentException
     */
    public void performDeploy(WebApplication webApp, String warUrl,
            WebDescriptorParser parser) throws DeploymentException
    {
        //TODO: backwards compatibility?
        throw new UnsupportedOperationException("Backward compatibility not implemented");
    }

    /**
     * Old undeploy method from AbstractWebContainer.
     * 
     * TODO remove?
     * @param warUrl
     * @throws DeploymentException
     */
    public void performUndeploy(String warUrl) throws DeploymentException
    {
        //TODO backwards compatibility?
        throw new UnsupportedOperationException("Backward compatibility not implemented");
    }
    
    /** 
     * @see org.jboss.web.AbstractWebContainer#getDeployer(org.jboss.deployment.DeploymentInfo)
     */
    public AbstractWebDeployer getDeployer(DeploymentInfo di) throws Exception
    {
        JettyDeployer deployer = new JettyDeployer(_jetty, di);
        ConfigurationData configData = new ConfigurationData();
        configData.setMBeanDomain("jboss.jetty");
        configData.setAcceptNonWarDirs(getAcceptNonWarDirs());
        configData.setJava2ClassLoadingCompliance(getJava2ClassLoadingCompliance());
        configData.setLenientEjbLink(getLenientEjbLink());
        configData.setSubjectAttributeName(getSubjectAttributeName());
        configData.setSupportJSR77(getSupportJSR77());
        configData.setUnpackWars(getUnpackWars());
        configData.setWebDefaultResource(getWebDefaultResource());
        //defaultSecurityDomain was added at a certain point, so do it
        //this way so we have backwards compatibility
        try
        {
            Method method = AbstractWebContainer.class.getDeclaredMethod("getDefaultSecurityDomain", new Class[0]);
            String defaultSecurityDomain = (String) method.invoke(JettyService.this, new Object[0]);
            configData.setDefaultSecurityDomain(defaultSecurityDomain);
        }
        catch (Exception e)
        {
            // ignore - it means the currently executing version of jboss
            // does not support this method
            log.info("Getter/setter for DefaultSecurityDomain not available in this version of JBoss");
        }
        deployer.setServer(_server);
        deployer.init(configData);
        return deployer;
    }

}
