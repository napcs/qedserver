// ========================================================================
// $Id: TestJAASUserRealm.java 3680 2008-09-21 10:37:13Z janb $
// Copyright 2003-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plus.jaas;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Properties;
import java.util.Random;

import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.mortbay.jetty.Request;


/* ---------------------------------------------------- */
/** TestJAASUserRealm
 * <p> Test JAAS in Jetty - relies on the JDBCUserRealm.
 *
 * <p><h4>Notes</h4>
 * <p>
 *
 * <p><h4>Usage</h4>
 * <pre>
 */
/*
 * </pre>
 *
 * @see
 * @version 1.0 Mon Apr 28 2003
 * @author Jan Bartel (janb)
 */
public class TestJAASUserRealm extends TestCase

{
    private static boolean setupDone = false;
    private Random random = new Random();
   
    
    public TestJAASUserRealm(String name)
    throws Exception
    {
        super (name);
        
     
    }

    public static Test suite()
    {
        return new TestSuite(TestJAASUserRealm.class);
    }

    public void setUp ()
    throws Exception
    {
        if (setupDone)
            return;
        
        
        //set up the properties 
        File propsFile = File.createTempFile("props", null);
        propsFile.deleteOnExit();
        Properties props = new Properties ();
        props.put("user", "user,user,pleb");
        FileOutputStream fout=new FileOutputStream(propsFile);
        props.store(fout, "");
        fout.close();

        
 
        
        //set up config
        File configFile = File.createTempFile ("loginConf", null);
        configFile.deleteOnExit();
        PrintWriter writer = new PrintWriter(new FileWriter(configFile));
        writer.println ("props {");
        writer.println ("org.mortbay.jetty.plus.jaas.spi.PropertyFileLoginModule required");     
        writer.println ("debug=\"true\"");
        writer.println ("file=\""+propsFile.getCanonicalPath().replace('\\','/') +"\";");
        writer.println ("};");
        writer.println ("ds {");
        writer.println ("org.mortbay.jetty.plus.jaas.spi.DataSourceLoginModule required");
        writer.println ("debug=\"true\"");
        writer.println ("dbJNDIName=\"ds\"");
        writer.println ("userTable=\"myusers\"");
        writer.println ("userField=\"myuser\"");
        writer.println ("credentialField=\"mypassword\"");
        writer.println ("userRoleTable=\"myuserroles\"");
        writer.println ("userRoleUserField=\"myuser\"");
        writer.println ("userRoleRoleField=\"myrole\";");
        writer.println ("};");
        writer.flush();
        writer.close();
        
        BufferedReader reader = new BufferedReader(new FileReader(configFile));
        String s = "";
        for (s = reader.readLine(); (s != null); s = reader.readLine())
        {
            System.out.println (s);
        }
        
        //create a login module config file
        System.setProperty ("java.security.auth.login.config", configFile.toURL().toExternalForm());
        setupDone = true;
    }

    
    public void testItDataSource ()
    throws Exception
    {
        String tmpDir = System.getProperty("java.io.tmpdir")+System.getProperty("file.separator");
        System.setProperty("derby.system.home", tmpDir);
        String dbname = "derby-"+(int)(random.nextDouble()*10000);
        
        EmbeddedDataSource eds = new EmbeddedDataSource();
        
        Context comp = null;
        Context env = null;
        try
        {
            //make the java:comp/env
            InitialContext ic = new InitialContext();
            comp = (Context)ic.lookup("java:comp");
            env = comp.createSubcontext ("env");
            
            //make a DataSource    
            eds.setDatabaseName(dbname);          
            eds.setCreateDatabase("create");
            
                        
            env.createSubcontext("jdbc");
            env.bind("ds", eds);
            
            
            Connection connection = eds.getConnection();
          
            
            //create tables
            String sql = "create table myusers (myuser varchar(32) PRIMARY KEY, mypassword varchar(32))";
            Statement createStatement = connection.createStatement();
            createStatement.executeUpdate (sql);
            
            sql = " create table myuserroles (myuser varchar(32), myrole varchar(32))";
            createStatement.executeUpdate (sql);
            createStatement.close();
            
            //insert test users and roles
            sql = "insert into myusers (myuser, mypassword) values (?, ?)";
            
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString (1, "me");
            statement.setString (2, "me");
            
            statement.executeUpdate();
            sql = "insert into myuserroles (myuser, myrole) values ( ? , ? )";
            statement = connection.prepareStatement (sql);
            statement.setString (1, "me");
            statement.setString (2, "roleA");
            statement.executeUpdate();
            
            statement.setString(1, "me");
            statement.setString(2, "roleB");
            statement.executeUpdate();
            
            statement.close();
            connection.close();
            
            
            //create a JAASUserRealm
            JAASUserRealm realm = new JAASUserRealm ("testRealm");
            
            realm.setLoginModuleName ("ds");
            
            
            JAASUserPrincipal userPrincipal = (JAASUserPrincipal)realm.authenticate ("me", "blah",(Request)null);
            assertNull (userPrincipal);
            
            userPrincipal = (JAASUserPrincipal)realm.authenticate ("me", "me", (Request)null);
            
            assertNotNull (userPrincipal);
            assertNotNull (userPrincipal.getName());
            assertTrue (userPrincipal.getName().equals("me"));
            
            assertTrue (userPrincipal.isUserInRole("roleA"));
            assertTrue (userPrincipal.isUserInRole("roleB"));
            assertTrue (!userPrincipal.isUserInRole("roleC"));
            
            realm.pushRole (userPrincipal, "roleC");
            assertTrue (userPrincipal.isUserInRole("roleC"));
            assertTrue (!userPrincipal.isUserInRole("roleA"));
            assertTrue (!userPrincipal.isUserInRole("roleB"));
            
            realm.pushRole (userPrincipal, "roleD");
            assertTrue (userPrincipal.isUserInRole("roleD"));
            assertTrue (!userPrincipal.isUserInRole("roleC"));
            assertTrue (!userPrincipal.isUserInRole("roleA"));
            assertTrue (!userPrincipal.isUserInRole("roleB"));
            
            realm.popRole(userPrincipal);
            assertTrue (userPrincipal.isUserInRole("roleC"));
            assertTrue (!userPrincipal.isUserInRole("roleA"));
            assertTrue (!userPrincipal.isUserInRole("roleB"));
            
            realm.popRole(userPrincipal);
            assertTrue (!userPrincipal.isUserInRole("roleC"));
            assertTrue (userPrincipal.isUserInRole("roleA"));
            
            realm.disassociate(userPrincipal);
        }
        finally
        {
            comp.destroySubcontext("env");
            try
            {
                Connection c = eds.getConnection();
                Statement s = c.createStatement();
                s.executeUpdate("drop table myusers");
                s.executeUpdate("drop table myuserroles");
                s.close();
                c.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    
    
    public void testItPropertyFile ()
        throws Exception
    {
        //create a JAASUserRealm
        JAASUserRealm realm = new JAASUserRealm ("props");
        realm.setLoginModuleName ("props");

        JAASUserPrincipal userPrincipal = (JAASUserPrincipal)realm.authenticate ("user", "wrong",(Request)null);
        assertNull (userPrincipal);
        
        userPrincipal = (JAASUserPrincipal)realm.authenticate ("user", "user", (Request)null);

        assertNotNull (userPrincipal);
        assertTrue (userPrincipal.getName().equals("user"));

        assertTrue (userPrincipal.isUserInRole("pleb"));
        assertTrue (userPrincipal.isUserInRole("user"));
        assertTrue (!userPrincipal.isUserInRole("other"));       
       
        realm.disassociate (userPrincipal);  
    }

    public void tearDown ()
        throws Exception
    {
       
    }
    
    
}
