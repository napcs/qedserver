//========================================================================
//$Id: Configuration.java,v 1.2 2005/10/26 20:48:48 gregwilkins Exp $
//Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

import java.io.Serializable;

/* ------------------------------------------------------------------------------- */
/** Base Class for WebApplicationContext Configuration.
 * This class can be extended to customize or extend the configuration
 * of the WebApplicationContext.  If WebApplicationContext.setConfiguration is not
 * called, then an XMLConfiguration instance is created.
 * 
 * @author gregw
 */
public interface Configuration extends Serializable
{
    /* ------------------------------------------------------------------------------- */
    /** Set up a context on which to perform the configuration.
     * @param context
     */
    public void setWebAppContext (WebAppContext context);
    
    /* ------------------------------------------------------------------------------- */
    /** Get the context on which the configuration is performed.
     */
    public WebAppContext getWebAppContext();
    
    /* ------------------------------------------------------------------------------- */
    /** Configure ClassPath.
     * This method is called to configure the context ClassLoader.  It is called just
     * after a new WebAppClassLoader is constructed and before it has been used.
     * Class paths may be added, options changed or the loader totally replaced. 
     * @throws Exception
     */
    public void configureClassLoader()
    throws Exception;
    
    /* ------------------------------------------------------------------------------- */
    /** Configure Defaults.
     * This method is called to intialize the context to the containers default configuration.
     * Typically this would mean application of the webdefault.xml file. 
     * @throws Exception
     */
    public  void configureDefaults()
    throws Exception;
    
    
    /* ------------------------------------------------------------------------------- */
    /** Configure WebApp.
     * This method is called to apply the standard and vendor deployment descriptors.
     * Typically this is web.xml and jetty-web.xml.  
     * @throws Exception
     */
    public  void configureWebApp()
    throws Exception;

    /* ------------------------------------------------------------------------------- */
    /** DeConfigure WebApp.
     * This method is called to undo all configuration done to this webapphandler. This is
     * called to allow the context to work correctly over a stop/start cycle
     * @throws Exception
     */
    public  void deconfigureWebApp()
    throws Exception;
    
    
}
