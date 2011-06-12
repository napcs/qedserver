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

import org.mortbay.jetty.HandlerContainer;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.util.URIUtil;

/* ------------------------------------------------------------ */
/** Moved ContextHandler.
 * This context can be used to replace a context that has changed
 * location.  Requests are redirected (either to a fixed URL or to a
 * new context base). 
 */
public class MovedContextHandler extends ContextHandler
{
    String _newContextURL;
    boolean _discardPathInfo;
    boolean _discardQuery;
    boolean _permanent;
    Redirector _redirector;

    public MovedContextHandler()
    {
        _redirector=new Redirector();
        addHandler(_redirector);
    }
    
    public MovedContextHandler(HandlerContainer parent, String contextPath, String newContextURL)
    {
        super(parent,contextPath);
        _newContextURL=newContextURL;
        _redirector=new Redirector();
        addHandler(_redirector);
    }

    public boolean isDiscardPathInfo()
    {
        return _discardPathInfo;
    }

    public void setDiscardPathInfo(boolean discardPathInfo)
    {
        _discardPathInfo = discardPathInfo;
    }

    public String getNewContextURL()
    {
        return _newContextURL;
    }

    public void setNewContextURL(String newContextURL)
    {
        _newContextURL = newContextURL;
    }

    public boolean isPermanent()
    {
        return _permanent;
    }

    public void setPermanent(boolean permanent)
    {
        _permanent = permanent;
    }

    public boolean isDiscardQuery()
    {
        return _discardQuery;
    }

    public void setDiscardQuery(boolean discardQuery)
    {
        _discardQuery = discardQuery;
    }
    
    private class Redirector extends AbstractHandler
    {
        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
        {
            if (_newContextURL==null)
                return;
            
            Request base_request=(request instanceof Request)?(Request)request:HttpConnection.getCurrentConnection().getRequest();
            
            String url = _newContextURL;
            if (!_discardPathInfo && request.getPathInfo()!=null)
                url=URIUtil.addPaths(url, request.getPathInfo());
            if (!_discardQuery && request.getQueryString()!=null)
                url+="?"+request.getQueryString();
            
            response.sendRedirect(url);
            if (_permanent)
                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            
            base_request.setHandled(true);
        }
        
    }

}
