// ========================================================================
// Copyright 2006-2007 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.nio.DirectNIOBuffer;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.EofException;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.client.security.ProxyAuthorization;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.log.Log;
import org.mortbay.util.IO;

/**
 * Functional testing for HttpExchange.
 *
 * @author Matthew Purland
 * @author Greg Wilkins
 */
public class HttpExchangeTest extends TestCase
{
    private boolean _stress=Boolean.getBoolean("STRESS");
    protected int _maxConnectionsPerAddress = 2;
    protected String _scheme = "http://";
    protected Server _server;
    protected int _port;
    protected HttpClient _httpClient;
    protected Connector _connector;
    protected AtomicInteger _count = new AtomicInteger();

    protected void setUp() throws Exception
    {
        startServer();
        _httpClient=new HttpClient();
        _httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        _httpClient.setMaxConnectionsPerAddress(_maxConnectionsPerAddress);
        _httpClient.start();
    }

    protected void tearDown() throws Exception
    {
        _httpClient.stop();
        Thread.sleep(500);
        stopServer();
    }

    public void testPerf() throws Exception
    {
        sender(1,false);
        sender(1,true);
        sender(10,false);
        sender(10,true);
        sender(100,false);
        sender(100,true);
        if (_stress)
        {
            sender(1000,false);
            sender(1000,true);
        }
    }

    /**
     * Test sending data through the exchange.
     *
     * @throws IOException
     */
    public void sender(final int nb,final boolean close) throws Exception
    {
        _count.set(0);
        final CountDownLatch complete=new CountDownLatch(nb);
        final CountDownLatch latch=new CountDownLatch(nb);
        HttpExchange[] httpExchange = new HttpExchange[nb];
        long start=System.currentTimeMillis();
        for (int i=0; i<nb; i++)
        {
            final int n=i;

            httpExchange[n]=new HttpExchange()
            {
                String result="pending";
                int len=0;
                protected void onRequestCommitted()
                {
                    result="committed";
                    // System.err.println(n+" Request committed: "+close);
                }

                protected void onRequestComplete() throws IOException
                {
                    result="sent";
                }

                protected void onResponseStatus(Buffer version, int status, Buffer reason)
                {
                    result="status";
                    // System.err.println(n+" Response Status: " + version+" "+status+" "+reason);
                }

                protected void onResponseHeader(Buffer name, Buffer value)
                {
                    // System.err.println(n+" Response header: " + name + " = " + value);
                }

                protected void onResponseHeaderComplete() throws IOException
                {
                    result="content";
                    super.onResponseHeaderComplete();
                }

                protected void onResponseContent(Buffer content)
                {
                    len+=content.length();
                    // System.err.println(n+" Response content:" + content.length());
                }

                protected void onResponseComplete()
                {
                    result="complete";
                    // System.err.println(n+" Response completed "+len);
                    if (len==2009)
                        latch.countDown();
                    else
                        System.err.println(n+" ONLY "+len);
                    complete.countDown();
                }

                protected void onConnectionFailed(Throwable ex)
                {
                    complete.countDown();
                    result="failed";
                    System.err.println(n+" FAILED "+ex);
                    super.onConnectionFailed(ex);
                }

                protected void onException(Throwable ex)
                {
                    complete.countDown();
                    result="excepted";
                    System.err.println(n+" EXCEPTED "+ex);
                    super.onException(ex);
                }

                protected void onExpire()
                {
                    complete.countDown();
                    result="expired";
                    System.err.println(n+" EXPIRED "+len);
                    super.onExpire();
                }

                public String toString()
                {
                    return n+" "+result+" "+len;
                }
            };

            httpExchange[n].setURL(_scheme+"localhost:"+_port+"/"+n);
            httpExchange[n].addRequestHeader("arbitrary","value");
            if (close)
                httpExchange[n].setRequestHeader("Connection","close");

            _httpClient.send(httpExchange[n]);
        }

        assertTrue(complete.await(60,TimeUnit.SECONDS));
            
        long elapsed=System.currentTimeMillis()-start;
        // make windows-friendly ... System.currentTimeMillis() on windows is dope! 
        if(elapsed>0)
            System.err.println(nb+"/"+_count+" c="+close+" rate="+(nb*1000/elapsed));
        
        assertEquals("nb="+nb+" close="+close,0,latch.getCount());
    }

