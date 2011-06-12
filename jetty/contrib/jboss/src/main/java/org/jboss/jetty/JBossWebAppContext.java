//========================================================================
//$Id: $
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
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
package org.jboss.jetty;



import org.jboss.jetty.security.JBossUserRealm;
import org.jboss.logging.Logger;
import org.jboss.web.WebApplication;
import org.jboss.web.AbstractWebContainer.WebDescriptorParser;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.jetty.servlet.SessionHandler;
import org.mortbay.jetty.servlet.jsr77.Jsr77ServletHandler;
import org.mortbay.jetty.webapp.WebAppClassLoader;



/**
 * JBossWebApplicationContext
 *
 * Customize the jetty WebAppContext to jboss environment.
 *
 */
public class JBossWebAppContext extends WebAppContext
{
    protected static Logger __log=Logger.getLogger(JBossWebAppContext.class);

    protected WebDescriptorParser _descriptorParser;
    protected WebApplication _webApp;
    private String _subjAttrName="j_subject";//TODO what was this doing here?
    private JBossUserRealm _realm=null;
    // this is a hack - but we need the session timeout - in case we are
    // going to use a distributable session manager....
    protected boolean _timeOutPresent=false;
    protected int _timeOutMinutes=0;

    
    /**
     * Constructor
     * @param descriptorParser
     * @param webApp
     * @param warUrl
     * @throws Exception
     */
    public JBossWebAppContext(WebDescriptorParser descriptorParser,WebApplication webApp, String warUrl) 
    throws Exception
    {
        super(null,new SessionHandler(), new Jsr77ServletHandler(), null);
        setWar(warUrl);
        ((Jsr77ServletHandler)getServletHandler()).setWebAppContext(this);
        _descriptorParser=descriptorParser;
        _webApp=webApp;
        //very important - establish the classloader now, as it is the one
        //that is being used for the performDeploy step
        ClassLoader loader=Thread.currentThread().getContextClassLoader();
       /* if(isDistributable()&&getDistributableSessionManager()!=null)
            setUpDistributableSessionManager(loader);  */
        
        setClassLoader(new WebAppClassLoader(loader, this));
    }


    /* ------------------------------------------------------------ */
    public void doStop() throws Exception
    {
        super.doStop();
        _descriptorParser=null;
        _webApp=null;
        _subjAttrName=null;
        _realm = null;
    }


    public void setRealm (JBossUserRealm realm)
    {
        _realm = realm;
    }
    
    public JBossUserRealm getRealm ()
    {
        return _realm;
    }
    
    public void setSubjectAttribute (String subjAttr)
    {
        _subjAttrName = subjAttr;
    }
    
    
    public String getSubjectAttribute ()
    {
        return _subjAttrName;
    }

    public String getUniqueName ()
    {
        return _descriptorParser.getDeploymentInfo().getCanonicalName();
    }

    protected void startContext ()
    throws Exception
    {
        //set up the java:comp/env namespace so that it can be refered to
        //in other parts of the startup
        setUpENC(getClassLoader());      
        super.startContext();
        
        //ensure there is always a realm
        //this is primarily so that jboss's webservices impl will work, as it
        //sets up a SecurityAssociation that can only be cleared by jetty if
        //there is a realm
        if (_realm == null)
        {
            _realm = new JBossUserRealm("other", getSubjectAttribute());
            getSecurityHandler().setUserRealm(_realm);
        }

        //start the realm from within the webapp's classloader as it wants
        //to do JNDI lookups
        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try
        {
            _realm.init();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(currentLoader);
        }
    }
    /*
    protected void setUpDistributableSessionManager(ClassLoader loader)
    {
        try
        {
            Manager sm=(Manager)getDistributableSessionManager();
            Store store=sm.getStore();
            if(store instanceof AbstractReplicatedStore)
                ((AbstractReplicatedStore)store).setLoader(loader);
            if(_timeOutPresent)
                sm.setMaxInactiveInterval(_timeOutMinutes*60);
            getSessionHandler().setSessionManager(sm);
        }
        catch(Exception e)
        {
            __log.error("could not set up Distributable HttpSession Manager - using local one",e);
        }
    }
*/
    protected void setUpENC(ClassLoader loader) throws Exception
    {
        _webApp.setClassLoader(loader);
        _webApp.setName(getDisplayName());
        _webApp.setAppData(this);
        __log.debug("setting up ENC...");
        _descriptorParser.parseWebAppDescriptors(loader,_webApp.getMetaData());
        __log.debug("setting up ENC succeeded");
    }
}
