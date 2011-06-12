//========================================================================
//$Id: localContextRoot.java 1327 2006-11-27 18:40:14Z janb $
//Copyright 2000-2006 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.naming.local;

import java.util.Hashtable;
import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.mortbay.naming.NamingContext;

/**
 * 
 * localContext
 * 
 * @author janb
 * @version $Revision: 1327 $ $Date: 2006-11-28 05:40:14 +1100 (Tue, 28 Nov 2006) $
 * 
 */
public class localContextRoot implements Context
{
    private static final NamingContext _root;

    private final Hashtable _env;

    // make a root for the static namespace local:
    static
    {
        _root = new NamingContext();
        _root.setNameParser(new LocalNameParser());
    }

    static class LocalNameParser implements NameParser
    {
        Properties syntax = new Properties();

        LocalNameParser()
        {
            syntax.put("jndi.syntax.direction", "left_to_right");
            syntax.put("jndi.syntax.separator", "/");
            syntax.put("jndi.syntax.ignorecase", "false");
        }

        public Name parse(String name) throws NamingException
        {
            return new CompoundName(name, syntax);
        }
    }

    public localContextRoot(Hashtable env)
    {
        _env = new Hashtable(env);
    }

    /**
     * 
     * 
     * @see javax.naming.Context#close()
     */
    public void close() throws NamingException
    {

    }

    /**
     * 
     * 
     * @see javax.naming.Context#getNameInNamespace()
     */
    public String getNameInNamespace() throws NamingException
    {
        return "";
    }

    /**
     * 
     * 
     * @see javax.naming.Context#destroySubcontext(java.lang.String)
     */
    public void destroySubcontext(String name) throws NamingException
    {
        synchronized (_root)
        {
            _root.destroySubcontext(getSuffix(name));
        }
    }

    /**
     * 
     * 
     * @see javax.naming.Context#unbind(java.lang.String)
     */
    public void unbind(String name) throws NamingException
    {
        synchronized (_root)
        {
            _root.unbind(getSuffix(name));
        }
    }

    /**
     * 
     * 
     * @see javax.naming.Context#getEnvironment()
     */
    public Hashtable getEnvironment() throws NamingException
    {
        return _env;
    }

    /**
     * 
     * 
     * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
     */
    public void destroySubcontext(Name name) throws NamingException
    {
        synchronized (_root)
        {
            _root.destroySubcontext(getSuffix(name));
        }
    }

    /**
     * 
     * 
     * @see javax.naming.Context#unbind(javax.naming.Name)
     */
    public void unbind(Name name) throws NamingException
    {
        synchronized (_root)
        {
            _root.unbind(getSuffix(name));
        }
    }

    /**
     * 
     * 
     * @see javax.naming.Context#lookup(java.lang.String)
     */
    public Object lookup(String name) throws NamingException
    {
        synchronized (_root)
        {
            return _root.lookup(getSuffix(name));
        }
    }

    /**
     * 
     * 
     * @see javax.naming.Context#lookupLink(java.lang.String)
     */
    public Object lookupLink(String name) throws NamingException
    {
        synchronized (_root)
        {
            return _root.lookupLink(getSuffix(name));
        }
    }

    /**
     * 
     *       
     * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
     */
    public Object removeFromEnvironment(String propName) throws NamingException
    {
        return _env.remove(propName);
    }

    /**
     * 
     * 
     * @see javax.naming.Context#bind(java.lang.String, java.lang.Object)
     */
    public void bind(String name, Object obj) throws NamingException
    {
        synchronized (_root)
        {
            _root.bind(getSuffix(name), obj);
        }
    }

    /**
     * 
     * 
     * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
     */
    public void rebind(String name, Object obj) throws NamingException
    {
        synchronized (_root)
        {
            _root.rebind(getSuffix(name), obj);
        }
    }

    /**
     * 
     * 
     * @see javax.naming.Context#lookup(javax.naming.Name)
     */
    public Object lookup(Name name) throws NamingException
    {
        synchronized (_root)
        {
            return _root.lookup(getSuffix(name));
        }
    }

