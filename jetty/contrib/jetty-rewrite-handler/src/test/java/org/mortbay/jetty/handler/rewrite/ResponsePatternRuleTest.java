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

import org.mortbay.jetty.handler.rewrite.ResponsePatternRule;

public class ResponsePatternRuleTest extends AbstractRuleTestCase
{
    private ResponsePatternRule _rule;
    
    public void setUp() throws Exception
    {
        super.setUp();
        _rule = new ResponsePatternRule();
        _rule.setPattern("/test");
    }
    
    public void testStatusCodeNoReason() throws IOException
    {
        for (int i = 1; i < 400; i++)
        {
            _rule.setCode("" + i);
            _rule.apply(null, _request, _response);
            
            assertEquals(i, _response.getStatus());
        }
    }
    
    public void testStatusCodeWithReason() throws IOException
    {
        for (int i = 1; i < 400; i++)
        {
            _rule.setCode("" + i);
            _rule.setReason("reason" + i);
            _rule.apply(null, _request, _response);
            
            assertEquals(i, _response.getStatus());
            assertEquals(null, _response.getReason());
        }
    }
    
    public void testErrorStatusNoReason() throws IOException
    {
        for (int i = 400; i < 600; i++)
        {
            _rule.setCode("" + i);
            _rule.apply(null, _request, _response);
            
            assertEquals(i, _response.getStatus());
            assertEquals("", _response.getReason());
            super.reset();
        }
    }
    
    public void testErrorStatusWithReason() throws IOException
    {
        for (int i = 400; i < 600; i++)
        {
            _rule.setCode("" + i);
            _rule.setReason("reason-" + i);
            _rule.apply(null, _request, _response);
            
            assertEquals(i, _response.getStatus());
            assertEquals("reason-" + i, _response.getReason());
            super.reset();
        }
    }
}
