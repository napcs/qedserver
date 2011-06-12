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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.TestSuite;

import org.mortbay.util.IO;


/* ------------------------------------------------------------ */
/** Util meta Tests.
 * @author Greg Wilkins (gregw)
 */
public class IOTest extends junit.framework.TestCase
{
    public IOTest(String name)
    {
      super(name);
    }
    
    public static junit.framework.Test suite() {
        TestSuite suite = new TestSuite(IOTest.class);
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
    public void testIO() throws InterruptedException
    {
        // Only a little test
        ByteArrayInputStream in = new ByteArrayInputStream
            ("The quick brown fox jumped over the lazy dog".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IO.copyThread(in,out);
        Thread.sleep(1500);
        System.err.println(out);

        assertEquals( "copyThread",
                      out.toString(),
                      "The quick brown fox jumped over the lazy dog");
    }

    

    /* ------------------------------------------------------------ */
    public void testStringSpeed()
    {
        String s="012345678901234567890000000000000000000000000";
        char[] ca = new char[s.length()];
        int loops=1000000;
        
        long start=System.currentTimeMillis();
        long result=0;
        for (int loop=0;loop<loops;loop++)
        {
            for (int c=s.length();c-->0;)
                result+=s.charAt(c);
        }
        long end=System.currentTimeMillis();
        System.err.println("charAt   "+(end-start)+" "+result);
        
        start=System.currentTimeMillis();
        result=0;
        for (int loop=0;loop<loops;loop++)
        {
            s.getChars(0, s.length(), ca, 0);
            for (int c=s.length();c-->0;)
                result+=ca[c];
        }
        end=System.currentTimeMillis();
        System.err.println("getChars "+(end-start)+" "+result);
        
    }
}
