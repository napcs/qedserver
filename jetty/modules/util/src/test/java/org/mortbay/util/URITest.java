// ========================================================================
// Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.util;

import junit.framework.TestSuite;


/* ------------------------------------------------------------ */
/** Util meta Tests.
 * @author Greg Wilkins (gregw)
 */
public class URITest extends junit.framework.TestCase
{
    public URITest(String name)
    {
      super(name);
    }
    
    public static junit.framework.Test suite() {
        TestSuite suite = new TestSuite(URITest.class);
        return suite;                  
    }

    /* ------------------------------------------------------------ */
    /** main.
     */
    public static void main(String[] args)
    {
      junit.textui.TestRunner.run(suite());
    }    
    
    /* ------------------------------------------------------------ */
    public void testEncodePath()
    {
        // test basic encode/decode
        StringBuffer buf = new StringBuffer();
        
        
        buf.setLength(0);
        URIUtil.encodePath(buf,"/foo%23+;,:=/b a r/?info ");
        assertEquals("/foo%2523+%3B,:=/b%20a%20r/%3Finfo%20",buf.toString());

        assertEquals("/foo%2523+%3B,:=/b%20a%20r/%3Finfo%20",URIUtil.encodePath("/foo%23+;,:=/b a r/?info "));
                
        buf.setLength(0);
        URIUtil.encodeString(buf,"foo%23;,:=b a r",";,= ");
        assertEquals("foo%2523%3b%2c:%3db%20a%20r",buf.toString());
        
        buf.setLength(0);
        URIUtil.encodePath(buf,"/context/'list'/\"me\"/;<script>window.alert('xss');</script>");
        assertEquals("/context/%27list%27/%22me%22/%3B%3Cscript%3Ewindow.alert(%27xss%27)%3B%3C/script%3E", buf.toString());
    }    
    
    /* ------------------------------------------------------------ */
    public void testDecodePath()
    {
        assertEquals("foo%23;,:=b a r",URIUtil.decodePath("foo%2523%3b%2c:%3db%20a%20r")); 
        assertEquals("foo%23;,:=b a r=",URIUtil.decodePath("xxxfoo%2523%3b%2c:%3db%20a%20r%3Dxxx".getBytes(),3,30));
        assertEquals("fää%23;,:=b a r=",URIUtil.decodePath("fää%2523%3b%2c:%3db%20a%20r%3D"));   
        assertEquals("f\u0629\u0629%23;,:=b a r",URIUtil.decodePath("f%d8%a9%d8%a9%2523%3b%2c:%3db%20a%20r"));   
    }
    
