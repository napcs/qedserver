//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plus.jaas;

// ========================================================================
// $Id: SSOJAASUserRealm.java 1001 2008-02-01 09:31:51Z fred nizery $
//
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


import java.security.Principal;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.security.Credential;
import org.mortbay.jetty.security.SSORealm;




/* ---------------------------------------------------- */
/** SSOJAASUserRealm
 * <p>
 *
 * <p><h4>Notes</h4>
 * <p>
 *
 * <p><h4>Usage</h4>
 * For SSO realm that uses JAAS
 * The configuration must be the same as for JAASUserRealm plus
 * injection of an instance of class HashSSORealm using setSSORealm()
 * methode. This is intended to be used with the correct LoginModule
 * and its fitting .conf configuration file as described in JAAS documentation.
 *
 * @author Frederic Nizery <frederic.nizery@alcatel-lucent.fr>
 *
 * @org.apache.xbean.XBean element="ssoJaasUserRealm" description="Creates a UserRealm suitable for use with JAAS w/ support of SSO"
 */
public class SSOJAASUserRealm extends JAASUserRealm implements SSORealm
{
    private SSORealm _ssoRealm;

    /** Set the SSORealm.
     * A SSORealm implementation may be set to enable support for SSO.
     * @param ssoRealm The SSORealm to delegate single sign on requests to.
     */
    public void setSSORealm(SSORealm ssoRealm)
    {
        _ssoRealm = ssoRealm;
    }

    /* ------------------------------------------------------------ */
    public Credential getSingleSignOn(Request request,Response response)
    {
        if (_ssoRealm!=null)
            return _ssoRealm.getSingleSignOn(request,response);
        return null;
    }

    /* ------------------------------------------------------------ */
    public void setSingleSignOn(Request request,Response response,Principal principal,Credential credential)
    {
        if (_ssoRealm!=null)
            _ssoRealm.setSingleSignOn(request,response,principal,credential);
    }

    /* ------------------------------------------------------------ */
    public void clearSingleSignOn(String username)
    {
        if (_ssoRealm!=null)
            _ssoRealm.clearSingleSignOn(username);
    }

}
