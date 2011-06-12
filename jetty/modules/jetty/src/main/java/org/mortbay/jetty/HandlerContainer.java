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

public interface HandlerContainer extends LifeCycle
{
    public void addHandler(Handler handler);
    public void removeHandler(Handler handler);

    public Handler[] getChildHandlers();
    public Handler[] getChildHandlersByClass(Class byclass);
    public Handler getChildHandlerByClass(Class byclass);
}
