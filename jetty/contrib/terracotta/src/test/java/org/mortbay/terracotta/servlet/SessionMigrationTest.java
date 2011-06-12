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
import java.io.PrintWriter;
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
 * @version $Revision: 1645 $ $Date: 2009-09-15 20:31:07 +1000 (Tue, 15 Sep 2009) $
 */
public class SessionMigrationTest
{
    @Test
    public void testSessionMigration() throws Exception
    {
        Random random = new Random(System.nanoTime());

        String contextPath = "";
        String servletMapping = "/server";
        int port1 = random.nextInt(50000) + 10000;
        TerracottaJettyServer server1 = new TerracottaJettyServer(port1);
        server1.addContext(contextPath).addServlet(TestServlet.class, servletMapping);
        server1.start();
        try
        {
            int port2 = random.nextInt(50000) + 10000;
            TerracottaJettyServer server2 = new TerracottaJettyServer(port2);
            server2.addContext(contextPath).addServlet(TestServlet.class, servletMapping);
            server2.start();
            try
            {
                HttpClient client = new HttpClient();
                client.setConnectorType(HttpClient.CONNECTOR_SOCKET);
                client.start();
                try
                {
                    // Perform one request to server1 to create a session
                    int value = 1;
                    ContentExchange exchange1 = new ContentExchange(true);
                    exchange1.setMethod(HttpMethods.POST);
                    exchange1.setURL("http://localhost:" + port1 + contextPath + servletMapping + "?action=set&value=" + value);
                    client.send(exchange1);
                    exchange1.waitForDone();
                    assert exchange1.getResponseStatus() == HttpServletResponse.SC_OK;
                    String sessionCookie = exchange1.getResponseFields().getStringField("Set-Cookie");
                    assert sessionCookie != null;
                    // Mangle the cookie, replacing Path with $Path, etc.
                    sessionCookie = sessionCookie.replaceFirst("(\\W)(P|p)ath=", "$1\\$Path=");

                    // Perform a request to server2 using the session cookie from the previous request
                    // This should migrate the session from server1 to server2.
                    ContentExchange exchange2 = new ContentExchange(true);
                    exchange2.setMethod(HttpMethods.GET);
                    exchange2.setURL("http://localhost:" + port2 + contextPath + servletMapping + "?action=get");
                    exchange2.getRequestFields().add("Cookie", sessionCookie);
                    client.send(exchange2);
                    exchange2.waitForDone();
                    assert exchange2.getResponseStatus() == HttpServletResponse.SC_OK;
                    String response = exchange2.getResponseContent();
                    assert response.trim().equals(String.valueOf(value));
                }
                finally
                {
                    client.stop();
                }
            }
            finally
            {
                server2.stop();
            }
        }
        finally
        {
            server1.stop();
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
            if ("set".equals(action))
            {
                int value = Integer.parseInt(request.getParameter("value"));
                session.setAttribute("value", value);
                PrintWriter writer = response.getWriter();
                writer.println(value);
                writer.flush();
            }
            else if ("get".equals(action))
            {
                int value = (Integer)session.getAttribute("value");
                PrintWriter writer = response.getWriter();
                writer.println(value);
                writer.flush();
            }
        }
    }
}
