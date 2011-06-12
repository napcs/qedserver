//========================================================================
//$Id: ResponsePatternRule.java 966 2008-04-17 13:53:44Z gregw $
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
 * Sends the response code whenever the rule finds a match.
 */
public class ResponsePatternRule extends PatternRule
{
    private String _code;
    private String _reason = "";

    /* ------------------------------------------------------------ */
    public ResponsePatternRule()
    {
        _handling = true;
        _terminating = true;
    }

    /* ------------------------------------------------------------ */
    /**
     * Sets the response status code. 
     * @param code response code
     */
    public void setCode(String code)
    {
        _code = code;
    }

    /* ------------------------------------------------------------ */
    /**
     * Sets the reason for the response status code. Reasons will only reflect
     * if the code value is greater or equal to 400.
     * 
     * @param reason
     */
    public void setReason(String reason)
    {
        _reason = reason;
    }

    /* ------------------------------------------------------------ */
    /*
     * (non-Javadoc)
     * @see org.mortbay.jetty.handler.rules.RuleBase#apply(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public String apply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        int code = Integer.parseInt(_code);

        // status code 400 and up are error codes
        if (code >= 400)
        {
            response.sendError(code, _reason);
        }
        else
        {
            response.setStatus(code);
        }
        return target;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the code and reason string.
     */
    public String toString()
    {
        return super.toString()+"["+_code+","+_reason+"]";
    }
}
