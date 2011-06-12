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

import org.mortbay.component.LifeCycle;

/** 
 * A <code>RequestLog</code> can be attached to a {@link org.mortbay.jetty.handler.RequestLogHandler} to enable logging of requests/responses.
 * @author Nigel Canonizado
 * @see Server#setRequestLog
 */
public interface RequestLog extends LifeCycle
{
    public void log(Request request, Response response);
}
