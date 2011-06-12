// ========================================================================
// $Id: TestNamingEntries.java 3957 2008-10-30 04:26:05Z janb $
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

package org.mortbay.jetty.plus.naming;


import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

import junit.framework.TestCase;

/**
 * TestEnvEntry
 *
 *
 */
public class TestNamingEntries extends TestCase
{
    public class ScopeA extends Object
    {
        public String toString()
        {
            return this.getClass().getName()+"@"+super.hashCode();
        }
    }
    
    public class ScopeB extends ScopeA
    {}
   
    
    
    public static class SomeObject
    {
        private int value;
        public SomeObject (int value)
        {this.value = value;}
        
        public int getValue ()
        {
            return this.value;
        }
    }
    
    public static class SomeObjectFactory implements ObjectFactory
    {

        public SomeObjectFactory()
        {
            
        }
        /** 
         * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object, javax.naming.Name, javax.naming.Context, java.util.Hashtable)
         * @param arg0
         * @param arg1
         * @param arg2
         * @param arg3
         * @return
         * @throws Exception
         */
        public Object getObjectInstance(Object arg0, Name arg1, Context arg2, Hashtable arg3) throws Exception
        {
            Reference ref = (Reference)arg0;
            
            RefAddr refAddr = ref.get(0);
            String valueName = refAddr.getType();
            if (!valueName.equalsIgnoreCase("val"))
                throw new RuntimeException("Unrecognized refaddr type = "+valueName);
           
            String value = (String)refAddr.getContent();
            
            return new SomeObject(Integer.parseInt(value.trim()));
          
        }
        
    }
    
    public static class SomeOtherObject extends SomeObject implements Referenceable
    {

        private String svalue;
        public SomeOtherObject (String value)
        {
            super(Integer.parseInt(value.trim()));
           
        }
        
        /** 
         * @see javax.naming.Referenceable#getReference()
         * @return
         * @throws NamingException
         */
        public Reference getReference() throws NamingException
        {
            RefAddr refAddr = new StringRefAddr("val", String.valueOf(getValue()));
            Reference ref = new Reference(SomeOtherObject.class.getName(), refAddr, SomeOtherObjectFactory.class.getName(), null);
            return ref;
        }
    }
    
    public static class SomeOtherObjectFactory implements ObjectFactory
    {

        public SomeOtherObjectFactory()
        {
            
        }
        /** 
         * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object, javax.naming.Name, javax.naming.Context, java.util.Hashtable)
         * @param arg0
         * @param arg1
         * @param arg2
         * @param arg3
         * @return
         * @throws Exception
         */
        public Object getObjectInstance(Object arg0, Name arg1, Context arg2, Hashtable arg3) throws Exception
        {
          Reference ref = (Reference)arg0;
            
            RefAddr refAddr = ref.get(0);
            String valueName = refAddr.getType();
            if (!valueName.equalsIgnoreCase("val"))
                throw new RuntimeException("Unrecognized refaddr type = "+valueName);
           
            String value = (String)refAddr.getContent();
            
            return new SomeOtherObject(value.trim());
        }
        
    }

    
    public SomeObject someObject;
    
    public void setUp ()
    {
        this.someObject = new SomeObject(4);
    }

    public void testEnvEntryNoScope ()
    throws Exception
    {
        EnvEntry ee = new EnvEntry("nameZ", "zstring", true);
        List list = NamingEntryUtil.lookupNamingEntries(null, EnvEntry.class);
        assertNotNull(list);
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        Object o = list.get(0);
        assertTrue (o instanceof EnvEntry);
        EnvEntry eo = (EnvEntry)o;
        assertEquals ("nameZ", eo.getJndiName());
    }
 
    public void testEnvEntryOverride ()
    throws Exception
    {        
        ScopeA scope = new ScopeA();
        EnvEntry ee = new EnvEntry (scope, "nameA", someObject, true);

        NamingEntry ne = NamingEntryUtil.lookupNamingEntry(scope, "nameA");
        assertNotNull(ne);
        assertTrue(ne instanceof EnvEntry);
        assertTrue (((EnvEntry)ne).isOverrideWebXml());

        Context scopeContext = NamingEntryUtil.getContextForScope(scope);
        assertNotNull(scopeContext);
        Context namingEntriesContext = NamingEntryUtil.getContextForNamingEntries(scope);
        assertNotNull(namingEntriesContext);
        assertEquals(someObject, scopeContext.lookup("nameA"));
    }
   

