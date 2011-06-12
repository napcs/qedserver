// ========================================================================
// Copyright 2006 Mort Bay Consulting Pty. Ltd.
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
package org.mortbay.jetty.handler.rewrite;

import java.io.IOException;
import java.util.Enumeration;

import org.mortbay.jetty.handler.rewrite.HeaderPatternRule;


public class HeaderPatternRuleTest extends AbstractRuleTestCase
{
    private HeaderPatternRule _rule;
    
    public void setUp() throws Exception
    {
        super.setUp();

        _rule = new HeaderPatternRule();
        _rule.setPattern("*");
    }

    public void testHeaderWithTextValues() throws IOException
    {
        // different keys
        String headers[][] = { 
                { "hnum#1", "test1" }, 
                { "hnum#2", "2test2" },
                { "hnum#3", "test3" } 
        };

        assertHeaders(headers);
    }

    public void testHeaderWithNumberValues() throws IOException
    {
        String headers[][] = { 
                { "hello", "1" }, 
                { "hello", "-1" },
                { "hello", "100" },
                { "hello", "100" },
                { "hello", "100" },
                { "hello", "100" },
                { "hello", "100" },
                
                { "hello1", "200" }
        };

        assertHeaders(headers);
    }
    
    public void testHeaderOverwriteValues() throws IOException
    {
        String headers[][] = {
                { "size", "100" },
                { "size", "200" },
                { "size", "300" },
                { "size", "400" },
                { "size", "500" },
                { "title", "abc" },
                { "title", "bac" },
                { "title", "cba" },
                { "title1", "abba" },
                { "title1", "abba1" },
                { "title1", "abba" },
                { "title1", "abba1" }
        };
        
        assertHeaders(headers);
        
        Enumeration e = _response.getHeaders("size");
        int count = 0;
        while (e.hasMoreElements())
        {
            e.nextElement();
            count++;
        }
        
        assertEquals(1, count);
        assertEquals("500", _response.getHeader("size"));
        assertEquals("cba", _response.getHeader("title"));
        assertEquals("abba1", _response.getHeader("title1"));
    }

    private void assertHeaders(String headers[][]) throws IOException
    {
        for (int i = 0; i < headers.length; i++)
        {
            _rule.setName(headers[i][0]);
            _rule.setValue(headers[i][1]);

            _rule.apply(null, _request, _response);

            assertEquals(headers[i][1], _response.getHeader(headers[i][0]));
        }
    }
}
