//========================================================================
//$Id: HttpGeneratorTest.java,v 1.1 2005/10/05 14:09:41 janb Exp $
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

package org.mortbay.jetty;


import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionContext;

import junit.framework.TestCase;

import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.servlet.AbstractSessionManager;
import org.mortbay.jetty.servlet.HashSessionIdManager;
import org.mortbay.jetty.servlet.HashSessionManager;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ResponseTest extends TestCase
{
    Server server = new Server();
    LocalConnector connector = new LocalConnector();
    
    public ResponseTest(String arg0)
    {
        super(arg0);
        server.setConnectors(new Connector[]{connector});
        server.setHandler(new DumpHandler());
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(ResponseTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        server.start();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
        server.stop();
    }
    
    
    public void testContentType()
    	throws Exception
    {

        HttpConnection connection = new HttpConnection(connector,connector._endp,connector._server);
        Response response = connection.getResponse();
        
        assertEquals(null,response.getContentType());
        
        response.setContentType("foo/bar");
        assertEquals("foo/bar",response.getContentType());
        response.getWriter();
        assertEquals("foo/bar; charset=ISO-8859-1",response.getContentType());
        response.setContentType("foo2/bar2");
        assertEquals("foo2/bar2; charset=ISO-8859-1",response.getContentType());
        response.setHeader("name","foo");
        Enumeration en=response.getHeaders("name");
        assertEquals("foo",en.nextElement());
        assertFalse(en.hasMoreElements());
        response.addHeader("name","bar");
        en=response.getHeaders("name");
        assertEquals("foo",en.nextElement());
        assertEquals("bar",en.nextElement());
        assertFalse(en.hasMoreElements());
        
        response.recycle();
        
        response.setContentType("text/html");
        assertEquals("text/html",response.getContentType());
        response.getWriter();
        assertEquals("text/html; charset=iso-8859-1",response.getContentType());
        response.setContentType("foo2/bar2");
        assertEquals("foo2/bar2; charset=ISO-8859-1",response.getContentType());

        response.recycle();
    }


    public void testLocale()
        throws Exception
    {

        HttpConnection connection = new HttpConnection(connector,connector._endp,connector._server);
        Request request = connection.getRequest();
        Response response = connection.getResponse();
        ContextHandler context = new ContextHandler();
        context.addLocaleEncoding(Locale.ENGLISH.toString(),"ISO-8859-1");
        context.addLocaleEncoding(Locale.ITALIAN.toString(),"ISO-8859-2");
        request.setContext(context.getServletContext());

        response.setLocale(java.util.Locale.ITALIAN);
        assertEquals(null,response.getContentType());
        response.setContentType("text/plain");
        assertEquals("text/plain; charset=ISO-8859-2",response.getContentType());

        response.recycle();
        response.setContentType("text/plain");
        response.setCharacterEncoding("utf-8");
        response.setLocale(java.util.Locale.ITALIAN);
        assertEquals("text/plain; charset=utf-8",response.getContentType());
        assertTrue(response.toString().indexOf("charset=utf-8")>0);
    }
    
    public void testContentTypeCharacterEncoding()
        throws Exception
    {
        HttpConnection connection = new HttpConnection(connector,connector._endp,connector._server);
        
        Request request = connection.getRequest();
        Response response = connection.getResponse();
        
        
        response.setContentType("foo/bar");
        response.setCharacterEncoding("utf-8");
        assertEquals("foo/bar; charset=utf-8",response.getContentType());
        response.getWriter();
        assertEquals("foo/bar; charset=utf-8",response.getContentType());
        response.setContentType("foo2/bar2");
        assertEquals("foo2/bar2; charset=utf-8",response.getContentType());
        response.setCharacterEncoding("ISO-8859-1");
        assertEquals("foo2/bar2; charset=utf-8",response.getContentType());

        response.recycle();
        
        response.setContentType("text/html");
        response.setCharacterEncoding("utf-8");
        assertEquals("text/html; charset=utf-8",response.getContentType());
        response.getWriter();
        assertEquals("text/html; charset=utf-8",response.getContentType());
        response.setContentType("text/xml");
        assertEquals("text/xml; charset=utf-8",response.getContentType());
        response.setCharacterEncoding("ISO-8859-1");
        assertEquals("text/xml; charset=utf-8",response.getContentType());
        
    }
    
    public void testCharacterEncodingContentType()
    throws Exception
    {
        Response response = new Response(new HttpConnection(connector,connector._endp,connector._server));
        
        response.setCharacterEncoding("utf-8");
        response.setContentType("foo/bar");
        assertEquals("foo/bar; charset=utf-8",response.getContentType());
        response.getWriter();
        assertEquals("foo/bar; charset=utf-8",response.getContentType());
        response.setContentType("foo2/bar2");
        assertEquals("foo2/bar2; charset=utf-8",response.getContentType());
        response.setCharacterEncoding("ISO-8859-1");
        assertEquals("foo2/bar2; charset=utf-8",response.getContentType());
        
        response.recycle();
        
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/html");
        assertEquals("text/html; charset=utf-8",response.getContentType());
        response.getWriter();
        assertEquals("text/html; charset=utf-8",response.getContentType());
        response.setContentType("text/xml");
        assertEquals("text/xml; charset=utf-8",response.getContentType());
        response.setCharacterEncoding("iso-8859-1");
        assertEquals("text/xml; charset=utf-8",response.getContentType());
        
    }

    public void testContentTypeWithCharacterEncoding()
        throws Exception
    {
        Response response = new Response(new HttpConnection(connector,connector._endp,connector._server));
        
        response.setCharacterEncoding("utf16");
        response.setContentType("foo/bar; charset=utf-8");
        assertEquals("foo/bar; charset=utf-8",response.getContentType());
        response.getWriter();
        assertEquals("foo/bar; charset=utf-8",response.getContentType());
        response.setContentType("foo2/bar2");
        assertEquals("foo2/bar2; charset=utf-8",response.getContentType());
        response.setCharacterEncoding("ISO-8859-1");
        assertEquals("foo2/bar2; charset=utf-8",response.getContentType());

        response.recycle();

        response.setCharacterEncoding("utf16");
        response.setContentType("text/html; charset=utf-8");
        assertEquals("text/html; charset=utf-8",response.getContentType());
        response.getWriter();
        assertEquals("text/html; charset=utf-8",response.getContentType());
        response.setContentType("text/xml");
        assertEquals("text/xml; charset=utf-8",response.getContentType());
        response.setCharacterEncoding("iso-8859-1");
        assertEquals("text/xml; charset=utf-8",response.getContentType());
        
    }
    
    public void testContentTypeWithOther()
    throws Exception
    {
        Response response = new Response(new HttpConnection(connector,connector._endp,connector._server));
        
        response.setContentType("foo/bar; other=xyz");
        assertEquals("foo/bar; other=xyz",response.getContentType());
        response.getWriter();
        assertEquals("foo/bar; other=xyz; charset=ISO-8859-1",response.getContentType());
        response.setContentType("foo2/bar2");
        assertEquals("foo2/bar2; charset=ISO-8859-1",response.getContentType());
        
        response.recycle();
        
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/html; other=xyz");
        assertEquals("text/html; other=xyz; charset=utf-8",response.getContentType());
        response.getWriter();
        assertEquals("text/html; other=xyz; charset=utf-8",response.getContentType());
        response.setContentType("text/xml");
        assertEquals("text/xml; charset=utf-8",response.getContentType());       
    }
    

    public void testContentTypeWithCharacterEncodingAndOther()
        throws Exception
    {
        Response response = new Response(new HttpConnection(connector,connector._endp,connector._server));

        response.setCharacterEncoding("utf16");
        response.setContentType("foo/bar; charset=utf-8 other=xyz");
        assertEquals("foo/bar; charset=utf-8 other=xyz",response.getContentType());
        response.getWriter();
        assertEquals("foo/bar; charset=utf-8 other=xyz",response.getContentType());

        response.recycle();

        response.setCharacterEncoding("utf16");
        response.setContentType("text/html; other=xyz charset=utf-8");
        assertEquals("text/html; other=xyz charset=utf-8",response.getContentType());
        response.getWriter();
        assertEquals("text/html; other=xyz charset=utf-8",response.getContentType());

        response.recycle();
        
        response.setCharacterEncoding("utf16");
        response.setContentType("foo/bar; other=pq charset=utf-8 other=xyz");
        assertEquals("foo/bar; other=pq charset=utf-8 other=xyz",response.getContentType());
        response.getWriter();
        assertEquals("foo/bar; other=pq charset=utf-8 other=xyz",response.getContentType());

    }
    
    public void testStatusCodes() throws Exception
    {
        Response response=newResponse();

        response.sendError(404);
        assertEquals(404, response.getStatus());
        assertEquals(null, response.getReason());
        
        response=newResponse();
        
        response.sendError(500, "Database Error");
        assertEquals(500, response.getStatus());
        assertEquals("Database Error", response.getReason());
        assertEquals("must-revalidate,no-cache,no-store", response.getHeader(HttpHeaders.CACHE_CONTROL));

        response=newResponse();
        
        response.setStatus(200);
        assertEquals(200, response.getStatus());
        assertEquals(null, response.getReason());
        
        response=newResponse();
        
        response.sendError(406, "Super Nanny");
        assertEquals(406, response.getStatus());
        assertEquals("Super Nanny", response.getReason());
        assertEquals("must-revalidate,no-cache,no-store", response.getHeader(HttpHeaders.CACHE_CONTROL));
    }
    
    public void testEncodeRedirect()
        throws Exception
    {
        HttpConnection connection=new HttpConnection(connector,connector._endp,connector._server);
        Response response = new Response(connection);
        Request request = connection.getRequest();
        
        assertEquals("http://host:port/path/info;param?query=0&more=1#target",response.encodeRedirectUrl("http://host:port/path/info;param?query=0&more=1#target"));
       
        request.setRequestedSessionId("12345");
        request.setRequestedSessionIdFromCookie(false);
        AbstractSessionManager manager=new HashSessionManager();
        manager.setIdManager(new HashSessionIdManager());
        request.setSessionManager(manager);
        request.setSession(new TestSession(manager,"12345"));
        
        assertEquals("http://host:port/path/info;param;jsessionid=12345?query=0&more=1#target",response.encodeRedirectUrl("http://host:port/path/info;param?query=0&more=1#target"));
              
    }

    public void testSetBufferSize ()
    throws Exception
    {
        Response response = new Response(new HttpConnection(connector,connector._endp,connector._server));
        response.setBufferSize(20*1024);
        response.getWriter().print("hello");
        try
        {
            response.setBufferSize(21*1024);
            fail("Expected IllegalStateException on Request.setBufferSize");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalStateException);
        }
    }
    
    public void testHead() throws Exception
    {
        Server server = new Server();
        try
        {
            SocketConnector socketConnector = new SocketConnector();
            socketConnector.setPort(0);
            server.addConnector(socketConnector);
            server.addHandler(new AbstractHandler() 
            {
                public void handle(String string, HttpServletRequest request, HttpServletResponse response, int i) throws IOException, ServletException 
                {
                    response.setStatus(200);
                    response.setContentType("text/plain");
                    PrintWriter w = response.getWriter();
                    w.flush();
                    w.println("Geht");
                    w.flush();
                    w.println("Doch");
                    ((Request) request).setHandled(true);
                }
            });
            server.start();

            Socket socket = new Socket("localhost",socketConnector.getLocalPort());
            socket.getOutputStream().write("HEAD / HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes());
            socket.getOutputStream().write("GET / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".getBytes());
            socket.getOutputStream().flush();
            
            LineNumberReader reader = new LineNumberReader(new InputStreamReader(socket.getInputStream()));
            String line = reader.readLine();
            while (line!=null && line.length()>0)
                line = reader.readLine();

            while (line!=null && line.length()==0)
                line = reader.readLine();

            assertTrue(line.startsWith("HTTP/1.1 200 OK"));
            
        }
        finally
        {
            server.stop();
        }
    }
    
    private Response newResponse()
    {
        HttpConnection connection=new HttpConnection(connector,connector._endp,connector._server);
        connection.getGenerator().reset(false);
        HttpConnection.setCurrentConnection(connection);
        Response response = connection.getResponse();
        connection.getRequest().setRequestURI("/test");
        return response;
    }
    
    class TestSession extends AbstractSessionManager.Session
    {
        public TestSession(AbstractSessionManager abstractSessionManager, String id)
        {
            abstractSessionManager.super(System.currentTimeMillis(), id);
        }

        public Object getAttribute(String name) 
        {
            return null;
        }

        public Enumeration getAttributeNames()
        {

            return null;
        }

        public long getCreationTime()
        {

            return 0;
        }

        public String getId()
        {
            return "12345";
        }

        public long getLastAccessedTime()
        {
            return 0;
        }

        public int getMaxInactiveInterval()
        {
            return 0;
        }

        public ServletContext getServletContext()
        {
            return null;
        }

        public HttpSessionContext getSessionContext()
        {
            return null;
        }

        public Object getValue(String name)
        {
            return null;
        }

        public String[] getValueNames()
        {
            return null;
        }

        public void invalidate()
        {
        }

        public boolean isNew()
        {
            return false;
        }

        public void putValue(String name, Object value)
        {
        }

        public void removeAttribute(String name)
        {
        }

        public void removeValue(String name)
        {   
        }

        public void setAttribute(String name, Object value)
        {
        }

        public void setMaxInactiveInterval(int interval)
        {
        }

        protected Map newAttributeMap()
        {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
