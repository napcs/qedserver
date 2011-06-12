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
public class ImmortalSessionTest
{
    @Test
    public void testImmortalSession() throws Exception
    {
        Random random = new Random(System.nanoTime());

        String contextPath = "";
        String servletMapping = "/server";
        int port = random.nextInt(50000) + 10000;
        int scavengePeriod = 2;
        TerracottaJettyServer server = new TerracottaJettyServer(port, -1, scavengePeriod);
        server.addContext(contextPath).addServlet(TestServlet.class, servletMapping);
        server.start();
        try
        {
            HttpClient client = new HttpClient();
            client.setConnectorType(HttpClient.CONNECTOR_SOCKET);
            client.start();
            try
            {
                int value = 42;
                ContentExchange exchange = new ContentExchange(true);
                exchange.setMethod(HttpMethods.GET);
                exchange.setURL("http://localhost:" + port + contextPath + servletMapping + "?action=set&value=" + value);
                client.send(exchange);
                exchange.waitForDone();
                assert exchange.getResponseStatus() == HttpServletResponse.SC_OK;
                String sessionCookie = exchange.getResponseFields().getStringField("Set-Cookie");
                assert sessionCookie != null;
                // Mangle the cookie, replacing Path with $Path, etc.
                sessionCookie = sessionCookie.replaceFirst("(\\W)(P|p)ath=", "$1\\$Path=");

                String response = exchange.getResponseContent();
                assert response.trim().equals(String.valueOf(value));

                // Let's wait for the scavenger to run, waiting 2.5 times the scavenger period
                Thread.sleep(scavengePeriod * 2500L);

                // Be sure the session is still there
                exchange = new ContentExchange(true);
                exchange.setMethod(HttpMethods.GET);
                exchange.setURL("http://localhost:" + port + contextPath + servletMapping + "?action=get");
                exchange.getRequestFields().add("Cookie", sessionCookie);
                client.send(exchange);
                exchange.waitForDone();
                assert exchange.getResponseStatus() == HttpServletResponse.SC_OK;
                response = exchange.getResponseContent();
                assert response.trim().equals(String.valueOf(value));
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
            String result = null;
            String action = request.getParameter("action");
            if ("set".equals(action))
            {
                String value = request.getParameter("value");
                HttpSession session = request.getSession(true);
                session.setAttribute("value", value);
                result = value;
            }
            else if ("get".equals(action))
            {
                HttpSession session = request.getSession(false);
                result = (String)session.getAttribute("value");
            }
            PrintWriter writer = response.getWriter();
            writer.println(result);
            writer.flush();
        }
    }
}
