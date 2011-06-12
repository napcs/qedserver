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
import java.util.Collections;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.client.ContentExchange;
import org.mortbay.jetty.client.HttpClient;
import org.mortbay.jetty.servlet.Context;
import org.testng.annotations.Test;

/**
 * @version $Revision: 1645 $ $Date: 2009-09-15 20:31:07 +1000 (Tue, 15 Sep 2009) $
 */
public class ClientCrossContextSessionTest
{
    @Test
    public void testCrossContextDispatch() throws Exception
    {
        Random random = new Random(System.nanoTime());

        String contextA = "/contextA";
        String contextB = "/contextB";
        String servletMapping = "/server";
        int port = random.nextInt(50000) + 10000;
        TerracottaJettyServer server = new TerracottaJettyServer(port);
        Context ctxA = server.addContext(contextA);
        ctxA.addServlet(TestServletA.class, servletMapping);
        Context ctxB = server.addContext(contextB);
        ctxB.addServlet(TestServletB.class, servletMapping);
        server.start();
        try
        {
            HttpClient client = new HttpClient();
            client.setConnectorType(HttpClient.CONNECTOR_SOCKET);
            client.start();
            try
            {
                // Perform a request to contextA
                ContentExchange exchangeA = new ContentExchange(true);
                exchangeA.setMethod(HttpMethods.GET);
                exchangeA.setURL("http://localhost:" + port + contextA + servletMapping);
                client.send(exchangeA);
                exchangeA.waitForDone();
                assert exchangeA.getResponseStatus() == HttpServletResponse.SC_OK;
                String sessionCookie = exchangeA.getResponseFields().getStringField("Set-Cookie");
                assert sessionCookie != null;
                // Mangle the cookie, replacing Path with $Path, etc.
                sessionCookie = sessionCookie.replaceFirst("(\\W)(P|p)ath=", "$1\\$Path=");

                // Perform a request to contextB with the same session cookie
                ContentExchange exchangeB = new ContentExchange(true);
                exchangeB.setMethod(HttpMethods.GET);
                exchangeB.setURL("http://localhost:" + port + contextB + servletMapping);
                exchangeB.getRequestFields().add("Cookie", sessionCookie);
                client.send(exchangeB);
                exchangeB.waitForDone();
                assert exchangeB.getResponseStatus() == HttpServletResponse.SC_OK;
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

    public static class TestServletA extends HttpServlet
    {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            HttpSession session = request.getSession(false);
            if (session == null) session = request.getSession(true);

            // Add something to the session
            session.setAttribute("A", "A");

            // Check that we don't see things put in session by contextB
            Object objectB = session.getAttribute("B");
            assert objectB == null;
            System.out.println("A: session.getAttributeNames() = " + Collections.list(session.getAttributeNames()));
        }
    }

    public static class TestServletB extends HttpServlet
    {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse httpServletResponse) throws ServletException, IOException
        {
            HttpSession session = request.getSession(false);
            if (session == null) session = request.getSession(true);

            // Add something to the session
            session.setAttribute("B", "B");

            // Check that we don't see things put in session by contextA
            Object objectA = session.getAttribute("A");
            assert objectA == null;
            System.out.println("B: session.getAttributeNames() = " + Collections.list(session.getAttributeNames()));
        }
    }
}