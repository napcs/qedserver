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

import org.mortbay.jetty.HttpFields;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.handler.rewrite.CookiePatternRule;


public class CookiePatternRuleTest extends AbstractRuleTestCase
{   
    public void setUp() throws Exception
    {
        super.setUp();
    }
    
    public void testSingleCookie() throws IOException
    {
        String[][] cookie = {
                {"cookie", "value"}
        };

        assertCookies(cookie);
    }
    
    public void testMultipleCookies() throws IOException
    {
        String[][] cookies = {
                {"cookie", "value"},
                {"name", "wolfgangpuck"},
                {"age", "28"}
        };
        
        assertCookies(cookies);
    }
    
    private void assertCookies(String[][] cookies) throws IOException
    {
        for (int i = 0; i < cookies.length; i++)
        {
            String[] cookie = cookies[i];
            
            // set cookie pattern
            CookiePatternRule rule = new CookiePatternRule();
            rule.setPattern("*");
            rule.setName(cookie[0]);
            rule.setValue(cookie[1]);

            System.out.println(rule.toString());

            // apply cookie pattern
            rule.apply(_request.getRequestURI(), _request, _response);
            
            // verify
            HttpFields httpFields = _response.getHttpFields();
            Enumeration e = httpFields.getValues(HttpHeaders.SET_COOKIE_BUFFER);
            int index = 0;
            while (e.hasMoreElements())
            {
                String[] result = ((String)e.nextElement()).split("=");
                assertEquals(cookies[index][0], result[0]);
                assertEquals(cookies[index][1], result[1]);
                
                // +1 cookies index
                index++;
            }
        }
    }
}
