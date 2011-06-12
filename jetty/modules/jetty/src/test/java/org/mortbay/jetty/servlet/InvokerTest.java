// ========================================================================
// Copyright 2008 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.servlet;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.mortbay.jetty.LocalConnector;
import org.mortbay.jetty.Server;

/**
 * @author athena
 *
 */
public class InvokerTest extends TestCase
{
    Server _server;
    LocalConnector _connector;
    Context _context;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        _server = new Server();
        _connector = new LocalConnector();
        _context = new Context(Context.SESSIONS|Context.SECURITY);
        
        _server.setSendServerVersion(false);
        _server.addConnector(_connector);
        _server.addHandler(_context);
        
        _context.setContextPath("/");
        
        ServletHolder holder = _context.addServlet(Invoker.class, "/servlet/*");
        holder.setInitParameter("nonContextServlets","true");
        _server.start();   
    }

    public void testInvoker() throws Exception
    {
        String requestPath = "/servlet/"+TestServlet.class.getName();
        String request =  "GET "+requestPath+" HTTP/1.0\r\n"+
            "Host: tester\r\n"+
            "\r\n";
 
        _connector.reopen();
        String expectedResponse = "HTTP/1.1 200 OK\r\n" +
            "Content-Length: 20\r\n" +
            "\r\n" +
            "Invoked TestServlet!";
        
        String response = _connector.getResponses(request);
        assertEquals(expectedResponse, response);
    }
    
    public static class TestServlet extends HttpServlet implements Servlet
    {
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            response.getWriter().print("Invoked TestServlet!");
            response.getWriter().close();            
        }
    }
}
