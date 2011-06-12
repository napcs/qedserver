//========================================================================
//$Id: JettyWebXmlConfiguration.java,v 1.5 2005/11/19 00:32:42 gregwilkins Exp $
//Copyright 2000-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.webapp;

import org.mortbay.log.Log;
import org.mortbay.resource.Resource;
import org.mortbay.xml.XmlConfiguration;


/**
 * 
 * JettyWebConfiguration.
 * 
 * Looks for Xmlconfiguration files in WEB-INF.  Searches in order for the first of jetty6-web.xml, jetty-web.xml or web-jetty.xml
 *
 * @author janb
 *
 */
public class JettyWebXmlConfiguration implements Configuration
{
    private WebAppContext _context;

    
    /**
     * @see Configuration#setWebAppContext
     */
    public void setWebAppContext (WebAppContext context)
    {
       _context = context;
    }

    public WebAppContext getWebAppContext ()
    {
        return _context;
    }
    
    /** configureClassPath
     * Not used.
     * @see Configuration#configureClassLoader
     */
    public void configureClassLoader () throws Exception
    {
    }

    /** configureDefaults
     * Not used.
     * @see Configuration#configureDefaults
     */
    public void configureDefaults () throws Exception
    {
    }

    /** configureWebApp
     * Apply web-jetty.xml configuration
     * @see Configuration#configureWebApp()
     */
    public void configureWebApp () throws Exception
    {
        //cannot configure if the _context is already started
        if (_context.isStarted())
        {
            if (Log.isDebugEnabled()){Log.debug("Cannot configure webapp after it is started");}
            return;
        }
        
        if(Log.isDebugEnabled())
            Log.debug("Configuring web-jetty.xml");
        
        Resource web_inf=getWebAppContext().getWebInf();
        // handle any WEB-INF descriptors
        if(web_inf!=null&&web_inf.isDirectory())
        {
            // do jetty.xml file
            Resource jetty=web_inf.addPath("jetty6-web.xml");
            if(!jetty.exists())
                jetty=web_inf.addPath("jetty-web.xml");
            if(!jetty.exists())
                jetty=web_inf.addPath("web-jetty.xml");

            if(jetty.exists())
            {
                // No server classes while configuring 
                String[] old_server_classes = _context.getServerClasses();
                try
                {
                    _context.setServerClasses(null);
                    if(Log.isDebugEnabled())
                        Log.debug("Configure: "+jetty);
                    XmlConfiguration jetty_config=new XmlConfiguration(jetty.getURL());
                    jetty_config.configure(getWebAppContext());
                }
                finally
                {
                    if (_context.getServerClasses()==null)
                        _context.setServerClasses(old_server_classes);
                }
            }
        }
    }
    
    
    /** deconfigureWebApp
     * @see Configuration#deconfigureWebApp()
     */
    public void deconfigureWebApp () throws Exception
    {
    
    }
    
}
