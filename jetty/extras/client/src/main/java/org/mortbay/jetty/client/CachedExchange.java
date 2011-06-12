//========================================================================
//Copyright 2006-2007 Mort Bay Consulting Pty. Ltd.
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
package org.mortbay.jetty.client;

import java.io.IOException;

import org.mortbay.io.Buffer;
import org.mortbay.jetty.HttpFields;

/**
 * An exchange that caches response status and fields for later use.
 * 
 * @author gregw
 *
 */
public class CachedExchange extends HttpExchange
{
    protected int _responseStatus;
    protected HttpFields _responseFields;

    public CachedExchange(boolean cacheFields)
    {
        if (cacheFields)
            _responseFields = new HttpFields();
    }

    /* ------------------------------------------------------------ */
    public int getResponseStatus()
    {
        if (getStatus() < HttpExchange.STATUS_PARSING_HEADERS)
            throw new IllegalStateException("Response not received");
        return _responseStatus;
    }

    /* ------------------------------------------------------------ */
    public HttpFields getResponseFields()
    {
        if (getStatus() < HttpExchange.STATUS_PARSING_HEADERS)
            throw new IllegalStateException("Headers not complete");
        return _responseFields;
    }

    /* ------------------------------------------------------------ */
    protected void onResponseStatus(Buffer version, int status, Buffer reason) throws IOException
    {
        _responseStatus = status;
        super.onResponseStatus(version,status,reason);
    }

    /* ------------------------------------------------------------ */
    protected void onResponseHeader(Buffer name, Buffer value) throws IOException
    {
        if (_responseFields != null)
            _responseFields.add(name,value);
        super.onResponseHeader(name,value);
    }

}