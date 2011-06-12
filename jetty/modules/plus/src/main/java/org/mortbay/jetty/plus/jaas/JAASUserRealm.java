// ========================================================================
// $Id: JAASUserRealm.java 5888 2010-03-22 22:37:15Z gregw $
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

import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.plus.jaas.callback.AbstractCallbackHandler;
import org.mortbay.jetty.plus.jaas.callback.DefaultCallbackHandler;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.log.Log;
import org.mortbay.util.Loader;




/* ---------------------------------------------------- */
/** JAASUserRealm
 * <p>
 *
 * <p><h4>Notes</h4>
 * <p>
 *
 * <p><h4>Usage</h4>
 * 
 *
 *
 * 
 * @org.apache.xbean.XBean element="jaasUserRealm" description="Creates a UserRealm suitable for use with JAAS"
 */
public class JAASUserRealm implements UserRealm
{
    public static String DEFAULT_ROLE_CLASS_NAME = "org.mortbay.jetty.plus.jaas.JAASRole";
    public static String[] DEFAULT_ROLE_CLASS_NAMES = {DEFAULT_ROLE_CLASS_NAME};
	
    protected String[] roleClassNames = DEFAULT_ROLE_CLASS_NAMES;
    protected String callbackHandlerClass;
    protected String realmName;
    protected String loginModuleName;
    protected RoleCheckPolicy roleCheckPolicy;
    protected JAASUserPrincipal defaultUser = new JAASUserPrincipal(null, null);
    
 

    /* ---------------------------------------------------- */
    /**
     * Constructor.
     *
     */
    public JAASUserRealm ()
    {
    }
    

    /* ---------------------------------------------------- */
    /**
     * Constructor.
     *
     * @param name the name of the realm
     */
    public JAASUserRealm(String name)
    {
        this();
        realmName = name;
    }


    /* ---------------------------------------------------- */
    /**
     * Get the name of the realm.
     *
     * @return name or null if not set.
     */
    public String getName()
    {
        return realmName;
    }


    /* ---------------------------------------------------- */
    /**
     * Set the name of the realm
     *
     * @param name a <code>String</code> value
     */
    public void setName (String name)
    {
        realmName = name;
    }



    /**
     * Set the name to use to index into the config
     * file of LoginModules.
     *
     * @param name a <code>String</code> value
     */
    public void setLoginModuleName (String name)
    {
        loginModuleName = name;
    }


    public void setCallbackHandlerClass (String classname)
    {
        callbackHandlerClass = classname;
    }
    
    public void setRoleClassNames (String[] classnames)
    {
        ArrayList tmp = new ArrayList();
        
        if (classnames != null)
            tmp.addAll(Arrays.asList(classnames));
         
        if (!tmp.contains(DEFAULT_ROLE_CLASS_NAME))
            tmp.add(DEFAULT_ROLE_CLASS_NAME);
        roleClassNames = (String[])tmp.toArray(new String[tmp.size()]);
    }

    public String[] getRoleClassNames()
    {
        return roleClassNames;
    }
    
    public void setRoleCheckPolicy (RoleCheckPolicy policy)
    {
        roleCheckPolicy = policy;
    }

    //TODO: delete?!
    public Principal getPrincipal(String username)
    {
        return null;
    }


    /* ------------------------------------------------------------ */
    public boolean isUserInRole(Principal user, String role)
    {
        JAASUserPrincipal thePrincipal = null;
        
        if (user == null)
            thePrincipal = defaultUser;
        else
        {
            if (! (user instanceof JAASUserPrincipal))
                return false;
            
            thePrincipal = (JAASUserPrincipal)user;
        }
        return thePrincipal!=null && thePrincipal.isUserInRole(role);
    }


