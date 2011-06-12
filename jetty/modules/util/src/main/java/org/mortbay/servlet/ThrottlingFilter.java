/* ------------------------------------------------------------------------
 * $Id$
 * Copyright 2006 Tim Vernum
 * ------------------------------------------------------------------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ------------------------------------------------------------------------
 */

package org.mortbay.servlet;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.log.Log;
import org.mortbay.util.ajax.Continuation;

/**
 * This filter protects a web application from having to handle an unmanageable load. 
 * <p>
 * For servers where there is 1 application with standardized resource restrictions, then this affect can be easily
 *  controlled by limiting the size of the {@link org.mortbay.jetty.Server#setThreadPool server thread pool}, however
 *  where there are multiple applications, or a single application has different resource requirements for different
 *  URLs, then this filter can assist in managing the number of requests being services at any point in time.
 * <p>
 * The filter has 3 configurable values, which may be set as init parameters:
 * <OL>
 *  <LI><code>maximum</code> determines the maxmimum number of requests that may be on the filter chain at any point in time.
 *      <i>(See below for a more detailed explanation)</i></LI>
 * <LI><code>block</code> determines how long (in milliseconds) a request will be queued before it is rejected. 
 *      Set this to -1 to block indefinately.</LI>
 * <LI><code>queue</code> determines how many requests can be queued simultaneously - any additional requests will be rejected.
 *      Set this to 0 to turn off queueing.</LI>
 * </OL>
 * 
 * <b>Request Counting</b>: The filter counts how many requests are currently being services by the rest of the filter chain
 *  (including any servlets that may be configured to handle the request). Request counting is <i>per instance</i> of the filter.
 *  There is no syncronization between virtual machines, and the request count is not shared between multiple instances of the filter.
 *  Thus a web.xml file such as <pre>
 *  &lt;filter&gt;&lt;filter-name&gt;throttle1&lt;/filter-name&gt;
 *          &lt;filter-class&gt;org.adjective.spiral.filter.ThrottlingFilter&lt;/filter-class&gt;
 *  &lt;filter&gt;&lt;filter-name&gt;throttle2&lt;/filter-name&gt;
 *          &lt;filter-class&gt;org.adjective.spiral.filter.ThrottlingFilter&lt;/filter-class&gt;</pre>
 *  creates 2 separate filters with individual request counts.
 * <p>
 * <b>Queueing</b>: When the number of active requests exceed the <code>maximum</code> requests will be queued. This queue regulates
 *  the flow of connections. Once the number of requests on the queue reached the <code>queue</code> threshold, then any new requests
 *  will be rejected. Requests are queued for a maximum of <code>block</code> milliseconds - is no capacity is made available in this
 *  time then the request will be rejected. The oldest pending request is removed from the queue and processed as soon as the number
 *  of pending requests falls below the <code>maximum</code> value (<i>i.e.</i> when a request is completed)
 * <p> 
 * <b>Rejection</b>: Requests are rejected when the number of requests in progress has reached <i>maximum</i> and either the queue
 * is full; or a request has been queued for more than <code>block</code> milliseconds. The rejection is performed by calling the
 * method {@link #rejectRequest}. By default this method sends the HTTP status code {@link HttpServletResponse#SC_SERVICE_UNAVAILABLE 503},
 * but this may be over-ridden in derived classes. 
 * <p>
 * This filter works best with the {@link org.mortbay.jetty.nio.SelectChannelConnector}, as {@link org.mortbay.jetty.RetryRequest} based 
 * {@link org.mortbay.util.ajax.Continuation}s can be used to free the thread and other resources associated with the queued requests.
 * 
 * @author - Tim Vernum
 */
public class ThrottlingFilter implements Filter
{

    private int _maximum;
    private int _current;
    private long _queueTimeout;
    private long _queueSize;
    private final Object _lock;
    private final List _queue;

    public ThrottlingFilter()
    {
        _current = 0;
        _lock = new Object();
        _queue = new LinkedList();
    }

    public void init(FilterConfig filterConfig) 
        throws ServletException
    {
        _maximum = getIntegerParameter(filterConfig, "maximum", 10);
        _queueTimeout = getIntegerParameter(filterConfig, "block", 5000);
        _queueSize = getIntegerParameter(filterConfig, "queue", 500);

        if (_queueTimeout == -1)
        {
            _queueTimeout = Integer.MAX_VALUE;
        }

        Log.debug("Config{maximum:" + _maximum + ", block:" + _queueTimeout + ", queue:" + _queueSize + "}", null, null);
    }

    private int getIntegerParameter(FilterConfig filterConfig, String name, int defaultValue) 
        throws ServletException
    {
        String value = filterConfig.getInitParameter(name);
        if (value == null)
        {
            return defaultValue;
        }
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            throw new ServletException("Parameter " + name + " must be a number (was " + value + " instead)");
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
        throws IOException, ServletException
    {
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) 
        throws IOException, ServletException
    {
        Continuation continuation = getContinuation(request);

        boolean accepted = false;
        try
        {
            // Is the request accepted?
            accepted=acceptRequest();
            if (!accepted)
            {
                // Has the request been tried before?
                if (continuation.isPending())
                {
                    Log.debug("Request {} / {} was already queued, rejecting", request.getRequestURI(), continuation);
                    dropFromQueue(continuation);
                    continuation.reset();
                }
                // No if we can queue the request
                else if (queueRequest(request, response, continuation))
                    // Try to get it accepted again (after wait in queue).
                    accepted=acceptRequest();
            }
            
            // Handle if we are accepted, else reject
            if (accepted)
                chain.doFilter(request, response);
            else
                rejectRequest(request, response);
        }
        finally
        {
            if (accepted)
            {
                releaseRequest();
                popQueue();
            }
        }

    }

    private void dropFromQueue(Continuation continuation)
    {
        _queue.remove(continuation);
        continuation.reset();
    }

    protected void rejectRequest(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Too many active connections to resource "
                + request.getRequestURI());
    }

    private void popQueue()
    {
        Continuation continuation;
        synchronized (_queue)
        {
            if (_queue.isEmpty())
            {
                return;
            }
            continuation = (Continuation) _queue.remove(0);
        }
        Log.debug("Resuming continuation {}", continuation, null);
        continuation.resume();
    }

    private void releaseRequest()
    {
        synchronized (_lock)
        {
            _current--;
        }
    }

    private boolean acceptRequest()
    {
        synchronized (_lock)
        {
            if (_current < _maximum)
            {
                _current++;
                return true;
            }
        }
        return false;
    }

    private boolean queueRequest(HttpServletRequest request, HttpServletResponse response, Continuation continuation) throws IOException,
            ServletException
    {
        synchronized (_queue)
        {
            if (_queue.size() >= _queueSize)
            {
                Log.debug("Queue is full, rejecting request {}", request.getRequestURI(), null);
                return false;
            }
            
            Log.debug("Queuing request {} / {}", request.getRequestURI(), continuation);
            _queue.add(continuation);
        }

        continuation.suspend(_queueTimeout);
        Log.debug("Resuming blocking continuation for request {}", request.getRequestURI(), null);
        return true;
    }

    private Continuation getContinuation(ServletRequest request)
    {
        return (Continuation) request.getAttribute("org.mortbay.jetty.ajax.Continuation");
    }

    public void destroy()
    {
        _queue.clear();
    }

}
