//========================================================================
//Copyright 2007 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import junit.framework.TestCase;

public class ResourceCollectionTest extends TestCase
{
    
    public void testMutlipleSources1() throws Exception
    {
        ResourceCollection rc1 = new ResourceCollection(new String[]{
                "src/test/resources/org/mortbay/resource/one/",
                "src/test/resources/org/mortbay/resource/two/",
                "src/test/resources/org/mortbay/resource/three/"
        });
        assertEquals("1 - one", getContent(rc1, "1.txt"));
        assertEquals("2 - two", getContent(rc1, "2.txt"));
        assertEquals("3 - three", getContent(rc1, "3.txt"));        
        
        
        ResourceCollection rc2 = new ResourceCollection(
                "src/test/resources/org/mortbay/resource/one/," +
                "src/test/resources/org/mortbay/resource/two/," +
                "src/test/resources/org/mortbay/resource/three/"
        );
        assertEquals("1 - one", getContent(rc2, "1.txt"));
        assertEquals("2 - two", getContent(rc2, "2.txt"));
        assertEquals("3 - three", getContent(rc2, "3.txt"));
        String[] list = rc1.list();
        for(int i=0; i<list.length; i++)
            System.err.println(list[i]);        
    }
    
    public void testMergedDir() throws Exception
    {
        ResourceCollection rc = new ResourceCollection(new String[]{
                "src/test/resources/org/mortbay/resource/one/",
                "src/test/resources/org/mortbay/resource/two/",
                "src/test/resources/org/mortbay/resource/three/"
        });
        
        Resource r = rc.addPath("dir");
        assertTrue(r instanceof ResourceCollection);
        rc=(ResourceCollection)r;
        assertEquals("1 - one", getContent(rc, "1.txt"));
        assertEquals("2 - two", getContent(rc, "2.txt"));
        assertEquals("3 - three", getContent(rc, "3.txt"));  
    }
    
    static String getContent(ResourceCollection rc, String path) throws Exception
    {
        StringBuffer buffer = new StringBuffer();
        String line = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(rc.addPath(path).getURL().openStream()));
        while((line=br.readLine())!=null)
            buffer.append(line);
        br.close();        
        return buffer.toString();
    }
    
}
