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

import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.handler.rewrite.RedirectPatternRule;


public class RedirectPatternRuleTest extends AbstractRuleTestCase
{
    private RedirectPatternRule _rule;
    
    public void setUp() throws Exception
    {
        super.setUp();
        _rule = new RedirectPatternRule();
        _rule.setPattern("*");
    }
    
    public void tearDown()
    {
        _rule = null;
    }
    
    public void testLocation() throws IOException
    {
        String location = "http://mortbay.com";
        
        _rule.setLocation(location);
        _rule.apply(null, _request, _response);
        
        assertEquals(location, _response.getHeader(HttpHeaders.LOCATION));
    }
    
}
