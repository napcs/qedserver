//========================================================================
//Copyright 2006-2008 Mort Bay Consulting Pty. Ltd.
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

/**
 * 
 * @author jesse
 * 
 */
public interface HttpEventListener
{

    // TODO review the methods here, we can probably trim these down on what to expose
    
    public void onRequestCommitted() throws IOException;


    public void onRequestComplete() throws IOException;


    public void onResponseStatus(Buffer version, int status, Buffer reason) throws IOException;


    public void onResponseHeader(Buffer name, Buffer value) throws IOException;

    
    public void onResponseHeaderComplete() throws IOException;

    
    public void onResponseContent(Buffer content) throws IOException;


    public void onResponseComplete() throws IOException;


    public void onConnectionFailed(Throwable ex);


    public void onException(Throwable ex);


    public void onExpire();
    
    public void onRetry();
    

}
