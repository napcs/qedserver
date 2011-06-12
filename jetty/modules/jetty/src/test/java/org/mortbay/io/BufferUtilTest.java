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
 */
public class BufferUtilTest extends TestCase
{

    /**
     * Constructor for BufferUtilTest.
     * @param arg0
     */
    public BufferUtilTest(String arg0)
    {
        super(arg0);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(BufferUtilTest.class);
    }

    public void testToInt()
        throws Exception
    {
        Buffer buf[] = 
        {
            new ByteArrayBuffer("0"),
            new ByteArrayBuffer(" 42 "),
            new ByteArrayBuffer("   43abc"),
            new ByteArrayBuffer("-44"),
            new ByteArrayBuffer(" - 45;"),
            new ByteArrayBuffer("-2147483648"),
            new ByteArrayBuffer("2147483647"),
        };
        
        int val[] =
        {
            0,42,43,-44,-45,-2147483648,2147483647
        };
        
        for (int i=0;i<buf.length;i++)
            assertEquals("t"+i, val[i], BufferUtil.toInt(buf[i]));
    }

    public void testPutInt()
        throws Exception
    {
        int val[] =
        {
            0,42,43,-44,-45,-2147483648,2147483647
        };
        
        String str[] =
        {
            "0","42","43","-44","-45","-2147483648","2147483647"
        };
        
        Buffer buffer = new ByteArrayBuffer(12);

        for (int i=0;i<val.length;i++)
        {
            buffer.clear();
            BufferUtil.putDecInt(buffer,val[i]);
            assertEquals("t"+i,str[i],BufferUtil.to8859_1_String(buffer));
        }       
    }

    public void testPutLong()
        throws Exception
    {
        long val[] =
        {
            0,42,43,-44,-45,Long.MIN_VALUE,Long.MAX_VALUE
        };
        
        String str[] =
        {
            "0","42","43","-44","-45",""+Long.MIN_VALUE,""+Long.MAX_VALUE
        };
        
        Buffer buffer = new ByteArrayBuffer(32);

        for (int i=0;i<val.length;i++)
        {
            buffer.clear();
            BufferUtil.putDecLong(buffer,val[i]);
            assertEquals("t"+i,str[i],BufferUtil.to8859_1_String(buffer));
        }       
    }

    public void testPutHexInt()
        throws Exception
    {
        int val[] =
        {
            0,42,43,-44,-45,-2147483648,2147483647
        };
        
        String str[] =
        {
            "0","2A","2B","-2C","-2D","-80000000","7FFFFFFF"
        };
        
        Buffer buffer = new ByteArrayBuffer(12);

        for (int i=0;i<val.length;i++)
        {
            buffer.clear();
            BufferUtil.putHexInt(buffer,val[i]);
            assertEquals("t"+i,str[i],BufferUtil.to8859_1_String(buffer));
        }       
    }
}
