// ========================================================================
// $Id: TestMailSessionReference.java 3680 2008-09-21 10:37:13Z janb $
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.naming.factories;

import java.util.Properties;

import javax.mail.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameParser;

import org.mortbay.naming.NamingUtil;

import junit.framework.TestCase;

/**
 * TestMailSessionReference
 *
 *
 */
public class TestMailSessionReference extends TestCase
{
    public void testMailSessionReference ()
    throws Exception
    {
        InitialContext icontext = new InitialContext();
        MailSessionReference sref = new MailSessionReference();
        sref.setUser("janb");
        sref.setPassword("OBF:1xmk1w261z0f1w1c1xmq");
        Properties props = new Properties ();
        props.put("mail.smtp.host", "xxx");
        props.put("mail.debug", "true");
        sref.setProperties(props);
        NamingUtil.bind(icontext, "mail/Session", sref);
        Object x = icontext.lookup("mail/Session");
        assertNotNull(x);
        assertTrue(x instanceof javax.mail.Session);
        javax.mail.Session session = (javax.mail.Session)x;
        Properties sessionProps =  session.getProperties();
        assertEquals(props, sessionProps);
        assertTrue (session.getDebug());
        
        Context foo = icontext.createSubcontext("foo");
        NameParser parser = icontext.getNameParser("");
        Name objectNameInNamespace = parser.parse(icontext.getNameInNamespace());
        objectNameInNamespace.addAll(parser.parse("mail/Session"));
        
        NamingUtil.bind(foo, "mail/Session", new LinkRef(objectNameInNamespace.toString()));
        
        Object o = foo.lookup("mail/Session");
        assertNotNull(o);
        Session fooSession = (Session)o;
        assertEquals(props, fooSession.getProperties());
        assertTrue(fooSession.getDebug());
        
        icontext.destroySubcontext("mail");
        icontext.destroySubcontext("foo");
    }
}
