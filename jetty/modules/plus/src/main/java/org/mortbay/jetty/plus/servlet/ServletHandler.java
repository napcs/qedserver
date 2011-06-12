//========================================================================
//$Id: ServletHandler.java 1448 2006-12-29 20:46:57Z janb $
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

package org.mortbay.jetty.plus.servlet;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.mortbay.jetty.plus.annotation.InjectionCollection;
import org.mortbay.jetty.plus.annotation.LifeCycleCallbackCollection;

/**
 * ServletHandler
 *
 *
 */
public class ServletHandler extends org.mortbay.jetty.servlet.ServletHandler
{

    private InjectionCollection _injections = null;
    private LifeCycleCallbackCollection _callbacks = null;
    


    /**
     * @return the callbacks
     */
    public LifeCycleCallbackCollection getCallbacks()
    {
        return _callbacks;
    }



    /**
     * @param callbacks the callbacks to set
     */
    public void setCallbacks(LifeCycleCallbackCollection callbacks)
    {
        this._callbacks = callbacks;
    }



    /**
     * @return the injections
     */
    public InjectionCollection getInjections()
    {
        return _injections;
    }



    /**
     * @param injections the injections to set
     */
    public void setInjections(InjectionCollection injections)
    {
        this._injections = injections;
    }
    
    /** 
     * @see org.mortbay.jetty.servlet.ServletHandler#customizeFilter(javax.servlet.Filter)
     */
    public Filter customizeFilter(Filter filter) throws Exception
    {
        if (_injections != null)
            _injections.inject(filter);
        
        if (_callbacks != null)
            _callbacks.callPostConstructCallback(filter);
        return super.customizeFilter(filter); 
    }
    
    

    /** 
     * @see org.mortbay.jetty.servlet.ServletHandler#customizeServlet(javax.servlet.Servlet)
     */
    public Servlet customizeServlet(Servlet servlet) throws Exception
    {      
        if (_injections != null)
            _injections.inject(servlet);
        if (_callbacks != null)
            _callbacks.callPostConstructCallback(servlet);
        return super.customizeServlet(servlet);
    }



    /** 
     * @see org.mortbay.jetty.servlet.ServletHandler#cusomizeFilterDestroy(javax.servlet.Filter)
     */
    public Filter customizeFilterDestroy(Filter filter) throws Exception
    {
        if (_callbacks != null)
            _callbacks.callPreDestroyCallback(filter);
        return super.customizeFilterDestroy(filter);
    }



    /** 
     * @see org.mortbay.jetty.servlet.ServletHandler#customizeServletDestroy(javax.servlet.Servlet)
     */
    public Servlet customizeServletDestroy(Servlet servlet) throws Exception
    {
        if (_callbacks != null)
            _callbacks.callPreDestroyCallback(servlet);
        return super.customizeServletDestroy(servlet);
    }
}