    public void testPostWithContentExchange() throws Exception
    {
        for (int i=0;i<200;i++)
        {
            ContentExchange httpExchange=new ContentExchange();
            //httpExchange.setURL(_scheme+"localhost:"+_port+"/");
            httpExchange.setURL(_scheme+"localhost:"+_port);
            httpExchange.setMethod(HttpMethods.POST);
            httpExchange.setRequestContent(new ByteArrayBuffer("<hello />"));
            _httpClient.send(httpExchange);
            int status = httpExchange.waitForDone();
            //httpExchange.waitForStatus(HttpExchange.STATUS_COMPLETED);
            String result=httpExchange.getResponseContent();
            assertEquals(HttpExchange.STATUS_COMPLETED, status);
            assertEquals("i="+i,"<hello />",result);
        }
    }

    public void testGetWithContentExchange() throws Exception
    {
        for (int i=0;i<200;i++)
        {
            ContentExchange httpExchange=new ContentExchange();
            httpExchange.setURL(_scheme+"localhost:"+_port+"/?i="+i);
            httpExchange.setMethod(HttpMethods.GET);
            _httpClient.send(httpExchange);
            int status = httpExchange.waitForDone();
            //httpExchange.waitForStatus(HttpExchange.STATUS_COMPLETED);
            String result=httpExchange.getResponseContent();
            assertEquals("i="+i,0,result.indexOf("<hello>"));
            assertEquals("i="+i,result.length()-10,result.indexOf("</hello>"));
            assertEquals(HttpExchange.STATUS_COMPLETED, status);
            Thread.sleep(5);
        }
    }
    public void testBigPostWithContentExchange() throws Exception
    {   
        int size =32;
        ContentExchange httpExchange=new ContentExchange();

        Buffer babuf = new ByteArrayBuffer(size*36*1024);
        Buffer niobuf = new DirectNIOBuffer(size*36*1024);

        byte[] bytes="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();

        for (int i=0;i<size*1024;i++)
        {
            babuf.put(bytes);
            niobuf.put(bytes);
        }
        
        httpExchange.setURL(_scheme+"localhost:"+_port+"/");
        httpExchange.setMethod(HttpMethods.POST);
        httpExchange.setRequestContentType("application/data");
        httpExchange.setRequestContent(babuf);
        
        _httpClient.send(httpExchange);
        int status = httpExchange.waitForDone();
        String result=httpExchange.getResponseContent();
        assertEquals(babuf.length(),result.length());
        assertEquals(HttpExchange.STATUS_COMPLETED, status);

        httpExchange.reset();
        httpExchange.setURL(_scheme+"localhost:"+_port+"/");
        httpExchange.setMethod(HttpMethods.POST);
        httpExchange.setRequestContentType("application/data");
        httpExchange.setRequestContent(niobuf);
        _httpClient.send(httpExchange);
        status = httpExchange.waitForDone();
        result=httpExchange.getResponseContent();
        assertEquals(niobuf.length(),result.length());
        assertEquals(HttpExchange.STATUS_COMPLETED, status);
    }
    public void testProxy() throws Exception
    {
        if (_scheme.equals("https://"))
            return;
        try
        {
            _httpClient.setProxy(new Address("127.0.0.1",_port));
            _httpClient.setProxyAuthentication(new ProxyAuthorization("user","password"));

            ContentExchange httpExchange=new ContentExchange();
            httpExchange.setAddress(new Address("jetty.mortbay.org",8080));
            httpExchange.setMethod(HttpMethods.GET);
            httpExchange.setURI("/jetty-6");
            _httpClient.send(httpExchange);
            int status = httpExchange.waitForDone();
            //httpExchange.waitForStatus(HttpExchange.STATUS_COMPLETED);
            String result=httpExchange.getResponseContent();
            result=result.trim();
            assertEquals(HttpExchange.STATUS_COMPLETED, status);
            assertTrue(result.startsWith("Proxy request: http://jetty.mortbay.org:8080/jetty-6"));
            assertTrue(result.endsWith("Basic dXNlcjpwYXNzd29yZA=="));
        }
        finally
        {
            _httpClient.setProxy(null);
        }
    }

    
    public void testReserveConnections () throws Exception
    {
       final HttpDestination destination = _httpClient.getDestination (new Address("localhost", _port), _scheme.equalsIgnoreCase("https://"));
       final org.mortbay.jetty.client.HttpConnection[] connections = new org.mortbay.jetty.client.HttpConnection[_maxConnectionsPerAddress];
       for (int i=0; i < _maxConnectionsPerAddress; i++)
       {
           connections[i] = destination.reserveConnection(200);
           assertNotNull(connections[i]);
           HttpExchange ex = new ContentExchange();
           ex.setURL(_scheme+"localhost:"+_port+"/?i="+i);
           ex.setMethod(HttpMethods.GET);
           connections[i].send(ex);
       }
      
       //try to get a connection, and only wait 500ms, as we have
       //already reserved the max, should return null
       org.mortbay.jetty.client.HttpConnection c = destination.reserveConnection(500);
       assertNull(c);
       
       //unreserve first connection
       destination.returnConnection(connections[0], false);
       
       //reserving one should now work
       c = destination.reserveConnection(500);
       assertNotNull(c);
       
        
    }
    public static void copyStream(InputStream in, OutputStream out)
    {
        try
        {
            byte[] buffer=new byte[1024];
            int len;
            while ((len=in.read(buffer))>=0)
            {
                out.write(buffer,0,len);
            }
        }
        catch (EofException e)
        {
            System.err.println(e);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    protected void newServer() throws Exception
    {
        _server=new Server();
        _server.setGracefulShutdown(500);
        _connector=new SelectChannelConnector();

        _connector.setPort(0);
        _server.setConnectors(new Connector[] { _connector });
    }

    protected void startServer() throws Exception
    {
        newServer();
        _server.setHandler(new AbstractHandler()
        {
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
            {
                int i=0;
                try
                {
                    Request base_request=(request instanceof Request)?(Request)request:HttpConnection.getCurrentConnection().getRequest();
                    base_request.setHandled(true);
                    response.setStatus(200);
                    _count.incrementAndGet();
                    
                    if (request.getServerName().equals("jetty.mortbay.org"))
                    {
                        // System.err.println("HANDLING Proxy");
                        response.getOutputStream().println("Proxy request: "+request.getRequestURL());
                        response.getOutputStream().println(request.getHeader(HttpHeaders.PROXY_AUTHORIZATION));
                    }
                    else if (request.getMethod().equalsIgnoreCase("GET"))
                    {
                        // System.err.println("HANDLING Hello "+request.getRequestURI());
                        response.getOutputStream().println("<hello>");
                        for (; i<100; i++)
                        {
                            response.getOutputStream().println("  <world>"+i+"</world");
                            if (i%20==0)
                                response.getOutputStream().flush();
                        }
                        response.getOutputStream().println("</hello>");
                    }
                    else
                    {
                        response.setContentType(request.getContentType());
                        int size=request.getContentLength();
                        ByteArrayOutputStream bout = new ByteArrayOutputStream(size>0?size:32768);
                        IO.copy(request.getInputStream(),bout);
                        response.getOutputStream().write(bout.toByteArray());
                    }
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                    throw e;
                }
                catch(Throwable e)
                {
                    e.printStackTrace();
                    throw new ServletException(e);
                }
                finally
                {
                    // System.err.println("HANDLED "+i);
                }
            }
        });
        _server.start();
        _port=_connector.getLocalPort();
    }

    private void stopServer() throws Exception
    {
        _server.stop();
    }

}
