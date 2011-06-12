// ========================================================================
// Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.ServletException;

import org.mortbay.component.Container;
import org.mortbay.component.LifeCycle;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.HandlerWrapper;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.log.Log;
import org.mortbay.thread.QueuedThreadPool;
import org.mortbay.thread.ThreadPool;
import org.mortbay.util.Attributes;
import org.mortbay.util.AttributesMap;
import org.mortbay.util.LazyList;
import org.mortbay.util.MultiException;

/* ------------------------------------------------------------ */
/** Jetty HTTP Servlet Server.
 * This class is the main class for the Jetty HTTP Servlet server.
 * It aggregates Connectors (HTTP request receivers) and request Handlers.
 * The server is itself a handler and a ThreadPool.  Connectors use the ThreadPool methods
 * to run jobs that will eventually call the handle method.
 *
 *  @org.apache.xbean.XBean  description="Creates an embedded Jetty web server"
 */
public class Server extends HandlerWrapper implements Attributes
{
    public final static String UNKNOWN_VERSION="6.1.x";
    public final static String SNAPSHOT_VERSION="6.1-SNAPSHOT";
    private static ShutdownHookThread hookThread = new ShutdownHookThread();
    private static String _version = (Server.class.getPackage()!=null && Server.class.getPackage().getImplementationVersion()!=null)
        ?Server.class.getPackage().getImplementationVersion()
        :UNKNOWN_VERSION;

    private ThreadPool _threadPool;
    private Connector[] _connectors;
    private UserRealm[] _realms;
    private Container _container=new Container();
    private SessionIdManager _sessionIdManager;
    private boolean _sendServerVersion = true; //send Server: header
    private boolean _sendDateHeader = false; //send Date: header 
    private AttributesMap _attributes = new AttributesMap();
    private List _dependentLifeCycles=new ArrayList();
    private int _graceful=0;
    
    /* ------------------------------------------------------------ */
    public Server()
    {
        setServer(this); 
    }
    
    /* ------------------------------------------------------------ */
    /** Convenience constructor
     * Creates server and a {@link SocketConnector} at the passed port.
     */
    public Server(int port)
    {
        setServer(this);

        Connector connector=new SocketConnector();
        connector.setPort(port);
        setConnectors(new Connector[]{connector});
    }


