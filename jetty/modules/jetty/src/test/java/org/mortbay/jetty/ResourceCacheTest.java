//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty;

import java.io.File;
import java.io.FileOutputStream;

import org.mortbay.jetty.ResourceCache.Content;
import org.mortbay.resource.Resource;
import org.mortbay.resource.ResourceFactory;

import junit.framework.TestCase;

public class ResourceCacheTest extends TestCase
{
    Resource directory;
    File[] files=new File[10];
    String[] names=new String[files.length];
    ResourceCache cache = new ResourceCache(new MimeTypes());
    ResourceFactory factory;
    
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        for (int i=0;i<files.length;i++)
        {
            files[i]=File.createTempFile("RCT",".txt");
            files[i].deleteOnExit();
            names[i]=files[i].getName();
            FileOutputStream out = new FileOutputStream(files[i]);
            for (int j=0;j<(i*10-1);j++)
                out.write(' ');
            out.write('\n');
            out.close();
        }
        
        directory=Resource.newResource(files[0].getParentFile().getAbsolutePath());
        
        factory = new ResourceFactory()
        {
            public Resource getResource(String path)
            {
                try
                {
                    return directory.addPath(path);
                }
                catch(Exception e)
                {
                    return null;
                }
            }
            
        };
        cache.setMaxCacheSize(95);
        cache.setMaxCachedFileSize(85);
        cache.setMaxCachedFiles(4);
        cache.start();
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        cache.stop();
    }

    /* ------------------------------------------------------------ */
    public void testResourceCache() throws Exception
    {
        assertTrue(cache.lookup("does not exist",factory)==null);
        assertTrue(cache.lookup(names[9],factory)==null);
        
        Content content;
        content=cache.lookup(names[8],factory);
        assertTrue(content!=null);
        assertEquals(80,content.getContentLength());
     
        assertEquals(80,cache.getCachedSize());
        assertEquals(1,cache.getCachedFiles());

        content=cache.lookup(names[1],factory);
        assertEquals(90,cache.getCachedSize());
        assertEquals(2,cache.getCachedFiles());
        
        content=cache.lookup(names[2],factory);
        assertEquals(30,cache.getCachedSize());
        assertEquals(2,cache.getCachedFiles());
        
        content=cache.lookup(names[3],factory);
        assertEquals(60,cache.getCachedSize());
        assertEquals(3,cache.getCachedFiles());
        
        content=cache.lookup(names[4],factory);
        assertEquals(90,cache.getCachedSize());
        assertEquals(3,cache.getCachedFiles());
        
        content=cache.lookup(names[5],factory);
        assertEquals(90,cache.getCachedSize());
        assertEquals(2,cache.getCachedFiles());
        
        content=cache.lookup(names[6],factory);
        assertEquals(60,cache.getCachedSize());
        assertEquals(1,cache.getCachedFiles());
        
        FileOutputStream out = new FileOutputStream(files[6]);
        out.write(' ');
        out.close();
        content=cache.lookup(names[7],factory);
        assertEquals(70,cache.getCachedSize());
        assertEquals(1,cache.getCachedFiles());

        content=cache.lookup(names[6],factory);
        assertEquals(71,cache.getCachedSize());
        assertEquals(2,cache.getCachedFiles());
        
        content=cache.lookup(names[0],factory);
        assertEquals(72,cache.getCachedSize());
        assertEquals(3,cache.getCachedFiles());
        
        content=cache.lookup(names[1],factory);
        assertEquals(82,cache.getCachedSize());
        assertEquals(4,cache.getCachedFiles());
        
        content=cache.lookup(names[2],factory);
        assertEquals(32,cache.getCachedSize());
        assertEquals(4,cache.getCachedFiles());
        
        content=cache.lookup(names[3],factory);
        assertEquals(61,cache.getCachedSize());
        assertEquals(4,cache.getCachedFiles());
        
        cache.flushCache();
        assertEquals(0,cache.getCachedSize());
        assertEquals(0,cache.getCachedFiles());
        
        
    }
}
