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

/*
 * Created on 9/01/2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.mortbay.jetty;

import java.io.IOException;
import java.io.PrintWriter;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.mortbay.log.Log;
import org.mortbay.log.Logger;
import org.mortbay.log.StdErrLog;


import junit.framework.TestCase;

/**
 * @author gregw
 *
 */
public class HttpConnectionTest extends TestCase
{
    Server server = new Server();
    LocalConnector connector = new LocalConnector();
    
    /**
     * Constructor 
     * @param arg0
     */
    public HttpConnectionTest(String arg0)
    {
        super(arg0);
        server.setConnectors(new Connector[]{connector});
        server.setHandler(new DumpHandler());
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        connector.setHeaderBufferSize(1024);
        connector.setRequestBufferSize(2048);
        connector.setResponseBufferSize(4096);
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
    
    

    /* --------------------------------------------------------------- */
    public void testFragmentedChunk()
    {        
        
        String response=null;
        try
        {
            int offset=0;
            
            // Chunk last
            offset=0; connector.reopen();
            response=connector.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "12345\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"HTTP/1.1 200");
            offset = checkContains(response,offset,"/R1");
            offset = checkContains(response,offset,"12345");
            

            response=connector.getResponses("GET /R2 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain\n"+
                                           "\015\012"+
                                           "5;\015\012",true);
            response=connector.getResponses("ABCDE\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"HTTP/1.1 200");
            offset = checkContains(response,offset,"/R2");
            offset = checkContains(response,offset,"ABCDE");
            
            
        }
        catch(Exception e)
        {
            e.printStackTrace();
            assertTrue(false);
            if (response!=null)
                System.err.println(response);
        }
    }

    /* --------------------------------------------------------------- */
    public void testEmpty() throws Exception
    {        
        String response=connector.getResponses("GET /R1 HTTP/1.1\n"+
                "Host: localhost\n"+
                "Transfer-Encoding: chunked\n"+
                "Content-Type: text/plain\n"+
                "\015\012"+
        "0\015\012\015\012");

        int offset=0;
        offset = checkContains(response,offset,"HTTP/1.1 200");
        offset = checkContains(response,offset,"/R1");
    }

    /* --------------------------------------------------------------- */
    public void testAutoFlush() throws Exception
    {        
        
        String response=null;
            int offset=0;
            
            offset=0; connector.reopen();
            response=connector.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "12345\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"HTTP/1.1 200");
            checkNotContained(response,offset,"IgnoreMe");
            offset = checkContains(response,offset,"/R1");
            offset = checkContains(response,offset,"12345");
    }
    
    /* --------------------------------------------------------------- */
    public void testCharset()
    {        
        
        String response=null;
        try
        {
            int offset=0;
            
            offset=0; connector.reopen();
            response=connector.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain; charset=utf-8\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "12345\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"HTTP/1.1 200");
            offset = checkContains(response,offset,"/R1");
            offset = checkContains(response,offset,"encoding=utf-8");
            offset = checkContains(response,offset,"12345");

            offset=0; connector.reopen();
            response=connector.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain; charset =  iso-8859-1 ; other=value\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "12345\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"HTTP/1.1 200");
            offset = checkContains(response,offset,"encoding=iso-8859-1");
            offset = checkContains(response,offset,"/R1");
            offset = checkContains(response,offset,"12345");

            offset=0; connector.reopen();
            response=connector.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain; charset=unknown\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "12345\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"HTTP/1.1 200");
            offset = checkContains(response,offset,"encoding=unknown");
            offset = checkContains(response,offset,"/R1");
            offset = checkContains(response,offset,"UnsupportedEncodingException");

            
        }
        catch(Exception e)
        {
            e.printStackTrace();
            assertTrue(false);
            if (response!=null)
                System.err.println(response);
        }
    }

    /* --------------------------------------------------------------- */
    public void testPipeline()
    {        
        
        String response=null;
        String requests=null;
        try
        {
            int offset=0;
            
            offset=0; connector.reopen();
            requests="GET /R1 HTTP/1.1\n"+
                "Host: localhost\n"+
                "Content-Type: text/plain; charset=utf-8\n"+
                "Content-Length: 10\n"+
                "\n"+
                "0123456789\n"+
                "GET /R2 HTTP/1.1\n"+
                "Host: localhost\n"+
                "Content-Type: text/plain; charset=utf-8\n"+
                "Content-Length: 10\n"+
                "\n"+
                "abcdefghij\n";
            
            response=connector.getResponses(requests);
            offset = checkContains(response,offset,"HTTP/1.1 200");
            offset = checkContains(response,offset,"/R1");
            offset = checkContains(response,offset,"encoding=utf-8");
            offset = checkContains(response,offset,"0123456789");
            offset = checkContains(response,offset,"HTTP/1.1 200");
            offset = checkContains(response,offset,"/R2");
            offset = checkContains(response,offset,"encoding=utf-8");
            offset = checkContains(response,offset,"abcdefghij");


            offset=0; connector.reopen();
            requests="GET /R1 HTTP/1.1\n"+
                "Host: localhost\n"+
                "Content-Type: text/plain; charset=utf-8\n"+
                "Content-Length: 1026\n"+
                "\n";
            
            for (int i=0;i<100;i++)
                requests+="0123456789";
            requests+="abcdefghijklmnopqrstuvwxyz";
            requests+=
                "GET /R2 HTTP/1.1\n"+
                "Host: localhost\n"+
                "Content-Type: text/plain; charset=utf-8\n"+
                "Content-Length: 10\n"+
                "\n"+
                "0987654321\n";
            
            response=connector.getResponses(requests);
            offset = checkContains(response,offset,"HTTP/1.1 200");
            offset = checkContains(response,offset,"/R1");
            offset = checkContains(response,offset,"encoding=utf-8");
            offset = checkContains(response,offset,"abcdefghijklmnopqrstuvwxyz");
            offset = checkContains(response,offset,"HTTP/1.1 200");
            offset = checkContains(response,offset,"/R2");
            offset = checkContains(response,offset,"encoding=utf-8");
            offset = checkContains(response,offset,"0987654321");
            
        }
        catch(Exception e)
        {
            e.printStackTrace();
            assertTrue(false);
            if (response!=null)
                System.err.println(response);
        }
    }
    
    /* --------------------------------------------------------------- */
    public void testUnconsumedError() throws Exception
    {        

        String response=null;
        String requests=null;
        int offset=0;

        offset=0; connector.reopen();
        requests="GET /R1?error=500 HTTP/1.1\n"+
        "Host: localhost\n"+
        "Transfer-Encoding: chunked\n"+
        "Content-Type: text/plain; charset=utf-8\n"+
        "\015\012"+
        "5;\015\012"+
        "12345\015\012"+
        "5;\015\012"+
        "67890\015\012"+
        "0;\015\012\015\012"+
        "GET /R2 HTTP/1.1\n"+
        "Host: localhost\n"+
        "Content-Type: text/plain; charset=utf-8\n"+
        "Content-Length: 10\n"+
        "\n"+
        "abcdefghij\n";

        response=connector.getResponses(requests);
        offset = checkContains(response,offset,"HTTP/1.1 500");
        offset = checkContains(response,offset,"HTTP/1.1 200");
        offset = checkContains(response,offset,"/R2");
        offset = checkContains(response,offset,"encoding=utf-8");
        offset = checkContains(response,offset,"abcdefghij");
        
    }
    
    /* --------------------------------------------------------------- */
    public void testUnconsumedException() throws Exception
    {        
        String response=null;
        String requests=null;
        int offset=0;

        offset=0; connector.reopen();
        requests="GET /R1?ISE=true HTTP/1.1\n"+
        "Host: localhost\n"+
        "Transfer-Encoding: chunked\n"+
        "Content-Type: text/plain; charset=utf-8\n"+
        "\015\012"+
        "5;\015\012"+
        "12345\015\012"+
        "5;\015\012"+
        "67890\015\012"+
        "0;\015\012\015\012"+
        "GET /R2 HTTP/1.1\n"+
        "Host: localhost\n"+
        "Content-Type: text/plain; charset=utf-8\n"+
        "Content-Length: 10\n"+
        "\n"+
        "abcdefghij\n";

        Logger logger=null;
        try
        {
            if (!Log.isDebugEnabled())
            {
                logger=Log.getLog();
                Log.setLog(null);
            }
            response=connector.getResponses(requests);
            offset = checkContains(response,offset,"HTTP/1.1 500");
            offset = checkContains(response,offset,"Connection: close");
            checkNotContained(response,offset,"HTTP/1.1 200");
        }
        finally
        {
            if (logger!=null)
                Log.setLog(logger);
        }
    }

    
    public void testConnection ()
    { 
        String response=null;
        try
        {
            int offset=0;
           
            offset=0; connector.reopen();
            response=connector.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Connection: TE, close"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain; charset=utf-8\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "12345\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"Connection: TE");
            offset = checkContains(response,offset,"Connection: close");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertTrue(false);
            if (response!=null)
                 System.err.println(response);
        }
    }
    
    public void testOversizedBuffer() 
    {
        String response = null;
        connector.reopen();
        try 
        {
            int offset = 0;
            String cookie = "thisisastringthatshouldreachover1kbytes";
            for (int i=0;i<100;i++)
                cookie+="xxxxxxxxxxxx";
            response = connector.getResponses("GET / HTTP/1.1\n"+
                "Host: localhost\n" +
                "Cookie: "+cookie+"\n"+
                "\015\012"
             );
            offset = checkContains(response, offset, "HTTP/1.1 413");
        } 
        catch(Exception e)
        {
            e.printStackTrace();
            assertTrue(false);
            if(response != null)
                System.err.println(response);
                
        }
    }
    
    
    public void testOversizedResponse ()
    throws Exception
    {  
        String str = "thisisastringthatshouldreachover1kbytes";
        for (int i=0;i<400;i++)
            str+="xxxxxxxxxxxx";
        final String longstr = str;
        String response = null;
        server.stop();
        server.setHandler(new DumpHandler()
        {
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
            {
                try
                {
                    Request base_request = (request instanceof Request) ? (Request)request:HttpConnection.getCurrentConnection().getRequest();
                    base_request.setHandled(true);
                    response.setHeader(HttpHeaders.CONTENT_TYPE,MimeTypes.TEXT_HTML);
                    response.setHeader("LongStr", longstr);
                    PrintWriter writer = response.getWriter();
                    writer.write("<html><h1>FOO</h1></html>");  
                    writer.flush();
                    writer.close();
                    throw new RuntimeException("SHOULD NOT GET HERE");
                }
                catch(ArrayIndexOutOfBoundsException e)
                {
                    Log.debug(e);
                    Log.info("correctly ignored "+e);
                }
            }
        });
        server.start();
        
        connector.reopen();
        try 
        {
            int offset = 0;
          
            response = connector.getResponses("GET / HTTP/1.1\n"+
                "Host: localhost\n" +
                "\015\012"
             );
          
            offset = checkContains(response, offset, "HTTP/1.1 500");
        } 
        catch(Exception e)
        {
            e.printStackTrace();
            if(response != null)
                System.err.println(response);
            fail("Exception");      
        }
    }
    
    public void testAsterisk()
    {
        String response = null;

        try 
        {
            int offset=0;
            
            offset=0; connector.reopen();
            response=connector.getResponses("OPTIONS * HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain; charset=utf-8\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "12345\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"HTTP/1.1 200");
            offset = checkContains(response,offset,"*");
            
            // to prevent the DumpHandler from picking this up and returning 200 OK
            server.setHandler(null);
            offset=0; connector.reopen();
            response=connector.getResponses("GET * HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain; charset=utf-8\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "12345\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"HTTP/1.1 404 Not Found");

            offset=0; connector.reopen();
            response=connector.getResponses("GET ** HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain; charset=utf-8\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "12345\015\012"+
                                           "0;\015\012\015\012");
            offset = checkContains(response,offset,"HTTP/1.1 400 Bad Request");
        } 
        catch (Exception e)
        {
            e.printStackTrace();
            assertTrue(false);
            if (response!=null)
                 System.err.println(response);
        }

    }
    
    private int checkContains(String s,int offset,String c)
    {
        int o=s.indexOf(c,offset);
        if (o<offset)
        {
            System.err.println("FAILED");
            System.err.println("'"+c+"' not in:");
            System.err.println(s.substring(offset));
            System.err.flush();
            System.out.println("--\n"+s);
            System.out.flush();
            assertTrue(false);
        }
        return o;
    }

    private void checkNotContained(String s,int offset,String c)
    {
        int o=s.indexOf(c,offset);
        if (o>=offset)
        {
            System.err.println("FAILED");
            System.err.println("'"+c+"' IS in:");
            System.err.println(s.substring(offset));
            System.err.flush();
            System.out.println("--\n"+s);
            System.out.flush();
            assertTrue(false);
        }
    }


    

}