    /* ------------------------------------------------------------ */
    public static String getVersion()
    {
        return _version;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the container.
     */
    public Container getContainer()
    {
        return _container;
    }

    /* ------------------------------------------------------------ */
    public boolean getStopAtShutdown()
    {
        return hookThread.contains(this);
    }
    
    /* ------------------------------------------------------------ */
    public void setStopAtShutdown(boolean stop)
    {
        if (stop)
            hookThread.add(this);
        else
            hookThread.remove(this);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the connectors.
     */
    public Connector[] getConnectors()
    {
        return _connectors;
    }
    

    /* ------------------------------------------------------------ */
    public void addConnector(Connector connector)
    {
        setConnectors((Connector[])LazyList.addToArray(getConnectors(), connector, Connector.class));
    }

    /* ------------------------------------------------------------ */
    /**
     * Conveniance method which calls {@link #getConnectors()} and {@link #setConnectors(Connector[])} to 
     * remove a connector.
     * @param connector The connector to remove.
     */
    public void removeConnector(Connector connector) {
        setConnectors((Connector[])LazyList.removeFromArray (getConnectors(), connector));
    }

    /* ------------------------------------------------------------ */
    /** Set the connectors for this server.
     * Each connector has this server set as it's ThreadPool and its Handler.
     * @param connectors The connectors to set.
     */
    public void setConnectors(Connector[] connectors)
    {
        if (connectors!=null)
        {
            for (int i=0;i<connectors.length;i++)
                connectors[i].setServer(this);
        }
        
        _container.update(this, _connectors, connectors, "connector");
        _connectors = connectors;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the threadPool.
     */
    public ThreadPool getThreadPool()
    {
        return _threadPool;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param threadPool The threadPool to set.
     */
    public void setThreadPool(ThreadPool threadPool)
    {
        _container.update(this,_threadPool,threadPool, "threadpool",true);
        _threadPool = threadPool;
    }

    /* ------------------------------------------------------------ */
    protected void doStart() throws Exception
    {
        Log.info("jetty-"+_version);
        HttpGenerator.setServerVersion(_version);
        MultiException mex=new MultiException();
      
        for (int i=0;_realms !=null && i<_realms.length; i++)
        {
            if (_realms[i] instanceof LifeCycle)
                ((LifeCycle)_realms[i]).start();
        }

        Iterator itor = _dependentLifeCycles.iterator();
        while (itor.hasNext())
        {   
            try
            {
                ((LifeCycle)itor.next()).start(); 
            }
            catch (Throwable e) {mex.add(e);}
        }
        
        if (_threadPool==null)
        {
            QueuedThreadPool tp=new QueuedThreadPool();
            setThreadPool(tp);
        }
        
        if (_sessionIdManager!=null)
            _sessionIdManager.start();
        
        try
        {
            if (_threadPool instanceof LifeCycle)
                ((LifeCycle)_threadPool).start();
        } 
        catch(Throwable e) { mex.add(e);}
        
        try 
        { 
            super.doStart(); 
        } 
        catch(Throwable e) 
        { 
            Log.warn("Error starting handlers",e);
        }
        
        if (_connectors!=null)
        {
            for (int i=0;i<_connectors.length;i++)
            {
                try{_connectors[i].start();}
                catch(Throwable e)
                {
                    mex.add(e);
                }
            }
        }
        mex.ifExceptionThrow();
    }

    /* ------------------------------------------------------------ */
    protected void doStop() throws Exception
    {
        MultiException mex=new MultiException();
        
        for (int i=0;_realms !=null && i<_realms.length; i++)
        {
            if (_realms[i] instanceof LifeCycle)
                ((LifeCycle)_realms[i]).stop();
        }
        
        if (_graceful>0)
        {
            if (_connectors!=null)
            {
                for (int i=_connectors.length;i-->0;)
                {
                    Log.info("Graceful shutdown {}",_connectors[i]);
                    try{_connectors[i].close();}catch(Throwable e){mex.add(e);}
                }
            }
            
            Handler[] contexts = getChildHandlersByClass(Graceful.class);
            for (int c=0;c<contexts.length;c++)
            {
                Graceful context=(Graceful)contexts[c];
                Log.info("Graceful shutdown {}",context);
                context.setShutdown(true);
            }
            Thread.sleep(_graceful);
        }
        
        if (_connectors!=null)
        {
            for (int i=_connectors.length;i-->0;)
                try{_connectors[i].stop();}catch(Throwable e){mex.add(e);}
        }

        try {super.doStop(); } catch(Throwable e) { mex.add(e);}
        
        if (_sessionIdManager!=null)
            _sessionIdManager.stop();
        
        try
        {
            if (_threadPool instanceof LifeCycle)
                ((LifeCycle)_threadPool).stop();
        }
        catch(Throwable e){mex.add(e);}
        
        if (!_dependentLifeCycles.isEmpty())
        {
            ListIterator itor = _dependentLifeCycles.listIterator(_dependentLifeCycles.size());
            while (itor.hasPrevious())
            {
                try
                {
                    ((LifeCycle)itor.previous()).stop(); 
                }
                catch (Throwable e) {mex.add(e);}
            }
        }
       
        mex.ifExceptionThrow();
    }

    /* ------------------------------------------------------------ */
    /* Handle a request from a connection.
     * Called to handle a request on the connection when either the header has been received,
     * or after the entire request has been received (for short requests of known length).
     */
    public void handle(HttpConnection connection) throws IOException, ServletException
    {
        String target=connection.getRequest().getPathInfo();
        if (Log.isDebugEnabled())
        {
            Log.debug("REQUEST "+target+" on "+connection);
            handle(target, connection.getRequest(), connection.getResponse(), Handler.REQUEST);
            Log.debug("RESPONSE "+target+"  "+connection.getResponse().getStatus());
        }
        else
            handle(target, connection.getRequest(), connection.getResponse(), Handler.REQUEST);
    }

    /* ------------------------------------------------------------ */
    public void join() throws InterruptedException 
    {
        getThreadPool().join();
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Map of realm name to UserRealm instances.
     */
    public UserRealm[] getUserRealms()
    {
        return _realms;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param realms Map of realm name to UserRealm instances.
     */
    public void setUserRealms(UserRealm[] realms)
    {
        _container.update(this,_realms,realms, "realm",true);
        _realms=realms;
    }
    
    /* ------------------------------------------------------------ */
    public void addUserRealm(UserRealm realm)
    {
        setUserRealms((UserRealm[])LazyList.addToArray(getUserRealms(), realm, UserRealm.class));
    }
    
    /* ------------------------------------------------------------ */
    public void removeUserRealm(UserRealm realm)
    {
        setUserRealms((UserRealm[])LazyList.removeFromArray(getUserRealms(), realm));
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the sessionIdManager.
     */
    public SessionIdManager getSessionIdManager()
    {
        return _sessionIdManager;
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /**
     * @param sessionIdManager The sessionIdManager to set.
     */
    public void setSessionIdManager(SessionIdManager sessionIdManager)
    {
        _container.update(this,_sessionIdManager,sessionIdManager, "sessionIdManager",true);
        _sessionIdManager = sessionIdManager;
    }

    /* ------------------------------------------------------------ */
    public void setSendServerVersion (boolean sendServerVersion)
    {
        _sendServerVersion = sendServerVersion;
    }

    /* ------------------------------------------------------------ */
    public boolean getSendServerVersion()
    {
        return _sendServerVersion;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param sendDateHeader
     */
    public void setSendDateHeader(boolean sendDateHeader)
    {
        _sendDateHeader = sendDateHeader;
    }

    /* ------------------------------------------------------------ */
    public boolean getSendDateHeader()
    {
        return _sendDateHeader;
    }
    
    
    /**
     * Add a LifeCycle object to be started/stopped
     * along with the Server.
     * @param c
     */
    public void addLifeCycle (LifeCycle c)
    {
        if (c == null)
            return;
        if (!_dependentLifeCycles.contains(c)) 
        {
            _dependentLifeCycles.add(c);
            _container.addBean(c);
        }
        try
        {
            if (isStarted())
                ((LifeCycle)c).start();
        }
        catch (Exception e)
        {
            throw new RuntimeException (e);
        }
    }
    
    /**
     * Remove a LifeCycle object to be started/stopped 
     * along with the Server
     * @param c
     */
    public void removeLifeCycle (LifeCycle c)
    {
        if (c == null)
            return;
        _dependentLifeCycles.remove(c);
        _container.removeBean(c);
    }
    
 
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /**
     * ShutdownHook thread for stopping all servers.
     * 
     * Thread is hooked first time list of servers is changed.
     */
    private static class ShutdownHookThread extends Thread
    {
        private boolean hooked = false;
        private ArrayList servers = new ArrayList();

        /**
         * Hooks this thread for shutdown.
         * 
         * @see java.lang.Runtime#addShutdownHook(java.lang.Thread)
         */
        private void createShutdownHook()
        {
            if (!Boolean.getBoolean("JETTY_NO_SHUTDOWN_HOOK") && !hooked)
            {
                try
                {
                    Method shutdownHook = java.lang.Runtime.class.getMethod("addShutdownHook", new Class[]
                    { java.lang.Thread.class});
                    shutdownHook.invoke(Runtime.getRuntime(), new Object[]
                    { this});
                    this.hooked = true;
                }
                catch (Exception e)
                {
                    if (Log.isDebugEnabled())
                        Log.debug("No shutdown hook in JVM ", e);
                }
            }
        }

        /**
         * Add Server to servers list.
         */
        public boolean add(Server server)
        {
            createShutdownHook();
            return this.servers.add(server);
        }

        /**
         * Contains Server in servers list?
         */
        public boolean contains(Server server)
        {
            return this.servers.contains(server);
        }

        /**
         * Append all Servers from Collection
         */
        public boolean addAll(Collection c)
        {
            createShutdownHook();
            return this.servers.addAll(c);
        }

        /**
         * Clear list of Servers.
         */
        public void clear()
        {
            createShutdownHook();
            this.servers.clear();
        }

        /**
         * Remove Server from list.
         */
        public boolean remove(Server server)
        {
            createShutdownHook();
            return this.servers.remove(server);
        }

        /**
         * Remove all Servers in Collection from list.
         */
        public boolean removeAll(Collection c)
        {
            createShutdownHook();
            return this.servers.removeAll(c);
        }

        /**
         * Stop all Servers in list.
         */
        public void run()
        {
            setName("Shutdown");
            Log.info("Shutdown hook executing");
            Iterator it = servers.iterator();
            while (it.hasNext())
            {
                Server svr = (Server) it.next();
                if (svr == null)
                    continue;
                try
                {
                    svr.stop();
                }
                catch (Exception e)
                {
                    Log.warn(e);
                }
                Log.info("Shutdown hook complete");

                // Try to avoid JVM crash
                try
                {
                    Thread.sleep(1000);
                }
                catch (Exception e)
                {
                    Log.warn(e);
                }
            }
        }
    }

    
    

    /* ------------------------------------------------------------ */
    /**
     */
    public void addHandler(Handler handler)
    {
        if (getHandler() == null) 
            setHandler(handler);
        else if (getHandler() instanceof HandlerCollection)
            ((HandlerCollection)getHandler()).addHandler(handler);
        else
        {
            HandlerCollection collection=new HandlerCollection();
            collection.setHandlers(new Handler[]{getHandler(),handler});
            setHandler(collection);
        }
    }
    
    /* ------------------------------------------------------------ */
    /**
     */
    public void removeHandler(Handler handler)
    {
        if (getHandler() instanceof HandlerCollection)
            ((HandlerCollection)getHandler()).removeHandler(handler);
    }

    /* ------------------------------------------------------------ */
    /**
     */
    public Handler[] getHandlers()
    {
        if (getHandler() instanceof HandlerCollection)
            return ((HandlerCollection)getHandler()).getHandlers();
        
        return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     */
    public void setHandlers(Handler[] handlers)
    {
        HandlerCollection collection;
        if (getHandler() instanceof HandlerCollection)
            collection=(HandlerCollection)getHandler();
        else
        {
            collection=new HandlerCollection();
            setHandler(collection);
        }
            
        collection.setHandlers(handlers);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.util.AttributesMap#clearAttributes()
     */
    public void clearAttributes()
    {
        _attributes.clearAttributes();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.util.AttributesMap#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name)
    {
        return _attributes.getAttribute(name);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.util.AttributesMap#getAttributeNames()
     */
    public Enumeration getAttributeNames()
    {
        return AttributesMap.getAttributeNamesCopy(_attributes);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.util.AttributesMap#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String name)
    {
        _attributes.removeAttribute(name);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.util.AttributesMap#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String name, Object attribute)
    {
        _attributes.setAttribute(name, attribute);
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the graceful
     */
    public int getGracefulShutdown()
    {
        return _graceful;
    }

    /* ------------------------------------------------------------ */
    /**
     * Set graceful shutdown timeout.  If set, the {@link #doStop()} method will not immediately stop the 
     * server. Instead, all {@link Connector}s will be closed so that new connections will not be accepted
     * and all handlers that implement {@link Graceful} will be put into the shutdown mode so that no new requests
     * will be accepted, but existing requests can complete.  The server will then wait the configured timeout 
     * before stopping.
     * @param timeoutMS the milliseconds to wait for existing request to complete before stopping the server.
     * 
     */
    public void setGracefulShutdown(int timeoutMS)
    {
        _graceful=timeoutMS;
    }

    /* ------------------------------------------------------------ */
    /* A component that can be gracefully shutdown.
     * Called by doStop if a {@link #setGracefulShutdown} period is set.
     */
    public interface Graceful
    {
        public void setShutdown(boolean shutdown);
    }
}
