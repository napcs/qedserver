// ========================================================================
// Copyright 2008 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.SessionIdManager;
import org.mortbay.jetty.SessionManager;
import org.mortbay.jetty.handler.ContextHandlerCollection;

/**
 * SessionTestServer
 * 
 * Base class for common backend to test various session plugin
 * implementations.
 * 
 * The backend runs 2 jetty servers, with 2 contexts each:
 * 
 * contextA/session    - dumps and allows create/delete of a session
 * contextA/dispatch/forward/contextB/session - forwards to contextB
 * contextB/session - dumps and allows create/delete of a session
 *
 * Subclasses should implement the configureEnvironment(), 
 * configureSessionIdManager(), configureSessionManager1(),
 * configureSessionManager2() in order to provide the session
 * management implementations to test.
 */
public abstract class SessionTestServer extends Server
{
    protected SessionIdManager _sessionIdMgr;
    protected SessionManager _sessionMgr1;
    protected SessionManager _sessionMgr2;
    protected String _workerName;

    
    /**
     * ForwardingServlet
     * Do dispatch forward to test re-use of session id (BUT NOT CONTENTS!)
     *
     */
    public class ForwardingServlet extends HttpServlet
    {
        public void doGet (HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
        {
            String pathInfo = request.getPathInfo();
      
            HttpSession session = request.getSession(false);
  
            if (pathInfo.startsWith("/forward/"))
            {
                pathInfo = pathInfo.substring(8);
                String cpath = pathInfo.substring(0, pathInfo.indexOf('/', 1));
                pathInfo = pathInfo.substring(cpath.length());
                ServletContext context = getServletConfig().getServletContext().getContext(cpath);
                RequestDispatcher dispatcher = context.getRequestDispatcher(pathInfo);          
                dispatcher.forward(request, response);
            }
        }
    }
    
 
    
    /**
     * SessionDumpServlet
     *
     * Servlet to dump the contents of the session.
     */
    public class SessionDumpServlet extends HttpServlet
    {
            int redirectCount=0;

