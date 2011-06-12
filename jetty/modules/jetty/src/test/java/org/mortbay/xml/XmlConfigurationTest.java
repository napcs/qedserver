//========================================================================
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

package org.mortbay.xml;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class XmlConfigurationTest extends TestCase
{
    public final static String __CRLF = "\015\012";
    
    /* ------------------------------------------------------------ */
    public static void testXmlParser() throws Exception
    {
        XmlParser parser = new XmlParser();

        URL configURL = XmlConfiguration.class.getClassLoader().getResource("org/mortbay/xml/configure_6_0.dtd");
        parser.redirectEntity("configure.dtd", configURL);
        parser.redirectEntity("configure_1_3.dtd", configURL);
        parser.redirectEntity("http://jetty.mortbay.org/configure.dtd", configURL);
        parser.redirectEntity("-//Mort Bay Consulting//DTD Configure//EN", configURL);
        parser.redirectEntity("http://jetty.mortbay.org/configure_1_3.dtd", configURL);
        parser.redirectEntity("-//Mort Bay Consulting//DTD Configure 1.3//EN", configURL);
        parser.redirectEntity("configure_1_2.dtd", configURL);
        parser.redirectEntity("http://jetty.mortbay.org/configure_1_2.dtd", configURL);
        parser.redirectEntity("-//Mort Bay Consulting//DTD Configure 1.2//EN", configURL);
        parser.redirectEntity("configure_1_1.dtd", configURL);
        parser.redirectEntity("http://jetty.mortbay.org/configure_1_1.dtd", configURL);
        parser.redirectEntity("-//Mort Bay Consulting//DTD Configure 1.1//EN", configURL);
        parser.redirectEntity("configure_1_0.dtd", configURL);
        parser.redirectEntity("http://jetty.mortbay.org/configure_1_0.dtd", configURL);
        parser.redirectEntity("-//Mort Bay Consulting//DTD Configure 1.0//EN", configURL);
        
        URL url = XmlConfigurationTest.class.getClassLoader().getResource("org/mortbay/xml/configure.xml");
        XmlParser.Node testDoc = parser.parse(url.toString());
        String testDocStr = testDoc.toString().trim();
        
        assertTrue(testDocStr.startsWith("<Configure"));
        assertTrue(testDocStr.endsWith("</Configure>"));
        
    }

    /* ------------------------------------------------------------ */
    public static void testXmlConfiguration() throws Exception
    {
        Map properties = new HashMap();
        properties.put("whatever", "xxx");
        
        URL url = XmlConfigurationTest.class.getClassLoader().getResource("org/mortbay/xml/configure.xml");
        XmlConfiguration configuration =
            new XmlConfiguration(url);
        TestConfiguration tc = new TestConfiguration();
        configuration.setProperties(properties);
        configuration.configure(tc);
        
        assertEquals("Set String","SetValue",tc.testObject);
        assertEquals("Set Type",2,tc.testInt);
        
        assertEquals("Put","PutValue",tc.get("Test"));
        assertEquals("Put dft","2",tc.get("TestDft"));
        assertEquals("Put type",new Integer(2),tc.get("TestInt"));
        
        assertEquals("Trim","PutValue",tc.get("Trim"));
        assertEquals("Null",null,tc.get("Null"));
        assertEquals("NullTrim",null,tc.get("NullTrim"));
        
        assertEquals("ObjectTrim",new Double(1.2345),tc.get("ObjectTrim"));
        assertEquals("Objects","-1String",tc.get("Objects"));
        assertEquals( "ObjectsTrim", "-1String",tc.get("ObjectsTrim"));
        assertEquals( "String", "\n    PutValue\n  ",tc.get("String"));
        assertEquals( "NullString", "",tc.get("NullString"));
        assertEquals( "WhateSpace", "\n  ",tc.get("WhiteSpace"));
        assertEquals( "ObjectString", "\n    1.2345\n  ",tc.get("ObjectString"));
        assertEquals( "ObjectsString", "-1String",tc.get("ObjectsString"));
        assertEquals( "ObjectsWhiteString", "-1\n  String",tc.get("ObjectsWhiteString"));
        
        assertEquals( "SystemProperty", System.getProperty("user.dir")+"/stuff",tc.get("SystemProperty"));
        assertEquals( "Property", "xxx", tc.get("Property"));
        
       
        assertEquals( "Called", "Yes",tc.get("Called"));
        
        assertTrue(TestConfiguration.called);
        
        assertEquals("oa[0]","Blah",tc.oa[0]);
        assertEquals("oa[1]","1.2.3.4:5678",tc.oa[1]);
        assertEquals("oa[2]",new Double(1.2345),tc.oa[2]);
        assertEquals("oa[3]",null,tc.oa[3]);
        
        assertEquals("ia[0]",1,tc.ia[0]);
        assertEquals("ia[1]",2,tc.ia[1]);
        assertEquals("ia[2]",3,tc.ia[2]);
        assertEquals("ia[3]",0,tc.ia[3]);
        
        TestConfiguration tc2=tc.nested;
        assertTrue(tc2!=null);
        assertEquals( "Called(bool)", new Boolean(true),tc2.get("Arg"));
        
        assertEquals("nested config",null,tc.get("Arg"));
        assertEquals("nested config",new Boolean(true),tc2.get("Arg"));
        
        assertEquals("nested config","Call1",tc2.testObject);
        assertEquals("nested config",4,tc2.testInt);
        assertEquals( "nested call", "http://www.mortbay.com/",tc2.url.toString());
        
        configuration =
            new XmlConfiguration("<Configure class=\"org.mortbay.xml.TestConfiguration\"><Set name=\"Test\">SetValue</Set><Set name=\"Test\" type=\"int\">2</Set></Configure>");
        TestConfiguration tc3 = new TestConfiguration();
        configuration.configure(tc3);
        assertEquals("Set String 3","SetValue",tc3.testObject);
        assertEquals("Set Type 3",2,tc3.testInt);
        
        assertEquals("static to field",tc.testField1,77);
        assertEquals("field to field",tc.testField2,2);
        assertEquals("literal to static",TestConfiguration.VALUE,42);
        
    }



}
