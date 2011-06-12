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

import org.mortbay.io.Buffer;
import org.mortbay.jetty.client.HttpExchange;
import org.mortbay.log.Log;


public class WebdavSupportedExchange extends HttpExchange
{
    private boolean _webdavSupported = false;
    private boolean _isComplete = false;

    protected void onResponseHeader(Buffer name, Buffer value) throws IOException
    {
        if (Log.isDebugEnabled())
            Log.debug("WebdavSupportedExchange:Header:" + name.toString() + " / " + value.toString() );
        if ( "DAV".equals( name.toString() ) )
        {
            if ( value.toString().indexOf( "1" ) >= 0 || value.toString().indexOf( "2" ) >= 0 )
            {
                _webdavSupported = true;
            }
        }

        super.onResponseHeader(name, value);
    }

    public void waitTilCompletion() throws InterruptedException
    {
        synchronized (this)
        {
            while ( !_isComplete)
            {
                this.wait();
            }
        }
    }

    protected void onResponseComplete() throws IOException
    {
        _isComplete = true;

        super.onResponseComplete();
    }

    public boolean isWebdavSupported()
    {
        return _webdavSupported;
    }
}
