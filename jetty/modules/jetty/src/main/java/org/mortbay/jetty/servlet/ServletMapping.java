//========================================================================
//$Id: ServletMapping.java,v 1.2 2005/11/01 11:42:53 gregwilkins Exp $
//Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.servlet;

import java.util.Arrays;


public class ServletMapping
{
    private String[] _pathSpecs;
    private String _servletName;

    /* ------------------------------------------------------------ */
    public ServletMapping()
    {
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the pathSpec.
     */
    public String[] getPathSpecs()
    {
        return _pathSpecs;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the servletName.
     */
    public String getServletName()
    {
        return _servletName;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param pathSpec The pathSpec to set.
     */
    public void setPathSpecs(String[] pathSpecs)
    {
        _pathSpecs = pathSpecs;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param pathSpec The pathSpec to set.
     */
    public void setPathSpec(String pathSpec)
    {
        _pathSpecs = new String[]{pathSpec};
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param servletName The servletName to set.
     */
    public void setServletName(String servletName)
    {
        _servletName = servletName;
    }
    

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "(S="+_servletName+","+(_pathSpecs==null?"[]":Arrays.asList(_pathSpecs).toString())+")"; 
    }
}