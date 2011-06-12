//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

package com.acme;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

/** CometServlet
 * This servlet implements the Comet API from tc6.x with the exception of the read method.
 * 
 * @author gregw
 *
 */
public class CometServlet extends HttpServlet
{
    public void begin(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        request.setAttribute("org.apache.tomcat.comet",Boolean.TRUE);
    }

    public void end(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        synchronized(request)
        {
            request.removeAttribute("org.apache.tomcat.comet");
            
            Continuation continuation=ContinuationSupport.getContinuation(request,request);
            if (continuation.isPending())
                continuation.resume();
        }
    }

    public void error(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        end(request,response);
    }

    public boolean read(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        throw new UnsupportedOperationException();
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        synchronized(request)
        {
            // TODO: wrap response so we can reset timeout on writes.
            
            Continuation continuation=ContinuationSupport.getContinuation(request,request);
            
            if (!continuation.isPending())
                begin(request,response);
            
            Integer timeout=(Integer)request.getAttribute("org.apache.tomcat.comet.timeout");
            boolean resumed=continuation.suspend(timeout==null?60000:timeout.intValue());
            
            if (!resumed)
                error(request,response);
        }
    }

    public void setTimeout(HttpServletRequest request, HttpServletResponse response, int timeout) throws IOException, ServletException,
            UnsupportedOperationException
    {
        request.setAttribute("org.apache.tomcat.comet.timeout",new Integer(timeout));
    }
}
