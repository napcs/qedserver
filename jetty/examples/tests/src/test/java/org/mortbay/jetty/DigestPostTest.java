package org.mortbay.jetty;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.MessageDigest;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.jetty.client.ContentExchange;
import org.mortbay.jetty.client.HttpClient;
import org.mortbay.jetty.client.security.Realm;
import org.mortbay.jetty.client.security.SimpleRealmResolver;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.DigestAuthenticator;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.log.Log;
import org.mortbay.util.IO;
import org.mortbay.util.StringUtil;
import org.mortbay.util.TypeUtil;

public class DigestPostTest extends TestCase
{
    private static final String NC = "00000001";
    
    public final static String __message = 
        "0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 \n"+
        "9876543210 9876543210 9876543210 9876543210 9876543210 9876543210 9876543210 9876543210 \n"+
        "1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 \n"+
        "0987654321 0987654321 0987654321 0987654321 0987654321 0987654321 0987654321 0987654321 \n"+
        "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz \n"+
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ ABCDEFGHIJKLMNOPQRSTUVWXYZ ABCDEFGHIJKLMNOPQRSTUVWXYZ \n"+
        "Now is the time for all good men to come to the aid of the party.\n"+
        "How now brown cow.\n"+
        "The quick brown fox jumped over the lazy dog.\n";
    
    public volatile static String _received = null;
    private Server _server;

    public void setUp()
    {
        try
        {
            _server = new Server();
            _server.setConnectors(new Connector[]
            { new SelectChannelConnector() });

            Context context = new Context(Context.SECURITY);
            context.setContextPath("/test");
            context.addServlet(PostServlet.class,"/");
            context.setAllowNullPathInfo(true);

            HashUserRealm realm = new HashUserRealm("test");
            realm.put("testuser","password");
            realm.addUserToRole("testuser","test");
            _server.setUserRealms(new UserRealm[]{realm});
            
            SecurityHandler security=context.getSecurityHandler();
            security.setAuthenticator(new DigestAuthenticator());
            security.setUserRealm(realm);
           
            Constraint constraint = new Constraint("SecureTest","test");
            constraint.setAuthenticate(true);
            ConstraintMapping mapping = new ConstraintMapping();
            mapping.setConstraint(constraint);
            mapping.setPathSpec("/*");
            security.setConstraintMappings(new ConstraintMapping[]{mapping});
            
            HandlerCollection handlers = new HandlerCollection();
            handlers.setHandlers(new Handler[]
            { context, new DefaultHandler() });
            _server.setHandler(handlers);
            
            _server.start();
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
    }


    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        _server.stop();
    }

    public void testServerDirectlyHTTP10() throws Exception
    {
        Socket socket = new Socket("127.0.0.1",_server.getConnectors()[0].getLocalPort());
        byte[] bytes = __message.getBytes("UTF-8");

        _received=null;
        socket.getOutputStream().write(
                ("POST /test HTTP/1.0\r\n"+
                "Host: 127.0.0.1:"+_server.getConnectors()[0].getLocalPort()+"\r\n"+
                "Content-Length: "+bytes.length+"\r\n"+
                "\r\n").getBytes("UTF-8"));
        socket.getOutputStream().write(bytes);
        socket.getOutputStream().flush();

        String result = IO.toString(socket.getInputStream());

        
        assertTrue(result.startsWith("HTTP/1.1 401 Unauthorized"));
        assertEquals(null,_received);
        
        int n=result.indexOf("nonce=");
        String nonce=result.substring(n+7,result.indexOf('"',n+7));
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] b= md.digest(String.valueOf(System.currentTimeMillis()).getBytes(StringUtil.__ISO_8859_1));            
        String cnonce=encode(b);
        String digest="Digest username=\"testuser\" realm=\"test\" nonce=\""+nonce+"\" uri=\"/test\" algorithm=MD5 response=\""+
        newResponse("POST","/test",cnonce,"testuser","test","password",nonce,"auth")+
        "\" qop=auth nc="+NC+" cnonce=\""+cnonce+"\"";
              
        
        socket = new Socket("127.0.0.1",_server.getConnectors()[0].getLocalPort());

        _received=null;
        socket.getOutputStream().write(
                ("POST /test HTTP/1.0\r\n"+
                "Host: 127.0.0.1:"+_server.getConnectors()[0].getLocalPort()+"\r\n"+
                "Content-Length: "+bytes.length+"\r\n"+
                "Authorization: "+digest+"\r\n"+
                "\r\n").getBytes("UTF-8"));
        socket.getOutputStream().write(bytes);
        socket.getOutputStream().flush();

        result = IO.toString(socket.getInputStream());

