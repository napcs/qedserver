// ========================================================================
// Copyright 2009 Webtide LLC
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

public class RedirectRegexRuleTest extends AbstractRuleTestCase
{
    private RedirectRegexRule _rule;
    
    public void setUp() throws Exception
    {
        super.setUp();
        _rule = new RedirectRegexRule();
    }
    
    public void tearDown()
    {
        _rule = null;
    }
    
    public void testLocationWithReplacementGroupEmpty() throws IOException
    {
        _rule.setRegex("/my/dir/file/(.*)$");
        _rule.setReplacement("http://www.mortbay.org/$1");

        // Resource is dir
        _rule.matchAndApply("/my/dir/file/", _request, _response);
        assertEquals("http://www.mortbay.org/", _response.getHeader(HttpHeaders.LOCATION));
    }
    
    public void testLocationWithReplacmentGroupSimple() throws IOException
    {
        _rule.setRegex("/my/dir/file/(.*)$");
        _rule.setReplacement("http://www.mortbay.org/$1");

        // Resource is an image
        _rule.matchAndApply("/my/dir/file/image.png", _request, _response);
        assertEquals("http://www.mortbay.org/image.png", _response.getHeader(HttpHeaders.LOCATION));
    }
    
    public void testLocationWithReplacementGroupDeepWithParams() throws IOException
    {
        _rule.setRegex("/my/dir/file/(.*)$");
        _rule.setReplacement("http://www.mortbay.org/$1");

        // Resource is api with parameters
        _rule.matchAndApply("/my/dir/file/api/rest/foo?id=100&sort=date", _request, _response);
        assertEquals("http://www.mortbay.org/api/rest/foo?id=100&sort=date", _response.getHeader(HttpHeaders.LOCATION));
    }
    
}
