//========================================================================
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.handler; 

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.RequestLog;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.Server;
import org.mortbay.log.Log;



/** 
 * RequestLogHandler.
 * This handler can be used to wrap an individual context for context logging.
 * 
 * @author Nigel Canonizado
 * @org.apache.xbean.XBean
 */
public class RequestLogHandler extends HandlerWrapper
{
    private RequestLog _requestLog;
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.Handler#handle(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, int)
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
            throws IOException, ServletException
    {
        super.handle(target, request, response, dispatch);
        if (dispatch==REQUEST && _requestLog!=null)
            _requestLog.log((Request)request, (Response)response);
    }

    /* ------------------------------------------------------------ */
    public void setRequestLog(RequestLog requestLog)
    {
        //are we changing the request log impl?
        try
        {
            if (_requestLog != null)
                _requestLog.stop();
        }
        catch (Exception e)
        {
            Log.warn (e);
        }
        
        if (getServer()!=null)
            getServer().getContainer().update(this, _requestLog, requestLog, "logimpl",true);
        
        _requestLog = requestLog;
        
        //if we're already started, then start our request log
        try
        {
            if (isStarted() && (_requestLog != null))
                _requestLog.start();
        }
        catch (Exception e)
        {
            throw new RuntimeException (e);
        }
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.handler.HandlerWrapper#setServer(org.mortbay.jetty.Server)
     */
    public void setServer(Server server)
    {
        if (_requestLog!=null)
        {
            if (getServer()!=null && getServer()!=server)
                getServer().getContainer().update(this, _requestLog, null, "logimpl",true);
            super.setServer(server);
            if (server!=null && server!=getServer())
                server.getContainer().update(this, null,_requestLog, "logimpl",true);
        }
        else
            super.setServer(server);
    }

    /* ------------------------------------------------------------ */
    public RequestLog getRequestLog() 
    {
        return _requestLog;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.handler.HandlerWrapper#doStart()
     */
    protected void doStart() throws Exception
    {
        super.doStart();
        if (_requestLog!=null)
            _requestLog.start();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.handler.HandlerWrapper#doStop()
     */
    protected void doStop() throws Exception
    {
        super.doStop();
        if (_requestLog!=null)
            _requestLog.stop();
    }

    
}
