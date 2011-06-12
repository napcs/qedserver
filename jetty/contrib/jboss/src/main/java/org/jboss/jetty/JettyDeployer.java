//========================================================================
//$Id: JettyDeployer.java 4067 2008-11-18 09:23:11Z janb $
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under LGPL.
//See license terms at http://www.gnu.org/licenses/lgpl.html
//========================================================================

package org.jboss.jetty;

import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.management.ObjectName;

import org.jboss.deployment.DeploymentException;
import org.jboss.deployment.DeploymentInfo;
import org.jboss.logging.Logger;
import org.jboss.web.AbstractWebDeployer;
import org.jboss.web.WebApplication;
import org.jboss.web.AbstractWebContainer.WebDescriptorParser;
import org.mortbay.jetty.SessionManager;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.util.LazyList;


/**
 * JettyDeployer
 *
 * Implementation of the jboss AbstractWebDeployer
 * for deploying webapps to jetty.
 */
public class JettyDeployer extends AbstractWebDeployer
{
    protected static final Logger _log = Logger.getLogger("org.jboss.jetty");

    protected Jetty _jetty;
    protected ContextHandlerCollection _contexts;
    protected DeploymentInfo _deploymentInfo;
    protected JettyService.ConfigurationData  _configData;
    protected SessionManager _distributableSessionManagerPrototype;
    protected boolean _forceDistributable = false;

    /**
     * use Hashtable because is is synchronised
     */
    Hashtable _deployed = new Hashtable();

    public JettyDeployer(Jetty jetty, DeploymentInfo di)
    {
        _jetty = jetty;
        _deploymentInfo = di;
        _contexts = (ContextHandlerCollection)_jetty.getChildHandlerByClass(ContextHandlerCollection.class);
    }


    public void init(Object containerConfig) throws Exception
    {
        _configData = (JettyService.ConfigurationData)containerConfig;          
        setLenientEjbLink(_configData.getLenientEjbLink());
        setDefaultSecurityDomain(_configData.getDefaultSecurityDomain());
        setJava2ClassLoadingCompliance(_configData.getJava2ClassLoadingCompliance());
        setUnpackWars(_configData.getUnpackWars());
    }

