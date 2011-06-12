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

package org.mortbay.jetty.plus.jaas.spi;
import java.sql.Connection;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.sql.DataSource;
// ========================================================================
// $Id: DataSourceLoginModule.java 3462 2008-07-31 04:12:51Z gregw $
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

/**
 * DataSourceLoginModule
 *
 * A LoginModule that uses a DataSource to retrieve user authentication
 * and authorisation information.
 * 
 * @see org.mortbay.jetty.plus.jaas.spi.JDBCLoginModule
 *
 */
public class DataSourceLoginModule extends AbstractDatabaseLoginModule
{

    private String dbJNDIName;
    private DataSource dataSource;
    
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
            
            //get the datasource jndi name
            dbJNDIName = (String)options.get("dbJNDIName");
            
            InitialContext ic = new InitialContext();
            dataSource = (DataSource)ic.lookup("java:comp/env/"+dbJNDIName);
        }
        catch (NamingException e)
        {
            throw new IllegalStateException (e.toString());
        }
    }


    /** 
     * Get a connection from the DataSource
     * @see org.mortbay.jetty.plus.jaas.spi.AbstractDatabaseLoginModule#getConnection()
     * @return
     * @throws Exception
     */
    public Connection getConnection ()
    throws Exception
    {
        return dataSource.getConnection();
    }


    
  

}
