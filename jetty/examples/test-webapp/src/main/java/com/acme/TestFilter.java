//========================================================================
//$Id: TestFilter.java,v 1.5 2005/11/01 11:42:53 gregwilkins Exp $
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

package com.acme;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.log.Log;


/* ------------------------------------------------------------ */
/** TestFilter.
 * 
 * This filter checks for a none local request, and if the init parameter
 * "remote" is not set to true, then all non local requests are forwarded
 * to /remote.html
 * 
 */
public class TestFilter implements Filter
{
    private boolean _remote;
    private ServletContext _context;
    private final Set _allowed = new HashSet();
    
    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) throws ServletException
    {
        _context= filterConfig.getServletContext();
        _remote=Boolean.parseBoolean(filterConfig.getInitParameter("remote"));
        _allowed.add("/favicon.ico");
        _allowed.add("/jetty_banner.gif");
        
        Log.debug("TestFilter#remote="+_remote);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        String from = request.getRemoteHost();
        String to = request.getServerName();
        String path=((HttpServletRequest)request).getServletPath();
        
        if (!_remote && !_allowed.contains(path) && (
             !from.equals("localhost") && !from.startsWith("127.") && from.indexOf(":1")<0 ||
             !to.equals("localhost")&&!to.startsWith("127.0.0.") && to.indexOf(":1")<0))
        {
            System.err.println("REMOTE "+from+" "+to+" "+path);
            if ("/".equals(path))
                _context.getRequestDispatcher("/remote.html").forward(request,response);
            else
                ((HttpServletResponse)response).sendRedirect("/remote.html");
            return;
        }
        
        Integer old_value=null;
        ServletRequest r = request;
        while (r instanceof ServletRequestWrapper)
            r=((ServletRequestWrapper)r).getRequest();
        
        try
        {
            old_value=(Integer)request.getAttribute("testFilter");
            
            Integer value=(old_value==null)?new Integer(1):new Integer(old_value.intValue()+1);
                        
            request.setAttribute("testFilter", value);
            
            String qString = ((HttpServletRequest)request).getQueryString();
            if (qString != null && qString.indexOf("wrap")>=0)
            {
                request=new HttpServletRequestWrapper((HttpServletRequest)request);
            }
            _context.setAttribute("request"+r.hashCode(),value);
            
            chain.doFilter(request, response);
        }
        finally
        {
            request.setAttribute("testFilter", old_value);
            _context.setAttribute("request"+r.hashCode(),old_value);
        }
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy()
    {
    }

}