    /* ------------------------------------------------------------ */
    public void testAddPaths()
    {
        assertEquals("null+null", URIUtil.addPaths(null,null),null);
        assertEquals("null+", URIUtil.addPaths(null,""),"");
        assertEquals("null+bbb", URIUtil.addPaths(null,"bbb"),"bbb");
        assertEquals("null+/", URIUtil.addPaths(null,"/"),"/");
        assertEquals("null+/bbb", URIUtil.addPaths(null,"/bbb"),"/bbb");
        
        assertEquals("+null", URIUtil.addPaths("",null),"");
        assertEquals("+", URIUtil.addPaths("",""),"");
        assertEquals("+bbb", URIUtil.addPaths("","bbb"),"bbb");
        assertEquals("+/", URIUtil.addPaths("","/"),"/");
        assertEquals("+/bbb", URIUtil.addPaths("","/bbb"),"/bbb");
        
        assertEquals("aaa+null", URIUtil.addPaths("aaa",null),"aaa");
        assertEquals("aaa+", URIUtil.addPaths("aaa",""),"aaa");
        assertEquals("aaa+bbb", URIUtil.addPaths("aaa","bbb"),"aaa/bbb");
        assertEquals("aaa+/", URIUtil.addPaths("aaa","/"),"aaa/");
        assertEquals("aaa+/bbb", URIUtil.addPaths("aaa","/bbb"),"aaa/bbb");
        
        assertEquals("/+null", URIUtil.addPaths("/",null),"/");
        assertEquals("/+", URIUtil.addPaths("/",""),"/");
        assertEquals("/+bbb", URIUtil.addPaths("/","bbb"),"/bbb");
        assertEquals("/+/", URIUtil.addPaths("/","/"),"/");
        assertEquals("/+/bbb", URIUtil.addPaths("/","/bbb"),"/bbb");
        
        assertEquals("aaa/+null", URIUtil.addPaths("aaa/",null),"aaa/");
        assertEquals("aaa/+", URIUtil.addPaths("aaa/",""),"aaa/");
        assertEquals("aaa/+bbb", URIUtil.addPaths("aaa/","bbb"),"aaa/bbb");
        assertEquals("aaa/+/", URIUtil.addPaths("aaa/","/"),"aaa/");
        assertEquals("aaa/+/bbb", URIUtil.addPaths("aaa/","/bbb"),"aaa/bbb");
        
        assertEquals(";JS+null", URIUtil.addPaths(";JS",null),";JS");
        assertEquals(";JS+", URIUtil.addPaths(";JS",""),";JS");
        assertEquals(";JS+bbb", URIUtil.addPaths(";JS","bbb"),"bbb;JS");
        assertEquals(";JS+/", URIUtil.addPaths(";JS","/"),"/;JS");
        assertEquals(";JS+/bbb", URIUtil.addPaths(";JS","/bbb"),"/bbb;JS");
        
        assertEquals("aaa;JS+null", URIUtil.addPaths("aaa;JS",null),"aaa;JS");
        assertEquals("aaa;JS+", URIUtil.addPaths("aaa;JS",""),"aaa;JS");
        assertEquals("aaa;JS+bbb", URIUtil.addPaths("aaa;JS","bbb"),"aaa/bbb;JS");
        assertEquals("aaa;JS+/", URIUtil.addPaths("aaa;JS","/"),"aaa/;JS");
        assertEquals("aaa;JS+/bbb", URIUtil.addPaths("aaa;JS","/bbb"),"aaa/bbb;JS");
        
        assertEquals("aaa;JS+null", URIUtil.addPaths("aaa/;JS",null),"aaa/;JS");
        assertEquals("aaa;JS+", URIUtil.addPaths("aaa/;JS",""),"aaa/;JS");
        assertEquals("aaa;JS+bbb", URIUtil.addPaths("aaa/;JS","bbb"),"aaa/bbb;JS");
        assertEquals("aaa;JS+/", URIUtil.addPaths("aaa/;JS","/"),"aaa/;JS");
        assertEquals("aaa;JS+/bbb", URIUtil.addPaths("aaa/;JS","/bbb"),"aaa/bbb;JS");
        
        assertEquals("?A=1+null", URIUtil.addPaths("?A=1",null),"?A=1");
        assertEquals("?A=1+", URIUtil.addPaths("?A=1",""),"?A=1");
        assertEquals("?A=1+bbb", URIUtil.addPaths("?A=1","bbb"),"bbb?A=1");
        assertEquals("?A=1+/", URIUtil.addPaths("?A=1","/"),"/?A=1");
        assertEquals("?A=1+/bbb", URIUtil.addPaths("?A=1","/bbb"),"/bbb?A=1");
        
        assertEquals("aaa?A=1+null", URIUtil.addPaths("aaa?A=1",null),"aaa?A=1");
        assertEquals("aaa?A=1+", URIUtil.addPaths("aaa?A=1",""),"aaa?A=1");
        assertEquals("aaa?A=1+bbb", URIUtil.addPaths("aaa?A=1","bbb"),"aaa/bbb?A=1");
        assertEquals("aaa?A=1+/", URIUtil.addPaths("aaa?A=1","/"),"aaa/?A=1");
        assertEquals("aaa?A=1+/bbb", URIUtil.addPaths("aaa?A=1","/bbb"),"aaa/bbb?A=1");
        
        assertEquals("aaa?A=1+null", URIUtil.addPaths("aaa/?A=1",null),"aaa/?A=1");
        assertEquals("aaa?A=1+", URIUtil.addPaths("aaa/?A=1",""),"aaa/?A=1");
        assertEquals("aaa?A=1+bbb", URIUtil.addPaths("aaa/?A=1","bbb"),"aaa/bbb?A=1");
        assertEquals("aaa?A=1+/", URIUtil.addPaths("aaa/?A=1","/"),"aaa/?A=1");
        assertEquals("aaa?A=1+/bbb", URIUtil.addPaths("aaa/?A=1","/bbb"),"aaa/bbb?A=1");
        
        assertEquals(";JS?A=1+null", URIUtil.addPaths(";JS?A=1",null),";JS?A=1");
        assertEquals(";JS?A=1+", URIUtil.addPaths(";JS?A=1",""),";JS?A=1");
        assertEquals(";JS?A=1+bbb", URIUtil.addPaths(";JS?A=1","bbb"),"bbb;JS?A=1");
        assertEquals(";JS?A=1+/", URIUtil.addPaths(";JS?A=1","/"),"/;JS?A=1");
        assertEquals(";JS?A=1+/bbb", URIUtil.addPaths(";JS?A=1","/bbb"),"/bbb;JS?A=1");
        
        assertEquals("aaa;JS?A=1+null", URIUtil.addPaths("aaa;JS?A=1",null),"aaa;JS?A=1");
        assertEquals("aaa;JS?A=1+", URIUtil.addPaths("aaa;JS?A=1",""),"aaa;JS?A=1");
        assertEquals("aaa;JS?A=1+bbb", URIUtil.addPaths("aaa;JS?A=1","bbb"),"aaa/bbb;JS?A=1");
        assertEquals("aaa;JS?A=1+/", URIUtil.addPaths("aaa;JS?A=1","/"),"aaa/;JS?A=1");
        assertEquals("aaa;JS?A=1+/bbb", URIUtil.addPaths("aaa;JS?A=1","/bbb"),"aaa/bbb;JS?A=1");
        
        assertEquals("aaa;JS?A=1+null", URIUtil.addPaths("aaa/;JS?A=1",null),"aaa/;JS?A=1");
        assertEquals("aaa;JS?A=1+", URIUtil.addPaths("aaa/;JS?A=1",""),"aaa/;JS?A=1");
        assertEquals("aaa;JS?A=1+bbb", URIUtil.addPaths("aaa/;JS?A=1","bbb"),"aaa/bbb;JS?A=1");
        assertEquals("aaa;JS?A=1+/", URIUtil.addPaths("aaa/;JS?A=1","/"),"aaa/;JS?A=1");
        assertEquals("aaa;JS?A=1+/bbb", URIUtil.addPaths("aaa/;JS?A=1","/bbb"),"aaa/bbb;JS?A=1");

    }

