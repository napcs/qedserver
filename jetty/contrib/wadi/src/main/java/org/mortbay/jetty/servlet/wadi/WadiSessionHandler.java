package org.mortbay.jetty.servlet.wadi;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.web.impl.WebInvocation;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpException;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.SessionManager;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.servlet.SessionHandler;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;

public class WadiSessionHandler extends SessionHandler
{	
	public WadiSessionHandler(SessionManager sessionManager)
	{
        super(sessionManager);
	}

    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
    {
        setRequestedId(request, dispatch);
        
        WadiClusteredInvocation invocation = new WadiClusteredInvocation(target,request,response,dispatch);
        try
        {
            invocation.invoke();
        }
        catch (Exception e)
        {
            Log.warn(e);
            Throwable cause = e.getCause();
            if (cause instanceof HttpException) 
            {
                throw (HttpException) cause;
            } 
            else if (cause instanceof IOException) 
            {
                throw (IOException) cause;
            } 
            else 
            {
                throw (IOException) new IOException().initCause(cause);
            }
        }
        
    }

	protected class WadiClusteredInvocation
    {
        protected final String target;
        protected final HttpServletRequest request;
        protected final HttpServletResponse response;
        protected final int dispatch;

        protected WadiClusteredInvocation(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) {
            this.target = target;
            this.request = request;
            this.response = response;
            this.dispatch = dispatch;
        }

        public void invoke() throws Exception 
        {
            WebInvocation invocation = new WebInvocation();
            invocation.setDoNotExecuteOnEndProcessing(true);
            FilterChain chainAdapter = new FilterChain() {
                public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException 
                {
                    try 
                    {
                        invokeLocally();
                    }
                    catch (Exception e) 
                    {
                        throw (IOException) new IOException().initCause(e);
                    }
                }
            };
            invocation.init(request, response, chainAdapter);
            try 
            {
                WadiSessionManager wSessionManager = (WadiSessionManager)getSessionManager();
                wSessionManager.getClusteredManager().contextualise(invocation);
            } 
            catch (InvocationException e) {
                Throwable throwable = e.getCause();
                if (throwable instanceof IOException) 
                {
                    throw new Exception(throwable);
                } 
                else if 
                (throwable instanceof ServletException) 
                {
                    throw new Exception(throwable);
                } 
                else 
                {
                    throw new Exception(e);
                }
            }
        }
        
        protected void invokeLocally() throws  Exception
        {
            WadiSessionHandler.super.handle(target, request, response, dispatch);
        }

        public String getRequestedSessionId() 
        {
            return request.getRequestedSessionId();
        }
    }
    
    public static void main(String args[]) throws Exception
    {
        String jetty_home=System.getProperty("jetty.home","../../..");

        String jetty_port=System.getProperty("jetty.port", "8080");

        String node_name=System.getProperty("node.name", "red");

        Server server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(Integer.parseInt(jetty_port));
        server.setConnectors(new Connector[]{connector});
        
        HandlerCollection handlers = new HandlerCollection();
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        
        //TODO: find a way to dynamically get the endpoint url
        WadiCluster wadiCluster = new WadiCluster("CLUSTER", node_name, "http://localhost:"+jetty_port+"/test");
        wadiCluster.doStart();
        
        WadiSessionManager wadiManager = new WadiSessionManager(wadiCluster, 2, 24, 360);
        
        WadiSessionHandler wSessionHandler = new WadiSessionHandler(wadiManager);
        WebAppContext wah = new WebAppContext(null, wSessionHandler, null, null);
        wah.setContextPath("/test");
        wah.setResourceBase(jetty_home+"/webapps/test");
        
        contexts.setHandlers(new Handler[]{wah});
        handlers.setHandlers(new Handler[]{contexts,new DefaultHandler()});
        server.setHandler(handlers);

        HashUserRealm hur = new HashUserRealm();
        hur.setName("Test Realm");
        hur.setConfig(jetty_home+"/etc/realm.properties");
        wah.getSecurityHandler().setUserRealm(hur);
        
        server.start();
        server.join();
    }
}
