//========================================================================
//$Id: RequestParameterCallback.java 305 2006-03-07 10:32:14Z janb $
//Copyright 2000-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plus.jaas.callback;

import java.util.List;

import javax.security.auth.callback.Callback;


/**
 * 
 * RequestParameterCallback
 * 
 * Allows a JAAS callback handler to access any parameter from the j_security_check FORM.
 * This means that a LoginModule can access form fields other than the j_username and j_password
 * fields, and use it, for example, to authenticate a user.
 *
 * @author janb
 * @version $Revision: 305 $ $Date: 2006-03-07 21:32:14 +1100 (Tue, 07 Mar 2006) $
 *
 */
public class RequestParameterCallback implements Callback
{
    private String paramName;
    private List paramValues;
    
    public void setParameterName (String name)
    {
        paramName = name;
    }
    public String getParameterName ()
    {
        return paramName;
    }
    
    public void setParameterValues (List values)
    {
        paramValues = values;
    }
    
    public List getParameterValues ()
    {
        return paramValues;
    }
}