    /**
     * 
     * 
     * @see javax.naming.Context#lookupLink(javax.naming.Name)
     */
    public Object lookupLink(Name name) throws NamingException
    {
        synchronized (_root)
        {
            return _root.lookupLink(getSuffix(name));
        }
    }

    /**
     * 
     * 
     * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
     */
    public void bind(Name name, Object obj) throws NamingException
    {
        synchronized (_root)
        {
            _root.bind(getSuffix(name), obj);
        }
    }

    /**
     *
     * 
     * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
     */
    public void rebind(Name name, Object obj) throws NamingException
    {
        synchronized (_root)
        {
            _root.rebind(getSuffix(name), obj);
        }
    }

    /**
     * 
     * 
     * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
     */
    public void rename(String oldName, String newName) throws NamingException
    {
        synchronized (_root)
        {
            _root.rename(getSuffix(oldName), getSuffix(newName));
        }
    }

    /**
     * 
     * 
     * @see javax.naming.Context#createSubcontext(java.lang.String)
     */
    public Context createSubcontext(String name) throws NamingException
    {
        synchronized (_root)
        {
            return _root.createSubcontext(getSuffix(name));
        }
    }

    /**
     * 
     * 
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     */
    public Context createSubcontext(Name name) throws NamingException
    {
        synchronized (_root)
        {
            return _root.createSubcontext(getSuffix(name));
        }
    }

    /**
     * 
     * 
     * @see javax.naming.Context#rename(javax.naming.Name, javax.naming.Name)
     */
    public void rename(Name oldName, Name newName) throws NamingException
    {
        synchronized (_root)
        {
            _root.rename(getSuffix(oldName), getSuffix(newName));
        }
    }

    /**
     *
     * 
     * @see javax.naming.Context#getNameParser(java.lang.String)
     */
    public NameParser getNameParser(String name) throws NamingException
    {
        return _root.getNameParser(name);
    }

    /**
     * 
     * 
     * @see javax.naming.Context#getNameParser(javax.naming.Name)
     */
    public NameParser getNameParser(Name name) throws NamingException
    {
        return _root.getNameParser(name);
    }

    /**
     * 
     * 
     * @see javax.naming.Context#list(java.lang.String)
     */
    public NamingEnumeration list(String name) throws NamingException
    {
        synchronized (_root)
        {
            return _root.list(getSuffix(name));
        }
    }

    /**
     *
     * 
     * @see javax.naming.Context#listBindings(java.lang.String)
     */
    public NamingEnumeration listBindings(String name) throws NamingException
    {
        synchronized (_root)
        {
            return _root.listBindings(getSuffix(name));
        }
    }

    /**
     *
     * 
     * @see javax.naming.Context#list(javax.naming.Name)
     */
    public NamingEnumeration list(Name name) throws NamingException
    {
        synchronized (_root)
        {
            return _root.list(getSuffix(name));
        }
    }

    /**
     *
     * 
     * @see javax.naming.Context#listBindings(javax.naming.Name)
     */
    public NamingEnumeration listBindings(Name name) throws NamingException
    {
        synchronized (_root)
        {
            return _root.listBindings(getSuffix(name));
        }
    }

    /**
     *
     * 
     * @see javax.naming.Context#addToEnvironment(java.lang.String,
     *      java.lang.Object)
     */
    public Object addToEnvironment(String propName, Object propVal)
            throws NamingException
    {
        return _env.put(propName, propVal);
    }

    /**
     *
     * 
     * @see javax.naming.Context#composeName(java.lang.String, java.lang.String)
     */
    public String composeName(String name, String prefix)
            throws NamingException
    {
        return _root.composeName(name, prefix);
    }

    /**
     *
     * 
     * @see javax.naming.Context#composeName(javax.naming.Name,
     *      javax.naming.Name)
     */
    public Name composeName(Name name, Name prefix) throws NamingException
    {
        return _root.composeName(name, prefix);
    }

    protected String getSuffix(String url) throws NamingException
    {
        return url;
    }

    protected Name getSuffix(Name name) throws NamingException
    {
        return name;
    }

}
