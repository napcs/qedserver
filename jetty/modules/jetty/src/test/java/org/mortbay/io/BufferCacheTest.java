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

package org.mortbay.io;

import junit.framework.TestCase;

/* ------------------------------------------------------------------------------- */
/**
 * 
 * @author gregw
 */
public class BufferCacheTest extends TestCase
{
    final static String[] S=
    { "S0", "S1", "s2", "s3" };

    BufferCache cache;

    public BufferCacheTest(String arg0)
    {
        super(arg0);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(BufferCacheTest.class);
    }

    /**
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        cache=new BufferCache();
        cache.add(S[1],1);
        cache.add(S[2],2);
        cache.add(S[3],3);
    }

    /**
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testLookupIndex()
    {
        for (int i=0; i<S.length; i++)
        {
            String s="S0S1s2s3";
            ByteArrayBuffer buf=new ByteArrayBuffer(s.getBytes(),i*2,2);
            BufferCache.CachedBuffer b=cache.get(buf);
            int index=b==null?-1:b.getOrdinal();

            if (i>0)
                assertEquals(i,index);
            else
                assertEquals(-1,index);
        }
    }

    public void testGetBuffer()
    {
        for (int i=0; i<S.length; i++)
        {
            String s="S0S1s2s3";
            ByteArrayBuffer buf=new ByteArrayBuffer(s.getBytes(),i*2,2);
            Buffer b=cache.get(buf);

            if (i>0)
                assertEquals(i,b.peek(1)-'0');
            else
                assertEquals(null,b);
        }
    }

    public void testLookupBuffer()
    {
        for (int i=0; i<S.length; i++)
        {
            String s="S0S1s2s3";
            ByteArrayBuffer buf=new ByteArrayBuffer(s.getBytes(),i*2,2);
            Buffer b=cache.lookup(buf);

            assertEquals(S[i],b.toString());
            if (i>0)
                assertTrue(""+i,S[i]==b.toString());
            else
            {
                assertTrue(""+i,S[i]!=b.toString());
                assertEquals(""+i,S[i],b.toString());
            }
        }
    }

    public void testLookupPartialBuffer()
    {
        cache.add("44444",4);
        
        ByteArrayBuffer buf=new ByteArrayBuffer("44444");
        Buffer b=cache.lookup(buf);
        assertEquals("44444",b.toString());
        assertEquals(4,cache.getOrdinal(b));
        
        buf=new ByteArrayBuffer("4444");
        b=cache.lookup(buf);
        assertEquals(-1,cache.getOrdinal(b));
        
        buf=new ByteArrayBuffer("44444x");
        b=cache.lookup(buf);
        assertEquals(-1,cache.getOrdinal(b));
        

    }

    public void testInsensitiveLookupBuffer()
    {
        for (int i=0; i<S.length; i++)
        {
            String s="s0s1S2S3";
            ByteArrayBuffer buf=new ByteArrayBuffer(s.getBytes(),i*2,2);
            Buffer b=cache.lookup(buf);

            assertTrue("test"+i,S[i].equalsIgnoreCase(b.toString()));
            if (i>0)
                assertTrue("test"+i,S[i]==b.toString());
            else
                assertTrue("test"+i,S[i]!=b.toString());
        }
    }

    public void testToString()
    {
        for (int i=0; i<S.length; i++)
        {
            String s="S0S1s2s3";
            ByteArrayBuffer buf=new ByteArrayBuffer(s.getBytes(),i*2,2);
            String b=cache.toString(buf);

            assertEquals(S[i],b);
            if (i>0)
                assertTrue(S[i]==b);
            else
                assertTrue(S[i]!=b);
        }
    }

}