    /* ------------------------------------------------------------ */
    public boolean reauthenticate(Principal user)
    {
        if (user instanceof JAASUserPrincipal)
            return true;
        else
            return false;
    }

    
    /* ---------------------------------------------------- */
    /**
     * Authenticate a user.
     * 
     *
     * @param username provided by the user at login
     * @param credentials provided by the user at login
     * @param request a <code>Request</code> value
     * @return authenticated JAASUserPrincipal or  null if authenticated failed
     */
    public Principal authenticate(String username,
            Object credentials,
            Request request)
    {
        try
        {              
            AbstractCallbackHandler callbackHandler = null;
            
            //user has not been authenticated
            if (callbackHandlerClass == null)
            {
                Log.warn("No CallbackHandler configured: using DefaultCallbackHandler");
                callbackHandler = new DefaultCallbackHandler();
            }
            else
            {
                callbackHandler = (AbstractCallbackHandler)Loader.loadClass(JAASUserRealm.class, callbackHandlerClass).getConstructors()[0].newInstance(new Object[0]);
            }
            
            if (callbackHandler instanceof DefaultCallbackHandler)
            {
                ((DefaultCallbackHandler)callbackHandler).setRequest (request);
            }
            
            callbackHandler.setUserName(username);
            callbackHandler.setCredential(credentials);
            
            
            //set up the login context
            LoginContext loginContext = new LoginContext(loginModuleName,
                    callbackHandler);
            
            loginContext.login();
            
            //login success
            JAASUserPrincipal userPrincipal = new JAASUserPrincipal(this, username);
            userPrincipal.setSubject(loginContext.getSubject());
            userPrincipal.setRoleCheckPolicy (roleCheckPolicy);
            userPrincipal.setLoginContext(loginContext);
            
            
            
            return userPrincipal;       
        }
        catch (Exception e)
        {
            Log.warn(e.toString());
            Log.debug(e);
            return null;
        }     
    }

    

    /* ---------------------------------------------------- */
    /**
     * Removes any auth info associated with eg. the thread.
     *
     * @param user a UserPrincipal to disassociate
     */
    public void disassociate(Principal user)
    {
        //TODO: should this apply to the default user?
        if (user == null)
            defaultUser.disassociate();
        else
            ((JAASUserPrincipal)user).disassociate();
    }

    

    /* ---------------------------------------------------- */
    /**
     * Temporarily adds a role to a user.
     *
     * Temporarily granting a role pushes the role onto a stack
     * of temporary roles. Temporary roles must therefore be
     * removed in order.
     *
     * @param user the Principal to which to add the role
     * @param role the role name
     * @return the Principal with the role added
     */
    public Principal pushRole(Principal user, String role)
    {
        JAASUserPrincipal thePrincipal = (JAASUserPrincipal)user;
        
        //use the default user
        if (thePrincipal == null)
            thePrincipal = defaultUser;
        
    
        thePrincipal.pushRole(role);
        return thePrincipal;
    }
    
    /* ------------------------------------------------------------ */
    public Principal popRole(Principal user)
    {
        JAASUserPrincipal thePrincipal = (JAASUserPrincipal)user;
        
        //use the default user
        if (thePrincipal == null)
            thePrincipal = defaultUser;
        
        thePrincipal.popRole();
        return thePrincipal;
    }


    public Group getRoles (JAASUserPrincipal principal)
    {
        //get all the roles of the various types
        String[] roleClassNames = getRoleClassNames();
        Group roleGroup = new JAASGroup(JAASGroup.ROLES);
        
        try
        {
            JAASUserPrincipal thePrincipal = principal;
            
            if (thePrincipal == null)
                thePrincipal = defaultUser;
            
            for (int i=0; i<roleClassNames.length;i++)
            {
                Class load_class=Thread.currentThread().getContextClassLoader().loadClass(roleClassNames[i]);
                Set rolesForType = thePrincipal.getSubject().getPrincipals (load_class);
                Iterator itor = rolesForType.iterator();
                while (itor.hasNext())
                {
                    roleGroup.addMember((Principal) itor.next());
                }
            }
            
            return roleGroup;
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }


    /* ---------------------------------------------------- */
    /**
     * Logout a previously logged in user.
     * This can only work for FORM authentication
     * as BasicAuthentication is stateless.
     * 
     * The user's LoginContext logout() method is called.
     * @param user an <code>Principal</code> value
     */
    public void logout(Principal user)
    {
        try
        {
            JAASUserPrincipal authenticUser = null;
            
            if (user == null)
                authenticUser = defaultUser; //TODO: should the default user ever be logged in?
            
            if (!(user instanceof JAASUserPrincipal))
                throw new IllegalArgumentException (user + " is not a JAASUserPrincipal");
            
 
            authenticUser = (JAASUserPrincipal)user;
 
            authenticUser.getLoginContext().logout();
            
            Log.debug (user+" has been LOGGED OUT");
        }
        catch (LoginException e)
        {
            Log.warn (e);
        }
    }


   
    
}
