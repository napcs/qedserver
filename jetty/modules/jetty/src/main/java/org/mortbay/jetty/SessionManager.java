// ========================================================================
// Copyright 1996-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty;

import java.util.EventListener;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.mortbay.component.LifeCycle;
import org.mortbay.jetty.servlet.SessionHandler;


    
/* --------------------------------------------------------------------- */
/** Session Manager.
 * The API required to manage sessions for a servlet context.
 *
 * @author Greg Wilkins
 */
public interface SessionManager extends LifeCycle
{
    
    /* ------------------------------------------------------------ */
    /** Session cookie name.
     * Defaults to JSESSIONID, but can be set with the
     * org.mortbay.jetty.servlet.SessionCookie context init parameter.
     */
    public final static String __SessionCookieProperty = "org.mortbay.jetty.servlet.SessionCookie";
    public final static String __DefaultSessionCookie = "JSESSIONID";   
    
    
    /* ------------------------------------------------------------ */
    /** Session URL parameter name.
     * Defaults to jsessionid, but can be set with the
     * org.mortbay.jetty.servlet.SessionURL context init parameter.  If set to null or 
     * "none" no URL rewriting will be done.
     */
    public final static String __SessionURLProperty = "org.mortbay.jetty.servlet.SessionURL";
    public final static String __DefaultSessionURL = "jsessionid";
    
  

    /* ------------------------------------------------------------ */
    /** Session Domain.
     * If this property is set as a ServletContext InitParam, then it is
     * used as the domain for session cookies. If it is not set, then
     * no domain is specified for the session cookie.
     */
    public final static String __SessionDomainProperty="org.mortbay.jetty.servlet.SessionDomain";
    public final static String __DefaultSessionDomain = null;
    
    
    /* ------------------------------------------------------------ */
    /** Session Path.
     * If this property is set as a ServletContext InitParam, then it is
     * used as the path for the session cookie.  If it is not set, then
     * the context path is used as the path for the cookie.
     */
    public final static String __SessionPathProperty="org.mortbay.jetty.servlet.SessionPath";
    
    /* ------------------------------------------------------------ */
    /** Session Max Age.
     * If this property is set as a ServletContext InitParam, then it is
     * used as the max age for the session cookie.  If it is not set, then
     * a max age of -1 is used.
     */
    public final static String __MaxAgeProperty="org.mortbay.jetty.servlet.MaxAge";
    
    /* ------------------------------------------------------------ */
    public HttpSession getHttpSession(String id);
    
    /* ------------------------------------------------------------ */
    public HttpSession newHttpSession(HttpServletRequest request);

    /* ------------------------------------------------------------ */
    /** @return true if session cookies should be secure
     */
    public boolean getSecureCookies();

    /* ------------------------------------------------------------ */
    /** @return true if session cookies should be httponly (microsoft extension)
     */
    public boolean getHttpOnly();

    /* ------------------------------------------------------------ */
    public int getMaxInactiveInterval();

    /* ------------------------------------------------------------ */
    public void setMaxInactiveInterval(int seconds);
    
    /* ------------------------------------------------------------ */
    public void setSessionHandler(SessionHandler handler);

    /* ------------------------------------------------------------ */
    /** Add an event listener.
     * @param listener An Event Listener. Individual SessionManagers
     * implemetations may accept arbitrary listener types, but they
     * are expected to at least handle
     *   HttpSessionActivationListener,
     *   HttpSessionAttributeListener,
     *   HttpSessionBindingListener,
     *   HttpSessionListener
     */
    public void addEventListener(EventListener listener);
    
    /* ------------------------------------------------------------ */
    public void removeEventListener(EventListener listener);
    
    /* ------------------------------------------------------------ */
    public void clearEventListeners();

    /* ------------------------------------------------------------ */
    /** Get a Cookie for a session.
     * @param session The session to which the cookie should refer.
     * @param contextPath The context to which the cookie should be linked. The client will only send the cookie value
     *    when requesting resources under this path.
     * @param requestIsSecure Whether the client is accessing the server over a secure protocol (i.e. HTTPS). 
     * @return If this <code>SessionManager</code> uses cookies, then this method will return a new 
     *   {@link Cookie cookie object} that should be set on the client in order to link future HTTP requests
     *   with the <code>session</code>. If cookies are not in use, this method returns <code>null</code>. 
     */
    public Cookie getSessionCookie(HttpSession session,String contextPath, boolean requestIsSecure);
    
    /* ------------------------------------------------------------ */
    /**
     * @return the cross context session meta manager.
     */
    public SessionIdManager getIdManager();
    
    /* ------------------------------------------------------------ */
    /**
     * @return the cross context session id manager.
     * @deprecated use {@link #getIdManager()}
     */
    public SessionIdManager getMetaManager();
    
    /* ------------------------------------------------------------ */
    /**
     * @param meta the cross context session meta manager.
     */
    public void setIdManager(SessionIdManager meta);
    
    /* ------------------------------------------------------------ */
    public boolean isValid(HttpSession session);
    
    /* ------------------------------------------------------------ */
    /** Get the session node id
     * @param session
     * @return The unique id of the session within the cluster, extended with an optional node id.
     */
    public String getNodeId(HttpSession session);
    
    /* ------------------------------------------------------------ */
    /** Get the session cluster id
     * @param session
     * @return The unique id of the session within the cluster (without a node id extension)
     */
    public String getClusterId(HttpSession session);
    
    /* ------------------------------------------------------------ */
    /** Called by the {@link SessionHandler} when a session is access by a request
     * @return Cookie If non null, this cookie should be set on the response to either migrate 
     * the session or to refresh a cookie that may expire.
     */
    public Cookie access(HttpSession session,boolean secure);
    
    /* ------------------------------------------------------------ */
    /** Called by the {@link SessionHandler} when a reqeuest is not longer 
     * handling a session.  Not this includes new sessions, so there may not
     * be a matching call to {@link #access(HttpSession)}.
     * 
     */
    public void complete(HttpSession session);

    
    public void setSessionCookie (String cookieName);
    
    public String getSessionCookie ();
    
    public void setSessionURL (String url);
    
    public String getSessionURL ();
    
    public String getSessionURLPrefix();
    
    public void setSessionDomain (String domain);
    
    public String getSessionDomain ();
    
    public void setSessionPath (String path);
    
    public String getSessionPath ();
    
    public void setMaxCookieAge (int maxCookieAgeInSeconds);
    
    public int getMaxCookieAge();
    
    public boolean isUsingCookies();
    
}
