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

package org.mortbay.jetty.servlet;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

import junit.framework.TestCase;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.servlet.AbstractSessionManager.Session;

/**
 * @version $Revision$
 */
public class SessionManagerTest extends TestCase
{
    TestSessionIdManager idManager = new TestSessionIdManager();
    HashSessionManager sessionManager = new HashSessionManager();
    SessionHandler handler = new SessionHandler(sessionManager);
    Server server = new Server();
    
    protected void setUp() throws Exception
    {
        sessionManager.setIdManager(idManager);
        ContextHandler context=new ContextHandler();
        sessionManager.setSessionHandler(handler);
        server.setHandler(context);
        context.setHandler(handler);
        server.start();
    }

    protected void tearDown() throws Exception
    {
        server.stop();
    }

    public void testSetAttributeToNullIsTheSameAsRemoveAttribute() throws Exception
    {
        HttpSession session = sessionManager.newHttpSession(null);

        assertNull(session.getAttribute("foo"));
        assertFalse(session.getAttributeNames().hasMoreElements());
        session.setAttribute("foo", this);
        assertNotNull(session.getAttribute("foo"));
        assertTrue(session.getAttributeNames().hasMoreElements());
        session.removeAttribute("foo");
        assertNull(session.getAttribute("foo"));
        assertFalse(session.getAttributeNames().hasMoreElements());
        session.setAttribute("foo", this);
        assertNotNull(session.getAttribute("foo"));
        assertTrue(session.getAttributeNames().hasMoreElements());
        session.setAttribute("foo", null);
        assertNull(session.getAttribute("foo"));
        assertFalse(session.getAttributeNames().hasMoreElements());
    }
    
    public void testSessionPassivation() throws Exception 
    {
        HttpSession session = sessionManager.newHttpSession(null);

        TestListener listener = new TestListener();
        session.setAttribute("key", listener);
        
        sessionManager.removeSession(session, false);

        assertNull(listener.activateEvent);
        assertNotNull(listener.passivateEvent);
    }
    
    public void testSessionActivation() throws Exception
    {
        HttpSession session = sessionManager.newHttpSession(null);

        TestListener listener = new TestListener();
        session.setAttribute("key", listener);

        sessionManager.removeSession(session, false);
        listener.passivateEvent = null;
        
        sessionManager.addSession((Session) session, false);

        assertNotNull(listener.activateEvent);
        assertNull(listener.passivateEvent);
    }
    
    public void testWorker() throws Exception
    {
        try
        {
            idManager.setWorkerName("node0");
            HttpSession session = sessionManager.newHttpSession(null);
            
            assertTrue(!session.getId().endsWith(".node0"));
            String nodeId=sessionManager.getNodeId(session);
            String clusterId=session.getId();
            String id1=clusterId+".node1";
            
            assertEquals(session.getId()+".node0",nodeId);
            
            assertTrue(sessionManager.getSessionCookie(session,"/context",false)!=null);
            
            assertEquals(session,sessionManager.getHttpSession(nodeId));
            assertTrue(sessionManager.access(session,false)==null);
            
            assertEquals(session,sessionManager.getHttpSession(id1));
            Cookie cookie = sessionManager.access(session,false);
            assertTrue(cookie!=null);
            assertEquals("JSESSIONID",cookie.getName());
            assertEquals(nodeId,cookie.getValue());
            
        }
        finally
        {
            idManager.setWorkerName(null);
        }
    }
    
    
    class TestSessionIdManager extends HashSessionIdManager
    {
        
        public boolean idInUse(String id)
        {
            return false;
        }

        public void addSession(HttpSession session)
        {
            // Ignore
        }

        public void invalidateAll(String id)
        {
            // Ignore
        }

        public String newSessionId(HttpServletRequest request, long created)
        {
            return "xyzzy";
        }

        public void removeSession(HttpSession session)
        {
            // ignore
        }

    }
    
    class TestListener implements HttpSessionActivationListener
    {
        private HttpSessionEvent activateEvent;
        private HttpSessionEvent passivateEvent;
        
        public void sessionDidActivate(HttpSessionEvent event)
        {
            activateEvent = event;
        }

        public void sessionWillPassivate(HttpSessionEvent event)
        {
            passivateEvent = event;
        }
        
    }
}
