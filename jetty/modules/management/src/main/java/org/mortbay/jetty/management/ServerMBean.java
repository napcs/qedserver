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

package org.mortbay.jetty.management;

import java.util.Arrays;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.management.ObjectMBean;

/**
 *
 */
public class ServerMBean extends ObjectMBean
{
    private final long startupTime;
    private final Server server;

    public ServerMBean(Object managedObject)
    {
        super(managedObject);
        startupTime = System.currentTimeMillis();
        server = (Server)managedObject;
    }

    public Handler[] getContexts()
    {
        return server.getChildHandlersByClass(ContextHandler.class);
    }

    public long getStartupTime()
    {
        return startupTime;
    }
}
