//========================================================================
//Copyright 2008 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.client.webdav;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.mortbay.io.Buffer;
import org.mortbay.jetty.client.HttpExchange;
import org.mortbay.log.Log;


public class PropfindExchange extends HttpExchange
{
    boolean _propertyExists = false;

    /* ------------------------------------------------------------ */
    protected void onResponseStatus(Buffer version, int status, Buffer reason) throws IOException
    {
        if ( status == HttpServletResponse.SC_OK )
        {
            Log.debug( "PropfindExchange:Status: Exists" );
            _propertyExists = true;
        }
        else
        {
            Log.debug( "PropfindExchange:Status: Not Exists" );
        }

        super.onResponseStatus(version, status, reason);
    }

    public boolean exists()
    {
        return _propertyExists;
    }
}