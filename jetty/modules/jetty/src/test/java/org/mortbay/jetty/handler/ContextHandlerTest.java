//========================================================================
//$Id$
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.mortbay.resource.Resource;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.LocalConnector;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.HttpConnection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

/**
 * @version $Revision$
 */
public class ContextHandlerTest extends TestCase
{
    public void testGetResourcePathsWhenSuppliedPathEndsInSlash() throws Exception
    {
        checkResourcePathsForExampleWebApp("/WEB-INF/");
    }

    public void testGetResourcePathsWhenSuppliedPathDoesNotEndInSlash() throws Exception
    {
        checkResourcePathsForExampleWebApp("/WEB-INF");
    }

    private void checkResourcePathsForExampleWebApp(String root) throws IOException, MalformedURLException
    {
        File testDirectory = setupTestDirectory();

        ContextHandler handler = new ContextHandler();

        assertTrue("Not a directory " + testDirectory,testDirectory.isDirectory());
        handler.setBaseResource(Resource.newResource(testDirectory.toURL()));

        List paths = new ArrayList(handler.getResourcePaths(root));
        assertEquals(2,paths.size());

        Collections.sort(paths);
        assertEquals("/WEB-INF/jsp/",paths.get(0));
        assertEquals("/WEB-INF/web.xml",paths.get(1));
    }

    private File setupTestDirectory() throws IOException
    {
        File tmp = File.createTempFile("cht",null);
        tmp.delete();
        tmp.mkdir();
        tmp.deleteOnExit();
        File root = new File(tmp,getClass().getName());
        root.mkdir();

        File webInf = new File(root,"WEB-INF");
        webInf.mkdir();

        new File(webInf,"jsp").mkdir();
        new File(webInf,"web.xml").createNewFile();

        return root;
    }

    public void testVirtualHostNormalization() throws Exception
    {
        Server server = new Server();
        LocalConnector connector = new LocalConnector();
        server.setConnectors(new Connector[]
        { connector });

        ContextHandler contextA = new ContextHandler("/");
        contextA.setVirtualHosts(new String[]
        { "www.example.com" });
        IsHandledHandler handlerA = new IsHandledHandler();
        contextA.setHandler(handlerA);

        ContextHandler contextB = new ContextHandler("/");
        IsHandledHandler handlerB = new IsHandledHandler();
        contextB.setHandler(handlerB);
        contextB.setVirtualHosts(new String[]
        { "www.example2.com." });

        ContextHandler contextC = new ContextHandler("/");
        IsHandledHandler handlerC = new IsHandledHandler();
        contextC.setHandler(handlerC);

        HandlerCollection c = new HandlerCollection();

        c.addHandler(contextA);
        c.addHandler(contextB);
        c.addHandler(contextC);

        server.setHandler(c);

        try
        {
            server.start();
            connector.getResponses("GET / HTTP/1.1\n" + "Host: www.example.com.\n\n");

            assertTrue(handlerA.isHandled());
            assertFalse(handlerB.isHandled());
            assertFalse(handlerC.isHandled());

            handlerA.reset();
            handlerB.reset();
            handlerC.reset();

            connector.getResponses("GET / HTTP/1.1\n" + "Host: www.example2.com\n\n");

            assertFalse(handlerA.isHandled());
            assertTrue(handlerB.isHandled());
            assertFalse(handlerC.isHandled());

        }
        finally
        {
            server.stop();
        }

    }

