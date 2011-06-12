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

import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;

/* ------------------------------------------------------------ */
/** Client Certificate Authenticator.
 * This Authenticator uses a client certificate to authenticate the user.
 * Each client certificate supplied is tried against the realm using the
 * Principal name as the username and a string representation of the
 * certificate as the credential.
 * @author Greg Wilkins (gregw)
 */
public class ClientCertAuthenticator implements Authenticator
{
    private int _maxHandShakeSeconds =60;
    
    /* ------------------------------------------------------------ */
    public ClientCertAuthenticator()
    {
    }
    
    /* ------------------------------------------------------------ */
    public int getMaxHandShakeSeconds()
    {
        return _maxHandShakeSeconds;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param maxHandShakeSeconds Maximum time to wait for SSL handshake if
     * Client certification is required.
     */
    public void setMaxHandShakeSeconds(int maxHandShakeSeconds)
    {
        _maxHandShakeSeconds = maxHandShakeSeconds;
    }
    
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
        java.security.cert.X509Certificate[] certs =
            (java.security.cert.X509Certificate[])
            request.getAttribute("javax.servlet.request.X509Certificate");
            
        // Need certificates.
        if (certs==null || certs.length==0 || certs[0]==null)
        {
            if (response != null)
                response.sendError(HttpServletResponse.SC_FORBIDDEN,"A client certificate is required for accessing this web application but the server's listener is not configured for mutual authentication (or the client did not provide a certificate).");
            return null;
        }
        
        Principal principal = certs[0].getSubjectDN();
        if (principal==null)
            principal=certs[0].getIssuerDN();
        String username=principal==null?"clientcert":principal.getName();
        
        Principal user = realm.authenticate(username,certs,request);
        if (user == null)
        {
            if (response != null)
                response.sendError(HttpServletResponse.SC_FORBIDDEN,"The provided client certificate does not correspond to a trusted user.");
            return null;
        }
        
        request.setAuthType(Constraint.__CERT_AUTH);
        request.setUserPrincipal(user);                
        return user;
    }
    
    /* ------------------------------------------------------------ */
    public String getAuthMethod()
    {
        return Constraint.__CERT_AUTH;
    }

}
