//========================================================================
//$Id: ContinuationSupport.java,v 1.1 2005/11/14 17:45:56 gregwilkins Exp $
//Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.util.ajax;

import javax.servlet.http.HttpServletRequest;

/* ------------------------------------------------------------ */
/** ContinuationSupport.
 * Conveniance class to avoid classloading visibility issues.
 * @author gregw
 *
 */
public class ContinuationSupport
{
    public static Continuation getContinuation(HttpServletRequest request, Object lock)
    {
        Continuation continuation = (Continuation) request.getAttribute("org.mortbay.jetty.ajax.Continuation");
        if (continuation==null)
            continuation=new WaitingContinuation(lock);
        else if (continuation instanceof WaitingContinuation  && lock!=null)
            ((WaitingContinuation)continuation).setMutex(lock);
        return continuation;
    }
}
