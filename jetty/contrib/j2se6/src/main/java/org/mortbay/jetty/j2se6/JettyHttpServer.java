//========================================================================
//$$Id: JettyHttpServer.java 1647 2009-09-18 09:14:03Z janb $$
//
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

package org.mortbay.jetty.j2se6;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.log.Log;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;

/**
 * Jetty implementation of {@link com.sun.net.httpserver.HttpServer}.
 * 
 * One sun HttpServer instance relates to one jetty Server instance, which
 * has one Connector.
 * 
 */
public class JettyHttpServer extends com.sun.net.httpserver.HttpServer
{
    private Server _server;
    
    private ContextHandlerCollection _contextCollection;
    
    private InetSocketAddress _addr;

    private ThreadPoolExecutor _executor;
    
    private boolean _started = false;


    public JettyHttpServer()
    {
        _server = new Server();
        HandlerCollection handlerCollection = new HandlerCollection();
        _contextCollection = new ContextHandlerCollection();
        handlerCollection.setHandlers(new Handler[] {_contextCollection, new DefaultHandler()});
        _server.setHandler(handlerCollection);
    }

    @Override
    public void bind(InetSocketAddress addr, int backlog) throws IOException
    {
        if (_started)
            throw new BindException ("Already started");

        // check if there is already a connector listening
        Connector[] connectors = _server.getConnectors();
        if (connectors != null)
            throw new BindException ("Server already bound");
       
        this._addr = addr;
        if (_executor != null && _server.getThreadPool() == null)
        {
            if (Log.isDebugEnabled()) Log.debug("using given Executor for server thread pool");
            _server.setThreadPool(new ThreadPoolExecutorAdapter(_executor));
        }

        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setAcceptors(1);
        connector.setAcceptQueueSize(backlog);
        connector.setPort(addr.getPort());
        connector.setHost(addr.getHostName());
        _server.addConnector(connector);
    }

    @Override
    public InetSocketAddress getAddress()
    {
        return _addr;
    }

    @Override
    public void start()
    {
        if (_started)
            throw new IllegalStateException ("Already started");
        
        try
        {
            _server.start();
            _started = true;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void setExecutor(Executor executor)
    {
        if (_started)
            throw new IllegalStateException ("Server started");
        
        if (!(executor instanceof ThreadPoolExecutor))
            throw new IllegalArgumentException("only ThreadPoolExecutor are allowed");

        this._executor = (ThreadPoolExecutor) executor;
    }

    @Override
    public Executor getExecutor()
    {
        return _executor;
    }

    @Override
    public void stop(int delay)
    {
        if (delay < 0)
            throw new IllegalStateException ("Delay is negative");
        
        _server.setGracefulShutdown(delay * 1000);
      
        try
        {
            _server.stop();
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

  

    @Override
    public HttpContext createContext(String path, HttpHandler handler)
    {
        checkIfContextIsFree(path);

        JettyHttpContext context = new JettyHttpContext(this, path, handler);
        J2SE6ContextHandler jettyContextHandler = context.getJettyContextHandler();
        _contextCollection.addHandler(jettyContextHandler);
     
        return context;
    }

    private void checkIfContextIsFree(String path)
    {
        Handler[] handlers = _contextCollection.getChildHandlersByClass(ContextHandler.class);
        for (int i = 0; handlers!= null && i < handlers.length; i++)
        {
            ContextHandler ctx = (ContextHandler) handlers[i];
            if (ctx.getContextPath().equals(path))
                throw new IllegalArgumentException("another context already bound to path " + path);  
        }
    }

    @Override
    public HttpContext createContext(String path)
    {
        return createContext(path, null);
    }

    @Override
    public void removeContext(String path) throws IllegalArgumentException
    {
        Handler handler = null;
        Handler[] handlers = _contextCollection.getChildHandlersByClass(ContextHandler.class);
        for (int i = 0; handlers!= null && i < handlers.length && handler == null; i++)
        {
            ContextHandler ctx = (ContextHandler) handlers[i];
            if (ctx.getContextPath().equals(path))
                handler = ctx;
        }
        if (handler != null)
            _contextCollection.removeHandler(handler);
    }

    @Override
    public void removeContext(HttpContext context)
    {
        removeContext(context.getPath());
    }

}
