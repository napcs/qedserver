// ========================================================================
// Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.terracotta.servlet;

import java.util.concurrent.TimeUnit;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.SessionIdManager;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.SessionHandler;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * @version $Revision: 1319 $ $Date: 2008-11-14 10:55:54 +1100 (Fri, 14 Nov 2008) $
 */
public class TerracottaJettyServer
{
    private final Server server;
    private final int maxInactivePeriod;
    private final int scavengePeriod;
    private final ContextHandlerCollection contexts;
    private SessionIdManager sessionIdManager;

    public TerracottaJettyServer(int port)
    {
        this(port, 30, 10);
    }

    public TerracottaJettyServer(int port, int maxInactivePeriod, int scavengePeriod)
    {
        this.server = new Server(port);
        this.maxInactivePeriod = maxInactivePeriod;
        this.scavengePeriod = scavengePeriod;
        this.contexts = new ContextHandlerCollection();
        this.sessionIdManager = new TerracottaSessionIdManager(server);
    }

    public void start() throws Exception
    {
        // server -> contexts collection -> context handler -> session handler -> servlet handler
        server.setHandler(contexts);
        server.start();
    }

    public Context addContext(String contextPath)
    {
        Context context = new Context(contexts, contextPath);

        TerracottaSessionManager sessionManager = new TestTerracottaSessionManager();
        sessionManager.setIdManager(sessionIdManager);
        sessionManager.setMaxInactiveInterval(maxInactivePeriod);
        sessionManager.setScavengePeriodMs(TimeUnit.SECONDS.toMillis(scavengePeriod));

        SessionHandler sessionHandler = new TerracottaSessionHandler(sessionManager);
        sessionManager.setSessionHandler(sessionHandler);
        context.setSessionHandler(sessionHandler);

        return context;
    }

    public void stop() throws Exception
    {
        server.stop();
    }

    public WebAppContext addWebAppContext(String warPath, String contextPath)
    {
        WebAppContext context = new WebAppContext(contexts, warPath, contextPath);

        TerracottaSessionManager sessionManager = new TestTerracottaSessionManager();
        sessionManager.setIdManager(sessionIdManager);
        sessionManager.setMaxInactiveInterval(maxInactivePeriod);
        sessionManager.setScavengePeriodMs(TimeUnit.SECONDS.toMillis(scavengePeriod));

        SessionHandler sessionHandler = new TerracottaSessionHandler(sessionManager);
        sessionManager.setSessionHandler(sessionHandler);
        context.setSessionHandler(sessionHandler);

        return context;
	}

    public static class TestTerracottaSessionManager extends TerracottaSessionManager
    {
        private static final ThreadLocal<Integer> depth = new ThreadLocal<Integer>()
        {
            @Override
            protected Integer initialValue()
            {
                return 0;
            }
        };

        @Override
        public void enter(Request request)
        {
            depth.set(depth.get() + 1);
            super.enter(request);
        }

        @Override
        public void exit(Request request)
        {
            super.exit(request);
            depth.set(depth.get() - 1);
            if (depth.get() == 0)
            {
                assert Lock.getLocks().size() == 0 : Lock.getLocks();
            }
        }
    }
}
