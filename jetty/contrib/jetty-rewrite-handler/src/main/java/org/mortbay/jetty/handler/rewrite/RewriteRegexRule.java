//========================================================================
//$Id: RewriteRegexRule.java 966 2008-04-17 13:53:44Z gregw $
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
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Rewrite the URI by matching with a regular expression. 
 * The replacement string may use $n" to replace the nth capture group.
 */
public class RewriteRegexRule extends RegexRule
{
    private String _replacement;

    /* ------------------------------------------------------------ */
    public RewriteRegexRule()
    {
        _handling = false;
        _terminating = false;
    }

    /* ------------------------------------------------------------ */
    /**
     * Whenever a match is found, it replaces with this value.
     * 
     * @param replacement the replacement string.
     */
    public void setReplacement(String replacement)
    {
        _replacement = replacement;
    }


    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.jetty.handler.rules.RegexRule#apply(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.util.regex.Matcher)
     */
    public String apply(String target, HttpServletRequest request, HttpServletResponse response, Matcher matcher) throws IOException
    {
        target=_replacement;
        for (int g=1;g<=matcher.groupCount();g++)
        {
            String group = matcher.group(g);
            target=target.replaceAll("\\$"+g,group);
        }

        return target;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the replacement string.
     */
    public String toString()
    {
        return super.toString()+"["+_replacement+"]";
    }
}
