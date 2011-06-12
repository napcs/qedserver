//========================================================================
//$Id: CookiePatternRule.java 966 2008-04-17 13:53:44Z gregw $
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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Sets the cookie in the response whenever the rule finds a match.
 * 
 * @see Cookie
 */
public class CookiePatternRule extends PatternRule
{
    private String _name;
    private String _value;

    /* ------------------------------------------------------------ */
    public CookiePatternRule()
    {
        _handling = false;
        _terminating = false;
    }

    /* ------------------------------------------------------------ */
    /**
     * Assigns the cookie name.
     * 
     * @param name a <code>String</code> specifying the name of the cookie.
     */
    public void setName(String name)
    {
        _name = name;
    }

    /* ------------------------------------------------------------ */
    /**
     * Assigns the cookie value.
     * 
     * @param value a <code>String</code> specifying the value of the cookie
     * @see Cookie#setValue(String)
     */
    public void setValue(String value)
    {
        _value = value;
    }

    /* ------------------------------------------------------------ */
    /*
     * (non-Javadoc)
     * @see org.mortbay.jetty.handler.rules.RuleBase#apply(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public String apply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.addCookie(new Cookie(_name, _value));
        return target;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the cookie contents.
     */
    public String toString()
    {
        return super.toString()+"["+_name+","+_value + "]";
    }
}
