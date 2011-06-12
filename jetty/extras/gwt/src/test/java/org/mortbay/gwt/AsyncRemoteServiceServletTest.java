//========================================================================
//Copyright 2007 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.gwt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.TestCase;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;

public class AsyncRemoteServiceServletTest extends TestCase
{
    
    private Server _server;
    
    public void setUp() throws Exception
    {
        startServer();
    }
    
    public void tearDown() throws Exception
    {
        stopServer();
    }
    
    private void startServer() throws Exception
    {
        _server = new Server();
        SelectChannelConnector scc = new SelectChannelConnector();
        scc.setPort(8010);
        _server.addConnector(scc);
        Context context = new Context();
        context.setContextPath("/");
        context.addServlet(HelloWorldServiceImpl.class, "/helloworld");
        _server.setHandler(context);
        _server.start();
    }
    
    private void stopServer() throws Exception
    {
        _server.stop();
        _server = null;
    }
    
    public void testRPC() throws Exception
    {
        // place holder... have to figure out the exact output string the gwt-client sends.
        // sending "foo" to HelloWorldServiceImpl.sayHello(String);
        String content = "3￿0￿6￿http://localhost:8080/org.mortbay.gwt.example.HelloWorld/" +
        		"￿5BA8A5B3E35F40698BB0BF65F390BCF2￿" +
        		"org.mortbay.gwt.example.client.HelloWorldService￿" +
        		"sayHello￿java.lang.String￿foo￿1￿2￿3￿4￿1￿5￿6￿";
        
        long now = System.currentTimeMillis();
        URL url = new URL("http://localhost:8010/helloworld");        
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/x-gwt-rpc; charset=utf-8");
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(true);
        conn.connect();        
        conn.getOutputStream().write(content.getBytes());
        conn.getOutputStream().flush();
        isTokenPresent(conn, "//OK");
        conn.disconnect();
        System.err.println("ELAPSED: " + (System.currentTimeMillis() - now));        
    }
    
    protected static boolean isTokenPresent(HttpURLConnection conn, String token) throws Exception
    {        
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));      
        String line = null;
        boolean present = false;
        System.err.println("RESPONSE");
        while((line=br.readLine())!=null)
        {
            System.err.println(line);
            if(line.indexOf(token)!=-1)
            {                
                present = true;
                break;
            }                    
        }
        conn.disconnect();
        return present;
    }

}
