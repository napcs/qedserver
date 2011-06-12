//========================================================================
//$Id: PatternRule.java 966 2008-04-17 13:53:44Z gregw $
//Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================
package org.mortbay.jetty.handler.rewrite;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.servlet.PathMap;

/**
 * Abstract rule that use a {@link PathMap} for pattern matching. It uses the 
 * servlet pattern syntax.
 */
public abstract class PatternRule extends Rule
{
    protected String _pattern;


    /* ------------------------------------------------------------ */
    public String getPattern()
    {
        return _pattern;
    }

    /* ------------------------------------------------------------ */
    /**
     * Sets the rule pattern.
     * 
     * @param pattern the pattern
     */
    public void setPattern(String pattern)
    {
        _pattern = pattern;
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.jetty.handler.rules.RuleBase#matchAndApply(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public String matchAndApply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if (PathMap.match(_pattern, target))
        {
            return apply(target,request, response);
        }
        return null;
    }

    /* ------------------------------------------------------------ */
    /** Apply the rule to the request
     * @param target field to attempt match
     * @param request request object
     * @param response response object
     * @return The target (possible updated)
     * @throws IOException exceptions dealing with operating on request or response objects  
     */
    protected abstract String apply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException;

    /**
     * Returns the rule pattern.
     */
    public String toString()
    {
        return super.toString()+"["+_pattern+"]";                
    }
}
