//========================================================================
//$Id: RegexRule.java 966 2008-04-17 13:53:44Z gregw $
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
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Abstract rule to use as a base class for rules that match with a regular expression.
 */
public abstract class RegexRule extends Rule
{
    protected Pattern _regex; 

    /* ------------------------------------------------------------ */
    /**
     * Sets the regular expression string used to match with string URI.
     * 
     * @param regex the regular expression.
     */
    public void setRegex(String regex)
    {
        _regex=Pattern.compile(regex);
    }

    /* ------------------------------------------------------------ */
    /**
     * @return get the regular expression
     */
    public String getRegex()
    {
        return _regex==null?null:_regex.pattern();
    }
    

    /* ------------------------------------------------------------ */
    public String matchAndApply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        Matcher matcher=_regex.matcher(target);
        boolean matches = matcher.matches();
        if (matches)
            return apply(target,request,response, matcher);
        return null;
    }

    /* ------------------------------------------------------------ */
    /** 
     * Apply this rule to the request/response pair.
     * Called by {@link #matchAndApply(String, HttpServletRequest, HttpServletResponse)} if the regex matches.
     * @param target field to attempt match
     * @param request request object
     * @param response response object
     * @param matcher The Regex matcher that matched the request (with capture groups available for replacement).
     * @return The target (possible updated).
     * @throws IOException exceptions dealing with operating on request or response objects
     */
    protected abstract String apply(String target, HttpServletRequest request, HttpServletResponse response, Matcher matcher) throws IOException;
    

    /* ------------------------------------------------------------ */
    /**
     * Returns the regular expression string.
     */
    public String toString()
    {
        return super.toString()+"["+_regex+"]";
    }
}
