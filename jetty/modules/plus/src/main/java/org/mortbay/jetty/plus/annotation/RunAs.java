//========================================================================
//$Id: RunAs.java 1594 2007-02-14 02:45:12Z janb $
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plus.annotation;

import javax.servlet.Servlet;

import org.mortbay.jetty.servlet.ServletHolder;

/**
 * RunAs
 *
 * Represents a &lt;run-as&gt; element in web.xml, or a runAs annotation.
 */
public class RunAs
{
    private Class _targetClass;
    private String _roleName;
    
    public RunAs()
    {}
    
    
    public void setTargetClass (Class clazz)
    {
        _targetClass=clazz;
    }

    public Class getTargetClass ()
    {
        return _targetClass;
    }
    
    public void setRoleName (String roleName)
    {
        _roleName = roleName;
    }
    
    public String getRoleName ()
    {
        return _roleName;
    }
    
    
    public void setRunAs (ServletHolder holder)
    {
        if (holder==null)
            return;
        if (holder.getClassName().equals(_targetClass.getName()))
            holder.setRunAs(_roleName); 
    }
}
