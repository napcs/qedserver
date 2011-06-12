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

package org.mortbay.jetty.ajp;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;

public class Ajp13Request extends Request
{
    protected String _remoteAddr;
    protected String _remoteHost;
    protected String _remoteUser;
    protected boolean _sslSecure;

    /* ------------------------------------------------------------ */
    public Ajp13Request()
    {
        super();
        _remoteAddr = null;
        _remoteHost = null;
        _remoteUser = null;        
        _sslSecure = false;
    }

    /* ------------------------------------------------------------ */
    protected void setConnection(HttpConnection connection)
    {
        super.setConnection(connection);
    }

    /* ------------------------------------------------------------ */
    public void setRemoteUser(String remoteUser)
    {
        _remoteUser = remoteUser;
    }

    /* ------------------------------------------------------------ */
    public String getRemoteUser()
    {
        if(_remoteUser != null)
            return _remoteUser;
        return super.getRemoteUser();
    }

    /* ------------------------------------------------------------ */
    public String getRemoteAddr()
    {
        if (_remoteAddr != null)
            return _remoteAddr;
        if (_remoteHost != null)
            return _remoteHost;
        return super.getRemoteAddr();
    }



    /* ------------------------------------------------------------ */
    public void setRemoteAddr(String remoteAddr)
    {
        _remoteAddr = remoteAddr;
    }

    /* ------------------------------------------------------------ */
    public String getRemoteHost()
    {
        if (_remoteHost != null)
            return _remoteHost;
        if (_remoteAddr != null)
            return _remoteAddr;
        return super.getRemoteHost();
    }

    /* ------------------------------------------------------------ */
    public void setRemoteHost(String remoteHost)
    {
        _remoteHost = remoteHost;
    }

    /* ------------------------------------------------------------ */
    public boolean isSslSecure()
    {
        return _sslSecure;
    }

    /* ------------------------------------------------------------ */
    public void setSslSecure(boolean sslSecure)
    {
        _sslSecure = sslSecure;
    }

    /* ------------------------------------------------------------ */
    protected void recycle()
    {
        super.recycle();
        _remoteAddr = null;
        _remoteHost = null;
        _remoteUser = null;
        _sslSecure = false;
    }

}
