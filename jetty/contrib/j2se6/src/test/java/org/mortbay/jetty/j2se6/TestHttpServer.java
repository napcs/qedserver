package org.mortbay.jetty.j2se6;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.security.B64Code;
import org.mortbay.util.IO;
import org.mortbay.util.StringUtil;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import junit.framework.TestCase;

public class TestHttpServer extends TestCase
{
    public class FooFilter extends Filter
    {
        public String description()
        {
            return "FooFilter";
        }

        public void doFilter(HttpExchange exchange, Chain chain) throws IOException
        {
            try
            {
                System.err.println("FooFilter called");
                exchange.setAttribute("fooFilter", "Was Here");
                chain.doFilter(exchange);
            }
            finally
            {
                System.err.println("FooFilter called back");
            }

        }
    }
    
    public class BarFilter extends Filter
    {
        public String description()
        {
            return "BarFilter";
        }

        public void doFilter(HttpExchange exchange, Chain chain) throws IOException
        {
            try
            {
                System.err.println("BarFilter called");
                exchange.setAttribute("barFilter", "Was Also Here");
                chain.doFilter(exchange);
            }
            finally
            {
                System.err.println("BarFilter called back");
            }

        }
    }
    
    public class FooBasicAuthenticator extends BasicAuthenticator
    {

        public FooBasicAuthenticator(String realm)
        {
            super(realm);
        }

        public boolean checkCredentials(String name, String credential)
        {
            assertEquals("humpty", name);
            if ("humpty".equals(name) && ("dumpty".equals(credential)))
                return true;
            
            return false;
        }     
    }
    
    
    public class FooHandler implements HttpHandler
    {
        public void handle(HttpExchange exchange) throws IOException
        {
            assertEquals("HTTP/1.1", exchange.getProtocol());
            assertEquals("POST", exchange.getRequestMethod());
            Headers headers = exchange.getRequestHeaders();
            assertTrue (headers.containsKey("foo"));
            String val = headers.getFirst("foo");
          
            assertEquals ("fooValue", val);
            assertNotNull (exchange.getHttpContext());
            assertEquals ("/foo", exchange.getHttpContext().getPath());
            assertTrue (this == exchange.getHttpContext().getHandler());
            assertTrue (exchange.getHttpContext().getAttributes().containsKey("fooAttribute"));
            assertEquals("fooValue", (String)exchange.getHttpContext().getAttributes().get("fooAttribute"));
           
            assertEquals ("Was Here", (String)exchange.getAttribute("fooFilter"));
            assertEquals ("Was Also Here", (String)exchange.getAttribute("barFilter"));
            
            assertNotNull(exchange.getPrincipal());
            assertEquals("humpty", exchange.getPrincipal().getName());
            
            String response = "Hello World!";
            InputStream is = exchange.getRequestBody();
            String body = IO.toString(is);
            assertEquals(0, body.length());
            
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.add("bar", "barValue");
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public void testServer()
    throws Exception
    {
        Server server = new Server();

        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.setHandlers(new Handler[] {new ContextHandlerCollection(), new DefaultHandler()});
        server.setHandler(handlerCollection);
        
        //make a new Server
        JettyHttpServer httpServer = new JettyHttpServer();
        httpServer.bind(new InetSocketAddress ("localhost", 8080), 0);
        
        HttpContext context = httpServer.createContext("/foo", new FooHandler());
        context.getAttributes().put("fooAttribute", "fooValue");
        context.getFilters().add(new FooFilter());
        context.getFilters().add(new BarFilter());       
        context.setAuthenticator(new FooBasicAuthenticator("foorealm"));
        
        httpServer.start();  
       
        URL url = new URL("http://localhost:8080/foo");       
        HttpURLConnection httpConn = (HttpURLConnection)url.openConnection();
        
        String authenticationString = "Basic " + B64Code.encode( "humpty" + ":" + "dumpty", StringUtil.__ISO_8859_1);
        httpConn.setRequestMethod("POST");
        httpConn.addRequestProperty("foo", "fooValue");
        httpConn.addRequestProperty("Authorization", authenticationString);
        assertEquals(200, httpConn.getResponseCode());
        assertEquals("barValue", httpConn.getHeaderField("bar"));
        InputStream is = httpConn.getInputStream();
        String result = IO.toString(is);
        is.close();
          
        assertEquals("Hello World!", result);
                
        url = new URL("http://localhost:8080/other");
        httpConn = (HttpURLConnection)url.openConnection();
        httpConn.setRequestMethod("GET");
        assertEquals(404, httpConn.getResponseCode());
        
        httpServer.stop(0);
    }
}
