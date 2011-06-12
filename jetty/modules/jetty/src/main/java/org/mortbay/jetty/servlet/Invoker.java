// ========================================================================
// $Id: Invoker.java 3539 2008-08-21 04:46:59Z dyu $
// Copyright 199-2004 Mort Bay Consulting Pty. Ltd.
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

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.HandlerWrapper;
import org.mortbay.log.Log;
import org.mortbay.util.LazyList;
import org.mortbay.util.URIUtil;
;

/* ------------------------------------------------------------ */
/**  Dynamic Servlet Invoker.  
 * This servlet invokes anonymous servlets that have not been defined   
 * in the web.xml or by other means. The first element of the pathInfo  
 * of a request passed to the envoker is treated as a servlet name for  
 * an existing servlet, or as a class name of a new servlet.            
 * This servlet is normally mapped to /servlet/*                        
 * This servlet support the following initParams:                       
 * <PRE>                                                                     
 *  nonContextServlets       If false, the invoker can only load        
 *                           servlets from the contexts classloader.    
 *                           This is false by default and setting this  
 *                           to true may have security implications.    
 *                                                                      
 *  verbose                  If true, log dynamic loads                 
 *                                                                      
 *  *                        All other parameters are copied to the     
 *                           each dynamic servlet as init parameters    
 * </PRE>
 * @version $Id: Invoker.java 3539 2008-08-21 04:46:59Z dyu $
 * @author Greg Wilkins (gregw)
 */
public class Invoker extends HttpServlet
{

    private ContextHandler _contextHandler;
    private ServletHandler _servletHandler;
    private Map.Entry _invokerEntry;
    private Map _parameters;
    private boolean _nonContextServlets;
    private boolean _verbose;
        