        assertTrue(result.startsWith("HTTP/1.1 200 OK"));
        assertEquals(__message,_received);
    }

    public void testServerDirectlyHTTP11() throws Exception
    {
        Socket socket = new Socket("127.0.0.1",_server.getConnectors()[0].getLocalPort());
        byte[] bytes = __message.getBytes("UTF-8");

        _received=null;
        socket.getOutputStream().write(
                ("POST /test HTTP/1.1\r\n"+
                "Host: 127.0.0.1:"+_server.getConnectors()[0].getLocalPort()+"\r\n"+
                "Content-Length: "+bytes.length+"\r\n"+
                "\r\n").getBytes("UTF-8"));
        socket.getOutputStream().write(bytes);
        socket.getOutputStream().flush();

        Thread.sleep(100);
        
        byte[] buf=new byte[4096];
        int len=socket.getInputStream().read(buf);
        String result=new String(buf,0,len,"UTF-8");

        assertTrue(result.startsWith("HTTP/1.1 401 Unauthorized"));
        assertEquals(null,_received);
        
        int n=result.indexOf("nonce=");
        String nonce=result.substring(n+7,result.indexOf('"',n+7));
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] b= md.digest(String.valueOf(System.currentTimeMillis()).getBytes(StringUtil.__ISO_8859_1));            
        String cnonce=encode(b);
        String digest="Digest username=\"testuser\" realm=\"test\" nonce=\""+nonce+"\" uri=\"/test\" algorithm=MD5 response=\""+
        newResponse("POST","/test",cnonce,"testuser","test","password",nonce,"auth")+
        "\" qop=auth nc="+NC+" cnonce=\""+cnonce+"\"";

        _received=null;
        socket.getOutputStream().write(
                ("POST /test HTTP/1.0\r\n"+
                "Host: 127.0.0.1:"+_server.getConnectors()[0].getLocalPort()+"\r\n"+
                "Content-Length: "+bytes.length+"\r\n"+
                "Authorization: "+digest+"\r\n"+
                "\r\n").getBytes("UTF-8"));
        socket.getOutputStream().write(bytes);
        socket.getOutputStream().flush();

        result = IO.toString(socket.getInputStream());

        assertTrue(result.startsWith("HTTP/1.1 200 OK"));
        assertEquals(__message,_received);
    }


    public void testServerDirectlyHTTP11Chunked() throws Exception
    {
        // Log.getLog().setDebugEnabled(true);
        
        Socket socket = new Socket("127.0.0.1",_server.getConnectors()[0].getLocalPort());
        byte[] bytes = __message.getBytes("UTF-8");

        _received=null;
        socket.getOutputStream().write(
                ("POST /test HTTP/1.1\r\n"+
                "Host: 127.0.0.1:"+_server.getConnectors()[0].getLocalPort()+"\r\n"+
                "Content-Type: text/plain\r\n"+
                "Transfer-Encoding: chunked\r\n"+
                "\r\n").getBytes("UTF-8"));
        socket.getOutputStream().flush();
        Thread.sleep(10);
        socket.getOutputStream().write("10\r\n".getBytes("UTF-8"));
        socket.getOutputStream().write(bytes,0,16);
        socket.getOutputStream().flush();
        Thread.sleep(10);
        socket.getOutputStream().write("\r\n20\r\n".getBytes("UTF-8"));
        socket.getOutputStream().write(bytes,16,32);
        socket.getOutputStream().flush();
        Thread.sleep(10);
        socket.getOutputStream().write(("\r\n"+Integer.toHexString(bytes.length-48)+"\r\n").getBytes("UTF-8"));
        socket.getOutputStream().write(bytes,48,bytes.length-48);
        socket.getOutputStream().write("\r\n0\r\n".getBytes("UTF-8"));
        socket.getOutputStream().flush();

        Thread.sleep(100);
        
        byte[] buf=new byte[4096];
        int len=socket.getInputStream().read(buf);
        String result=new String(buf,0,len,"UTF-8");

        assertTrue(result.startsWith("HTTP/1.1 401 Unauthorized"));
        assertEquals(null,_received);
        
        int n=result.indexOf("nonce=");
        String nonce=result.substring(n+7,result.indexOf('"',n+7));
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] b= md.digest(String.valueOf(System.currentTimeMillis()).getBytes(StringUtil.__ISO_8859_1));            
        String cnonce=encode(b);
        String digest="Digest username=\"testuser\" realm=\"test\" nonce=\""+nonce+"\" uri=\"/test\" algorithm=MD5 response=\""+
        newResponse("POST","/test",cnonce,"testuser","test","password",nonce,"auth")+
        "\" qop=auth nc="+NC+" cnonce=\""+cnonce+"\"";

        _received=null;
        socket.getOutputStream().write(
                ("POST /test HTTP/1.1\r\n"+
                "Host: 127.0.0.1:"+_server.getConnectors()[0].getLocalPort()+"\r\n"+
                "Content-Type: text/plain\r\n"+
                "Transfer-Encoding: Chunked\r\n"+
                "Authorization: "+digest+"\r\n"+
                "\r\n").getBytes("UTF-8"));
        socket.getOutputStream().flush();
        Thread.sleep(10);
        socket.getOutputStream().write("10\r\n".getBytes("UTF-8"));
        socket.getOutputStream().write(bytes,0,16);
        socket.getOutputStream().flush();
        Thread.sleep(10);
        socket.getOutputStream().write("\r\n20\r\n".getBytes("UTF-8"));
        socket.getOutputStream().write(bytes,16,32);
        socket.getOutputStream().flush();
        Thread.sleep(10);
        socket.getOutputStream().write(("\r\n"+Integer.toHexString(bytes.length-48)+"\r\n").getBytes("UTF-8"));
        socket.getOutputStream().write(bytes,48,bytes.length-48);
        socket.getOutputStream().write("\r\n0\r\n".getBytes("UTF-8"));
        socket.getOutputStream().flush();
        
        Thread.sleep(100);
        buf=new byte[4096];
        len=socket.getInputStream().read(buf);
        result=new String(buf,0,len,"UTF-8");
        
        assertTrue(result.startsWith("HTTP/1.1 200 OK"));
        assertEquals(__message,_received);
        
        socket.close();
    }

    public void testServerWithHttpClientStringContent() throws Exception
    {
        HttpClient client = new HttpClient();
        client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        client.setRealmResolver(new SimpleRealmResolver(new TestRealm()));
        client.start();

        String srvUrl = "http://127.0.0.1:" + _server.getConnectors()[0].getLocalPort() + "/test";

        ContentExchange ex = new ContentExchange();
        ex.setMethod(HttpMethods.POST);
        ex.setURL(srvUrl);
        ex.setRequestContent(new ByteArrayBuffer(__message,"UTF-8"));

        _received=null;
        client.send(ex);
        ex.waitForDone();

        assertEquals(__message,_received);
        assertEquals(200,ex.getResponseStatus());
    }
    
    
    public void testServerWithHttpClientStreamContent() throws Exception
    {
        HttpClient client = new HttpClient();
        client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        client.setRealmResolver(new SimpleRealmResolver(new TestRealm()));
        client.start();

        String srvUrl = "http://127.0.0.1:" + _server.getConnectors()[0].getLocalPort() + "/test";

        ContentExchange ex = new ContentExchange();
        ex.setMethod(HttpMethods.POST);
        ex.setURL(srvUrl);
        ex.setRequestContentSource(new BufferedInputStream(new FileInputStream("src/test/resources/message.txt")));

        _received=null;
        client.send(ex);
        ex.waitForDone();

        String sent = IO.toString(new FileInputStream("src/test/resources/message.txt"));
        assertEquals(sent,_received);

        assertEquals(200,ex.getResponseStatus());
    }

    public static class TestRealm implements Realm
    {
        public String getPrincipal()
        {
            return "testuser";
        }

        public String getId()
        {
            return "test";
        }

        public String getCredentials()
        {
            return "password";
        }
    }

    public static class PostServlet extends HttpServlet
    {

        public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException
        {
            String received = IO.toString(request.getInputStream());
            _received = received;
            
            // System.out.println("received: "+received.length()+":\n"+received);

            response.setStatus(200);
            response.getWriter().println("Received "+received.length()+" bytes");
        }

    }

    protected String newResponse(String method, String uri, String cnonce, String principal, String realm, String credentials, String nonce, String qop)
        throws Exception
    {       
        MessageDigest md = MessageDigest.getInstance("MD5");

        // calc A1 digest
        md.update(principal.getBytes(StringUtil.__ISO_8859_1));
        md.update((byte)':');
        md.update(realm.getBytes(StringUtil.__ISO_8859_1));
        md.update((byte)':');
        md.update(credentials.getBytes(StringUtil.__ISO_8859_1));
        byte[] ha1 = md.digest();
        // calc A2 digest
        md.reset();
        md.update(method.getBytes(StringUtil.__ISO_8859_1));
        md.update((byte)':');
        md.update(uri.getBytes(StringUtil.__ISO_8859_1));
        byte[] ha2=md.digest();

        md.update(TypeUtil.toString(ha1,16).getBytes(StringUtil.__ISO_8859_1));
        md.update((byte)':');
        md.update(nonce.getBytes(StringUtil.__ISO_8859_1));
        md.update((byte)':');
        md.update(NC.getBytes(StringUtil.__ISO_8859_1));
        md.update((byte)':');
        md.update(cnonce.getBytes(StringUtil.__ISO_8859_1));
        md.update((byte)':');
        md.update(qop.getBytes(StringUtil.__ISO_8859_1));
        md.update((byte)':');
        md.update(TypeUtil.toString(ha2,16).getBytes(StringUtil.__ISO_8859_1));
        byte[] digest=md.digest();

        // check digest
        return encode(digest);
    }
    
    private static String encode(byte[] data)
    {
        StringBuffer buffer = new StringBuffer();
        for (int i=0; i<data.length; i++) 
        {
            buffer.append(Integer.toHexString((data[i] & 0xf0) >>> 4));
            buffer.append(Integer.toHexString(data[i] & 0x0f));
        }
        return buffer.toString();
    }
}
