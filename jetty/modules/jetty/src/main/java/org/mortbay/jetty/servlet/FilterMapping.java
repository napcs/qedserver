//========================================================================
//$Id: FilterMapping.java,v 1.2 2005/11/01 11:42:53 gregwilkins Exp $
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

import org.mortbay.jetty.Handler;


public class FilterMapping
{
    private int _dispatches=Handler.REQUEST;
    private String _filterName;
    private transient FilterHolder _holder;
    private String[] _pathSpecs;
    private String[] _servletNames;

    /* ------------------------------------------------------------ */
    public FilterMapping()
    {}
    
    /* ------------------------------------------------------------ */
    /** Check if this filter applies to a path.
     * @param path The path to check or null to just check type
     * @param type The type of request: __REQUEST,__FORWARD,__INCLUDE or __ERROR.
     * @return True if this filter applies
     */
    boolean appliesTo(String path, int type)
    {
       if ( ((_dispatches&type)!=0 || (_dispatches==0 && type==Handler.REQUEST)) && _pathSpecs!=null )
       {
           for (int i=0;i<_pathSpecs.length;i++)
               if (_pathSpecs[i]!=null &&  PathMap.match(_pathSpecs[i], path,true))
                   return true;
       }
       return false;
    }
    
    /* ------------------------------------------------------------ */
    /** Check if this filter applies to a particular dispatch type.
     * @param type The type of request:
     *      {@link Handler#REQUEST}, {@link Handler#FORWARD}, {@link Handler#INCLUDE} or {@link Handler#ERROR}.
     * @return <code>true</code> if this filter applies
     */
    boolean appliesTo(int type)
    {
       if ( ((_dispatches&type)!=0 || (_dispatches==0 && type==Handler.REQUEST)))
           return true;
       return false;
    }

    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the dispatches.
     */
    public int getDispatches()
    {
        return _dispatches;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the filterName.
     */
    public String getFilterName()
    {
        return _filterName;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the holder.
     */
    FilterHolder getFilterHolder()
    {
        return _holder;
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
     * @param dispatches The dispatches to set.
     * @see Handler#DEFAULT
     * @see Handler#REQUEST
     * @see Handler#ERROR
     * @see Handler#FORWARD
     * @see Handler#INCLUDE
     */
    public void setDispatches(int dispatches)
    {
        _dispatches = dispatches;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param filterName The filterName to set.
     */
    public void setFilterName(String filterName)
    {
        _filterName = filterName;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param holder The holder to set.
     */
    void setFilterHolder(FilterHolder holder)
    {
        _holder = holder;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param pathSpecs The Path specifications to which this filter should be mapped. 
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
     * @return Returns the servletName.
     */
    public String[] getServletNames()
    {
        return _servletNames;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param servletNames Maps the {@link #setFilterName(String) named filter} to multiple servlets
     * @see #setServletName
     */
    public void setServletNames(String[] servletNames)
    {
        _servletNames = servletNames;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param servletName Maps the {@link #setFilterName(String) named filter} to a single servlet
     * @see #setServletNames
     */
    public void setServletName(String servletName)
    {
        _servletNames = new String[]{servletName};
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "(F="+_filterName+","+(_pathSpecs==null?"[]":Arrays.asList(_pathSpecs).toString())+","+(_servletNames==null?"[]":Arrays.asList(_servletNames).toString())+","+_dispatches+")"; 
    }
}
