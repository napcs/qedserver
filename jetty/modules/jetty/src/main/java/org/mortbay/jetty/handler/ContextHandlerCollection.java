//========================================================================
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

package org.mortbay.jetty.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HandlerContainer;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.servlet.PathMap;
import org.mortbay.log.Log;
import org.mortbay.util.LazyList;

/* ------------------------------------------------------------ */
/** ContextHandlerCollection.
 * 
 * This {@link org.mortbay.jetty.handler.HandlerCollection} is creates a 
 * {@link org.mortbay.jetty.servlet.PathMap} to it's contained handlers based
 * on the context path and virtual hosts of any contained {@link org.mortbay.jetty.handler.ContextHandler}s.
 * The contexts do not need to be directly contained, only children of the contained handlers.
 * Multiple contexts may have the same context path and they are called in order until one
 * handles the request.  
 * 
 * @org.apache.xbean.XBean element="contexts"
 */
public class ContextHandlerCollection extends HandlerCollection
{ 
    private PathMap _contextMap;
    private Class _contextClass = ContextHandler.class;
    
    /* ------------------------------------------------------------ */
    /**
     * Remap the context paths.
     */
    public void mapContexts()
    {
        PathMap contextMap = new PathMap();
        Handler[] branches = getHandlers();
        
        
        for (int b=0;branches!=null && b<branches.length;b++)
        {
            Handler[] handlers=null;
            
            if (branches[b] instanceof ContextHandler)
            {
                handlers = new Handler[]{ branches[b] };
            }
            else if (branches[b] instanceof HandlerContainer)
            {
                handlers = ((HandlerContainer)branches[b]).getChildHandlersByClass(ContextHandler.class);
            }
            else 
                continue;
            
            for (int i=0;i<handlers.length;i++)
            {
                ContextHandler handler=(ContextHandler)handlers[i];

                String contextPath=handler.getContextPath();

                if (contextPath==null || contextPath.indexOf(',')>=0 || contextPath.startsWith("*"))
                    throw new IllegalArgumentException ("Illegal context spec:"+contextPath);

                if(!contextPath.startsWith("/"))
                    contextPath='/'+contextPath;

                if (contextPath.length()>1)
                {
                    if (contextPath.endsWith("/"))
                        contextPath+="*";
                    else if (!contextPath.endsWith("/*"))
                        contextPath+="/*";
                }

                Object contexts=contextMap.get(contextPath);
                String[] vhosts=handler.getVirtualHosts();

                
                if (vhosts!=null && vhosts.length>0)
                {
                    Map hosts;

                    if (contexts instanceof Map)
                        hosts=(Map)contexts;
                    else
                    {
                        hosts=new HashMap(); 
                        hosts.put("*",contexts);
                        contextMap.put(contextPath, hosts);
                    }

                    for (int j=0;j<vhosts.length;j++)
                    {
                        String vhost=vhosts[j];
                        contexts=hosts.get(vhost);
                        contexts=LazyList.add(contexts,branches[b]);
                        hosts.put(vhost,contexts);
                    }
                }
                else if (contexts instanceof Map)
                {
                    Map hosts=(Map)contexts;
                    contexts=hosts.get("*");
                    contexts= LazyList.add(contexts, branches[b]);
                    hosts.put("*",contexts);
                }
                else
                {
                    contexts= LazyList.add(contexts, branches[b]);
                    contextMap.put(contextPath, contexts);
                }
            }
        }
        _contextMap=contextMap;

    }
    

    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.handler.HandlerCollection#setHandlers(org.mortbay.jetty.Handler[])
     */
    public void setHandlers(Handler[] handlers)
    {
        _contextMap=null;
        super.setHandlers(handlers);
        if (isStarted())
            mapContexts();
    }

    /* ------------------------------------------------------------ */
    protected void doStart() throws Exception
    {
        mapContexts();
        super.doStart();
    }
    

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.Handler#handle(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, int)
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
    {
        Handler[] handlers = getHandlers();
        if (handlers==null || handlers.length==0)
	    return;

        Request base_request = HttpConnection.getCurrentConnection().getRequest();
      
        // data structure which maps a request to a context
        // each match is called in turn until the request is handled
        // { context path => 
        //     { virtual host => context } 
        // }
	PathMap map = _contextMap;
	if (map!=null && target!=null && target.startsWith("/"))
	{
            // first, get all contexts matched by context path
	    Object contexts = map.getLazyMatches(target);

            for (int i=0; i<LazyList.size(contexts); i++)
            {
                // then, match against the virtualhost of each context
                Map.Entry entry = (Map.Entry)LazyList.get(contexts, i);
                Object list = entry.getValue();

                if (list instanceof Map)
                {
                    Map hosts = (Map)list;
                    String host = normalizeHostname(request.getServerName());

                    // explicitly-defined virtual hosts, most specific
                    list=hosts.get(host);
                    for (int j=0; j<LazyList.size(list); j++)
                    {
                        Handler handler = (Handler)LazyList.get(list,j);
                        handler.handle(target,request, response, dispatch);
                        if (base_request.isHandled())
                            return;
                    }

                    // wildcard for one level of names 
                    list=hosts.get("*."+host.substring(host.indexOf(".")+1));
                    for (int j=0; j<LazyList.size(list); j++)
                    {
                        Handler handler = (Handler)LazyList.get(list,j);
                        handler.handle(target,request, response, dispatch);
                        if (base_request.isHandled())
                            return;
                    }

                    // no virtualhosts defined for the context, least specific
                    // will handle any request that does not match to a specific virtual host above                    
                    list=hosts.get("*");
                    for (int j=0; j<LazyList.size(list); j++)
                    {
                        Handler handler = (Handler)LazyList.get(list,j);
                        handler.handle(target,request, response, dispatch);
                        if (base_request.isHandled())
                            return;
                    }
                }
                else
                {
                    for (int j=0; j<LazyList.size(list); j++)
                    {
                        Handler handler = (Handler)LazyList.get(list,j);
                        handler.handle(target,request, response, dispatch);
                        if (base_request.isHandled())
                            return;
                    }
                }
	    }
	}
	else
	{
            // This may not work in all circumstances... but then I think it should never be called
	    for (int i=0;i<handlers.length;i++)
	    {
		handlers[i].handle(target,request, response, dispatch);
		if ( base_request.isHandled())
		    return;
	    }
	}
    }
    
    
    /* ------------------------------------------------------------ */
    /** Add a context handler.
     * @param contextPath  The context path to add
     * @return
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public ContextHandler addContext(String contextPath,String resourceBase) 
    {
        try
        {
            ContextHandler context = (ContextHandler)_contextClass.newInstance();
            context.setContextPath(contextPath);
            context.setResourceBase(resourceBase);
            addHandler(context);
            return context;
        }
        catch (Exception e)
        {
            Log.debug(e);
            throw new Error(e);
        }
    }



    /* ------------------------------------------------------------ */
    /**
     * @return The class to use to add new Contexts
     */
    public Class getContextClass()
    {
        return _contextClass;
    }


    /* ------------------------------------------------------------ */
    /**
     * @param contextClass The class to use to add new Contexts
     */
    public void setContextClass(Class contextClass)
    {
        if (contextClass ==null || !(ContextHandler.class.isAssignableFrom(contextClass)))
            throw new IllegalArgumentException();
        _contextClass = contextClass;
    }
    
    /* ------------------------------------------------------------ */
    private String normalizeHostname( String host )
    {
        if ( host == null )
            return null;
        
        if ( host.endsWith( "." ) )
            return host.substring( 0, host.length() -1);
      
        return host;
    }

}
