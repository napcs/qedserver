// ========================================================================
// $Id: MailSessionReference.java 1386 2006-12-08 17:53:22Z janb $
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

package org.mortbay.naming.factories;


import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

import org.mortbay.jetty.security.Password;

/**
 * MailSessionReference
 * 
 * This is a subclass of javax.mail.Reference and an ObjectFactory for javax.mail.Session objects.
 * 
 * The subclassing of Reference allows all of the setup for a javax.mail.Session
 * to be captured without necessitating first instantiating a Session object. The
 * reference is bound into JNDI and it is only when the reference is looked up that
 * this object factory will create an instance of javax.mail.Session using the
 * information captured in the Reference.
 *
 */
public class MailSessionReference extends Reference implements ObjectFactory
{
 

    public static class PasswordAuthenticator extends Authenticator
    {
        PasswordAuthentication passwordAuthentication;
        private String user;
        private String password;

        public PasswordAuthenticator()
        {
            
        }
        
        public PasswordAuthenticator(String user, String password)
        {
            passwordAuthentication = new PasswordAuthentication (user, (password.startsWith(Password.__OBFUSCATE)?Password.deobfuscate(password):password));
        }

        public PasswordAuthentication getPasswordAuthentication()
        {
            return passwordAuthentication;
        }
        
        public void setUser (String user)
        {
            this.user = user;
        }
        public String getUser ()
        {
            return this.user;
        }
        
        public String getPassword ()
        {
            return this.password;
        }

        public void setPassword(String password)
        {
            this.password = password;
        }

       
    };
    
    
  

    
    /**
     * 
     */
    public MailSessionReference()
    {
       super ("javax.mail.Session", MailSessionReference.class.getName(), null); 
    }


    /** 
     * Create a javax.mail.Session instance based on the information passed in the Reference
     * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object, javax.naming.Name, javax.naming.Context, java.util.Hashtable)
     * @param ref the Reference
     * @param arg1 not used
     * @param arg2 not used
     * @param arg3 not used
     * @return
     * @throws Exception
     */
    public Object getObjectInstance(Object ref, Name arg1, Context arg2, Hashtable arg3) throws Exception
    {
        if (ref == null)
        return null;
        
        Reference reference = (Reference)ref;
        

        Properties props = new Properties();
        String user = null;
        String password = null;
        
        Enumeration refs = reference.getAll();
        while (refs.hasMoreElements())
        {
            RefAddr refAddr = (RefAddr)refs.nextElement();
            String name = refAddr.getType();           
            String value =  (String)refAddr.getContent();
            if (name.equalsIgnoreCase("user"))
                user = value;
            else if (name.equalsIgnoreCase("pwd"))
                password = value;
            else
                props.put(name, value);
        }

        if (password == null)
            return Session.getInstance(props);
        else
            return Session.getInstance(props, new PasswordAuthenticator(user, password));
    }
    
    
    public void setUser (String user)
    {
       StringRefAddr addr =  (StringRefAddr)get("user");
       if (addr != null)
       {
         throw new RuntimeException ("user already set on SessionReference, can't be changed");
       }
       add(new StringRefAddr("user", user));
    }
    
    public void setPassword (String password)
    {
        StringRefAddr addr = (StringRefAddr)get("pwd");
        if (addr != null)
            throw new RuntimeException ("password already set on SessionReference, can't be changed");
        add(new StringRefAddr ("pwd", password));
    }
    
    public void setProperties (Properties properties)
    {
        Iterator entries = properties.entrySet().iterator();
        while (entries.hasNext())
        {
            Map.Entry e = (Map.Entry)entries.next();
            StringRefAddr sref = (StringRefAddr)get((String)e.getKey());
            if (sref != null)
                throw new RuntimeException ("property "+e.getKey()+" already set on Session reference, can't be changed");
            add(new StringRefAddr((String)e.getKey(), (String)e.getValue()));
        }
    }
    
  
    
}
