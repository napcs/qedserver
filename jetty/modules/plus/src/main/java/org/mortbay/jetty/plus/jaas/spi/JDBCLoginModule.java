// ========================================================================
// $Id: JDBCLoginModule.java 478 2006-04-23 22:00:17Z gregw $
// Copyright 2002-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plus.jaas.spi;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

import org.mortbay.log.Log;
import org.mortbay.util.Loader;



/* ---------------------------------------------------- */
/** JDBCLoginModule
 * <p>JAAS LoginModule to retrieve user information from
 *  a database and authenticate the user.
 *
 * <p><h4>Notes</h4>
 * <p>This version uses plain old JDBC connections NOT
 * Datasources.
 *
 * <p><h4>Usage</h4>
 * <pre>
 */
/*
 * </pre>
 *
 * @see 
 * @version 1.0 Tue Apr 15 2003
 * @author Jan Bartel (janb)
 */
public class JDBCLoginModule extends AbstractDatabaseLoginModule
{
    private String dbDriver;
    private String dbUrl;
    private String dbUserName;
    private String dbPassword;

    

  

    
    /** 
     * Get a connection from the DriverManager
     * @see org.mortbay.jetty.plus.jaas.spi.AbstractDatabaseLoginModule#getConnection()
     * @return
     * @throws Exception
     */
    public Connection getConnection ()
    throws Exception
    {
        if (!((dbDriver != null)
                &&
                (dbUrl != null)))
            throw new IllegalStateException ("Database connection information not configured");
        
        if(Log.isDebugEnabled())Log.debug("Connecting using dbDriver="+dbDriver+"+ dbUserName="+dbUserName+", dbPassword="+dbUrl);
        
        return DriverManager.getConnection (dbUrl,
                dbUserName,
                dbPassword);
    }
   
   
    
    /* ------------------------------------------------ */
    /** Init LoginModule.
     * Called once by JAAS after new instance created.
     * @param subject 
     * @param callbackHandler 
     * @param sharedState 
     * @param options 
     */
    public void initialize(Subject subject,
                           CallbackHandler callbackHandler,
                           Map sharedState,
                           Map options)
    {
        try
        {
            super.initialize(subject, callbackHandler, sharedState, options);
            
            //get the jdbc  username/password, jdbc url out of the options
            dbDriver = (String)options.get("dbDriver");
            dbUrl = (String)options.get("dbUrl");
            dbUserName = (String)options.get("dbUserName");
            dbPassword = (String)options.get("dbPassword");

            if (dbUserName == null)
                dbUserName = "";

            if (dbPassword == null)
                dbPassword = "";
            
            if (dbDriver != null)
                Loader.loadClass(this.getClass(), dbDriver).newInstance();
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalStateException (e.toString());
        }
        catch (InstantiationException e)
        {
            throw new IllegalStateException (e.toString());
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException (e.toString());
        }
    }
}
