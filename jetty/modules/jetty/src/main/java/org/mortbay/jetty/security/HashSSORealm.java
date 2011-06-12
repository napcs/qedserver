// ========================================================================
// Copyright 2003-2005 Mort Bay Consulting Pty. Ltd.
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

import java.security.Principal;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Random;

import javax.servlet.http.Cookie;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;




public class HashSSORealm implements SSORealm
{
    
    /* ------------------------------------------------------------ */
    public static final String SSO_COOKIE_NAME = "SSO_ID";
    private HashMap _ssoId2Principal = new HashMap();
    private HashMap _ssoUsername2Id = new HashMap();
    private HashMap _ssoPrincipal2Credential = new HashMap();
    private transient Random _random = new SecureRandom();
    
    /* ------------------------------------------------------------ */
    public Credential getSingleSignOn(Request request, Response response)
    {
        String ssoID = null;
        Cookie[] cookies = request.getCookies();
        for (int i = 0; cookies!=null && i < cookies.length; i++)
        {
            if (cookies[i].getName().equals(SSO_COOKIE_NAME))
            {
                ssoID = cookies[i].getValue();
                break;
            }
        }
        if(Log.isDebugEnabled())Log.debug("get ssoID="+ssoID);
        
        Principal principal=null;
        Credential credential=null;
        synchronized(_ssoId2Principal)
        {
            principal=(Principal)_ssoId2Principal.get(ssoID);
            credential=(Credential)_ssoPrincipal2Credential.get(principal);
        }
        
        if(Log.isDebugEnabled())Log.debug("SSO principal="+principal);
        
        if (principal!=null && credential!=null)
        {
            // TODO - make this work for non webapps
            UserRealm realm = ((WebAppContext)(request.getContext().getContextHandler())).getSecurityHandler().getUserRealm();
            Principal authPrincipal = realm.authenticate(principal.getName(), credential, request);
            if (authPrincipal != null)
            {
                request.setUserPrincipal(authPrincipal);
                return credential;
            }
            else
            {
                synchronized(_ssoId2Principal)
                {
                    _ssoId2Principal.remove(ssoID);
                    _ssoPrincipal2Credential.remove(principal);
                    _ssoUsername2Id.remove(principal.getName());
                }    
            }
        }
        return null;
    }
    
    
    /* ------------------------------------------------------------ */
    public void setSingleSignOn(Request request,
                                Response response,
                                Principal principal,
                                Credential credential)
    {
        
        String ssoID=null;
        
        synchronized(_ssoId2Principal)
        {
            // Create new SSO ID
            while (true)
            {
                ssoID = Long.toString(Math.abs(_random.nextLong()),
                                      30 + (int)(System.currentTimeMillis() % 7));
                if (!_ssoId2Principal.containsKey(ssoID))
                    break;
            }
            
            if(Log.isDebugEnabled())Log.debug("set ssoID="+ssoID);
            _ssoId2Principal.put(ssoID,principal);
            _ssoPrincipal2Credential.put(principal,credential);
            _ssoUsername2Id.put(principal.getName(),ssoID);
        }
        
        Cookie cookie = new Cookie(SSO_COOKIE_NAME, ssoID);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
    
    
    /* ------------------------------------------------------------ */
    public void clearSingleSignOn(String username)
    {
        synchronized(_ssoId2Principal)
        {
            Object ssoID=_ssoUsername2Id.remove(username);
            Object principal=_ssoId2Principal.remove(ssoID);
            _ssoPrincipal2Credential.remove(principal);
        }        
    }
}
