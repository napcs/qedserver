// ========================================================================
// $Id: EnvConfiguration.java 3680 2008-09-21 10:37:13Z janb $
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.jetty.plus.webapp;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.mortbay.jetty.plus.naming.EnvEntry;
import org.mortbay.jetty.plus.naming.NamingEntry;
import org.mortbay.jetty.plus.naming.NamingEntryUtil;
import org.mortbay.jetty.plus.naming.Resource;
import org.mortbay.jetty.plus.naming.Transaction;
import org.mortbay.jetty.webapp.Configuration;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;
import org.mortbay.naming.NamingUtil;

import org.mortbay.xml.XmlConfiguration;

/**
 * EnvConfiguration
 *
 *
 */
public class EnvConfiguration implements Configuration
{
    private WebAppContext webAppContext;
    private Context compCtx;    
    private Context envCtx;
    private URL jettyEnvXmlUrl;

    protected void createEnvContext ()
    throws NamingException
    {
        Context context = new InitialContext();
        compCtx =  (Context)context.lookup ("java:comp");
        envCtx = compCtx.createSubcontext("env");
        if (Log.isDebugEnabled())
            Log.debug("Created java:comp/env for webapp "+getWebAppContext().getContextPath());
    }
    
    
    /** 
     * @see org.mortbay.jetty.webapp.Configuration#setWebAppContext(org.mortbay.jetty.webapp.WebAppContext)
     * @param context
     */
    public void setWebAppContext(WebAppContext context)
    {
        this.webAppContext = context;
    }

    public void setJettyEnvXml (URL url)
    {
        this.jettyEnvXmlUrl = url;
    }
    
    /** 
     * @see org.mortbay.jetty.webapp.Configuration#getWebAppContext()
     */
    public WebAppContext getWebAppContext()
    {
        return webAppContext;
    }

    /** 
     * @see org.mortbay.jetty.webapp.Configuration#configureClassLoader()
     * @throws Exception
     */
    public void configureClassLoader() throws Exception
    {
    }

    /** 
     * @see org.mortbay.jetty.webapp.Configuration#configureDefaults()
     * @throws Exception
     */
    public void configureDefaults() throws Exception
    {        
        //create a java:comp/env
        createEnvContext();
    }

    /** 
     * @see org.mortbay.jetty.webapp.Configuration#configureWebApp()
     * @throws Exception
     */
    public void configureWebApp() throws Exception
    {
        //check to see if an explicit file has been set, if not,
        //look in WEB-INF/jetty-env.xml
        if (jettyEnvXmlUrl == null)
        {
            
            //look for a file called WEB-INF/jetty-env.xml
            //and process it if it exists
            org.mortbay.resource.Resource web_inf = getWebAppContext().getWebInf();
            if(web_inf!=null && web_inf.isDirectory())
            {
                org.mortbay.resource.Resource jettyEnv = web_inf.addPath("jetty-env.xml");
                if(jettyEnv.exists())
                {
                    jettyEnvXmlUrl = jettyEnv.getURL();
                }
            }
        }
        if (jettyEnvXmlUrl != null)
        {
            XmlConfiguration configuration = new XmlConfiguration(jettyEnvXmlUrl);
            configuration.configure(getWebAppContext());
        }
        
        //add java:comp/env entries for any EnvEntries that have been defined so far
        bindEnvEntries();
    }

    /** 
     * Remove all jndi setup
     * @see org.mortbay.jetty.webapp.Configuration#deconfigureWebApp()
     * @throws Exception
     */
    public void deconfigureWebApp() throws Exception
    {
        //get rid of any bindings for comp/env for webapp
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(webAppContext.getClassLoader());
        compCtx.destroySubcontext("env");
        
        //unbind any NamingEntries that were configured in this webapp's name space
        try
        {
            Context scopeContext = NamingEntryUtil.getContextForScope(getWebAppContext());
            scopeContext.destroySubcontext(NamingEntry.__contextName);
        }
        catch (NameNotFoundException e)
        {
            Log.ignore(e);
            Log.debug("No naming entries configured in environment for webapp "+getWebAppContext());
        }
        Thread.currentThread().setContextClassLoader(oldLoader);
    }
    
    /**
     * Bind all EnvEntries that have been declared, so that the processing of the
     * web.xml file can potentially override them.
     * 
     * We first bind EnvEntries declared in Server scope, then WebAppContext scope.
     * @throws NamingException
     */
    public void bindEnvEntries ()
    throws NamingException
    {
        Log.debug("Binding env entries from the jvm scope");
        Object scope = null;
        List list = NamingEntryUtil.lookupNamingEntries(scope, EnvEntry.class);
        Iterator itor = list.iterator();
        while (itor.hasNext())
        {
            EnvEntry ee = (EnvEntry)itor.next();
            ee.bindToENC(ee.getJndiName());
            Name namingEntryName = NamingEntryUtil.makeNamingEntryName(null, ee);
            NamingUtil.bind(envCtx, namingEntryName.toString(), ee);//also save the EnvEntry in the context so we can check it later          
        }
        
        Log.debug("Binding env entries from the server scope");
        
        scope = getWebAppContext().getServer();
        list = NamingEntryUtil.lookupNamingEntries(scope, EnvEntry.class);
        itor = list.iterator();
        while (itor.hasNext())
        {
            EnvEntry ee = (EnvEntry)itor.next();
            ee.bindToENC(ee.getJndiName());
            Name namingEntryName = NamingEntryUtil.makeNamingEntryName(null, ee);
            NamingUtil.bind(envCtx, namingEntryName.toString(), ee);//also save the EnvEntry in the context so we can check it later          
        }
        
        Log.debug("Binding env entries from the context scope");
        scope = getWebAppContext();
        list = NamingEntryUtil.lookupNamingEntries(scope, EnvEntry.class);
        itor = list.iterator();
        while (itor.hasNext())
        {
            EnvEntry ee = (EnvEntry)itor.next();
            ee.bindToENC(ee.getJndiName());
            Name namingEntryName = NamingEntryUtil.makeNamingEntryName(null, ee);
            NamingUtil.bind(envCtx, namingEntryName.toString(), ee);//also save the EnvEntry in the context so we can check it later
        }
    }  
}
