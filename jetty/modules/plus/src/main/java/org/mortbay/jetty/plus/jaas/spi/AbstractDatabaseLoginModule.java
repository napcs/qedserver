// ========================================================================
// $Id: AbstractDatabaseLoginModule.java 3463 2008-07-31 04:39:59Z dyu $
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

package org.mortbay.jetty.plus.jaas.spi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

import org.mortbay.jetty.security.Credential;
import org.mortbay.log.Log;

/**
 * AbstractDatabaseLoginModule
 *
 * Abstract base class for LoginModules that interact with a 
 * database to retrieve authentication and authorization information.
 * Used by the JDBCLoginModule and DataSourceLoginModule.
 *
 */
public abstract class AbstractDatabaseLoginModule extends AbstractLoginModule
{
    private String userQuery;
    private String rolesQuery;
    private String dbUserTable;
    private String dbUserTableUserField;
    private String dbUserTableCredentialField;
    private String dbUserRoleTable;
    private String dbUserRoleTableUserField;
    private String dbUserRoleTableRoleField;
    
    
    
    
    /**
     * @return a java.sql.Connection from the database
     * @throws Exception
     */
    public abstract Connection getConnection () throws Exception;
    
   
    
    /* ------------------------------------------------ */
    /** Load info from database
     * @param userName user info to load
     * @exception SQLException 
     */
    public UserInfo getUserInfo (String userName)
        throws Exception
    {
        Connection connection = null;
        
        try
        {
            connection = getConnection();
            
            //query for credential
            PreparedStatement statement = connection.prepareStatement (userQuery);
            statement.setString (1, userName);
            ResultSet results = statement.executeQuery();
            String dbCredential = null;
            if (results.next())
            {
                dbCredential = results.getString(1);
            }
            results.close();
            statement.close();
            
            //query for role names
            statement = connection.prepareStatement (rolesQuery);
            statement.setString (1, userName);
            results = statement.executeQuery();
            List roles = new ArrayList();
            
            while (results.next())
            {
                String roleName = results.getString (1);
                roles.add (roleName);
            }
            
            results.close();
            statement.close();
            
            return dbCredential==null ? null : new UserInfo (userName, 
                    Credential.getCredential(dbCredential), roles);
        }
        finally
        {
            if (connection != null) connection.close();
        }
    }
    

    public void initialize(Subject subject,
            CallbackHandler callbackHandler,
            Map sharedState,
            Map options)
    {
        super.initialize(subject, callbackHandler, sharedState, options);
        
        //get the user credential query out of the options
        dbUserTable = (String)options.get("userTable");
        dbUserTableUserField = (String)options.get("userField");
        dbUserTableCredentialField = (String)options.get("credentialField");
        
        userQuery = "select "+dbUserTableCredentialField+" from "+dbUserTable+" where "+dbUserTableUserField+"=?";
        
        
        //get the user roles query out of the options
        dbUserRoleTable = (String)options.get("userRoleTable");
        dbUserRoleTableUserField = (String)options.get("userRoleUserField");
        dbUserRoleTableRoleField = (String)options.get("userRoleRoleField");
        
        rolesQuery = "select "+dbUserRoleTableRoleField+" from "+dbUserRoleTable+" where "+dbUserRoleTableUserField+"=?";
        
        if(Log.isDebugEnabled())Log.debug("userQuery = "+userQuery);
        if(Log.isDebugEnabled())Log.debug("rolesQuery = "+rolesQuery);
    }
}
