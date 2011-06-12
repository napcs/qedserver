//========================================================================
//Copyright 2007 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.gwt;

import javax.servlet.http.HttpServletRequest;

import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

public class HelloWorldServiceImpl extends AsyncRemoteServiceServlet implements HelloWorldService
{
    
    private static final long serialVersionUID = 1L;

    public String sayHello(String sender) 
    {        
        HttpServletRequest request = getThreadLocalRequest();
        Continuation continuation = ContinuationSupport.getContinuation(request, null);
        if(continuation.isNew() || !continuation.isPending())
        {
            request.setAttribute("ts", Long.valueOf(System.currentTimeMillis()));
            continuation.suspend(5000);        
        }
        long elapsed = System.currentTimeMillis() - ((Long)request.getAttribute("ts")).longValue();
        return "Hello world *" + sender + "* resumed after " + elapsed + " ms";
    }    

}
