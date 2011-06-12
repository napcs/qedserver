//========================================================================
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.management;


import junit.framework.TestCase;

import org.mortbay.jetty.Server;

import com.acme.Derived;

public class ObjectMBeanTest extends TestCase
{
    public ObjectMBeanTest(String arg0)
    {
        super(arg0);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(ObjectMBeanTest.class);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public void testMbeanInfo()
    {
        Derived derived = new Derived();
        ObjectMBean mbean = new ObjectMBean(derived);
        assertTrue(mbean.getMBeanInfo()!=null); // TODO do more than just run it
    }
    
    public void testMbeanFor()
    {
        Derived derived = new Derived();
        assertTrue(ObjectMBean.mbeanFor(derived)!=null); // TODO do more than just run it
        Server server = new Server();
        assertTrue(ObjectMBean.mbeanFor(server)!=null); // TODO do more than just run it
    }
}
