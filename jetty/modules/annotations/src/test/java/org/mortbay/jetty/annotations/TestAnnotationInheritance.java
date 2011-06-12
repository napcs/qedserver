//========================================================================
//$Id: TestAnnotationInheritance.java 3680 2008-09-21 10:37:13Z janb $
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

package org.mortbay.jetty.annotations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.annotations.resources.ResourceA;
import org.mortbay.jetty.annotations.resources.ResourceB;
import org.mortbay.jetty.plus.annotation.InjectionCollection;
import org.mortbay.jetty.plus.annotation.LifeCycleCallbackCollection;
import org.mortbay.jetty.plus.annotation.RunAsCollection;
import org.mortbay.jetty.plus.naming.NamingEntry;
import org.mortbay.jetty.webapp.WebAppContext;


import junit.framework.TestCase;

/**
 * TestAnnotationInheritance
 *
 *
 */
public class TestAnnotationInheritance extends TestCase
{
    
    public void testInheritance ()
    throws Exception
    {          
        AnnotationParser processor = new AnnotationParser();
        AnnotationCollection collection = processor.processClass(ClassB.class);
        
        List classes = collection.getClasses();
        assertEquals(2, classes.size());
        
        //check methods
        List methods = collection.getMethods();
        assertTrue(methods!=null);
        assertFalse(methods.isEmpty());
        assertEquals(methods.size(), 4);
        Method m = ClassB.class.getDeclaredMethod("a", new Class[] {});       
        assertTrue(methods.indexOf(m) >= 0);
        Sample s = (Sample)m.getAnnotation(Sample.class);
        assertEquals(51, s.value());
        m = ClassA.class.getDeclaredMethod("a", new Class[] {});
        assertTrue(methods.indexOf(m) < 0); //check overridden public scope superclass method not in there
        
        m = ClassA.class.getDeclaredMethod("b", new Class[] {});
        assertTrue(methods.indexOf(m) >= 0);      
        
        m = ClassB.class.getDeclaredMethod("c", new Class[] {});
        assertTrue(methods.indexOf(m) >= 0);
        m = ClassA.class.getDeclaredMethod("c", new Class[] {});
        assertTrue(methods.indexOf(m) < 0); //check overridden superclass package scope method not in there
        
        m = ClassA.class.getDeclaredMethod("d", new Class[] {});
        assertTrue(methods.indexOf(m) >= 0);
        
        //check fields
        List fields = collection.getFields();
        assertFalse(fields.isEmpty());
        assertEquals(1, fields.size());
        
        Field f = ClassA.class.getDeclaredField("m");
        assertTrue(fields.indexOf(f) >= 0);
    }
    
    
    public void testResourceAnnotations ()
    throws Exception
    {
        Server server = new Server();
        WebAppContext wac = new WebAppContext();
        wac.setServer(server);
        
        InitialContext ic = new InitialContext();
        Context comp = (Context)ic.lookup("java:comp");
        Context env = comp.createSubcontext("env");
        
        org.mortbay.jetty.plus.naming.EnvEntry resourceA = new org.mortbay.jetty.plus.naming.EnvEntry(server, "resA", new Integer(1000), false);
        org.mortbay.jetty.plus.naming.EnvEntry resourceB = new org.mortbay.jetty.plus.naming.EnvEntry(server, "resB", new Integer(2000), false);
        
        
        AnnotationParser processor = new AnnotationParser();
        processor.processClass(ResourceA.class);
        InjectionCollection injections = new InjectionCollection();
        LifeCycleCallbackCollection callbacks = new LifeCycleCallbackCollection();
        RunAsCollection runAses = new RunAsCollection();
       
        AnnotationCollection collection = processor.processClass(ResourceB.class); 
        assertEquals(1, collection.getClasses().size());
        assertEquals(3, collection.getMethods().size());
        assertEquals(6, collection.getFields().size());
        
        //process with all the specific annotations turned into injections, callbacks etc
        processor.parseAnnotations(wac, ResourceB.class, runAses, injections, callbacks);
        
        //processing classA should give us these jndi name bindings:
        // java:comp/env/myf
        // java:comp/env/org.mortbay.jetty.annotations.resources.ResourceA/g
        // java:comp/env/mye
        // java:comp/env/org.mortbay.jetty.annotations.resources.ResourceA/h
        // java:comp/env/resA
        // java:comp/env/org.mortbay.jetty.annotations.resources.ResourceB/f
        // java:comp/env/org.mortbay.jetty.annotations.resources.ResourceA/n
        // 
        assertEquals(resourceB.getObjectToBind(), env.lookup("myf"));
        assertEquals(resourceA.getObjectToBind(), env.lookup("mye"));
        assertEquals(resourceA.getObjectToBind(), env.lookup("resA"));
        assertEquals(resourceA.getObjectToBind(), env.lookup("org.mortbay.jetty.annotations.resources.ResourceA/g")); 
        assertEquals(resourceA.getObjectToBind(), env.lookup("org.mortbay.jetty.annotations.resources.ResourceA/h"));
        assertEquals(resourceB.getObjectToBind(), env.lookup("org.mortbay.jetty.annotations.resources.ResourceB/f"));
        assertEquals(resourceB.getObjectToBind(), env.lookup("org.mortbay.jetty.annotations.resources.ResourceA/n"));
        
        //we should have these Injections
        assertNotNull(injections);
        
        List fieldInjections = injections.getFieldInjections(ResourceB.class);
        assertNotNull(fieldInjections);
        
        Iterator itor = fieldInjections.iterator();
        System.err.println("Field injections:");
        while (itor.hasNext())
        {
            System.err.println(itor.next());
        }
        assertEquals(5, fieldInjections.size());
        
        List methodInjections = injections.getMethodInjections(ResourceB.class);
        itor = methodInjections.iterator();
        System.err.println("Method injections:");
        while (itor.hasNext())
            System.err.println(itor.next());
        assertNotNull(methodInjections);
        assertEquals(3, methodInjections.size());
    }

}