    public void testVirtualHostWildcard() throws Exception
    {
        Server server = new Server();
        LocalConnector connector = new LocalConnector();
        server.setConnectors(new Connector[] { connector });

        ContextHandler context = new ContextHandler("/");
        
        IsHandledHandler handler = new IsHandledHandler();
        context.setHandler(handler);

        server.addHandler(context);

        try
        {
            server.start();
            checkWildcardHost(true,server,new String[] {"example.com", "*.example.com"}, new String[] {"example.com", ".example.com", "vhost.example.com"});
            checkWildcardHost(false,server,new String[] {"example.com", "*.example.com"}, new String[] {"badexample.com", ".badexample.com", "vhost.badexample.com"});
            
            checkWildcardHost(false,server,new String[] {"*."}, new String[] {"anything.anything"});
            
            checkWildcardHost(true,server,new String[] {"*.example.com"}, new String[] {"vhost.example.com", ".example.com"});
            checkWildcardHost(false,server,new String[] {"*.example.com"}, new String[] {"vhost.www.example.com", "example.com", "www.vhost.example.com"});

            checkWildcardHost(true,server,new String[] {"*.sub.example.com"}, new String[] {"vhost.sub.example.com", ".sub.example.com"});
            checkWildcardHost(false,server,new String[] {"*.sub.example.com"}, new String[] {".example.com", "sub.example.com", "vhost.example.com"});
            
            checkWildcardHost(false,server,new String[] {"example.*.com","example.com.*"}, new String[] {"example.vhost.com", "example.com.vhost", "example.com"});            
        }
        finally
        {
            server.stop();
        }
    }

    private void checkWildcardHost(boolean succeed, Server server, String[] contextHosts, String[] requestHosts) throws Exception
    {
        LocalConnector connector = (LocalConnector)server.getConnectors()[0];
        ContextHandler context = (ContextHandler)server.getHandler();
        context.setVirtualHosts(contextHosts);
        
        IsHandledHandler handler = (IsHandledHandler)context.getHandler();
        for(int i=0; i < requestHosts.length; ++i)
        {
            String host = requestHosts[i];
            connector.getResponses("GET / HTTP/1.1\n" + "Host: "+host+"\n\n");
            if(succeed) 
                assertTrue("'"+host+"' should have been handled.",handler.isHandled());
            else
                assertFalse("'"+host + "' should not have been handled.", handler.isHandled());
            handler.reset();
        }

    }

    public static final class IsHandledHandler extends AbstractHandler
    {
        private boolean handled;

        public boolean isHandled()
        {
            return handled;
        }

        public void handle(String s, HttpServletRequest request, HttpServletResponse response, int i) throws IOException, ServletException
        {
            Request base_request = (request instanceof Request)?(Request)request:HttpConnection.getCurrentConnection().getRequest();
            base_request.setHandled(true);
            this.handled = true;
        }

        public void reset()
        {
            handled = false;
        }
    }

    public void testAttributes() throws Exception
    {
        ContextHandler handler = new ContextHandler();
        handler.setAttribute("aaa","111");
        handler.getServletContext().setAttribute("bbb","222");
        assertEquals("111",handler.getServletContext().getAttribute("aaa"));
        assertEquals("222",handler.getAttribute("bbb"));
        
        handler.start();

        handler.getServletContext().setAttribute("aaa","000");
        handler.setAttribute("ccc","333");
        handler.getServletContext().setAttribute("ddd","444");
        assertEquals("111",handler.getServletContext().getAttribute("aaa"));
        assertEquals("222",handler.getServletContext().getAttribute("bbb"));
        assertEquals("333",handler.getServletContext().getAttribute("ccc"));
        assertEquals("444",handler.getServletContext().getAttribute("ddd"));
        
        assertEquals("111",handler.getAttribute("aaa"));
        assertEquals("222",handler.getAttribute("bbb"));
        assertEquals("333",handler.getAttribute("ccc"));
        assertEquals(null,handler.getAttribute("ddd"));
        

        handler.stop();

        assertEquals("111",handler.getServletContext().getAttribute("aaa"));
        assertEquals("222",handler.getServletContext().getAttribute("bbb"));
        assertEquals("333",handler.getServletContext().getAttribute("ccc"));
        assertEquals(null,handler.getServletContext().getAttribute("ddd"));
        

    }
}