            public void init(ServletConfig config)
                 throws ServletException
            {
                super.init(config);        
            }

            
            public void dump(HttpServletRequest request,
                    HttpServletResponse response) 
            throws ServletException, IOException
            {
                response.setContentType("text/html");

                HttpSession session = request.getSession(getURI(request).indexOf("new")>0);
                try
                {
                    if (session!=null) 
                        session.isNew();
                }
                catch(IllegalStateException e)
                {
                    session=null;
                    e.printStackTrace();
                }
                
                PrintWriter out = response.getWriter();
                out.println("<h1>Session Dump Servlet:</h1>"); 
                String submitUrl = getServletContext().getContextPath();
                submitUrl = (submitUrl.equals("")?"/":submitUrl);
                submitUrl = (submitUrl.endsWith("/")?submitUrl:submitUrl+"/");
                submitUrl += "session/";
                out.println("<form action=\""+submitUrl+"\" method=\"post\">");       
                
                if (session==null)
                {
                    out.println("<H3>No Session</H3>");
                    out.println("<input type=\"submit\" name=\"Action\" value=\"New Session\"/>");
                }
                else
                {
                    try
                    {  
                        out.println("<b>ID:</b> "+session.getId()+"<br/>");
                        out.println("<b>New:</b> "+session.isNew()+"<br/>");
                        out.println("<b>Created:</b> "+new Date(session.getCreationTime())+"<br/>");
                        out.println("<b>Last:</b> "+new Date(session.getLastAccessedTime())+"<br/>");
                        out.println("<b>Max Inactive:</b> "+session.getMaxInactiveInterval()+"<br/>");
                        out.println("<b>Context:</b> "+session.getServletContext()+"<br/>");
                        
                      
                        Enumeration keys=session.getAttributeNames();
                        while(keys.hasMoreElements())
                        {
                            String name=(String)keys.nextElement();
                            String value=""+session.getAttribute(name);

                            out.println("<b>"+name+":</b> "+value+"<br/>");
                        }

                        out.println("<b>Name:</b><input type=\"text\" name=\"Name\" /><br/>");
                        out.println("<b>Value:</b><input type=\"text\" name=\"Value\" /><br/>");

                        out.println("<input type=\"submit\" name=\"Action\" value=\"Set\"/>");
                        out.println("<input type=\"submit\" name=\"Action\" value=\"Remove\"/>");
                        out.println("<input type=\"submit\" name=\"Action\" value=\"Refresh\"/>");
                        out.println("<input type=\"submit\" name=\"Action\" value=\"Invalidate\"/><br/>");
                        
                        out.println("</form><br/>");
                        
                        if (request.isRequestedSessionIdFromCookie())
                            out.println("<P>Turn off cookies in your browser to try url encoding<BR>");
                        
                        if (request.isRequestedSessionIdFromURL())
                            out.println("<P>Turn on cookies in your browser to try cookie encoding<BR>");
                        out.println("<a href=\""+response.encodeURL(request.getRequestURI()+"?q=0")+"\">Encoded Link</a><BR>");
                        
                    }
                    catch (IllegalStateException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

           
            public String getServletInfo() {
                return "Session Dump Servlet";
            }

           
            public String getURI(HttpServletRequest request)
            {
                String uri=(String)request.getAttribute("javax.servlet.forward.request_uri");
                if (uri==null)
                    uri=request.getRequestURI();
                return uri;
            }
    }
    /**
     * SessionForwardedServlet
     *
     * Servlet that is target of a dispatch forward.
     * It will always try and make a new session, and then dump its
     * contents as html.
     */
    public class SessionForwardedServlet extends SessionDumpServlet
    {
        
        public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
        {
           handleForm(request, response);
           dump(request, response);
        }

        protected void handleForm(HttpServletRequest request,
                HttpServletResponse response) 
        {
            HttpSession session = request.getSession(false);
            String action = request.getParameter("Action");
            String name =  request.getParameter("Name");
            String value =  request.getParameter("Value");

            if (action!=null)
            {
                if(action.equals("New Session"))
                {   
                    session = request.getSession(true);
                    session.setAttribute("test","value");
                }
                else if (session!=null)
                {
                    if (action.equals("Invalidate"))
                        session.invalidate();
                    else if (action.equals("Set") && name!=null && name.length()>0)
                        session.setAttribute(name,value);
                    else if (action.equals("Remove"))
                        session.removeAttribute(name);
                }       
            }
        }
        
        
        public void doGet(HttpServletRequest request,
                              HttpServletResponse response)
        throws ServletException, IOException
        {
            request.getSession(true);
            dump(request, response);
        }
    }
    
    
    /**
     * SessionActionServlet
     *
     * Servlet to allow making a new session under user control
     * by clicking the "New Session" button (ie query params ?Action=New Session)
     */
    public class SessionActionServlet extends SessionDumpServlet
    {
        protected void handleForm(HttpServletRequest request,
                              HttpServletResponse response) 
        {
            HttpSession session = request.getSession(false);
            String action = request.getParameter("Action");
            String name =  request.getParameter("Name");
            String value =  request.getParameter("Value");

            if (action!=null)
            {
                if(action.equals("New Session"))
                {   
                    session = request.getSession(true);
                    session.setAttribute("test","value");
                }
                else if (session!=null)
                {
                    if (action.equals("Invalidate"))
                        session.invalidate();
                    else if (action.equals("Set") && name!=null && name.length()>0)
                        session.setAttribute(name,value);
                    else if (action.equals("Remove"))
                        session.removeAttribute(name);
                }       
            }
        }
      
        public void doPost(HttpServletRequest request,
                           HttpServletResponse response) 
            throws ServletException, IOException
        {
            handleForm(request,response);
            String nextUrl = getURI(request)+"?R="+redirectCount++;
            String encodedUrl=response.encodeRedirectURL(nextUrl);
            response.sendRedirect(encodedUrl);
        }
            
        public void doGet (HttpServletRequest request,
                HttpServletResponse response)
        throws ServletException, IOException
        {
            handleForm(request,response);
            dump(request, response);
        }
    }
       
    public SessionTestServer(int port, String workerName)
    {
        super(port);
        _workerName = workerName;
        configureEnvironment();
        configureIdManager();
        configureSessionManager1();
        configureSessionManager2();
        configureServer();
    }
    
    public abstract void configureEnvironment ();
   
    public abstract void configureIdManager();
    
    public abstract void configureSessionManager1();
    
    public abstract void configureSessionManager2();

    public void configureServer ()
    {
        if (_sessionIdMgr == null || _sessionMgr1 == null || _sessionMgr2 == null)
            throw new IllegalStateException ("Must set a SessionIdManager instance and 2 SessionManager instances");

        setSessionIdManager(_sessionIdMgr);
        //set up 2 contexts and a filter than can forward between them
        ContextHandlerCollection contextsA = new ContextHandlerCollection();
        setHandler(contextsA);
        setSessionIdManager(_sessionIdMgr);

        Context contextA1 = new Context(contextsA,"/contextA",Context.SESSIONS);
        contextA1.addServlet(new ServletHolder(new SessionActionServlet()), "/session/*");
        contextA1.addServlet(new ServletHolder(new ForwardingServlet()), "/dispatch/*");  
        contextA1.getSessionHandler().setSessionManager(_sessionMgr1);
        _sessionMgr1.setIdManager(_sessionIdMgr);


        Context contextA2 = new Context(contextsA, "/contextB", Context.SESSIONS);
        contextA2.addServlet(new ServletHolder(new SessionForwardedServlet()), "/session/*");
        contextA2.addServlet(new ServletHolder(new SessionActionServlet()), "/action/session/*");
        contextA2.getSessionHandler().setSessionManager(_sessionMgr2);
        _sessionMgr2.setIdManager(_sessionIdMgr);
    }

}
