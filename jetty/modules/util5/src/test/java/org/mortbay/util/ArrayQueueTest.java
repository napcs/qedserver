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

package org.mortbay.util;

import junit.framework.TestCase;

public class ArrayQueueTest extends TestCase
{
    
    public void testWrap() throws Exception
    {
        ArrayQueue<String> queue = new ArrayQueue<String>(3,3);
        
        assertEquals(0,queue.size());

        for (int i=0;i<10;i++)
        {
            queue.offer("one");
            assertEquals(1,queue.size());

            queue.offer("two");
            assertEquals(2,queue.size());

            queue.offer("three");
            assertEquals(3,queue.size());

            assertEquals("one",queue.get(0));
            assertEquals("two",queue.get(1));
            assertEquals("three",queue.get(2));

            assertEquals("[one, two, three]",queue.toString());

            assertEquals("two",queue.remove(1));
            assertEquals(2,queue.size());
            
            assertEquals("one",queue.remove());
            assertEquals(1,queue.size());

            assertEquals("three",queue.poll());
            assertEquals(0,queue.size());
            
            assertEquals(null,queue.poll());

            queue.offer("xxx");
            queue.offer("xxx");
            assertEquals(2,queue.size());
            assertEquals("xxx",queue.poll());
            assertEquals("xxx",queue.poll());
            assertEquals(0,queue.size());

        }

    }

    public void testRemove() throws Exception
    {
        ArrayQueue<String> queue = new ArrayQueue<String>(3,3);
       
        queue.add("0");
        queue.add("x");
        
        for (int i=1;i<100;i++)
        {
            queue.add(""+i);
            queue.add("x");
            queue.remove(queue.size()-3);
            queue.set(queue.size()-3,queue.get(queue.size()-3)+"!");
        }
        
        for (int i=0;i<99;i++)
            assertEquals(i+"!",queue.get(i));
    }

    public void testGrow() throws Exception
    {
        ArrayQueue<String> queue = new ArrayQueue<String>(3,5);
        assertEquals(3,queue.getCapacity());

        queue.add("0");
        queue.add("a");
        queue.add("b");
        assertEquals(3,queue.getCapacity());
        queue.add("c");
        assertEquals(8,queue.getCapacity());
        
        for (int i=0;i<4;i++)
            queue.add(""+('d'+i));
        assertEquals(8,queue.getCapacity());
        for (int i=0;i<4;i++)
            queue.poll();
        assertEquals(8,queue.getCapacity());
        for (int i=0;i<4;i++)
            queue.add(""+('d'+i));
        assertEquals(8,queue.getCapacity());
        for (int i=0;i<4;i++)
            queue.poll();
        assertEquals(8,queue.getCapacity());
        for (int i=0;i<4;i++)
            queue.add(""+('d'+i));
        assertEquals(8,queue.getCapacity());

        queue.add("z");
        assertEquals(13,queue.getCapacity());
        
        queue.clear();
        assertEquals(13,queue.getCapacity());
        for (int i=0;i<12;i++)
            queue.add(""+('a'+i));
        assertEquals(13,queue.getCapacity());
        queue.clear();
        assertEquals(13,queue.getCapacity());
        for (int i=0;i<12;i++)
            queue.add(""+('a'+i));
        assertEquals(13,queue.getCapacity());
        
            
    }
    
    public void testFullEmpty() throws Exception
    {
        ArrayQueue<String> queue = new ArrayQueue<String>(2);
        assertTrue(queue.offer("one"));
        assertTrue(queue.offer("two"));
        assertFalse(queue.offer("three"));
        
        try
        {
            queue.add("four");
            assertTrue(false);
        }
        catch(Exception e)
        {
            
        }

        assertEquals("one",queue.peek());
        assertEquals("one",queue.remove());
        assertEquals("two",queue.remove());
        try
        {
            assertEquals("three",queue.remove());
            assertTrue(false);
        }
        catch(Exception e)
        {
            
        }

        assertEquals(null,queue.poll());
    }
        
}
