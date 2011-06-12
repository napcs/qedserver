//========================================================================
//$Id: RedirectPatternRule.java 966 2008-04-17 13:53:44Z gregw $
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Redirects the response whenever the rule finds a match.
 */
public class RedirectPatternRule extends PatternRule
{
    private String _location;

    /* ------------------------------------------------------------ */
    public RedirectPatternRule()
    {
        _handling = true;
        _terminating = true;
    }

    /* ------------------------------------------------------------ */
    /**
     * Sets the redirect location.
     * 
     * @param value the location to redirect.
     */
    public void setLocation(String value)
    {
        _location = value;
    }

    /* ------------------------------------------------------------ */
    /*
     * (non-Javadoc)
     * @see org.mortbay.jetty.handler.rules.RuleBase#apply(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public String apply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.sendRedirect(_location);
        return target;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the redirect location.
     */
    public String toString()
    {
        return super.toString()+"["+_location+"]";
    }
}
