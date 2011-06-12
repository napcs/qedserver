//========================================================================
//Copyright 2006-2008 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.client.security;


import java.io.IOException;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.client.HttpExchange;
import org.mortbay.jetty.security.B64Code;
import org.mortbay.util.StringUtil;

/**
 * Sets authentication headers for BASIC authentication challenges
 * 
 * @author jesse
 */
public class BasicAuthorization implements Authorization
{
    private Buffer _authorization;
    
    public BasicAuthorization(Realm realm) throws IOException
    {
        String authenticationString = "Basic " + B64Code.encode( realm.getPrincipal() + ":" + realm.getCredentials(), StringUtil.__ISO_8859_1);
        _authorization= new ByteArrayBuffer(authenticationString);
    }
    
    /**
     * BASIC authentication is of the form
     * 
     * encoded credentials are of the form: username:password
     * 
     * 
     */
    public void setCredentials( HttpExchange exchange ) throws IOException
    {
        exchange.setRequestHeader( HttpHeaders.AUTHORIZATION_BUFFER, _authorization);
    }
}
