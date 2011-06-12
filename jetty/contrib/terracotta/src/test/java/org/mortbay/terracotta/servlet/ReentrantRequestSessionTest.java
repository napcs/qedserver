// ========================================================================
// Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.terracotta.servlet;

import java.io.IOException;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.client.ContentExchange;
import org.mortbay.jetty.client.HttpClient;
import org.testng.annotations.Test;

/**
 * @version $Revision: 1319 $ $Date: 2008-11-14 10:55:54 +1100 (Fri, 14 Nov 2008) $
 */
public class ReentrantRequestSessionTest
{
    @Test
    public void testReentrantRequestSession() throws Exception
    {
        Random random = new Random(System.nanoTime());

        String contextPath = "";
        String servletMapping = "/server";
        int port = random.nextInt(50000) + 10000;
        TerracottaJettyServer server = new TerracottaJettyServer(port);
        server.addContext(contextPath).addServlet(TestServlet.class, servletMapping);
        server.start();
        try
        {
            HttpClient client = new HttpClient();
            client.setConnectorType(HttpClient.CONNECTOR_SOCKET);
            client.start();
            try
            {
                ContentExchange exchange = new ContentExchange(true);
                exchange.setMethod(HttpMethods.GET);
                exchange.setURL("http://localhost:" + port + contextPath + servletMapping + "?action=reenter&port=" + port + "&path=" + contextPath + servletMapping);
                client.send(exchange);
                exchange.waitForDone();
                assert exchange.getResponseStatus() == HttpServletResponse.SC_OK;
            }
            finally
            {
                client.stop();
            }
        }
        finally
        {
            server.stop();
        }
    }

    public static class TestServlet extends HttpServlet
    {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            doPost(request, response);
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            HttpSession session = request.getSession(false);
            if (session == null) session = request.getSession(true);

            String action = request.getParameter("action");
            if ("reenter".equals(action))
            {
                int port = Integer.parseInt(request.getParameter("port"));
                String path = request.getParameter("path");

                // We want to make another request with a different session
                // while this request is still pending, to see if the locking is
                // fine grained (per session at least).
                try
                {
                    HttpClient client = new HttpClient();
                    client.setConnectorType(HttpClient.CONNECTOR_SOCKET);
                    client.start();
                    try
                    {
                        ContentExchange exchange = new ContentExchange(true);
                        exchange.setMethod(HttpMethods.GET);
                        exchange.setURL("http://localhost:" + port + path + "?action=none");
                        client.send(exchange);
                        exchange.waitForDone();
                        assert exchange.getResponseStatus() == HttpServletResponse.SC_OK;
                    }
                    finally
                    {
                        client.stop();
                    }
                }
                catch (Exception x)
                {
                    throw new ServletException(x);
                }
            }
            else
            {
                // Reentrancy was successful, just return
            }
        }
    }
}
