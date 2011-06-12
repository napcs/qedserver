// ========================================================================
// Copyright 2006 Mort Bay Consulting Pty. Ltd.
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

import java.io.InputStream;
import java.net.URL;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.SessionIdManager;
import org.mortbay.jetty.SessionManager;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.thread.BoundedThreadPool;
import org.mortbay.util.IO;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * @version $Revision$
 */
public class SessionTest extends TestCase
{
    Server server;
    WebAppContext test0;
    WebAppContext test1;
    SessionManager session0;
    SessionManager session1;
    SessionIdManager ids;
    String url;

    protected void setUp() throws Exception
    {
        server = new Server(0);
             
        HandlerCollection handlers = new HandlerCollection();
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        handlers.setHandlers(new Handler[]{contexts,new DefaultHandler(),requestLogHandler});
        server.setHandler(handlers);
        
        test0 = new WebAppContext(contexts,"../../webapps/test","/test0");
        test1 = new WebAppContext(contexts,"../../webapps/test","/test1");

        HashUserRealm userRealm = new HashUserRealm();
        userRealm.setName("Test Realm");
        userRealm.setConfig("../../etc/realm.properties");
        server.setUserRealms(new UserRealm[]{userRealm});
        
        server.start();
        
        url="http://127.0.0.1:"+server.getConnectors()[0].getLocalPort();
        
        session0=test0.getSessionHandler().getSessionManager();
        session1=test1.getSessionHandler().getSessionManager();
        ids=test0.getSessionHandler().getSessionManager().getMetaManager();
        assertEquals(ids,test1.getSessionHandler().getSessionManager().getMetaManager());
    }
    
    protected void tearDown() throws Exception
    {
        server.stop();
    }
    
    public void testSession() throws Exception
    {
        // no sessions to start with.
        testContains("/test0/session","No Session");
        testContains("/test1/session","No Session");
        
        // create context in context 0
        String id0=getID("/test0/session?Action=New+Session");
        assertTrue(id0!=null);
        assertEquals(id0,getID("/test0/session;jsessionid="+id0));
        testContains("/test1/session;jsessionid="+id0,"No Session");
        
        // test setting value
        testContains("/test0/session;jsessionid="+id0+"?Action=Set&Name=name0&Value=value0","<b>name0:</b> value0<br/>");
        testContains("/test0/session;jsessionid="+id0,"<b>name0:</b> value0<br/>");
        testContains("/test1/session;jsessionid="+id0,"No Session");
        
        // Direct request to context 1 without session ID
        String id1=getID("/test1/session;jsessionid=unknown?Action=New+Session");
        assertTrue(id1!=null);
        assertFalse(id0.equals(id1));
        assertEquals(id0,getID("/test0/session;jsessionid="+id0));
        assertEquals(id1,getID("/test1/session;jsessionid="+id1));
        testContains("/test0/session;jsessionid="+id1,"No Session");
        testContains("/test1/session;jsessionid="+id0,"No Session");
        
        // test setting value
        testContains("/test1/session;jsessionid="+id1+"?Action=Set&Name=name1&Value=value1","<b>name1:</b> value1<br/>");
        testContains("/test0/session;jsessionid="+id0,"<b>name0:</b> value0<br/>");
        testContains("/test1/session;jsessionid="+id1,"<b>name1:</b> value1<br/>");
        testNotContains("/test0/session;jsessionid="+id0,"<b>name1:</b> value1<br/>");
        testNotContains("/test1/session;jsessionid="+id1,"<b>name0:</b> value0<br/>");
        
        // Invalidate context 1
        testContains("/test1/session;jsessionid="+id1+"?Action=Invalidate","No Session");

        // Direct request to context 1 with session ID
        id1=getID("/test1/session;jsessionid="+id0+"?Action=New+Session");
        assertTrue(id1!=null);
        assertTrue(id0.equals(id1));
        assertEquals(id0,getID("/test0/session;jsessionid="+id0));
        assertEquals(id1,getID("/test1/session;jsessionid="+id1));

        // test setting value
        testContains("/test1/session;jsessionid="+id1+"?Action=Set&Name=name1&Value=value1","<b>name1:</b> value1<br/>");
        testContains("/test0/session;jsessionid="+id0,"<b>name0:</b> value0<br/>");
        testContains("/test1/session;jsessionid="+id1,"<b>name1:</b> value1<br/>");
        testNotContains("/test0/session;jsessionid="+id0,"<b>name1:</b> value1<br/>");
        testNotContains("/test1/session;jsessionid="+id1,"<b>name0:</b> value0<br/>");
        
        // test dispatch get
        assertEquals(id0,getID("/test0/dispatch/forwardC/test1/session;jsessionid="+id0));
        testContains("/test0/dispatch/forwardC/test1/session;jsessionid="+id0,"<b>name1:</b> value1<br/>");
        
        // invalidate all via dispatch
        testContains("/test0/dispatch/forwardC/test1/session;jsessionid="+id1+"?Action=Invalidate","No Session");
        testContains("/test0/session","No Session");
        testContains("/test1/session","No Session");
        
        // new sessions via dispatch
        id0=getID("/test0/session?Action=New+Session");
        assertFalse(id0.equals(id1));
        id1=getID("/test0/dispatch/forwardC/test1/session;jsessionid="+id0+"?Action=New+Session");
        assertTrue(id1!=null);
        assertTrue(id0.equals(id1));

        // test values again
        testContains("/test0/session;jsessionid="+id0+"?Action=Set&Name=name0&Value=value0","<b>name0:</b> value0<br/>");
        testContains("/test1/session;jsessionid="+id1+"?Action=Set&Name=name1&Value=value1","<b>name1:</b> value1<br/>");
        testContains("/test0/session;jsessionid="+id0,"<b>name0:</b> value0<br/>");
        testContains("/test1/session;jsessionid="+id1,"<b>name1:</b> value1<br/>");
        testNotContains("/test0/session;jsessionid="+id0,"<b>name1:</b> value1<br/>");
        testNotContains("/test1/session;jsessionid="+id1,"<b>name0:</b> value0<br/>");
        testContains("/test0/dispatch/forwardC/test1/session;jsessionid="+id0,"<b>name1:</b> value1<br/>");
        testContains("/test1/dispatch/forwardC/test0/session;jsessionid="+id0,"<b>name0:</b> value0<br/>");

        // direct invalidate
        testContains("/test1/session;jsessionid="+id1+"?Action=Invalidate","No Session");
        testContains("/test0/session","No Session");
        testContains("/test1/session","No Session");
        
    }
    
    protected void testContains(String uri, String string)
        throws Exception
    {
        String result=IO.toString((InputStream)new URL(url+uri).getContent()); 
        // System.err.println(uri+" ==> "+result); 
        try
        {
            assertTrue(result!=null && result.indexOf(string)>=0);
        }
        catch(AssertionFailedError e)
        {
            System.err.println("'"+string+"' not in '"+result+"'"); 
            throw e;
        }
    }

    protected void testNotContains(String uri, String string)
    throws Exception
    {
        String result=IO.toString((InputStream)new URL(url+uri).getContent());
        assertTrue(result!=null && result.indexOf(string)<0);
    }

    protected String getID(String uri)
        throws Exception
    {
        String result=IO.toString((InputStream)new URL(url+uri).getContent());
        assertTrue(result!=null && result.indexOf("ID:</b> ")>=0);
        int i0 = result.indexOf("ID:</b> ");
        int i1 = result.indexOf("<br/>",i0);
        String id = result.substring(i0+8,i1); 
        return id;
    }
    
}