    /* ------------------------------------------------------------ */
    public void testCompactPath()
    {
        assertEquals("/foo/bar", URIUtil.compactPath("/foo/bar"));
        assertEquals("/foo/bar?a=b//c", URIUtil.compactPath("/foo/bar?a=b//c"));

        assertEquals("/foo/bar", URIUtil.compactPath("//foo//bar"));
        assertEquals("/foo/bar?a=b//c", URIUtil.compactPath("//foo//bar?a=b//c"));
        
        assertEquals("/foo/bar", URIUtil.compactPath("/foo///bar"));
        assertEquals("/foo/bar?a=b//c", URIUtil.compactPath("/foo///bar?a=b//c"));
    }
    
    /* ------------------------------------------------------------ */
    public void testParentPath()
    {
        assertEquals("parent /aaa/bbb/","/aaa/", URIUtil.parentPath("/aaa/bbb/"));
        assertEquals("parent /aaa/bbb","/aaa/", URIUtil.parentPath("/aaa/bbb"));
        assertEquals("parent /aaa/","/", URIUtil.parentPath("/aaa/"));
        assertEquals("parent /aaa","/", URIUtil.parentPath("/aaa"));
        assertEquals("parent /",null, URIUtil.parentPath("/"));
        assertEquals("parent null",null, URIUtil.parentPath(null));

    }
    
    /* ------------------------------------------------------------ */
    public void testCanonicalPath()
    {
        String[][] canonical = 
        {
            {"/aaa/bbb/","/aaa/bbb/"},
            {"/aaa//bbb/","/aaa//bbb/"},
            {"/aaa///bbb/","/aaa///bbb/"},
            {"/aaa/./bbb/","/aaa/bbb/"},
            {"/aaa/../bbb/","/bbb/"},
            {"/aaa/./../bbb/","/bbb/"},
            {"/aaa/bbb/ccc/../../ddd/","/aaa/ddd/"},
            {"./bbb/","bbb/"},
            {"./aaa/../bbb/","bbb/"},
            {"./",""},
            {".//",".//"},
            {".///",".///"},
            {"/.","/"},
            {"//.","//"},
            {"///.","///"},
            {"/","/"},
            {"aaa/bbb","aaa/bbb"},
            {"aaa/","aaa/"},
            {"aaa","aaa"},
            {"/aaa/bbb","/aaa/bbb"},
            {"/aaa//bbb","/aaa//bbb"},
            {"/aaa/./bbb","/aaa/bbb"},
            {"/aaa/../bbb","/bbb"},
            {"/aaa/./../bbb","/bbb"},
            {"./bbb","bbb"},
            {"./aaa/../bbb","bbb"},
            {"aaa/bbb/..","aaa/"},
            {"aaa/bbb/../","aaa/"},
            {"/aaa//../bbb","/aaa/bbb"},
            {"/aaa/./../bbb","/bbb"},
            {"./",""},
            {".",""},
            {"",""},
            {"..",null},
            {"./..",null},
            {"aaa/../..",null},
            {"/foo/bar/../../..",null},
            {"/../foo",null},
            {"/foo/.","/foo/"},
            {"a","a"},
            {"a/","a/"},
            {"a/.","a/"},
            {"a/..",""},
            {"a/../..",null},
            {"/foo/../bar//","/bar//"},
        };

        for (int t=0;t<canonical.length;t++)
            assertEquals( "canonical "+canonical[t][0],
                          canonical[t][1],
                          URIUtil.canonicalPath(canonical[t][0])
                          );
        
    }
    

}
