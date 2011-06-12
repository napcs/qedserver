package org.mortbay.jetty.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.mortbay.io.EndPoint;
import org.mortbay.io.bio.SocketEndPoint;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class JettyClientTest extends TestCase
{
    private volatile Server _server;
    private volatile SocketConnector _connector;
    private volatile EndPoint _endp;

    private volatile HttpClient _client;

    private volatile static int _PORT = 0;
    private volatile static String _url;

    // Restart the Jetty server
    public synchronized void breakConnection() 
    {
        try
        {
            if (_endp!=null)
            {
                ((Socket)((SocketEndPoint)_endp).getTransport()).close();
                _endp=null;
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public synchronized void startServer() throws Exception
    {
        // Create a Jetty server with a simple servlet that returns immediately
        _server = new Server();
        _connector = new SocketConnector()
        {
            @Override
            public void customize(EndPoint endpoint, Request request) throws IOException
            {
                super.customize(endpoint,request);
                if (_endp!=null)
                    throw new IllegalStateException("ENDP SET");
                _endp=endpoint;
            }

        };
        _connector.setHost("127.0.0.1");
        _connector.setStatsOn(true);

        if (_PORT == 0)
        {
            _connector.setPort(0);
        }
        else
        {
            _connector.setPort(_PORT);
        }

        _server.addConnector(_connector);
        Context context = new Context(_server,"",Context.NO_SECURITY | Context.NO_SESSIONS);
        ServletHolder h = new ServletHolder(new HttpServlet()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
            {
                resp.setContentType("text/plain");
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("ok\n");
            }

            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
            {
                resp.setContentType("text/plain");
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("ok\n");
            }

        });
        context.addServlet(h,"/ping");
        _server.start();
        _PORT = _connector.getLocalPort();
        _url = String.format("http://localhost:%d/ping",_PORT);

    }

    // Restart the Jetty client
    public void startClient() throws Exception
    {
        if (_client != null)
        {
            _client.stop();
            _client = null;
        }

        _client = new HttpClient();
        _client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        _client.setIdleTimeout(60000);
        _client.setMaxRetries(0);
        _client.setTimeout(60000);
        _client.setSoTimeout(60000);
        _client.setMaxConnectionsPerAddress(1);
        _client.start();
    }

    // Simple HTTP exchange
    public static class SimpleExchange extends CachedExchange
    {
        SimpleExchange()
        {
            super(true);
        }

        public void setup(String url) throws IOException
        {
            setMethod("POST");
            setURL(url);
            byte[] content = "hello".getBytes();
            setRequestHeader(HttpHeaders.CONTENT_TYPE,"text/plain");
            setRequestHeader(HttpHeaders.CONTENT_LENGTH,Integer.toString(content.length));
            setRequestContentSource(new ByteArrayInputStream(content));
        }

        @Override
        protected void onException(Throwable exc)
        {
            // super.onException(exc);
        }

        @Override
        protected void onConnectionFailed(Throwable exc)
        {
            // super.onConnectionFailed(exc);
        }

        @Override
        protected void onExpire()
        {
            // super.onExpire();
        }

    }

    // An exchange that restarts the server while flushing data, to simulate
    // the case where the server dies while the client is sending.
    public class InterruptedExchange extends SimpleExchange
    {
        @Override
        public void setup(String url)
        {
            setMethod("POST");
            setURL(url);
            final byte[] content = new byte[4096];
            final int content_length=4096*4096;
            Arrays.fill(content,(byte)'x');
            setRequestHeader(HttpHeaders.CONTENT_TYPE,"text/plain");
            setRequestHeader(HttpHeaders.CONTENT_LENGTH,Integer.toString(content_length));

            // Restart the server every RESET_INTERVAL bytes
            setRequestContentSource(new InputStream()
            {
                private int pos = 0;

                private void checkReset() throws IOException
                {
                    if (_endp!=null)
                    {
                        breakConnection();
                    }
                }

                @Override
                public int read() throws IOException
                {
                    checkReset();
                    return (pos < content_length?(int)'x':-1);
                }

                @Override
                public int available()
                {
                    return content_length-pos;
                }

                @Override
                public int read(byte[] b, int offset, int length) throws IOException
                {
                    checkReset();
                    int available = available();
                    if (available == 0)
                        return -1;
                    if (length>4096)
                        length=4096;
                    length = Math.min(length,available);
                    System.arraycopy(content,0,b,offset,length);
                    pos += length;
                    return length;
                }
            });
        }

    }

    public int runExchange(SimpleExchange exchange) throws Exception
    {
        exchange.setup(_url);
        _client.send(exchange);
        exchange.waitForDone();
        return exchange.getStatus();
    }

    public void setUp() throws Exception
    {
        startServer();
        startClient();
    }

    public void tearDown() throws Exception
    {
        _server.stop();
        _server = null;
        _client.stop();
        _client = null;
    }

    public void testSimple() throws Exception
    {
        assertEquals(HttpExchange.STATUS_COMPLETED,runExchange(new SimpleExchange()));
    }

    public void testReconnect() throws Exception
    {
        for (int i = 0; i < 10; i++)
        {
            final int id=i;
            // System.err.println("\nerror "+i);
            assertEquals("error " + i,HttpExchange.STATUS_EXCEPTED,runExchange(new InterruptedExchange()
            {

                /* (non-Javadoc)
                 * @see org.mortbay.jetty.client.JettyClientTest.SimpleExchange#onException(java.lang.Throwable)
                 */
                @Override
                protected void onException(Throwable exc)
                {
                    // System.err.println(exc+" "+id);
                    super.onException(exc);
                }
                
            }));
            // System.err.println("success "+i);
            assertEquals("reconnect " + i,HttpExchange.STATUS_COMPLETED,runExchange(new SimpleExchange()));
        }
    }

}
