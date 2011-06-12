//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.webapp.management;

import org.mortbay.jetty.handler.management.ContextHandlerMBean;
import org.mortbay.jetty.webapp.WebAppContext;

public class WebAppContextMBean extends ContextHandlerMBean
{

    public WebAppContextMBean(Object managedObject)
    {
        super(managedObject);
    }

    /* ------------------------------------------------------------ */
    public String getObjectNameBasis()
    {
        String basis = super.getObjectNameBasis();
        if (basis!=null)
            return basis;
        
        if (_managed!=null && _managed instanceof WebAppContext)
        {
            WebAppContext context = (WebAppContext)_managed;
            String name = context.getWar();
            if (name!=null)
                return name;
        }
        return null;
    }
}
