// ========================================================================
// Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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
import org.mortbay.jetty.Request;
import org.mortbay.jetty.client.ContentExchange;
import org.mortbay.jetty.client.HttpClient;
import org.testng.annotations.Test;

/**
 * @version $Revision: 1645 $ $Date: 2009-09-15 20:31:07 +1000 (Tue, 15 Sep 2009) $
 */
public class LocalSessionScavengingTest
{
    @Test
    public void testLocalSessionsScavenging() throws Exception
    {
        Random random = new Random(System.nanoTime());
        String contextPath = "";
        String servletMapping = "/server";
        int port1 = random.nextInt(50000) + 10000;
        int inactivePeriod = 1;
        int scavengePeriod = 2;
        TerracottaJettyServer server1 = new TerracottaJettyServer(port1, inactivePeriod, scavengePeriod);
        server1.addContext(contextPath).addServlet(TestServlet.class, servletMapping);
        server1.start();
        try
        {
            int port2 = random.nextInt(50000) + 10000;
            TerracottaJettyServer server2 = new TerracottaJettyServer(port2, inactivePeriod, scavengePeriod * 3);
            server2.addContext(contextPath).addServlet(TestServlet.class, servletMapping);
            server2.start();
            try
            {
                HttpClient client = new HttpClient();
                client.setConnectorType(HttpClient.CONNECTOR_SOCKET);
                client.start();
                try
                {
                    String[] urls = new String[2];
                    urls[0] = "http://localhost:" + port1 + contextPath + servletMapping;
                    urls[1] = "http://localhost:" + port2 + contextPath + servletMapping;

                    // Create the session on node1
                    ContentExchange exchange1 = new ContentExchange(true);
                    exchange1.setMethod(HttpMethods.GET);
                    exchange1.setURL(urls[0] + "?action=init");
                    client.send(exchange1);
                    exchange1.waitForDone();
                    assert exchange1.getResponseStatus() == HttpServletResponse.SC_OK;
                    String sessionCookie = exchange1.getResponseFields().getStringField("Set-Cookie");
                    assert sessionCookie != null;
                    // Mangle the cookie, replacing Path with $Path, etc.
                    sessionCookie = sessionCookie.replaceFirst("(\\W)(P|p)ath=", "$1\\$Path=");

                    // Be sure the session is also present in node2
                    ContentExchange exchange2 = new ContentExchange(true);
                    exchange2.setMethod(HttpMethods.GET);
                    exchange2.setURL(urls[1] + "?action=test");
                    exchange2.getRequestFields().add("Cookie", sessionCookie);
                    client.send(exchange2);
                    exchange2.waitForDone();
                    assert exchange2.getResponseStatus() == HttpServletResponse.SC_OK;

                    // Wait for the scavenger to run on node1, waiting 2.5 times the scavenger period
                    Thread.sleep(scavengePeriod * 2500L);

                    // Check that node1 does not have any local session cached
                    exchange1 = new ContentExchange(true);
                    exchange1.setMethod(HttpMethods.GET);
                    exchange1.setURL(urls[0] + "?action=check");
                    client.send(exchange1);
                    exchange1.waitForDone();
                    assert exchange1.getResponseStatus() == HttpServletResponse.SC_OK;

                    // Wait for the scavenger to run on node2, waiting 2 times the scavenger period
                    // This ensures that the scavenger on node2 runs at least once.
                    Thread.sleep(scavengePeriod * 2000L);

                    // Check that node1 does not have any local session cached
                    exchange2 = new ContentExchange(true);
                    exchange2.setMethod(HttpMethods.GET);
                    exchange2.setURL(urls[1] + "?action=check");
                    client.send(exchange2);
                    exchange2.waitForDone();
                    assert exchange2.getResponseStatus() == HttpServletResponse.SC_OK;
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
        private TerracottaSessionManager sessionManager;

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse httpServletResponse) throws ServletException, IOException
        {
            String action = request.getParameter("action");
            if ("init".equals(action))
            {
                HttpSession session = request.getSession(true);
                session.setAttribute("test", "test");
                this.sessionManager = (TerracottaSessionManager)((Request)request).getSessionManager();
            }
            else if ("test".equals(action))
            {
                HttpSession session = request.getSession(false);
                session.setAttribute("test", "test");
                this.sessionManager = (TerracottaSessionManager)((Request)request).getSessionManager();
            }
            else if ("check".equals(action))
            {
                int size = sessionManager.getSessions();
                assert size == 0;
            }
        }
    }
}
