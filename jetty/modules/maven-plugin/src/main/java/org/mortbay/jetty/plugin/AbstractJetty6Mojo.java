//========================================================================
//$Id: AbstractJetty6Mojo.java 2147 2007-10-23 05:08:49Z gregw $
//Copyright 2000-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plugin;

import java.io.File;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.RequestLog;
import org.mortbay.jetty.plugin.util.JettyPluginServer;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.xml.XmlConfiguration;

/**
 * AbstractJetty6Mojo
 *
 * Base class for all jetty6 mojos.
 *
 */
public abstract class AbstractJetty6Mojo extends AbstractJettyMojo
{
    
    /**
     * List of connectors to use. If none are configured
     * then we use a single SelectChannelConnector at port 8080
     * 
     * @parameter 
     */
    private Connector[] connectors;
    
    
    /**
     * List of security realms to set up. Optional.
     * @parameter
     */
    private UserRealm[] userRealms;
    
    

    /**
     * A RequestLog implementation to use for the webapp at runtime.
     * Optional.
     * @parameter
     */
    private RequestLog requestLog;
    
    
    
    /**
     * @see org.mortbay.jetty.plugin.AbstractJettyMojo#getConfiguredUserRealms()
     */
    public Object[] getConfiguredUserRealms()
    {
        return this.userRealms;
    }

    /**
     * @see org.mortbay.jetty.plugin.AbstractJettyMojo#getConfiguredConnectors()
     */
    public Object[] getConfiguredConnectors()
    {
        return this.connectors;
    }


    public Object getConfiguredRequestLog()
    {
        return this.requestLog;
    }
 
    
    public void applyJettyXml() throws Exception
    {
        
        if (getJettyXmlFile() == null)
            return;
        
        getLog().info( "Configuring Jetty from xml configuration file = " + getJettyXmlFile() );        
        XmlConfiguration xmlConfiguration = new XmlConfiguration(getJettyXmlFile().toURL());
        xmlConfiguration.configure(getServer().getProxiedObject());   
    }

   
    /**
     * @see org.mortbay.jetty.plugin.AbstractJettyMojo#createServer()
     */
    public JettyPluginServer createServer() throws Exception
    {
        return new Jetty6PluginServer();
    }

}