    public void testEnvEntryNonOverride ()
    throws Exception
    {
        ScopeA scope = new ScopeA();
        EnvEntry ee = new EnvEntry (scope, "nameA", someObject, false);

        NamingEntry ne = NamingEntryUtil.lookupNamingEntry(scope, "nameA");
        assertNotNull(ne);
        assertTrue(ne instanceof EnvEntry);
        assertFalse (((EnvEntry)ne).isOverrideWebXml());

        Context scopeContext = NamingEntryUtil.getContextForScope(scope);
        assertNotNull(scopeContext);
        Context namingEntriesContext = NamingEntryUtil.getContextForNamingEntries(scope);
        assertNotNull(namingEntriesContext);
        assertEquals(someObject, scopeContext.lookup("nameA"));
    }

    
    public void testResource ()
    throws Exception
    {
        InitialContext icontext = new InitialContext();
 
        Resource resource = new Resource (null, "resourceA/b/c", someObject);
        NamingEntry ne = NamingEntryUtil.lookupNamingEntry(null, "resourceA/b/c");
        assertNotNull(ne);
        assertTrue(ne instanceof Resource);
        assertEquals(icontext.lookup("resourceA/b/c"), someObject);
        
        Object scope = new ScopeA();
        Resource resource2 = new Resource (scope, "resourceB", someObject);
        ne = NamingEntryUtil.lookupNamingEntry(scope, "resourceB");
        assertNotNull(ne);
        assertTrue(ne instanceof Resource);
        
        ne = NamingEntryUtil.lookupNamingEntry(null, "resourceB");
        assertNull(ne);
        
        ne = NamingEntryUtil.lookupNamingEntry(new ScopeB(), "resourceB");
        assertNull(ne);
    }
   
   
    public void testLink ()
    throws Exception
    {
        ScopeA scope = new ScopeA();
        InitialContext icontext = new InitialContext();
        Link link = new Link ("resourceA", "resourceB");
        NamingEntry ne = NamingEntryUtil.lookupNamingEntry(null, "resourceA");
        assertNotNull(ne);
        assertTrue(ne instanceof Link);
        assertEquals(icontext.lookup("resourceA"), "resourceB");

        link = new Link (scope, "jdbc/resourceX", "jdbc/resourceY");
        ne = NamingEntryUtil.lookupNamingEntry(scope, "jdbc/resourceX");        
        assertNotNull(ne);
        assertTrue(ne instanceof Link);
    }
    
    
    
    public void testResourceReferenceable ()
    throws Exception
    {
        SomeOtherObject someOtherObj = new SomeOtherObject("100");
        InitialContext icontext = new InitialContext();
        Resource res = new Resource("resourceByReferenceable", someOtherObj);
        Object o = icontext.lookup("resourceByReferenceable");
        assertNotNull(o);
        assertTrue (o instanceof SomeOtherObject);
        assertEquals(((SomeOtherObject)o).getValue(), 100);
        
    }
    
    public void testResourceReference ()
    throws Exception
    {
        RefAddr refAddr = new StringRefAddr("val", "10");
        Reference ref = new Reference(SomeObject.class.getName(), refAddr, SomeObjectFactory.class.getName(), null);
        
        InitialContext icontext = new InitialContext();
        Resource resource = new Resource (null, "resourceByRef", ref);
        NamingEntry ne = NamingEntryUtil.lookupNamingEntry(null, "resourceByRef");
        assertNotNull(ne);
        assertTrue (ne instanceof Resource);
        
        Object o = icontext.lookup("resourceByRef");
        assertNotNull (o);
        assertTrue (o instanceof SomeObject);
        
        assertEquals(((SomeObject)o).getValue(), 10);
    }
}
