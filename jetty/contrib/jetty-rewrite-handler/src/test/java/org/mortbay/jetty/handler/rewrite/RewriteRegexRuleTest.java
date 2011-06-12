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

import org.mortbay.jetty.handler.rewrite.RewriteRegexRule;

public class RewriteRegexRuleTest extends AbstractRuleTestCase
{
    private RewriteRegexRule _rule;

    String[][] _tests=
    {       
            {"/foo/bar",".*","/replace","/replace"},
            {"/foo/bar","/xxx.*","/replace",null},       
            {"/foo/bar","/(.*)/(.*)","/$2/$1/xxx","/bar/foo/xxx"},
    };
    
    public void setUp() throws Exception
    {
        super.setUp();
        _rule=new RewriteRegexRule();
    }
    
    public void testRequestUriEnabled() throws IOException
    {
        for (int i=0;i<_tests.length;i++)
        {
            _rule.setRegex(_tests[i][1]);
            _rule.setReplacement(_tests[i][2]);
            
            String result = _rule.matchAndApply(_tests[i][0], _request, _response);
        
            assertEquals(_tests[i][1],_tests[i][3], result);
        }
    }

}
