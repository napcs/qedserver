//========================================================================
//$$Id: J2SE6ContextHandler.java 1919 2010-10-18 03:54:40Z gregw $$
//
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

package org.mortbay.jetty.j2se6;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.ContextHandler;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;

/**
 * Jetty ContextHandler that bridges requests to {@link HttpHandler}.
 * 
 */
public class J2SE6ContextHandler extends ContextHandler
{

    private HttpContext _context;

    private HttpHandler _handler;

    public J2SE6ContextHandler(HttpContext context, HttpHandler handler)
    {
        this._context = context;
        this._handler = handler;
    }

    @Override
    public void handle(String target, HttpServletRequest req,
                       HttpServletResponse resp, int dispatch) throws IOException,
                       ServletException
                       {
        if (!target.startsWith(getContextPath())) return;
        JettyHttpExchange jettyHttpExchange = new JettyHttpExchange(_context, req, resp);
        try
        {
            Authenticator auth = _context.getAuthenticator();

            if (auth != null)
            {
                Authenticator.Result authResult = auth.authenticate(jettyHttpExchange);
                if (authResult instanceof Authenticator.Success)
                {
                    HttpPrincipal p = ((Authenticator.Success)authResult).getPrincipal();
                    jettyHttpExchange.setPrincipal(p);
                    invokeHandler(jettyHttpExchange);
                }
                else  if (authResult instanceof Authenticator.Failure)
                {
                    int rc = ((Authenticator.Failure)authResult).getResponseCode();
                    resp.sendError(rc);
                }
                else if (authResult instanceof Authenticator.Retry)
                {
                    int rc = ((Authenticator.Retry)authResult).getResponseCode();
                    resp.sendError(rc);
                }
            }
            else
                invokeHandler(jettyHttpExchange);
        }
        catch(Exception ex)
        {
            PrintWriter writer = new PrintWriter(jettyHttpExchange.getResponseBody());

            resp.setStatus(500);
            writer.println("<h2>HTTP ERROR: 500</h2>");
            writer.println("<pre>INTERNAL_SERVER_ERROR</pre>");
            writer.println("<p>RequestURI=" + req.getRequestURI() + "</p>");

            writer.println("<pre>");
            ex.printStackTrace(writer);
            writer.println("</pre>");

            writer.println("<p><i><small><a href=\"http://jetty.mortbay.org\">Powered by jetty://</a></small></i></p>");

            writer.close();
        }
        finally
        {
            Request base_request = (req instanceof Request) ? (Request)req:HttpConnection.getCurrentConnection().getRequest();
            base_request.setHandled(true);
        }
    }

    protected void invokeHandler (JettyHttpExchange exchange) throws IOException
    {
        List<Filter> filters = _context.getFilters();
        if (filters != null && filters.size()>0)
        {
            List<Filter> copy = new ArrayList<Filter>(filters.subList(1, filters.size()));
            Filter.Chain chain = new Filter.Chain(copy, _handler);
            filters.get(0).doFilter(exchange, chain);    
        }
        else
            _handler.handle(exchange);
    }

    public HttpHandler getHttpHandler()
    {
        return _handler;
    }

    public void setHttpHandler(HttpHandler handler)
    {
        this._handler = handler;
    }

}