    /* ------------------------------------------------------------ */
    public void init()
    {
        ServletContext config=getServletContext();
        _contextHandler=((ContextHandler.SContext)config).getContextHandler();

        Handler handler=_contextHandler.getHandler();
        while (handler!=null && !(handler instanceof ServletHandler) && (handler instanceof HandlerWrapper))
            handler=((HandlerWrapper)handler).getHandler();
        _servletHandler = (ServletHandler)handler;
        Enumeration e = getInitParameterNames();
        while(e.hasMoreElements())
        {
            String param=(String)e.nextElement();
            String value=getInitParameter(param);
            String lvalue=value.toLowerCase();
            if ("nonContextServlets".equals(param))
            {
                _nonContextServlets=value.length()>0 && lvalue.startsWith("t");
            }
            if ("verbose".equals(param))
            {
                _verbose=value.length()>0 && lvalue.startsWith("t");
            }
            else
            {
                if (_parameters==null)
                    _parameters=new HashMap();
                _parameters.put(param,value);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    protected void service(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
    {
        // Get the requested path and info
        boolean included=false;
        String servlet_path=(String)request.getAttribute(Dispatcher.__INCLUDE_SERVLET_PATH);
        if (servlet_path==null)
            servlet_path=request.getServletPath();
        else
            included=true;
        String path_info = (String)request.getAttribute(Dispatcher.__INCLUDE_PATH_INFO);
        if (path_info==null)
            path_info=request.getPathInfo();
        
        // Get the servlet class
        String servlet = path_info;
        if (servlet==null || servlet.length()<=1 )
        {
            response.sendError(404);
            return;
        }
        
        
        int i0=servlet.charAt(0)=='/'?1:0;
        int i1=servlet.indexOf('/',i0);
        servlet=i1<0?servlet.substring(i0):servlet.substring(i0,i1);

        // look for a named holder
        ServletHolder[] holders = _servletHandler.getServlets();
        ServletHolder holder = getHolder (holders, servlet);
       
        if (holder!=null)
        {
            // Found a named servlet (from a user's web.xml file) so
            // now we add a mapping for it
            Log.debug("Adding servlet mapping for named servlet:"+servlet+":"+URIUtil.addPaths(servlet_path,servlet)+"/*");
            ServletMapping mapping = new ServletMapping();
            mapping.setServletName(servlet);
            mapping.setPathSpec(URIUtil.addPaths(servlet_path,servlet)+"/*");
            _servletHandler.setServletMappings((ServletMapping[])LazyList.addToArray(_servletHandler.getServletMappings(), mapping, ServletMapping.class));
        }
        else
        {
            // look for a class mapping
            if (servlet.endsWith(".class"))
                servlet=servlet.substring(0,servlet.length()-6);
            if (servlet==null || servlet.length()==0)
            {
                response.sendError(404);
                return;
            }   
        
            synchronized(_servletHandler)
            {
                // find the entry for the invoker (me)
                 _invokerEntry=_servletHandler.getHolderEntry(servlet_path);
            
                // Check for existing mapping (avoid threaded race).
                String path=URIUtil.addPaths(servlet_path,servlet);
                Map.Entry entry = _servletHandler.getHolderEntry(path);
               
                if (entry!=null && !entry.equals(_invokerEntry))
                {
                    // Use the holder
                    holder=(ServletHolder)entry.getValue();
                }
                else
                {
                    // Make a holder
                    Log.debug("Making new servlet="+servlet+" with path="+path+"/*");
                    holder=_servletHandler.addServletWithMapping(servlet, path+"/*");
                    
                    if (_parameters!=null)
                        holder.setInitParameters(_parameters);
                    
                    try {holder.start();}
                    catch (Exception e)
                    {
                        Log.debug(e);
                        throw new UnavailableException(e.toString());
                    }
                    
                    // Check it is from an allowable classloader
                    if (!_nonContextServlets)
                    {
                        Object s=holder.getServlet();
                        
                        if (_contextHandler.getClassLoader()!=
                            s.getClass().getClassLoader())
                        {
                            try 
                            {
                                holder.stop();
                            } 
                            catch (Exception e) 
                            {
                                Log.ignore(e);
                            }
                            
                            Log.warn("Dynamic servlet "+s+
                                         " not loaded from context "+
                                         request.getContextPath());
                            throw new UnavailableException("Not in context");
                        }
                    }

                    if (_verbose)
                        Log.debug("Dynamic load '"+servlet+"' at "+path);
                }
            }
        }
        
        if (holder!=null)
            holder.handle(new Request(request,included,servlet,servlet_path,path_info),
                          response);
        else
        {
            Log.info("Can't find holder for servlet: "+servlet);
            response.sendError(404);
        }
            
        
    }

    /* ------------------------------------------------------------ */
    class Request extends HttpServletRequestWrapper
    {
        String _servletPath;
        String _pathInfo;
        boolean _included;
        
        /* ------------------------------------------------------------ */
        Request(HttpServletRequest request,
                boolean included,
                String name,
                String servletPath,
                String pathInfo)
        {
            super(request);
            _included=included;
            _servletPath=URIUtil.addPaths(servletPath,name);
            _pathInfo=pathInfo.substring(name.length()+1);
            if (_pathInfo.length()==0)
                _pathInfo=null;
        }
        
        /* ------------------------------------------------------------ */
        public String getServletPath()
        {
            if (_included)
                return super.getServletPath();
            return _servletPath;
        }
        
        /* ------------------------------------------------------------ */
        public String getPathInfo()
        {
            if (_included)
                return super.getPathInfo();
            return _pathInfo;
        }
        
        /* ------------------------------------------------------------ */
        public Object getAttribute(String name)
        {
            if (_included)
            {
                if (name.equals(Dispatcher.__INCLUDE_REQUEST_URI))
                    return URIUtil.addPaths(URIUtil.addPaths(getContextPath(),_servletPath),_pathInfo);
                if (name.equals(Dispatcher.__INCLUDE_PATH_INFO))
                    return _pathInfo;
                if (name.equals(Dispatcher.__INCLUDE_SERVLET_PATH))
                    return _servletPath;
            }
            return super.getAttribute(name);
        }
    }
    
    
    private ServletHolder getHolder(ServletHolder[] holders, String servlet)
    {
        if (holders == null)
            return null;
       
        ServletHolder holder = null;
        for (int i=0; holder==null && i<holders.length; i++)
        {
            if (holders[i].getName().equals(servlet))
            {
                holder = holders[i];
            }
        }
        return holder;
    }
}
