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
import java.util.concurrent.TimeUnit;
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
public class OrphanedSessionTest
{
    /**
     * If nodeA creates a session, and just afterwards crashes, it is the only node that knows about the session.
     * Its session data will remain forever in the Terracotta server, but it will never be expired because
     * other nodes are not aware of that session if they never get hit by its session id.
     * We want to test that the session data is gone after scavenging.
     */
    @Test
    public void testOrphanedSession() throws Exception
    {
        Random random = new Random(System.nanoTime());

        // Disable scavenging for the first server, so that we simulate its "crash".
        String contextPath = "";
        String servletMapping = "/server";
        int port1 = random.nextInt(50000) + 10000;
        int inactivePeriod = 5;
        TerracottaJettyServer server1 = new TerracottaJettyServer(port1, inactivePeriod, -1);
        server1.addContext(contextPath).addServlet(TestServlet.class, servletMapping);
        server1.start();
        try
        {
            int port2 = random.nextInt(50000) + 10000;
            int scavengePeriod = 2;
            TerracottaJettyServer server2 = new TerracottaJettyServer(port2, inactivePeriod, scavengePeriod);
            server2.addContext(contextPath).addServlet(TestServlet.class, servletMapping);
            server2.start();
            try
            {
                HttpClient client = new HttpClient();
                client.setConnectorType(HttpClient.CONNECTOR_SOCKET);
                client.start();
                try
                {
                    // Connect to server1 to create a session and get its session cookie
                    ContentExchange exchange1 = new ContentExchange(true);
                    exchange1.setMethod(HttpMethods.GET);
                    exchange1.setURL("http://localhost:" + port1 + contextPath + servletMapping + "?action=init");
                    client.send(exchange1);
                    exchange1.waitForDone();
                    assert exchange1.getResponseStatus() == HttpServletResponse.SC_OK;
                    String sessionCookie = exchange1.getResponseFields().getStringField("Set-Cookie");
                    assert sessionCookie != null;
                    // Mangle the cookie, replacing Path with $Path, etc.
                    sessionCookie = sessionCookie.replaceFirst("(\\W)(P|p)ath=", "$1\\$Path=");

                    // Wait for the session to expire.
                    // The first node does not do any scavenging, but the session
                    // must be removed by scavenging done in the other node.
                    Thread.sleep(TimeUnit.SECONDS.toMillis(inactivePeriod + 2L * scavengePeriod));

                    // Perform one request to server2 to be sure that the session has been expired
                    ContentExchange exchange2 = new ContentExchange(true);
                    exchange2.setMethod(HttpMethods.GET);
                    exchange2.setURL("http://localhost:" + port2 + contextPath + servletMapping + "?action=check");
                    exchange2.getRequestFields().add("Cookie", sessionCookie);
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
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            String action = request.getParameter("action");
            if ("init".equals(action))
            {
                HttpSession session = request.getSession(true);
                session.setAttribute("A", "A");
            }
            else if ("check".equals(action))
            {
                HttpSession session = request.getSession(false);
                assert session == null;
            }
        }
    }
}
