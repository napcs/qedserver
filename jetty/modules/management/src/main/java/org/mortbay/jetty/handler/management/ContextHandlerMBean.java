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

package org.mortbay.jetty.handler.management;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.management.ObjectMBean;
import org.mortbay.util.Attributes;

public class ContextHandlerMBean extends ObjectMBean
{
    public ContextHandlerMBean(Object managedObject)
    {
        super(managedObject);
    }

    /* ------------------------------------------------------------ */
    public String getObjectNameBasis()
    {
        if (_managed!=null && _managed instanceof ContextHandler)
        {
            ContextHandler context = (ContextHandler)_managed;
            String name = context.getDisplayName();
            if (name!=null)
                return name;
            
            if (context.getBaseResource()!=null && context.getBaseResource().getName().length()>1)
                return context.getBaseResource().getName();
        }
        return super.getObjectNameBasis();
    }
    
    public Map getContextAttributes()
    {
        Map map = new HashMap();
        Attributes attrs = ((ContextHandler)_managed).getAttributes();
        Enumeration en = attrs.getAttributeNames();
        while (en.hasMoreElements())
        {
            String name = (String)en.nextElement();
            Object value = attrs.getAttribute(name);
            map.put(name,value);
        }
        return map;
    }
    
    public void setContextAttribute(String name, Object value)
    {
        Attributes attrs = ((ContextHandler)_managed).getAttributes();
        attrs.setAttribute(name,value);
    }
    
    public void setContextAttribute(String name, String value)
    {
        Attributes attrs = ((ContextHandler)_managed).getAttributes();
        attrs.setAttribute(name,value);
    }
    
    public void removeContextAttribute(String name)
    {
        Attributes attrs = ((ContextHandler)_managed).getAttributes();
        attrs.removeAttribute(name);
    }
}
