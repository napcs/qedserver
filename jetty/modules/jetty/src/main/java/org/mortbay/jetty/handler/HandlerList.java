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

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;

/* ------------------------------------------------------------ */
/** HandlerList.
 * This extension of {@link org.mortbay.jetty.handler.HandlerCollection} will call
 * each contained handler in turn until either an exception is thrown, the response 
 * is committed or a positive response status is set.
 */
public class HandlerList extends HandlerCollection
{
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.EventHandler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) 
        throws IOException, ServletException
    {
        Handler[] handlers = getHandlers();
        
        if (handlers!=null && isStarted())
        {
            Request base_request = HttpConnection.getCurrentConnection().getRequest();
            for (int i=0;i<handlers.length;i++)
            {
                handlers[i].handle(target,request, response, dispatch);
                if ( base_request.isHandled())
                    return;
            }
        }
    }
}
