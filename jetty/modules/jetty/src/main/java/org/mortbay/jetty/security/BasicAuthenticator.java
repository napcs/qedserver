// ========================================================================
// Copyright 2002-2005 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.jetty.security;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.log.Log;
import org.mortbay.util.StringUtil;

/* ------------------------------------------------------------ */
/** BASIC authentication.
 *
 * @author Greg Wilkins (gregw)
 */
public class BasicAuthenticator implements Authenticator
{
    /* ------------------------------------------------------------ */
    /** 
     * @return UserPrinciple if authenticated or null if not. If
     * Authentication fails, then the authenticator may have committed
     * the response as an auth challenge or redirect.
     * @exception IOException 
     */
    public Principal authenticate(UserRealm realm,
            String pathInContext,
            Request request,
            Response response)
    throws IOException
    {
        // Get the user if we can
        Principal user=null;
        String credentials = request.getHeader(HttpHeaders.AUTHORIZATION);
        
        if (credentials!=null )
        {
            try
            {
                if(Log.isDebugEnabled())Log.debug("Credentials: "+credentials);
                credentials = credentials.substring(credentials.indexOf(' ')+1);
                credentials = B64Code.decode(credentials,StringUtil.__ISO_8859_1);
                int i = credentials.indexOf(':');
                String username = credentials.substring(0,i);
                String password = credentials.substring(i+1);
                user = realm.authenticate(username,password,request);
                
                if (user==null)
                {
                    Log.warn("AUTH FAILURE: user {}",StringUtil.printable(username));
                }
                else
                {
                    request.setAuthType(Constraint.__BASIC_AUTH);
                    request.setUserPrincipal(user);                
                }
            }
            catch (Exception e)
            {
                Log.warn("AUTH FAILURE: "+e.toString());
                Log.ignore(e);
            }
        }

        // Challenge if we have no user
        if (user==null && response!=null)
            sendChallenge(realm,response);
        
        return user;
    }
    
    /* ------------------------------------------------------------ */
    public String getAuthMethod()
    {
        return Constraint.__BASIC_AUTH;
    }

    /* ------------------------------------------------------------ */
    public void sendChallenge(UserRealm realm,Response response)
        throws IOException
    {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\""+realm.getName()+'"');
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
    
}
    
