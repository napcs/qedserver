// ========================================================================
// Copyright 1996-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.servlet;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.UnavailableException;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.log.Log;
import org.mortbay.util.Loader;


/* --------------------------------------------------------------------- */
/** 
 * @author Greg Wilkins
 */
public class Holder extends AbstractLifeCycle implements Serializable
{
    protected transient Class _class;
    protected String _className;
    protected String _displayName;
    protected Map _initParams;
    protected boolean _extInstance;

    /* ---------------------------------------------------------------- */
    protected String _name;
    protected ServletHandler _servletHandler;

    protected Holder()
    {}

    /* ---------------------------------------------------------------- */
    protected Holder(Class held)
    {
        _class=held;
        if (held!=null)
        {
            _className=held.getName();
            _name=held.getName()+"-"+this.hashCode();
        }
    }

    /* ------------------------------------------------------------ */
    public void doStart()
        throws Exception
    {
        //if no class already loaded and no classname, make servlet permanently unavailable
        if (_class==null && (_className==null || _className.equals("")))
            throw new UnavailableException("No class for Servlet or Filter", -1);
        
        //try to load class
        if (_class==null)
        {
            try
            {
                _class=Loader.loadClass(Holder.class, _className);
                if(Log.isDebugEnabled())Log.debug("Holding {}",_class);
            }
            catch (Exception e)
            {
                Log.warn(e);
                throw new UnavailableException(e.getMessage(), -1);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    public void doStop()
    {
        if (!_extInstance)
            _class=null;
    }
    
    /* ------------------------------------------------------------ */
    public String getClassName()
    {
        return _className;
    }
    
    /* ------------------------------------------------------------ */
    public Class getHeldClass()
    {
        return _class;
    }
    
    /* ------------------------------------------------------------ */
    public String getDisplayName()
    {
        return _displayName;
    }

    /* ---------------------------------------------------------------- */
    public String getInitParameter(String param)
    {
        if (_initParams==null)
            return null;
        return (String)_initParams.get(param);
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration getInitParameterNames()
    {
        if (_initParams==null)
            return Collections.enumeration(Collections.EMPTY_LIST);
        return Collections.enumeration(_initParams.keySet());
    }

    /* ---------------------------------------------------------------- */
    public Map getInitParameters()
    {
        return _initParams;
    }
    
    /* ------------------------------------------------------------ */
    public String getName()
    {
        return _name;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the servletHandler.
     */
    public ServletHandler getServletHandler()
    {
        return _servletHandler;
    }
    
    /* ------------------------------------------------------------ */
    public synchronized Object newInstance()
        throws InstantiationException,
               IllegalAccessException
    {
        if (_class==null)
            throw new InstantiationException("!"+_className);
        return _class.newInstance();
    }
    
    public void destroyInstance(Object instance)
    throws Exception
    {
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param className The className to set.
     */
    public void setClassName(String className)
    {
        _className = className;
        _class=null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param className The className to set.
     */
    public void setHeldClass(Class held)
    {
        _class=held;
        _className = held!=null?held.getName():null;
    }
    
    /* ------------------------------------------------------------ */
    public void setDisplayName(String name)
    {
        _displayName=name;
    }
    
    /* ------------------------------------------------------------ */
    public void setInitParameter(String param,String value)
    {
        if (_initParams==null)
            _initParams=new HashMap(3);
        _initParams.put(param,value);
    }
    
    /* ---------------------------------------------------------------- */
    public void setInitParameters(Map map)
    {
        _initParams=map;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * The name is a primary key for the held object.
     * Ensure that the name is set BEFORE adding a Holder
     * (eg ServletHolder or FilterHolder) to a ServletHandler.
     * @param name The name to set.
     */
    public void setName(String name)
    {
        _name = name;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param servletHandler The {@link ServletHandler} that will handle requests dispatched to this servlet.
     */
    public void setServletHandler(ServletHandler servletHandler)
    {
        _servletHandler = servletHandler;
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        return _name;
    }
}





