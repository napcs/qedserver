//========================================================================
//$Id: Rule.java 966 2008-04-17 13:53:44Z gregw $
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
 * An abstract rule for creating rewrite rules.
 */
public abstract class Rule
{   
    protected boolean _terminating;
    protected boolean _handling;
    
    /**
     * This method calls tests the rule against the request/response pair and if the Rule 
     * applies, then the rule's action is triggered.
     * @param target The target of the request
     * @param request
     * @param response
     * 
     * @return The new target if the rule has matched, else null
     * @throws IOException TODO
     */
    public abstract String matchAndApply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException;   
    
    /**
     * Sets terminating to true or false.
     * If true, this rule will terminate the loop if this rule has been applied.
     * 
     * @param terminating
     */    
    public void setTerminating(boolean terminating)
    {
        _terminating = terminating;
    }
    
    /**
     * Returns the terminating flag value.
     * 
     * @return <code>true</code> if the rule needs to terminate; <code>false</code> otherwise. 
     */
    public boolean isTerminating()
    {
        return _terminating;
    }
    
    /**
     * Returns the handling flag value.
     * 
     * @return <code>true</code> if the rule handles the request and nested handlers should not be called.
     */
    public boolean isHandling()
    {
        return _handling;
    }
    
    /**
     * Set the handling flag value.
     * 
     * @param handling true if the rule handles the request and nested handlers should not be called.
     */
    public void setHandling(boolean handling)
    {
        _handling=handling;
    }
    
    /**
     * Returns the handling and terminating flag values.
     */
    public String toString()
    {
        return this.getClass().getName()+(_handling?"[H":"[h")+(_terminating?"T]":"t]");
    }
}