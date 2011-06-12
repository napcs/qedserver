//========================================================================
//$Id: HttpGeneratorTest.java,v 1.1 2005/10/05 14:09:41 janb Exp $
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

package org.mortbay.jetty.security;


import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.LocalConnector;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.servlet.SessionHandler;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ConstraintTest extends TestCase
{
    Server _server = new Server();
    LocalConnector _connector = new LocalConnector();
    ContextHandler _context = new ContextHandler();
    SessionHandler _session = new SessionHandler();
    SecurityHandler _security = new SecurityHandler();
    RequestHandler _handler = new RequestHandler();
    UserRealm _realm = new TestUserRealm();
    
    public ConstraintTest(String arg0)
    {
        super(arg0);
        _server.setConnectors(new Connector[]{_connector});
        
        _context.setContextPath("/ctx");
        
        _server.setHandler(_context);
        _context.setHandler(_session);
        _session.setHandler(_security);
        _security.setHandler(_handler);
        
        Constraint constraint0 = new Constraint();
        constraint0.setAuthenticate(true);
        constraint0.setName("forbid");
        ConstraintMapping mapping0 = new ConstraintMapping();
        mapping0.setPathSpec("/forbid/*");
        mapping0.setConstraint(constraint0);
        
        Constraint constraint1 = new Constraint();
        constraint1.setAuthenticate(true);
        constraint1.setName("auth");
        constraint1.setRoles(new String[]{Constraint.ANY_ROLE});
        ConstraintMapping mapping1 = new ConstraintMapping();
        mapping1.setPathSpec("/auth/*");
        mapping1.setConstraint(constraint1);
        
        _security.setUserRealm(_realm);
        _security.setConstraintMappings(new ConstraintMapping[]
        {
                mapping0,mapping1
        });
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(ConstraintTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        _server.start();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
        _server.stop();
    }
    
    
    public void testBasic()
    	throws Exception
    {
        _security.setAuthenticator(new BasicAuthenticator());
        String response;
        
        response=_connector.getResponses("GET /ctx/noauth/info HTTP/1.0\r\n\r\n");
        assertTrue(response.startsWith("HTTP/1.1 200 OK"));
        
        _connector.reopen();
        response=_connector.getResponses("GET /ctx/forbid/info HTTP/1.0\r\n\r\n");
        assertTrue(response.startsWith("HTTP/1.1 403 Forbidden"));
        
        _connector.reopen();
        response=_connector.getResponses("GET /ctx/auth/info HTTP/1.0\r\n\r\n");
        assertTrue(response.startsWith("HTTP/1.1 401 Unauthorized"));
        assertTrue(response.indexOf("WWW-Authenticate: Basic realm=\"TestRealm\"")>0);

        _connector.reopen();
        response=_connector.getResponses("GET /ctx/auth/info HTTP/1.0\r\n"+
            "Authorization: "+B64Code.encode("user:wrong")+"\r\n"+
            "\r\n");
        assertTrue(response.startsWith("HTTP/1.1 401 Unauthorized"));
        assertTrue(response.indexOf("WWW-Authenticate: Basic realm=\"TestRealm\"")>0);
        
        _connector.reopen();
        response=_connector.getResponses("GET /ctx/auth/info HTTP/1.0\r\n"+
            "Authorization: "+B64Code.encode("user:pass")+"\r\n"+
            "\r\n");
        assertTrue(response.startsWith("HTTP/1.1 200 OK"));
        
    }

    public void testForm()
        throws Exception
    {
        FormAuthenticator authenticator = new FormAuthenticator();
        authenticator.setErrorPage("/testErrorPage");
        authenticator.setLoginPage("/testLoginPage");
        _security.setAuthenticator(authenticator);
        String response;

        _connector.reopen();
        response=_connector.getResponses("GET /ctx/noauth/info HTTP/1.0\r\n\r\n");
        assertTrue(response.startsWith("HTTP/1.1 200 OK"));
        
        _connector.reopen();
        response=_connector.getResponses("GET /ctx/forbid/info HTTP/1.0\r\n\r\n");
        assertTrue(response.startsWith("HTTP/1.1 403 Forbidden"));
        
        _connector.reopen();
        response=_connector.getResponses("GET /ctx/auth/info HTTP/1.0\r\n\r\n");
        assertTrue(response.startsWith("HTTP/1.1 302 Found"));
        assertTrue(response.indexOf("Location")>0);
        assertTrue(response.indexOf("testLoginPage")>0);
        String session=response.substring(response.indexOf("JSESSIONID=")+11,response.indexOf(";Path=/ctx"));

        _connector.reopen();
        response=_connector.getResponses("POST /ctx/j_security_check HTTP/1.0\r\n"+
            "Cookie: JSESSIONID="+session+"\r\n"+
            "Content-Type: application/x-www-form-urlencoded\r\n"+
            "Content-Length: 31\r\n"+
            "\r\n"+
            "j_username=user&j_password=wrong\r\n");
        assertTrue(response.startsWith("HTTP/1.1 302 Found"));
        assertTrue(response.indexOf("Location")>0);
        assertTrue(response.indexOf("testErrorPage")>0);
        

        _connector.reopen();
        response=_connector.getResponses("POST /ctx/j_security_check HTTP/1.0\r\n"+
            "Cookie: JSESSIONID="+session+"\r\n"+
            "Content-Type: application/x-www-form-urlencoded\r\n"+
            "Content-Length: 31\r\n"+
            "\r\n"+
            "j_username=user&j_password=pass\r\n");
        assertTrue(response.startsWith("HTTP/1.1 302 Found"));
        assertTrue(response.indexOf("Location")>0);
        assertTrue(response.indexOf("/ctx/auth/info")>0);
        
        _connector.reopen();
        response=_connector.getResponses("GET /ctx/auth/info HTTP/1.0\r\n"+
                "Cookie: JSESSIONID="+session+"\r\n"+
                "\r\n");
        assertTrue(response.startsWith("HTTP/1.1 200 OK"));
        
    }

    public void testFormNoCookie()
        throws Exception
    {
        FormAuthenticator authenticator = new FormAuthenticator();
        authenticator.setErrorPage("/testErrorPage");
        authenticator.setLoginPage("/testLoginPage");
        _security.setAuthenticator(authenticator);
        String response;

        _connector.reopen();
        response=_connector.getResponses("GET /ctx/noauth/info HTTP/1.0\r\n\r\n");
        assertTrue(response.startsWith("HTTP/1.1 200 OK"));
        
        _connector.reopen();
        response=_connector.getResponses("GET /ctx/forbid/info HTTP/1.0\r\n\r\n");
        assertTrue(response.startsWith("HTTP/1.1 403 Forbidden"));
        
        _connector.reopen();
        response=_connector.getResponses("GET /ctx/auth/info HTTP/1.0\r\n\r\n");
        assertTrue(response.startsWith("HTTP/1.1 302 Found"));
        assertTrue(response.indexOf("Location")>0);
        assertTrue(response.indexOf("testLoginPage")>0);
        int jsession=response.indexOf(";jsessionid=");
        String session = response.substring(jsession + 12, response.indexOf("\r\n",jsession));
        

        _connector.reopen();
        response=_connector.getResponses("POST /ctx/j_security_check;jsessionid="+session+" HTTP/1.0\r\n"+
            "Content-Type: application/x-www-form-urlencoded\r\n"+
            "Content-Length: 31\r\n"+
            "\r\n"+
            "j_username=user&j_password=wrong\r\n");
        assertTrue(response.startsWith("HTTP/1.1 302 Found"));
        assertTrue(response.indexOf("Location")>0);
        assertTrue(response.indexOf("testErrorPage")>0);
        

        _connector.reopen();
        response=_connector.getResponses("POST /ctx/j_security_check;jsessionid="+session+" HTTP/1.0\r\n"+
            "Content-Type: application/x-www-form-urlencoded\r\n"+
            "Content-Length: 31\r\n"+
            "\r\n"+
            "j_username=user&j_password=pass\r\n");
        assertTrue(response.startsWith("HTTP/1.1 302 Found"));
        assertTrue(response.indexOf("Location")>0);
        assertTrue(response.indexOf("/ctx/auth/info")>0);
        
        _connector.reopen();
        response=_connector.getResponses("GET /ctx/auth/info;jsessionid="+session+" HTTP/1.0\r\n"+
                "\r\n");
        assertTrue(response.startsWith("HTTP/1.1 200 OK"));
        
    }
    
    
    class RequestHandler extends AbstractHandler
    {
        
        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
        {
            ((Request)request).setHandled(true);
            response.setStatus(200);
            response.getOutputStream().println(request.getRequestURI());
        }   
    }

    
    class TestUserRealm implements UserRealm
    {
        String _username="user";
        Object _credentials="pass";
        
        public Principal authenticate(String username, Object credentials, Request request)
        {
            if (_username!=null && _username.equals(username) &&
                _credentials!=null && _credentials.equals(credentials))
                return new Principal()
            {
                public String getName()
                {
                    return _username;
                }
            };
            
            return null;
            
        }

        public void disassociate(Principal user)
        {
            // TODO Auto-generated method stub
            
        }

        public String getName()
        {
            return "TestRealm";
        }

        public Principal getPrincipal(final String username)
        {
            return new Principal()
            {
                public String getName()
                {
                    return username;
                }
            };
        }

        public boolean isUserInRole(Principal user, String role)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public void logout(Principal user)
        {
            // TODO Auto-generated method stub
            
        }

        public Principal popRole(Principal user)
        {
            // TODO Auto-generated method stub
            return null;
        }

        public Principal pushRole(Principal user, String role)
        {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean reauthenticate(Principal user)
        {
            // TODO Auto-generated method stub
            return user!=null;
        }
        
    }
}
