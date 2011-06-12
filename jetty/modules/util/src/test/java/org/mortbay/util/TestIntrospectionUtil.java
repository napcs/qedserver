//========================================================================
//$Id: TestIntrospectionUtil.java 5767 2010-01-05 05:45:26Z gregw $
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

package org.mortbay.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import junit.framework.TestCase;

/**
 * TestInjection
 *
 *
 */
public class TestIntrospectionUtil extends TestCase
{
    public final Class[] __INTEGER_ARG = new Class[] {Integer.class};
    Field privateAField;
    Field protectedAField;
    Field publicAField;
    Field defaultAField;
    Field privateBField;
    Field protectedBField;
    Field publicBField;
    Field defaultBField;
    Method privateCMethod;
    Method protectedCMethod;
    Method publicCMethod;
    Method defaultCMethod;
    Method privateDMethod;
    Method protectedDMethod;
    Method publicDMethod;
    Method defaultDMethod;
    
    public class ServletA 
    {
        private Integer privateA; 
        protected Integer protectedA;
        Integer defaultA;
        public Integer publicA;
    }
    
    public class ServletB extends ServletA
    {
        private String privateB;
        protected String protectedB;
        public String publicB;
        String defaultB;
    }
    
    public class ServletC
    {
        private void setPrivateC (Integer c) {}      
        protected void setProtectedC (Integer c) {}
        public void setPublicC(Integer c) {}
        void setDefaultC(Integer c) {}
    }
    
    public class ServletD extends ServletC
    {
        private void setPrivateD(Integer d) {}
        protected void setProtectedD(Integer d) {}
        public void setPublicD(Integer d) {}
        void setDefaultD(Integer d) {}
    }
    
    public void setUp()
    throws Exception
    {
        privateAField = ServletA.class.getDeclaredField("privateA");
        protectedAField = ServletA.class.getDeclaredField("protectedA");
        publicAField = ServletA.class.getDeclaredField("publicA");
        defaultAField = ServletA.class.getDeclaredField("defaultA");
        privateBField = ServletB.class.getDeclaredField("privateB");
        protectedBField = ServletB.class.getDeclaredField("protectedB");
        publicBField = ServletB.class.getDeclaredField("publicB");
        defaultBField = ServletB.class.getDeclaredField("defaultB");
        privateCMethod = ServletC.class.getDeclaredMethod("setPrivateC", __INTEGER_ARG);
        protectedCMethod = ServletC.class.getDeclaredMethod("setProtectedC", __INTEGER_ARG);
        publicCMethod = ServletC.class.getDeclaredMethod("setPublicC", __INTEGER_ARG);
        defaultCMethod = ServletC.class.getDeclaredMethod("setDefaultC", __INTEGER_ARG);
        privateDMethod = ServletD.class.getDeclaredMethod("setPrivateD", __INTEGER_ARG); 
        protectedDMethod = ServletD.class.getDeclaredMethod("setProtectedD", __INTEGER_ARG);
        publicDMethod = ServletD.class.getDeclaredMethod("setPublicD", __INTEGER_ARG);
        defaultDMethod = ServletD.class.getDeclaredMethod("setDefaultD", __INTEGER_ARG);
    }
   
    
    public void testFieldPrivate ()
    throws Exception
    {
        //direct
        Field f = IntrospectionUtil.findField(ServletA.class, "privateA", Integer.class, true, false);
        assertEquals(privateAField,f);

        //inheritance
        try
        {
            IntrospectionUtil.findField(ServletB.class, "privateA", Integer.class, true, false);
            fail("Private fields should not be inherited");
        }
        catch (NoSuchFieldException e)
        {
            //expected
        }
    }
    
    public void testFieldProtected()    
    throws Exception
    {
        //direct
        Field f = IntrospectionUtil.findField(ServletA.class, "protectedA", Integer.class, true, false);
        assertEquals(f, protectedAField);
        
        //inheritance
        f = IntrospectionUtil.findField(ServletB.class, "protectedA", Integer.class, true, false);
        assertEquals(f, protectedAField);
    }
    
    public void testFieldPublic()
    throws Exception
    {
        //direct
        Field f = IntrospectionUtil.findField(ServletA.class, "publicA", Integer.class, true, false);
        assertEquals(f, publicAField);
        
        //inheritance
        f = IntrospectionUtil.findField(ServletB.class, "publicA", Integer.class, true, false);
        assertEquals(f, publicAField);
    }
    
    public void testFieldDefault()
    throws Exception
    {
        //direct
        Field f = IntrospectionUtil.findField(ServletA.class, "defaultA", Integer.class, true, false);
        assertEquals(f, defaultAField);
        
        //inheritance
        f = IntrospectionUtil.findField(ServletB.class, "defaultA", Integer.class, true, false);
        assertEquals(f, defaultAField);
    }
    
    public void testMethodPrivate ()
    throws Exception
    {
        //direct
        Method m = IntrospectionUtil.findMethod(ServletC.class, "setPrivateC", __INTEGER_ARG, true, false);
        assertEquals(m, privateCMethod);
        
        //inheritance
        try
        {
            IntrospectionUtil.findMethod(ServletD.class, "setPrivateC", __INTEGER_ARG, true, false);
            fail();
        }
        catch (NoSuchMethodException e)
        {
            //expected
        }
    }
    
    public void testMethodProtected ()
    throws Exception
    {
        // direct
        Method m = IntrospectionUtil.findMethod(ServletC.class, "setProtectedC", __INTEGER_ARG, true, false);
        assertEquals(m, protectedCMethod);
        
        //inherited
        m = IntrospectionUtil.findMethod(ServletD.class, "setProtectedC", __INTEGER_ARG, true, false);
        assertEquals(m, protectedCMethod);
    }
    
    public void testMethodPublic ()
    throws Exception
    {
        // direct
        Method m = IntrospectionUtil.findMethod(ServletC.class, "setPublicC",  __INTEGER_ARG, true, false);
        assertEquals(m, publicCMethod);
        
        //inherited
       m = IntrospectionUtil.findMethod(ServletD.class, "setPublicC",  __INTEGER_ARG, true, false);
       assertEquals(m, publicCMethod);
    }
    
    public void testMethodDefault ()
    throws Exception
    {
        // direct
        Method m = IntrospectionUtil.findMethod(ServletC.class, "setDefaultC", __INTEGER_ARG, true, false);
        assertEquals(m, defaultCMethod);
        
        //inherited
        m = IntrospectionUtil.findMethod(ServletD.class, "setDefaultC", __INTEGER_ARG, true, false);
        assertEquals(m, defaultCMethod);
    }
}
