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
import java.io.Reader;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.util.IO;
import org.mortbay.util.StringUtil;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RequestTest extends TestCase
{
    Server _server = new Server();
    LocalConnector _connector = new LocalConnector();
    RequestHandler _handler = new RequestHandler();
    
    {
        _connector.setHeaderBufferSize(512);
        _connector.setRequestBufferSize(1024);
        _connector.setResponseBufferSize(2048);
    }
    
    public RequestTest(String arg0)
    {
        super(arg0);
        _server.setConnectors(new Connector[]{_connector});
        
        _server.setHandler(_handler);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(RequestTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        _server.start();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
        _server.stop();
    }
    
    
    public void testContentTypeEncoding()
    	throws Exception
    {
        final ArrayList results = new ArrayList();
        _handler._checker = new RequestTester()
        {
            public boolean check(HttpServletRequest request,HttpServletResponse response)
            {
                results.add(request.getContentType());
                results.add(request.getCharacterEncoding());
                return true;
            }  
        };
        
        _connector.getResponses(
                "GET / HTTP/1.1\n"+
                "Host: whatever\n"+
                "Content-Type: text/test\n"+
                "\n"+
               
                "GET / HTTP/1.1\n"+
                "Host: whatever\n"+
                "Content-Type: text/html;charset=utf8\n"+
                "\n"+
                
                "GET / HTTP/1.1\n"+
                "Host: whatever\n"+
                "Content-Type: text/html; charset=\"utf8\"\n"+
                "\n"+
                
                "GET / HTTP/1.1\n"+
                "Host: whatever\n"+
                "Content-Type: text/html; other=foo ; blah=\"charset=wrong;\" ; charset =   \" x=z; \"   ; more=values \n"+
                "\n"
                );
        
        int i=0;
        assertEquals("text/test",results.get(i++));
        assertEquals(null,results.get(i++));
        
        assertEquals("text/html;charset=utf8",results.get(i++));
        assertEquals("utf8",results.get(i++));
        
        assertEquals("text/html; charset=\"utf8\"",results.get(i++));
        assertEquals("utf8",results.get(i++));
        
        assertTrue(((String)results.get(i++)).startsWith("text/html"));
        assertEquals(" x=z; ",results.get(i++));
        
        
    }
    

    
    public void testContent()
        throws Exception
    {
      
        final int[] length=new int[1];
        
        _handler._checker = new RequestTester()
        {
            public boolean check(HttpServletRequest request,HttpServletResponse response)
            {
                assertEquals(request.getContentLength(), ((Request)request).getContentRead());
                length[0]=request.getContentLength();
                return true;
            }  
        };
        
        String content="";
        
        for (int l=0;l<1025;l++)
        {
            String request="POST / HTTP/1.1\r\n"+
            "Host: whatever\r\n"+
            "Content-Type: text/test\r\n"+
            "Content-Length: "+l+"\r\n"+
            "Connection: close\r\n"+
            "\r\n"+
            content;           
            _connector.reopen();
            _connector.getResponses(request);
            assertEquals(l,length[0]);
            if (l>0)
                assertEquals(l,_handler._content.length());
            content+="x";
        }
    }


    public void testPartialRead()
        throws Exception
    {
        Handler handler = new AbstractHandler()
        {
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
            {
                Request baseRequest = (Request)request;
                baseRequest.setHandled(true);
                Reader reader=request.getReader();
                byte[] b=("read="+reader.read()+"\n").getBytes(StringUtil.__UTF8);
                response.setContentLength(b.length);
                response.getOutputStream().write(b);
                response.flushBuffer();
            }
            
        };
        _server.stop();
        _server.setHandler(handler);
        _server.start();

        String request="GET / HTTP/1.1\r\n"+
        "Host: whatever\r\n"+
        "Content-Type: text/plain\r\n"+
        "Content-Length: "+10+"\r\n"+
        "\r\n"+
        "0123456789\r\n"+
        "GET / HTTP/1.1\r\n"+
        "Host: whatever\r\n"+
        "Content-Type: text/plain\r\n"+
        "Content-Length: "+10+"\r\n"+
        "Connection: close\r\n"+
        "\r\n"+
        "ABCDEFGHIJ\r\n";

        String responses = _connector.getResponses(request);
        
        int index=responses.indexOf("read="+(int)'0');
        assertTrue(index>0);
        
        index=responses.indexOf("read="+(int)'A',index+7);
        assertTrue(index>0);
        
    }
    

    public void testQueryAfterRead()
        throws Exception
    {
        Handler handler = new AbstractHandler()
        {
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
            {
                Request baseRequest = (Request)request;
                baseRequest.setHandled(true);
                Reader reader=request.getReader();
                String in = IO.toString(reader);
                String param = request.getParameter("param");
                
                byte[] b=("read='"+in+"' param="+param+"\n").getBytes(StringUtil.__UTF8);
                response.setContentLength(b.length);
                response.getOutputStream().write(b);
                response.flushBuffer();
            }
            
        };
        _server.stop();
        _server.setHandler(handler);
        _server.start();

        String request="POST /?param=right HTTP/1.1\r\n"+
        "Host: whatever\r\n"+
        "Content-Type: application/x-www-form-urlencoded\r\n"+
        "Content-Length: "+11+"\r\n"+
        "Connection: close\r\n"+
        "\r\n"+
        "param=wrong\r\n";

        String responses = _connector.getResponses(request);
        
        assertTrue(responses.indexOf("read='param=wrong' param=right")>0);
        
    }
    
    
    public void testConnectionClose()
        throws Exception
    {
        String response;

        _handler._checker = new RequestTester()
        {
            public boolean check(HttpServletRequest request,HttpServletResponse response) throws IOException
            {
                response.getOutputStream().println("Hello World");
                return true;
            }  
        };

        _connector.reopen();
        response=_connector.getResponses(
                    "GET / HTTP/1.1\n"+
                    "Host: whatever\n"+
                    "\n"
                    );
        assertTrue(response.indexOf("200")>0);
        assertFalse(response.indexOf("Connection: close")>0);
        assertTrue(response.indexOf("Hello World")>0);
        
        _connector.reopen();
        response=_connector.getResponses(
                    "GET / HTTP/1.1\n"+
                    "Host: whatever\n"+
                    "Connection: close\n"+
                    "\n"
                    );
        assertTrue(response.indexOf("200")>0);
        assertTrue(response.indexOf("Connection: close")>0);
        assertTrue(response.indexOf("Hello World")>0);
        
        _connector.reopen();
        response=_connector.getResponses(
                    "GET / HTTP/1.1\n"+
                    "Host: whatever\n"+
                    "Connection: Other, close\n"+
                    "\n"
                    );

        assertTrue(response.indexOf("200")>0);
        assertTrue(response.indexOf("Connection: close")>0);
        assertTrue(response.indexOf("Hello World")>0);
        
        

        _connector.reopen();
        response=_connector.getResponses(
                    "GET / HTTP/1.0\n"+
                    "Host: whatever\n"+
                    "\n"
                    );
        assertTrue(response.indexOf("200")>0);
        assertFalse(response.indexOf("Connection: close")>0);
        assertTrue(response.indexOf("Hello World")>0);
        
        _connector.reopen();
        response=_connector.getResponses(
                    "GET / HTTP/1.0\n"+
                    "Host: whatever\n"+
                    "Connection: Other, close\n"+
                    "\n"
                    );
        assertTrue(response.indexOf("200")>0);
        assertTrue(response.indexOf("Connection: close")>0);
        assertTrue(response.indexOf("Hello World")>0);

        _connector.reopen();
        response=_connector.getResponses(
                    "GET / HTTP/1.0\n"+
                    "Host: whatever\n"+
                    "Connection: Other, keep-alive\n"+
                    "\n"
                    );
        assertTrue(response.indexOf("200")>0);
        assertTrue(response.indexOf("Connection: keep-alive")>0);
        assertTrue(response.indexOf("Hello World")>0);
        
        
        

        _handler._checker = new RequestTester()
        {
            public boolean check(HttpServletRequest request,HttpServletResponse response) throws IOException
            {
                response.setHeader("Connection","TE");
                response.addHeader("Connection","Other");
                response.getOutputStream().println("Hello World");
                return true;
            }  
        };
        
        _connector.reopen();
        response=_connector.getResponses(
                    "GET / HTTP/1.1\n"+
                    "Host: whatever\n"+
                    "\n"
                    );
        assertTrue(response.indexOf("200")>0);
        assertTrue(response.indexOf("Connection: TE,Other")>0);
        assertTrue(response.indexOf("Hello World")>0);
        
        _connector.reopen();
        response=_connector.getResponses(
                    "GET / HTTP/1.1\n"+
                    "Host: whatever\n"+
                    "Connection: close\n"+
                    "\n"
                    );
        assertTrue(response.indexOf("200")>0);
        assertTrue(response.indexOf("Connection: close")>0);
        assertTrue(response.indexOf("Hello World")>0);
    }
    
    public void testCookie()
        throws Exception
    {

        final String[] name=new String[20];
        final String[] cookie=new String[20];
        
        _handler._checker = new RequestTester()
        {
            public boolean check(HttpServletRequest request,HttpServletResponse response)
            {
                for (int i=0;i<cookie.length; i++)
                {
                    name[i]=null;
                    cookie[i]=null;
                }
                
                Cookie[] cookies = request.getCookies();
                for (int i=0;cookies!=null && i<cookies.length; i++)
                {
                    name[i]=cookies[i].getName();
                    cookie[i]=cookies[i].getValue();
                }
                return true;
            }  
        };
        
        
        String request="POST / HTTP/1.1\r\n"+
        "Host: whatever\r\n"+
        "Cookie: name=\"quoted=\\\"value\\\"\"\n" +
        "Connection: close\r\n"+
        "\r\n";

        _connector.reopen();
        _connector.getResponses(request);

        assertEquals("quoted=\"value\"",cookie[0]);
        assertEquals(null,cookie[1]);
        
        
        request="POST / HTTP/1.1\r\n"+
        "Host: whatever\r\n"+
        "Cookie: name=value\r\n"+
        "\r\n"
        +
        "POST / HTTP/1.1\r\n"+
        "Host: whatever\r\n"+
        "Cookie:\r\n"+
        "Connection: close\r\n"+
        "\r\n";

        _connector.reopen();
        _connector.getResponses(request);
        
        assertEquals(null,cookie[0]);
        assertEquals(null,cookie[1]);
        request="POST / HTTP/1.1\r\n"+
        "Host: whatever\r\n"+
        "Cookie: name=value\r\n"+
        "\r\n"
        +
        "POST / HTTP/1.1\r\n"+
        "Host: whatever\r\n"+
        "Cookie: name=value\r\n"+
        "Cookie:\r\n"+
        "Connection: close\r\n"+
        "\r\n";

        _connector.reopen();
        _connector.getResponses(request);
        
        assertEquals("value",cookie[0]);
        assertEquals(null,cookie[1]);
        

        request="POST / HTTP/1.1\r\n"+
        "Host: whatever\r\n"+
        "Cookie: name0=value0; name1 = value1 , \"\\\"name2\\\"\"  =  \"\\\"value2\\\"\"  \n" +
        "Cookie: $Version=2; name3=value3=value3;$path=/path;$domain=acme.com;$port=8080, name4=; name5 =  ; name6\n" +
        "Cookie: name7=value7;\n" +
        "Connection: close\r\n"+
        "\r\n";

        _connector.reopen();
        _connector.getResponses(request);

        assertEquals("name0" ,name[0]);
        assertEquals("value0" ,cookie[0]);
        assertEquals("name1" ,name[1]);
        assertEquals("value1" ,cookie[1]);
        assertEquals("\"name2\"" ,name[2]);
        assertEquals("\"value2\"" ,cookie[2]);
        assertEquals("name3" ,name[3]);
        assertEquals("value3=value3" ,cookie[3]);
        assertEquals("name4" ,name[4]);
        assertEquals("" ,cookie[4]);
        assertEquals("name5" ,name[5]);
        assertEquals("" ,cookie[5]);
        assertEquals("name6" ,name[6]);
        assertEquals("" ,cookie[6]);
        assertEquals("name7" ,name[7]);
        assertEquals("value7" ,cookie[7]);
    }
    
    
    
    
    interface RequestTester
    {
        boolean check(HttpServletRequest request,HttpServletResponse response) throws IOException;
    }
    
    class RequestHandler extends AbstractHandler
    {
        RequestTester _checker;
        String _content;
        
        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
        {
            ((Request)request).setHandled(true);
            
            if (request.getContentLength()>0)
                _content=IO.toString(request.getInputStream());
            
            if (_checker!=null && _checker.check(request,response))
                response.setStatus(200);
            else
                response.sendError(500); 
            
            
        }   
    }

}
