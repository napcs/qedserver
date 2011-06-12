//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

// JettyTest.java --
//
// Junit test that shows the Jetty SSL bug.
//

package org.mortbay.jetty.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.security.SslSelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.util.IO;

/**
 * HttpServer Tester.
 */
public class SSLEngineTest extends TestCase
{
    // ~ Static fields/initializers
    // ---------------------------------------------

    // Useful constants
    private static final int BODY_SIZE = 300;
    private static final String HELLO_WORLD="Hello world\r\n";
    private static final String JETTY_VERSION=Server.getVersion();
    private static final String PROTOCOL_VERSION="2.0";

    /** The request. */
    private static final String REQUEST0_HEADER="POST / HTTP/1.1\n"+"Host: localhost\n"+"Content-Type: text/xml\n"+"Content-Length: ";
    private static final String REQUEST1_HEADER="POST / HTTP/1.1\n"+"Host: localhost\n"+"Content-Type: text/xml\n"+"Connection: close\n"+"Content-Length: ";
    private static final String REQUEST_CONTENT="<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
            +"<requests xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"+"        xsi:noNamespaceSchemaLocation=\"commander.xsd\" version=\""
            +PROTOCOL_VERSION+"\">\n"+"</requests>";
    
    private static final String REQUEST0=REQUEST0_HEADER+REQUEST_CONTENT.getBytes().length+"\n\n"+REQUEST_CONTENT;
    private static final String REQUEST1=REQUEST1_HEADER+REQUEST_CONTENT.getBytes().length+"\n\n"+REQUEST_CONTENT;

    /** The expected response. */
    private static final String RESPONSE0="HTTP/1.1 200 OK\n"+"Content-Length: "+HELLO_WORLD.length()+"\n"+"Server: Jetty("+JETTY_VERSION+")\n"+'\n'+"Hello world\n";
    private static final String RESPONSE1="HTTP/1.1 200 OK\n"+"Connection: close\n"+"Server: Jetty("+JETTY_VERSION+")\n"+'\n'+"Hello world\n";


    private static class CredulousTM implements TrustManager, X509TrustManager
    {
        public X509Certificate[] getAcceptedIssuers()
        {
            return null;
        }

        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
        {
            return;
        }

        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
        {
            return;
        }
    }
    
    private static final TrustManager[] s_dummyTrustManagers=new TrustManager[]  { new CredulousTM() };


    /**
     * Feed the server the entire request at once.
     * 
     * @throws Exception
     */
    public void testBigResponse() throws Exception
    {

        Server server=new Server();
        SslSelectChannelConnector connector=new SslSelectChannelConnector();

        String keystore = System.getProperty("user.dir")+File.separator+"src"+File.separator+"test"+File.separator+"resources"+File.separator+"keystore";
        
        connector.setPort(0);
        connector.setKeystore(keystore);
        connector.setPassword("storepwd");
        connector.setKeyPassword("keypwd");

        server.setConnectors(new Connector[]
        { connector });
        server.setHandler(new HelloWorldHandler());
        server.start();
        
        SSLContext ctx=SSLContext.getInstance("SSLv3");
        ctx.init(null,s_dummyTrustManagers,new java.security.SecureRandom());

        int port=connector.getLocalPort();

        Socket client=ctx.getSocketFactory().createSocket("localhost",port);
        OutputStream os=client.getOutputStream();

        String request = 
            "GET /?dump=102400 HTTP/1.1\r\n"+
            "Host: localhost:8080\r\n"+
            "Connection: close\r\n"+
            "\r\n";
        
        os.write(request.getBytes());
        os.flush();
        
        String response = IO.toString(client.getInputStream());
        System.err.println("response "+response.length());
        assertTrue(response.length()>102400);
    }
    // ~ Methods
    // ----------------------------------------------------------------

    /**
     * Feed the server the entire request at once.
     * 
     * @throws Exception
     */
    public void testRequest1_jetty_https() throws Exception
    {
        Server server=new Server();
        SslSelectChannelConnector connector=new SslSelectChannelConnector();

        String keystore = System.getProperty("user.dir")+File.separator+"src"+File.separator+"test"+File.separator+"resources"+File.separator+"keystore";
        
        connector.setPort(0);
        connector.setKeystore(keystore);
        connector.setPassword("storepwd");
        connector.setKeyPassword("keypwd");

        server.setConnectors(new Connector[]
        { connector });
        server.setHandler(new HelloWorldHandler());
        final int numConns=200;
        Socket[] socket=new Socket[numConns];
        
        try
        {
            server.start();

            SSLContext ctx=SSLContext.getInstance("SSLv3");
            ctx.init(null,s_dummyTrustManagers,new java.security.SecureRandom());

            int port=connector.getLocalPort();

            for (int i=0; i<numConns; ++i)
            {
                // System.err.println("write:"+i);
                socket[i]=ctx.getSocketFactory().createSocket("localhost",port);
                OutputStream os=socket[i].getOutputStream();

                os.write(REQUEST0.getBytes());
                os.write(REQUEST0.getBytes());
                os.flush();
            }

            for (int i=0; i<numConns; ++i)
            {
                // System.err.println("flush:"+i);
                OutputStream os=socket[i].getOutputStream();
                os.write(REQUEST1.getBytes());
                os.flush();
            }

            for (int i=0; i<numConns; ++i)
            {
                // System.err.println("read:"+i);
                // Read the response.
                String responses=readResponse(socket[i]);
                // Check the response
                assertEquals(String.format("responses %d",i),RESPONSE0+RESPONSE0+RESPONSE1,responses);
            }
        }
        finally
        {
            for (int i=0; i<numConns; ++i)
            {
                if (socket[i]!=null)
                {
                    socket[i].close();
                }
            }
            server.stop();
        }
    }

