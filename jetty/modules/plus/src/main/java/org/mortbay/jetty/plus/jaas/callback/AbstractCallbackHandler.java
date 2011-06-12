// ========================================================================
// $Id: AbstractCallbackHandler.java 305 2006-03-07 10:32:14Z janb $
// Copyright 2003-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plus.jaas.callback;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;


public abstract class AbstractCallbackHandler implements CallbackHandler
{
    protected String _userName;
    protected Object _credential;

    public void setUserName (String userName)
    {
        _userName = userName;
    }

    public String getUserName ()
    {
        return _userName;
    }
    

    public void setCredential (Object credential)
    {
        _credential = credential;
    }

    public Object getCredential ()
    {
        return _credential;
    }
    
    public  void handle (Callback[] callbacks)
        throws IOException, UnsupportedCallbackException
    {
    }
    
    
}
