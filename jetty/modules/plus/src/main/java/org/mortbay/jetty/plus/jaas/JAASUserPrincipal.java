// ========================================================================
// $Id: JAASUserPrincipal.java 1001 2006-09-23 09:31:51Z janb $
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

package org.mortbay.jetty.plus.jaas;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Stack;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;



/* ---------------------------------------------------- */
/** JAASUserPrincipal
 * <p>Implements the JAAS version of the 
 *  org.mortbay.http.UserPrincipal interface.
 *
 * @version $Id: JAASUserPrincipal.java 1001 2006-09-23 09:31:51Z janb $
 * @author Jan Bartel (janb)
 */
public class JAASUserPrincipal implements Principal 
{

    
    /* ------------------------------------------------ */
    /** RoleStack
     * <P>
     *
     */
    public static class RoleStack
    {
        private static ThreadLocal local = new ThreadLocal();
        

        public static boolean empty ()
        {
            Stack s = (Stack)local.get();

            if (s == null)
                return false;

            return s.empty();
        }
        


        public static void push (JAASRole role)
        {
            Stack s = (Stack)local.get();

            if (s == null)
            {
                s = new Stack();
                local.set (s);
            }

            s.push (role);
        }


        public static void pop ()
        {
            Stack s = (Stack)local.get();

            if ((s == null) || s.empty())
                return;

            s.pop();
        }

        public static JAASRole peek ()
        {
            Stack s = (Stack)local.get();
            
            if ((s == null) || (s.empty()))
                return null;
            
            
            return (JAASRole)s.peek();
        }
        
        public static void clear ()
        {
            Stack s = (Stack)local.get();

            if ((s == null) || (s.empty()))
                return;

            s.clear();
        }
        
    }

    private Subject subject = null;
    private JAASUserRealm realm = null;
    private static RoleStack runAsRoles = new RoleStack();
    private RoleCheckPolicy roleCheckPolicy = null;
    private String name = null;
    private LoginContext loginContext = null;
    

    
    
    
    /* ------------------------------------------------ */
    /** Constructor. 
     * @param name the name identifying the user
     */
    public JAASUserPrincipal(JAASUserRealm realm, String name)
    {
        this.name = name;
        this.realm = realm;
    }
    
    
    public JAASUserRealm getRealm()
    {
        return this.realm;
    }

    /* ------------------------------------------------ */
    /** Check if user is in role
     * @param roleName role to check
     * @return true or false accordint to the RoleCheckPolicy.
     */
    public boolean isUserInRole (String roleName)
    {
        if (roleCheckPolicy == null)
            roleCheckPolicy = new StrictRoleCheckPolicy();
        

        return roleCheckPolicy.checkRole (roleName,
                                          runAsRoles.peek(),
                                          getRoles());
    }

    
    /* ------------------------------------------------ */
    /** Determine the roles that the LoginModule has set
     * @return  A {@link Group} of {@link Principal Principals} representing the roles this user holds
     */
    public Group getRoles ()
    {
        return getRealm().getRoles(this);
    }

    /* ------------------------------------------------ */
    /** Set the type of checking for isUserInRole
     * @param policy 
     */
    public void setRoleCheckPolicy (RoleCheckPolicy policy)
    {
        roleCheckPolicy = policy;
    }
    

    /* ------------------------------------------------ */
    /** Temporarily associate a user with a role.
     * @param roleName 
     */
    public void pushRole (String roleName)
    {
        runAsRoles.push (new JAASRole(roleName));
    }

    
    /* ------------------------------------------------ */
    /** Remove temporary association between user and role.
     */
    public void popRole ()
    {
        runAsRoles.pop ();
    }


    /* ------------------------------------------------ */
    /** Clean out any pushed roles that haven't been popped
     */
    public void disassociate ()
    {
        runAsRoles.clear();
    }


    /* ------------------------------------------------ */
    /** Get the name identifying the user
     */
    public String getName ()
    {
        return name;
    }
    
    
    /* ------------------------------------------------ */
    /** Sets the JAAS subject for this user.
     *  The subject contains:
     * <ul>
     * <li> the user's credentials
     * <li> Principal for the user's roles
     * @param subject 
     */
    protected void setSubject (Subject subject)
    {
        this.subject = subject;
    }
    
    /* ------------------------------------------------ */
    /** Provide access to the current Subject
     */
    public Subject getSubject ()
    {
        return this.subject;
    }
    
    protected void setLoginContext (LoginContext loginContext)
    {
        this.loginContext = loginContext;
    }
    
    protected LoginContext getLoginContext ()
    {
        return this.loginContext;
    }
    
    public String toString()
    {
        return getName();
    }
    
}