    public void performDeploy(WebApplication webApp, String warUrl, WebDescriptorParser parser) throws DeploymentException
    {
    	log.debug("deploying webapp at "+warUrl);        
        try
        {
            String contextPath = webApp.getMetaData().getContextRoot();
            if (contextPath.equalsIgnoreCase("/root"))
                contextPath = "/";

            webApp.setURL(new URL(warUrl));

            if (_deployed.get(warUrl) != null)
                throw new DeploymentException(warUrl+" is already deployed");

            //make a context for the webapp and configure it from the jetty jboss-service.xml defaults
            //and the jboss-web.xml descriptor
            JBossWebAppContext app = new JBossWebAppContext(parser, webApp, warUrl);
            app.setContextPath(contextPath);
            app.setConfigurationClasses (new String[]{ "org.mortbay.jetty.webapp.WebInfConfiguration","org.jboss.jetty.JBossWebXmlConfiguration", "org.mortbay.jetty.webapp.JettyWebXmlConfiguration",  "org.mortbay.jetty.webapp.TagLibConfiguration"});
            app.setExtractWAR(getUnpackWars());
            app.setParentLoaderPriority(getJava2ClassLoadingCompliance());
            
            //permit urls without a trailing '/' even though it is not a valid url
            //as the jboss webservice client tests seem to use these invalid urls
            if (_log.isDebugEnabled())
                _log.debug("Allowing non-trailing '/' on context path");
            app.setAllowNullPathInfo(true);
                 
            SessionManager manager = getDistributableSessionManagerPrototype();
            if (manager != null)
            {
                throw new UnsupportedOperationException("NOT IMPLEMENTED - please ask"); 
//                app.setDistributableSessionManager((Manager) manager.clone());
//                if (getForceDistributable())
//                    app.setDistributable(true);
            }

            // if a different webdefault.xml file has been provided, use it
            if (_configData.getWebDefaultResource() != null)
            {
                try
                {
                    URL url = getClass().getClassLoader().getResource(_configData.getWebDefaultResource());
                    String fixedUrl = (fixURL(url.toString()));
                    app.setDefaultsDescriptor(fixedUrl);
                    if (_log.isDebugEnabled())
                        _log.debug("webdefault specification is: " + _configData.getWebDefaultResource());
                }
                catch (Exception e)
                {
                    _log.error("Could not find resource: " + _configData.getWebDefaultResource()+" using default", e);
                }
            }

            Iterator hosts = webApp.getMetaData().getVirtualHosts();
            List hostList = new ArrayList();
            while(hosts.hasNext())
                hostList.add((String)hosts.next());
            app.setVirtualHosts((String[])LazyList.toArray(hostList, String.class));

            // Add the webapp to jetty 
            _contexts.addHandler(app);
            

            //tell jboss about the classloader the webapp is using - ensure
            //this is done before the context is started, because webservices
            //want to get access to this classloader
            System.err.println("In JettyDeployer, setting webapp.metadata.contextloader="+app.getClassLoader());
            webApp.getMetaData().setContextLoader(app.getClassLoader());

            //if jetty has been started, then start the
            //handler just added
            if (_contexts.isStarted())
                app.start();
            
            // keep track of deployed contexts for undeployment
            _deployed.put(warUrl, app);
    

            //tell jboss about the jsr77 mbeans we've created               
            //first check that there is an mbean for the webapp itself
            ObjectName webAppMBean = new ObjectName(_configData.getMBeanDomain() + ":J2EEServer=none,J2EEApplication=none,J2EEWebModule="+app.getUniqueName());
            if (server.isRegistered(webAppMBean))
                _deploymentInfo.deployedObject = webAppMBean;
            else
                throw new IllegalStateException("No mbean registered for webapp at "+app.getUniqueName());

            //now get all the mbeans that represent servlets and set them on the 
            //deployment info so they will be found by the jsr77 management system
            ObjectName servletQuery = new ObjectName
            (_configData.getMBeanDomain() + ":J2EEServer=none,J2EEApplication=none,J2EEWebModule="+app.getUniqueName()+ ",j2eeType=Servlet,*");
            Iterator iterator = server.queryNames(servletQuery, null).iterator();
            while (iterator.hasNext())
            {
                _deploymentInfo.mbeans.add((ObjectName) iterator.next());
            }


            _log.info("successfully deployed " + warUrl + " to " + contextPath);
        }
        catch (Exception e)
        {
            _log.error("Undeploying on start due to error", e);
            performUndeploy(warUrl, webApp);
            throw new DeploymentException(e);
        }
    }


    /** 
     * Undeploy a webapp
     * @see org.jboss.web.AbstractWebDeployer#performUndeploy(java.lang.String, org.jboss.web.WebApplication)
     */
    public void performUndeploy(String warUrl, WebApplication wa) throws DeploymentException
    {
        JBossWebAppContext app = (JBossWebAppContext) _deployed.get(warUrl);

        if (app == null)
            _log.warn("app (" + warUrl + ") not currently deployed");
        else
        {
            try
            {
                app.stop();
                _contexts.removeHandler(app);
                app.destroy();
                app = null;
                _log.info("Successfully undeployed " + warUrl);
            }
            catch (Exception e)
            {
                throw new DeploymentException(e);
            }
            finally
            {
                _deployed.remove(warUrl);
            }
        }
    }
   
    
    /**
     * Work around broken JarURLConnection caching...
     * @param url
     * @return
     */
    static String fixURL(String url)
    {
        String fixedUrl = url;
        
        // Get the separator of the JAR URL and the file reference
        int index = url.indexOf('!');
        if (index >= 0)
            index = url.lastIndexOf('/', index);
        else
            index = url.lastIndexOf('/');
       
        // If there is at least one forward slash, add a "/." before the JAR file 
        // change the path just slightly. Otherwise, the url is malformed, but
        // we will ignore that.
        if (index >= 0)
            fixedUrl = url.substring(0, index) + "/." + url.substring(index);

        return fixedUrl;
    } 
    
    public void setDistributableSessionManagerPrototype(SessionManager manager)
    {
        throw new UnsupportedOperationException("NOT SUPPORTED - please ask");
//        _distributableSessionManagerPrototype = manager;
    }

    public SessionManager getDistributableSessionManagerPrototype()
    {
        return _distributableSessionManagerPrototype;
    }
    
    public boolean getForceDistributable()
    {
        return _forceDistributable;
    }

    public void setForceDistributable(boolean distributable)
    {
        _forceDistributable = distributable;
    }
}
