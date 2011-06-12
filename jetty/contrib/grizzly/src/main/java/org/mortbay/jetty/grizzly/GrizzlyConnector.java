//========================================================================
// Parts Copyright 2006 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//========================================================================



package org.mortbay.jetty.grizzly;


import com.sun.grizzly.Context;
import com.sun.grizzly.Controller;
import com.sun.grizzly.Controller.Protocol;
import com.sun.grizzly.DefaultPipeline;
import com.sun.grizzly.DefaultProtocolChain;
import com.sun.grizzly.DefaultProtocolChainInstanceHandler;
import com.sun.grizzly.DefaultSelectionKeyHandler;
import com.sun.grizzly.Pipeline;
import com.sun.grizzly.ProtocolChain;
import com.sun.grizzly.TCPSelectorHandler;
import com.sun.grizzly.filter.ReadFilter;
import com.sun.grizzly.util.WorkerThread;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.mortbay.io.EndPoint;
import org.mortbay.io.nio.NIOBuffer;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpParser;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.nio.AbstractNIOConnector;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;
import org.mortbay.thread.BoundedThreadPool;
import org.mortbay.thread.QueuedThreadPool;

/* ------------------------------------------------------------------------------- */
/**
 * @author gregw
 *
 */
public class GrizzlyConnector extends AbstractNIOConnector
{
    protected Controller controller;

    
    /* ------------------------------------------------------------------------------- */
    /**
     * Constructor.
     *
     */
    public GrizzlyConnector()
    {
        
    }
    
    /* ------------------------------------------------------------ */
    public Object getConnection()
    {
        return controller.getSelectorHandler(Protocol.TCP);
    }
    
    /* ------------------------------------------------------------ */
    /*
     * @see org.mortbay.jetty.AbstractConnector#doStart()
     */
    protected void doStart() throws Exception
    {
        super.doStart();
        
        // TODO - is there a non-blocking way to do this?
        new Thread()
        {
            public void run()
            {
                try
                {
                    controller.start();
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }.start();
    }
    
    /* ------------------------------------------------------------ */
    /*
     * @see org.mortbay.jetty.AbstractConnector#doStop()
     */
    protected void doStop() throws Exception
    {
        super.doStop();
        controller.stop();
    }
    
    
    /* ------------------------------------------------------------ */
    public void open() throws IOException
    {
        controller = new Controller();
        TCPSelectorHandler selectorHandler = new TCPSelectorHandler();
        selectorHandler.setPort(getPort());
        if (getHost() != null)
        {
            selectorHandler.setInet(InetAddress.getByName(getHost()));
        }
        controller.setSelectorHandler(selectorHandler);
        selectorHandler.setSelectionKeyHandler(new DefaultSelectionKeyHandler());
        
        DefaultProtocolChainInstanceHandler instanceHandler
                = new DefaultProtocolChainInstanceHandler()
        {
            
            private ConcurrentLinkedQueue<ProtocolChain>
                    protocolChains = new ConcurrentLinkedQueue<ProtocolChain>();
            @Override
            public ProtocolChain poll()
            {
                ProtocolChain pc = protocolChains.poll();
                if (pc == null)
                {
                    final GrizzlyEndPoint endPoint;
                    final HttpParser parser;                    
                    try
                    {
                        endPoint=new GrizzlyEndPoint(GrizzlyConnector.this,null);
                        parser=(HttpParser)((HttpConnection)endPoint.getHttpConnection()).getParser();
                    }
                    catch (IOException ex)
                    {
                        throw new RuntimeException(ex);
                    }
                    
                    
                    pc = new DefaultProtocolChain();
                    ReadFilter readFilter = new JettyReadFilter(parser);
                    pc.addFilter(readFilter);
                    
                    HttpProtocolFilter httpProtocolFilter
                            = new HttpProtocolFilter();
                    httpProtocolFilter.setParser(parser);
                    httpProtocolFilter.setEndPoint(endPoint);
                    pc.addFilter(httpProtocolFilter);
                }
                return pc;
            }
            
            /**
             * Pool an instance of ProtocolChain.
             */
            @Override
            public boolean offer(ProtocolChain pc)
            {
                return protocolChains.offer(pc);
            }
        };
        
        controller.setProtocolChainInstanceHandler(instanceHandler);
        Pipeline pipeline = new DefaultPipeline();

        if(getServer().getThreadPool() instanceof BoundedThreadPool)
            pipeline.setMaxThreads(((BoundedThreadPool)getServer().getThreadPool()).getMaxThreads());
        else if (getServer().getThreadPool() instanceof QueuedThreadPool)
            pipeline.setMaxThreads(((QueuedThreadPool)getServer().getThreadPool()).getMaxThreads());
        controller.setPipeline(pipeline);
    }
    
    private class JettyReadFilter extends ReadFilter
    {
        
        public HttpParser parser;
        
        public JettyReadFilter(HttpParser parser)
        {
            this.parser = parser;
        }
        
        public boolean execute(Context ctx) throws IOException
        {
            WorkerThread workerThread =
                    (WorkerThread)Thread.currentThread();

            // Use Jetty's ByteBuffer
            ByteBuffer bb = ((NIOBuffer)parser.getHeaderBuffer()).getByteBuffer();
            workerThread.setByteBuffer(bb);
            return super.execute(ctx);
        }        
    }
    
    /* ------------------------------------------------------------ */
    public void close() throws IOException
    {
        // TODO Close server socket
        // XXX Only supported when calling selectorThread.stopEndpoint();
    }
    
    /* ------------------------------------------------------------ */
    public void accept(int acceptorID) throws IOException
    {
        try
        {
            // TODO - this may not exactly be right.  accept is called in a loop, so we
            // may need to wait on the _selectorThread somehow?
            // maybe we just set acceptors to zero and don't need to bother here as
            // grizzly has it's own accepting threads.
            if (controller.isStarted())
            {
                Thread.sleep(5000);
            }
            
        }
        catch (Throwable e)
        {
            // TODO Auto-generated catch block
            Log.ignore(e);
        }
        
    }
    
    /* ------------------------------------------------------------ */
    public void stopAccept(int acceptorID) throws Exception
    {
        // TODO
    }
    
    
    /* ------------------------------------------------------------------------------- */
    public void customize(EndPoint endpoint, Request request) throws IOException
    {
        super.customize(endpoint, request);
    }
    
    
    /* ------------------------------------------------------------------------------- */
    public int getLocalPort()
    {
        // TODO return the actual port we are listening on
        return getPort();
    }
    
    
    /* ------------------------------------------------------------------------------- */
    /** temp main - just to help testing */
    public static void main(String[] args)
    throws Exception
    {
        Server server = new Server();
        Connector connector=new GrizzlyConnector();
        connector.setPort(8080);
        server.setConnectors(new Connector[]{connector});
        
        HandlerCollection handlers = new HandlerCollection();
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        handlers.setHandlers(new Handler[]{contexts,new DefaultHandler()});
        server.setHandler(handlers);
        
        // TODO add javadoc context to contexts
        
        WebAppContext.addWebApplications(server, "../../webapps", "org/mortbay/jetty/webapp/webdefault.xml", true, false);
        
        HashUserRealm userRealm = new HashUserRealm();
        userRealm.setName("Test Realm");
        userRealm.setConfig("../../etc/realm.properties");
        server.setUserRealms(new UserRealm[]{userRealm});
        
        
        server.start();
        server.join();
        
    }
}