    /**
     * Read entire response from the client. Close the output.
     * 
     * @param client
     *                Open client socket.
     * 
     * @return The response string.
     * 
     * @throws IOException
     */
    private static String readResponse(Socket client) throws IOException
    {
        BufferedReader br=null;

        try
        {
            br=new BufferedReader(new InputStreamReader(client.getInputStream()));

            StringBuilder sb=new StringBuilder(1000);
            String line;

            while ((line=br.readLine())!=null)
            {
                sb.append(line);
                sb.append('\n');
            }

            return sb.toString();
        }
        finally
        {
            if (br!=null)
            {
                br.close();
            }
        }
    }

    private static class HelloWorldHandler extends AbstractHandler
    {
        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
        {
            PrintWriter out=response.getWriter();

            try
            {
                out.print(HELLO_WORLD);
                if (request.getParameter("dump")!=null)
                {
                    char[] buf = new char[Integer.valueOf(request.getParameter("dump"))];
                    for (int i=0;i<buf.length;i++)
                        buf[i]=(char)('0'+(i%10));
                    out.write(buf);
                }   
            }
            finally
            {
                out.close();
            }
        }
    }

    public void testServletPost() throws Exception
    {
        Server server=new Server();
        SslSelectChannelConnector connector=new SslSelectChannelConnector();

        String keystore = System.getProperty("user.dir")+File.separator+"src"+File.separator+"test"+File.separator+"resources"+File.separator+"keystore";
        
        connector.setPort(0);
        connector.setKeystore(keystore);
        connector.setPassword("storepwd");
        connector.setKeyPassword("keypwd");
        connector.setTruststore(keystore);
        connector.setTrustPassword("storepwd");

        server.setConnectors(new Connector[]
        { connector });

        ServletHandler handler = new ServletHandler();
        TestServlet servlet = new TestServlet();
        handler.addServletWithMapping(new ServletHolder(servlet),"/test");
        server.addHandler(handler);
        
        try
        {
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null,s_dummyTrustManagers,new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

            server.start();

            URL url = new URL("https://localhost:"+connector.getLocalPort()+"/test");

            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            if (conn instanceof HttpsURLConnection)
            {
                ((HttpsURLConnection)conn).setHostnameVerifier(new HostnameVerifier()
                {
                    public boolean verify(String urlHostName, SSLSession session)
                    {
                        return true;
                    }
                });
            }

            conn.setConnectTimeout(10000);
            conn.setReadTimeout(100000);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type","text/plain"); //$NON-NLS-1$
            conn.setChunkedStreamingMode(128);
            conn.connect();
            byte[] b = new byte[BODY_SIZE];
            for (int i = 0; i < BODY_SIZE; i++)
            {
                b[i] = 'x';
            }
            OutputStream os = conn.getOutputStream();
            os.write(b);
            os.flush();
            int rc = conn.getResponseCode();

            int len = 0;
            InputStream is = conn.getInputStream();
            int bytes=0;
            while ((len = is.read(b)) > -1)
                bytes+=len;
            is.close();

            assertEquals(BODY_SIZE,servlet.bytes);
            assertEquals(BODY_SIZE,bytes);
            
        }
        finally
        {
            server.stop();
        }
    }

    public static class TestServlet extends HttpServlet
    {
        public int bytes=0;
        
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
        {
            resp.setContentType("text/plain");
            resp.setBufferSize(128);
            byte[] b = new byte[BODY_SIZE];
            int len = 0;
            InputStream is = req.getInputStream();

            // !!!! UNDER HTTPS, FIRST CHUNK IS READ HERE BUT THEN SERVER IS
            // WAITING FOR
            // !!!! MORE DATA
            while ((len = is.read(b)) > -1)
            {
                bytes+=len;
            }

            OutputStream os = resp.getOutputStream();
            for (int i = 0; i < BODY_SIZE; i++)
            {
                b[i] = 'x';
            }
            os.write(b);
            resp.flushBuffer();

        }
    }
}
