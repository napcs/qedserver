//========================================================================
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty;

import java.io.IOException;

public class HttpException extends IOException
{
    int _status;
    String _reason;

    /* ------------------------------------------------------------ */
    public HttpException(int status)
    {
        _status=status;
        _reason=null;
    }

    /* ------------------------------------------------------------ */
    public HttpException(int status,String reason)
    {
        _status=status;
        _reason=reason;
    }

    /* ------------------------------------------------------------ */
    protected HttpException(int status,String reason, Throwable rootCause)
    {
        _status=status;
        _reason=reason;
        initCause(rootCause);
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the reason.
     */
    public String getReason()
    {
        return _reason;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param reason The reason to set.
     */
    public void setReason(String reason)
    {
        _reason = reason;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the status.
     */
    public int getStatus()
    {
        return _status;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param status The status to set.
     */
    public void setStatus(int status)
    {
        _status = status;
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return ("HttpException("+_status+","+_reason+","+super.getCause()+")");
    }
    
    
}
