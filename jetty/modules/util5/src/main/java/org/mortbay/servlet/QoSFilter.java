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
package org.mortbay.servlet;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mortbay.util.ArrayQueue;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

/**
 * Quality of Service Filter. 
 * 
 * <p>This filter limits the number of active requests
 * to the number set by the "maxRequests" init parameter (default 10). If more
 * requests are received, they are suspended and placed on priority queues.
 * Priorities are determined by the {@link #getPriority(ServletRequest)} method
 * and are a value between 0 and the value given by the "maxPriority" init
 * parameter (default 10), with higher values having higher priority.
 * <p>
 * This filter is ideal to prevent wasting threads waiting for slow/limited 
 * resources such as a JDBC connection pool.  It avoids the situation where all of a 
 * containers thread pool may be consumed blocking on such a slow resource.
 * By limiting the number of active threads, a smaller thread pool may be used as 
 * the threads are not wasted waiting.  Thus more memory may be available for use by 
 * the active threads.
 * <p>
 * Furthermore, this filter uses a priority when resuming waiting requests. So that if
 * a container is under load, and there are many requests waiting for resources,
 * the {@link #getPriority(ServletRequest)} method is used, so that more important 
 * requests are serviced first.     For example, this filter could be deployed with a 
 * maxRequest limit slightly smaller than the containers thread pool and a high priority 
 * allocated to admin users.  Thus regardless of load, admin users would always be
 * able to access the web application.
 * <p>
 * The maxRequest limit is policed by a {@link Semaphore} and the filter will
 * wait a short while attempting to acquire the semaphore. This wait is
 * controlled by the "waitMs" init parameter and allows the expense of a suspend
 * to be avoided if the semaphore is shortly available. If the semaphore cannot be 
 * obtained, the request will be suspended for the default suspend period of the 
 * container or the valued set as the "suspendMs" init parameter.
 * 
 * @author gregw
 * 
 */
public class QoSFilter implements Filter
{
    final static int __DEFAULT_MAX_PRIORITY=10;
    final static int __DEFAULT_PASSES=10;
    final static int __DEFAULT_WAIT_MS=50;
    final static long __DEFAULT_TIMEOUT_MS = 30000L;
    
    final static String MAX_REQUESTS_INIT_PARAM="maxRequests";
    final static String MAX_PRIORITY_INIT_PARAM="maxPriority";
    final static String MAX_WAIT_INIT_PARAM="maxWaitMs";
    final static String SUSPEND_INIT_PARAM="suspendMs";

    ServletContext _context;
    long _waitMs;
    long _suspendMs;
    Semaphore _passes;
    Queue<Continuation>[] _queue;
    String _suspended = "QoSFilter@" + this.hashCode();
    String _continuation = "org.mortbay.jetty.ajax.Continuation";

    public void init(FilterConfig filterConfig) 
    {
        _context=filterConfig.getServletContext();
        
        int max_priority=__DEFAULT_MAX_PRIORITY;
        if (filterConfig.getInitParameter(MAX_PRIORITY_INIT_PARAM)!=null)
            max_priority=Integer.parseInt(filterConfig.getInitParameter(MAX_PRIORITY_INIT_PARAM));
        _queue=new Queue[max_priority+1];
        for (int p=0;p<_queue.length;p++)
            _queue[p]=new ArrayQueue<Continuation>();
        
        int passes=__DEFAULT_PASSES;
        if (filterConfig.getInitParameter(MAX_REQUESTS_INIT_PARAM)!=null)
            passes=Integer.parseInt(filterConfig.getInitParameter(MAX_REQUESTS_INIT_PARAM));
        _passes=new Semaphore(passes,true);
        
        long wait = __DEFAULT_WAIT_MS;
        if (filterConfig.getInitParameter(MAX_WAIT_INIT_PARAM)!=null)
            wait=Integer.parseInt(filterConfig.getInitParameter(MAX_WAIT_INIT_PARAM));
        _waitMs=wait;
        
        long suspend = __DEFAULT_TIMEOUT_MS;
        if (filterConfig.getInitParameter(SUSPEND_INIT_PARAM)!=null)
            suspend=Integer.parseInt(filterConfig.getInitParameter(SUSPEND_INIT_PARAM));
        _suspendMs=suspend;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        
        boolean accepted = false;
        try
        {
            Boolean suspended = (Boolean)request.getAttribute(_suspended);
            if (suspended == null)
            {
                accepted = _passes.tryAcquire(_waitMs,TimeUnit.MILLISECONDS);

                if (accepted)
                {
                    request.setAttribute(_suspended,Boolean.FALSE);
                }
                else
                {
                    Continuation continuation =  ContinuationSupport.getContinuation((HttpServletRequest)request,_queue);
                    int priority = getPriority(request);
                    suspended=Boolean.TRUE;
                    request.setAttribute(_suspended,suspended);
                    synchronized (_queue)
                    {
                        _queue[priority].add(continuation);
                        continuation.suspend(_suspendMs);
                        // may fall through here if waiting continuation
                    }
                }
            }
             
            if (suspended!=null && suspended.booleanValue())
            {
                request.setAttribute(_suspended,Boolean.FALSE);
                Continuation continuation =  ContinuationSupport.getContinuation((HttpServletRequest)request,_queue);
                if (continuation.isResumed())
                {
                    _passes.acquire();
                    accepted = true;
                }
                else
                {
                    // Timeout! try 1 more time.
                    accepted = _passes.tryAcquire(_waitMs,TimeUnit.MILLISECONDS);
                }
            }
            else if (!accepted)
            {
                // pass through resume of previously accepted request
                _passes.acquire();
                accepted = true;
            }

            if (accepted)
            {
                chain.doFilter(request,response);
            }
            else
                ((HttpServletResponse)response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            
        }
        catch (InterruptedException e)
        {
            _context.log("QoS",e);
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
        finally
        {
            if (accepted)
            {
                synchronized (_queue)
                {
                    for (int p = _queue.length; p-- > 0;)
                    {
                        Continuation continuation = _queue[p].poll();
                        if (continuation != null)
                        {
                            continuation.resume();
                            break;
                        }
                    }
                }
                _passes.release();
            }
        }
    }

    /**
     * Get the request Priority.
     * <p> The default implementation assigns the following priorities:<ul>
     * <li> 2 - for a authenticated request
     * <li> 1 - for a request with valid /non new session 
     * <li> 0 - for all other requests.
     * </ul>
     * This method may be specialised to provide application specific priorities.
     * 
     * @param request
     * @return a value between 0 and the maximim priority
     */
    protected int getPriority(ServletRequest request)
    {
        HttpServletRequest base_request = (HttpServletRequest)request;
        if (base_request.getUserPrincipal() != null)
            return 2;
        else
        {
            HttpSession session = base_request.getSession(false);
            if (session != null && !session.isNew())
                return 1;
            else
                return 0;
        }
    }

    public void destroy()
    {
    }

}
