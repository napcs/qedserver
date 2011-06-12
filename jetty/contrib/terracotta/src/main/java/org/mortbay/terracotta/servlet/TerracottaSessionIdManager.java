// ========================================================================
// Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.SessionIdManager;
import org.mortbay.jetty.SessionManager;
import org.mortbay.jetty.servlet.AbstractSessionManager;
import org.mortbay.jetty.servlet.AbstractSessionManager.Session;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;

/**
 * A specialized SessionIdManager to be used with <a href="http://www.terracotta.org">Terracotta</a>.
 * See the {@link TerracottaSessionManager} javadocs for implementation notes.
 *
 * @see TerracottaSessionManager
 */
public class TerracottaSessionIdManager extends AbstractLifeCycle implements SessionIdManager
{
    private final static String __NEW_SESSION_ID = "org.mortbay.jetty.newSessionId";
    private final static String SESSION_ID_RANDOM_ALGORITHM = "SHA1PRNG";
    private final static String SESSION_ID_RANDOM_ALGORITHM_ALT = "IBMSecureRandom";
    private static final Object PRESENT = new Object();

    private final Server _server;
    private String _workerName;
    private Random _random;
    private boolean _weakRandom;
    private Map<String, Object> _sessionIds;

    public TerracottaSessionIdManager(Server server)
    {
        _server = server;
    }

    public void doStart()
    {
        if (_random == null)
        {
            try
            {
                _random = SecureRandom.getInstance(SESSION_ID_RANDOM_ALGORITHM);
            }
            catch (NoSuchAlgorithmException e)
            {
                try
                {
                    _random = SecureRandom.getInstance(SESSION_ID_RANDOM_ALGORITHM_ALT);
                    _weakRandom = false;
                }
                catch (NoSuchAlgorithmException e_alt)
                {
                    Log.warn("Could not generate SecureRandom for session-id randomness", e);
                    _random = new Random();
                    _weakRandom = true;
                }
            }
        }
        _random.setSeed(_random.nextLong() ^ System.currentTimeMillis() ^ hashCode() ^ Runtime.getRuntime().freeMemory());
        _sessionIds = newSessionIdsSet();
    }

    private Map<String, Object> newSessionIdsSet()
    {
        // We need a synchronized data structure to have node-local synchronization.
        // We use Hashtable because it is a natively synchronized collection that behaves
        // better in Terracotta than synchronized wrappers obtained with Collections.synchronized*().
        return new Hashtable();
    }

    public void doStop()
    {
    }

    public void addSession(HttpSession session)
    {
        String clusterId = ((TerracottaSessionManager.Session)session).getClusterId();
        // Use a unique constant object, because Strings are "copied" by Terracotta,
        // causing unnecessary traffic to the Terracotta server.
        _sessionIds.put(clusterId, PRESENT);
    }

    public String getWorkerName()
    {
        return _workerName;
    }

    public void setWorkerName(String workerName)
    {
        _workerName = workerName;
    }

    public boolean idInUse(String clusterId)
    {
        return _sessionIds.containsKey(clusterId);
    }

    /**
     * When told to invalidate all session instances that share the same id, we must
     * tell all contexts on the server for which it is defined to delete any session
     * object they might have matching the id.
     */
    public void invalidateAll(String clusterId)
    {
        Handler[] contexts = _server.getChildHandlersByClass(WebAppContext.class);
        for (int i = 0; contexts != null && i < contexts.length; i++)
        {
            WebAppContext webAppContext = (WebAppContext)contexts[i];
            SessionManager sessionManager = webAppContext.getSessionHandler().getSessionManager();
            if (sessionManager instanceof AbstractSessionManager)
            {
                Session session = ((AbstractSessionManager)sessionManager).getSession(clusterId);
                if (session != null) session.invalidate();
            }
        }
    }

    public String newSessionId(HttpServletRequest request, long created)
    {
        // Generate a unique cluster id. This id must be unique across all nodes in the cluster,
        // since it is stored in the distributed shared session ids set.

        // A requested session ID can only be used if it is in use already.
        String requested_id = request.getRequestedSessionId();
        if (requested_id != null && idInUse(requested_id))
            return requested_id;

        // Else reuse any new session ID already defined for this request.
        String new_id = (String)request.getAttribute(__NEW_SESSION_ID);
        if (new_id != null && idInUse(new_id))
            return new_id;

        // pick a new unique ID!
        String id = null;
        while (id == null || id.length() == 0 || idInUse(id))
        {
            long r = _weakRandom
                    ? (hashCode() ^ Runtime.getRuntime().freeMemory() ^ _random.nextInt() ^ (((long)request.hashCode()) << 32))
                    : _random.nextLong();
            r ^= created;
            if (request.getRemoteAddr() != null) r ^= request.getRemoteAddr().hashCode();
            if (r < 0) r = -r;
            id = Long.toString(r, 36);
        }

        request.setAttribute(__NEW_SESSION_ID, id);
        return id;
    }

    public void removeSession(HttpSession session)
    {
        String clusterId = ((TerracottaSessionManager.Session)session).getClusterId();
        _sessionIds.remove(clusterId);
    }

    public String getClusterId(String nodeId)
    {
        int dot = nodeId.lastIndexOf('.');
        return (dot > 0) ? nodeId.substring(0, dot) : nodeId;
    }

    public String getNodeId(String clusterId, HttpServletRequest request)
    {
        if (_workerName != null) return clusterId + '.' + _workerName;
        return clusterId;
    }
}
