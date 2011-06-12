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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
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
 * @version $Revision: 1319 $ $Date: 2008-11-14 10:55:54 +1100 (Fri, 14 Nov 2008) $
 */
public class ServerCrossContextSessionTest
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
                // Perform a request, on server side a cross context dispatch will be done
                ContentExchange exchange = new ContentExchange(true);
                exchange.setMethod(HttpMethods.GET);
                exchange.setURL("http://localhost:" + port + contextA + servletMapping);
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

    public static class TestServletA extends HttpServlet
    {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            HttpSession session = request.getSession(false);
            if (session == null) session = request.getSession(true);

            // Add something to the session
            session.setAttribute("A", "A");
            System.out.println("A: session.getAttributeNames() = " + Collections.list(session.getAttributeNames()));

            // Perform cross context dispatch to another context
            // Over there we will check that the session attribute added above is not visible
            ServletContext contextB = getServletContext().getContext("/contextB");
            RequestDispatcher dispatcherB = contextB.getRequestDispatcher(request.getServletPath());
            dispatcherB.forward(request, response);

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

            // Be sure nothing from contextA is present
            Object objectA = session.getAttribute("A");
            assert objectA == null;

            // Add something, so in contextA we can check if it is visible (it must not).
            session.setAttribute("B", "B");
            System.out.println("B: session.getAttributeNames() = " + Collections.list(session.getAttributeNames()));
        }
    }
}
