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

import java.util.Locale;
import java.util.TimeZone;

import junit.framework.TestSuite;


/* ------------------------------------------------------------ */
/** Util meta Tests.
 * @author Greg Wilkins (gregw)
 */
public class DateCacheTest extends junit.framework.TestCase
{
    public DateCacheTest(String name)
    {
      super(name);
    }
    
    public static junit.framework.Test suite() {
        TestSuite suite = new TestSuite(DateCacheTest.class);
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
    public void testDateCache() throws Exception
    {
        //@WAS: Test t = new Test("org.mortbay.util.DateCache");
        //                            012345678901234567890123456789
        DateCache dc = new DateCache("EEE, dd MMM yyyy HH:mm:ss zzz ZZZ",
                                     Locale.US);
            dc.setTimeZone(TimeZone.getTimeZone("GMT"));
            String last=dc.format(System.currentTimeMillis());
            boolean change=false;
            for (int i=0;i<15;i++)
            {
                Thread.sleep(100);
                String date=dc.format(System.currentTimeMillis());
                
                assertEquals( "Same Date",
                              last.substring(0,17),
                              date.substring(0,17));
                
                if (!last.substring(17).equals(date.substring(17)))
                    change=true;
                else
                {
                    int lh=Integer.parseInt(last.substring(17,19));
                    int dh=Integer.parseInt(date.substring(17,19));
                    int lm=Integer.parseInt(last.substring(20,22));
                    int dm=Integer.parseInt(date.substring(20,22));
                    int ls=Integer.parseInt(last.substring(23,25));
                    int ds=Integer.parseInt(date.substring(23,25));

                    // This won't work at midnight!
                    change|= ds!=ls || dm!=lm || dh!=lh;
                }
                last=date;
            }
            assertTrue("time changed", change);


            // Test string is cached
            dc = new DateCache();
            String s1=dc.format(System.currentTimeMillis());
            dc.format(1);
            String s2=dc.format(System.currentTimeMillis());
            dc.format(System.currentTimeMillis()+10*60*60);
            String s3=dc.format(System.currentTimeMillis());
            assertTrue(s1==s2 || s2==s3);
    }

}
