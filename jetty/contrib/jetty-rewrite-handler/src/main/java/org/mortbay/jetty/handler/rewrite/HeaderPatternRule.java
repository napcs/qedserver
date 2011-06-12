//========================================================================
//$Id: HeaderPatternRule.java 966 2008-04-17 13:53:44Z gregw $
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


/**
 * Sets the header in the response whenever the rule finds a match.
 */
public class HeaderPatternRule extends PatternRule
{
    private String _name;
    private String _value;
    private boolean _add=false;

    /* ------------------------------------------------------------ */
    public HeaderPatternRule()
    {
        _handling = false;
        _terminating = false;
    }

    /* ------------------------------------------------------------ */
    /**
     * Sets the header name.
     * 
     * @param name name of the header field
     */
    public void setName(String name)
    {
        _name = name;
    }

    /* ------------------------------------------------------------ */
    /**
     * Sets the header value. The value can be either a <code>String</code> or <code>int</code> value.
     * 
     * @param value of the header field
     */
    public void setValue(String value)
    {
        _value = value;
    }

    /* ------------------------------------------------------------ */
    /**
     * Sets the Add flag. 
     * @param add If true, the header is added to the response, otherwise the header it is set on the response.
     */
    public void setAdd(boolean add)
    {
        _add = add;
    }

    /* ------------------------------------------------------------ */
    /**
     * Invokes this method when a match found. If the header had already been set, 
     * the new value overwrites the previous one. Otherwise, it adds the new 
     * header name and value.
     * 
     *@see org.mortbay.jetty.handler.rewrite.Rule#matchAndApply(String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public String apply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        // process header
        if (_add)
            response.addHeader(_name, _value);
        else
            response.setHeader(_name, _value); 
        return target;
    }
    
    

    /* ------------------------------------------------------------ */
    /**
     * Returns the header name.
     * @return the header name.
     */
    public String getName()
    {
        return _name;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the header value.
     * @return the header value.
     */
    public String getValue()
    {
        return _value;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the add flag value.
     */
    public boolean isAdd()
    {
        return _add;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the header contents.
     */
    public String toString()
    {
        return super.toString()+"["+_name+","+_value+"]";
    }
}
