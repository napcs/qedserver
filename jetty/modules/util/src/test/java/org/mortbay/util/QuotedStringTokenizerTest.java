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

import junit.framework.TestCase;

/**
 * @author gregw
 *
 */
public class QuotedStringTokenizerTest extends TestCase
{

    /**
     * Constructor for QuotedStringTokenizerTest.
     * @param arg0
     */
    public QuotedStringTokenizerTest(String arg0)
    {
        super(arg0);
    }

    /*
     * Test for String nextToken()
     */
    public void testTokenizer0()
    {
        QuotedStringTokenizer tok = 
            new QuotedStringTokenizer("abc\n\"d\\\"'\"\n'p\\',y'\nz");
        checkTok(tok,false,false);
    }

    /*
     * Test for String nextToken()
     */
    public void testTokenizer1()
    {
        QuotedStringTokenizer tok = 
            new QuotedStringTokenizer("abc, \"d\\\"'\",'p\\',y' z", 
                                      " ,");
        checkTok(tok,false,false);
    }

    /*
     * Test for String nextToken()
     */
    public void testTokenizer2()
    {
        QuotedStringTokenizer tok = 
            new QuotedStringTokenizer("abc, \"d\\\"'\",'p\\',y' z", " ,",
            false);
        checkTok(tok,false,false);
        
        tok = new QuotedStringTokenizer("abc, \"d\\\"'\",'p\\',y' z", " ,",
                                        true);
        checkTok(tok,true,false);
    }
    
    /*
     * Test for String nextToken()
     */
    public void testTokenizer3()
    {
        QuotedStringTokenizer tok;
        
        tok = new QuotedStringTokenizer("abc, \"d\\\"'\",'p\\',y' z", " ,",
                                        false,false);
        checkTok(tok,false,false);
        
        tok = new QuotedStringTokenizer("abc, \"d\\\"'\",'p\\',y' z", " ,",
                                        false,true);
        checkTok(tok,false,true);
        
        tok = new QuotedStringTokenizer("abc, \"d\\\"'\",'p\\',y' z", " ,",
                                        true,false);
        checkTok(tok,true,false);
        
        tok = new QuotedStringTokenizer("abc, \"d\\\"'\",'p\\',y' z", " ,",
                                        true,true);
        checkTok(tok,true,true);
    }
    
    public void testQuote()
    {
        StringBuffer buf = new StringBuffer();
        
        buf.setLength(0);
        QuotedStringTokenizer.quote(buf,"abc \n efg");
        assertEquals("\"abc \\n efg\"",buf.toString());

        buf.setLength(0);
        QuotedStringTokenizer.quote(buf,"abcefg");
        assertEquals("\"abcefg\"",buf.toString());
        
        buf.setLength(0);
        QuotedStringTokenizer.quote(buf,"abcefg\"");
        assertEquals("\"abcefg\\\"\"",buf.toString());
        
        buf.setLength(0);
        QuotedStringTokenizer.quoteIfNeeded(buf,"abc \n efg");
        assertEquals("\"abc \\n efg\"",buf.toString());
        
        buf.setLength(0);
        QuotedStringTokenizer.quoteIfNeeded(buf,"abcefg");
        assertEquals("abcefg",buf.toString());
        
    }

    /*
     * Test for String nextToken()
     */
    public void testTokenizer4()
    {
        QuotedStringTokenizer tok = new QuotedStringTokenizer("abc'def,ghi'jkl",",");
        tok.setSingle(false);
        assertEquals("abc'def",tok.nextToken());
        assertEquals("ghi'jkl",tok.nextToken());
        tok = new QuotedStringTokenizer("abc'def,ghi'jkl",",");
        tok.setSingle(true);
        assertEquals("abcdef,ghijkl",tok.nextToken());
    }
    
    private void checkTok(QuotedStringTokenizer tok,boolean delim,boolean quotes)
    {
        assertTrue(tok.hasMoreElements());
        assertTrue(tok.hasMoreTokens());
        assertEquals("abc",tok.nextToken());
        if (delim)assertEquals(",",tok.nextToken());
        if (delim)assertEquals(" ",tok.nextToken());
            
        assertEquals(quotes?"\"d\\\"'\"":"d\"'",tok.nextElement());
        if (delim)assertEquals(",",tok.nextToken());
        assertEquals(quotes?"'p\\',y'":"p',y",tok.nextToken());
        if (delim)assertEquals(" ",tok.nextToken());
        assertEquals("z",tok.nextToken());
        assertFalse(tok.hasMoreTokens());
    }

    /*
     * Test for String quote(String, String)
     */
    public void testQuoteString()
    {
        assertEquals("abc",QuotedStringTokenizer.quote("abc", " ,"));
        assertEquals("\"a c\"",QuotedStringTokenizer.quote("a c", " ,"));
        assertEquals("\"a'c\"",QuotedStringTokenizer.quote("a'c", " ,"));  
        assertEquals("\"a\\n\\r\\t\"",QuotedStringTokenizer.quote("a\n\r\t")); 
    }


    public void testUnquote()
    {
        assertEquals("abc",QuotedStringTokenizer.unquote("abc"));
        assertEquals("a\"c",QuotedStringTokenizer.unquote("\"a\\\"c\""));
        assertEquals("a'c",QuotedStringTokenizer.unquote("\"a'c\""));
        assertEquals("a\n\r\t",QuotedStringTokenizer.unquote("\"a\\n\\r\\t\""));
    }

}
