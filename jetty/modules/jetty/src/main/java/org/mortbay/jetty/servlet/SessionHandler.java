//========================================================================
//$Id: SessionHandler.java,v 1.5 2005/11/11 22:55:39 gregwilkins Exp $
//Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.servlet;

import java.io.IOException;
import java.util.EventListener;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.RetryRequest;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.SessionManager;
import org.mortbay.jetty.handler.HandlerWrapper;
import org.mortbay.log.Log;
import org.mortbay.util.StringUtil;

/* ------------------------------------------------------------ */
/** SessionHandler.
 * 
 * @author gregw
 *
 */
public class SessionHandler extends HandlerWrapper
{
    /* -------------------------------------------------------------- */
    private SessionManager _sessionManager;

    /* ------------------------------------------------------------ */
    /** Constructor.
     * Construct a SessionHandler witha a HashSessionManager with a standard
     * java.util.Random generator is created.
     */
    public SessionHandler()
    {   
        this(new HashSessionManager());
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param manager The session manager
     */
    public SessionHandler(SessionManager manager)
    {
        setSessionManager(manager);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the sessionManager.
     */
    public SessionManager getSessionManager()
    {
        return _sessionManager;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param sessionManager The sessionManager to set.
     */
    public void setSessionManager(SessionManager sessionManager)
    {
        if (isStarted())
            throw new IllegalStateException();
        SessionManager old_session_manager = _sessionManager;
        
        if (getServer()!=null)
            getServer().getContainer().update(this, old_session_manager, sessionManager, "sessionManager",true);
        
        if (sessionManager!=null)
            sessionManager.setSessionHandler(this);
        
        _sessionManager = sessionManager;
        
        if (old_session_manager!=null)
            old_session_manager.setSessionHandler(null);
    }


    /* ------------------------------------------------------------ */
    public void setServer(Server server)
    {
        Server old_server=getServer();
        if (old_server!=null && old_server!=server)
            old_server.getContainer().update(this, _sessionManager, null, "sessionManager",true);
        super.setServer(server);
        if (server!=null && server!=old_server)
            server.getContainer().update(this, null,_sessionManager, "sessionManager",true);
    }
    
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.thread.AbstractLifeCycle#doStart()
     */
    protected void doStart() throws Exception
    {
        _sessionManager.start();
        super.doStart();
    }
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.thread.AbstractLifeCycle#doStop()
     */
    protected void doStop() throws Exception
    {
        super.doStop();
        _sessionManager.stop();
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.Handler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, int)
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
            throws IOException, ServletException
    {
        setRequestedId(request, dispatch);
        
        Request base_request = (request instanceof Request) ? (Request)request:HttpConnection.getCurrentConnection().getRequest();
        SessionManager old_session_manager=null;
        HttpSession old_session=null;
        
        try
        {
            old_session_manager = base_request.getSessionManager();
            old_session = base_request.getSession(false);
            
            if (old_session_manager != _sessionManager)
            {
                // new session context
                base_request.setSessionManager(_sessionManager);
                base_request.setSession(null);
            }
            
            // access any existing session
            HttpSession session=null;
            if (_sessionManager!=null)
            {
                session=base_request.getSession(false);
                if (session!=null)
                {
                    if(session!=old_session)
                    {
                        Cookie cookie = _sessionManager.access(session,request.isSecure());
                        if (cookie!=null ) // Handle changed ID or max-age refresh
                            response.addCookie(cookie);
                    }
                }
                else 
                {
                    session=base_request.recoverNewSession(_sessionManager);
                    if (session!=null)
                        base_request.setSession(session);
                }
            }
            
            if(Log.isDebugEnabled())
            {
                Log.debug("sessionManager="+_sessionManager);
                Log.debug("session="+session);
            }
        
            getHandler().handle(target, request, response, dispatch);
        }
        catch (RetryRequest r)
        {
            HttpSession session=base_request.getSession(false);
            if (session!=null && session.isNew())
                base_request.saveNewSession(_sessionManager,session);
            throw r;
        }
        finally
        {
            HttpSession session=request.getSession(false);

            if (old_session_manager != _sessionManager)
            {
                //leaving context, free up the session
                if (session!=null)
                    _sessionManager.complete(session);
                
                // Leave last session in place
                if (old_session_manager != null)
                {
                    base_request.setSessionManager(old_session_manager);
                    base_request.setSession(old_session);
                }
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Look for a requested session ID in cookies and URI parameters
     * @param request
     * @param dispatch
     */
    protected void setRequestedId(HttpServletRequest request, int dispatch) 
    {
        Request base_request = (request instanceof Request) ? (Request)request:HttpConnection.getCurrentConnection().getRequest();
        String requested_session_id=request.getRequestedSessionId();
        if (dispatch!=REQUEST || requested_session_id!=null)
        {
            return;
        }
        
        final SessionManager sessionManager = getSessionManager();
        boolean requested_session_id_from_cookie=false;
        HttpSession session=null;
        // Look for session id cookie    
        if (_sessionManager.isUsingCookies())
        {
            Cookie[] cookies=request.getCookies();
            if (cookies!=null && cookies.length>0)
            {
                for (int i=0;i<cookies.length;i++)
                {
                    if (sessionManager.getSessionCookie().equalsIgnoreCase(cookies[i].getName()))
                    {
                        if (requested_session_id!=null)
                        {
                            // Multiple jsessionid cookies. Probably due to
                            // multiple paths and/or domains. Pick the first
                            // known session or the last defined cookie.
                            if (sessionManager.getHttpSession(requested_session_id)!=null)
                                break;
                        }

                        requested_session_id=cookies[i].getValue();
                        requested_session_id_from_cookie = true;
                        if(Log.isDebugEnabled())Log.debug("Got Session ID "+requested_session_id+" from cookie");
                        
                        session=sessionManager.getHttpSession(requested_session_id);
                        if (session!=null)
                            base_request.setSession(session);
                    }
                }
            }
        }
        
        if (requested_session_id==null || session==null)
        {
            String uri = request.getRequestURI();

            String prefix=sessionManager.getSessionURLPrefix();
            if (prefix!=null)
            {
                int s = uri.indexOf(prefix);
                if (s>=0)
                {   
                    s+=prefix.length();
                    int i=s;
                    while (i<uri.length())
                    {
                        char c=uri.charAt(i);
                        if (c==';'||c=='#'||c=='?'||c=='/')
                            break;
                        i++;
                    }

                    requested_session_id = uri.substring(s,i);
                    requested_session_id_from_cookie = false;
                    if(Log.isDebugEnabled())
                        Log.debug("Got Session ID "+requested_session_id+" from URL");                    
                }
            }
        }
        
        base_request.setRequestedSessionId(requested_session_id);
        base_request.setRequestedSessionIdFromCookie(requested_session_id!=null && requested_session_id_from_cookie);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param listener
     */
    public void addEventListener(EventListener listener)
    {
        if(_sessionManager!=null)
            _sessionManager.addEventListener(listener);
    }

    /* ------------------------------------------------------------ */
    public void clearEventListeners()
    {
        if(_sessionManager!=null)
            _sessionManager.clearEventListeners();
    }
}
