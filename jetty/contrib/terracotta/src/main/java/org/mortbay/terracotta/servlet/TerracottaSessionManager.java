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

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.servlet.AbstractSessionManager;
import org.mortbay.log.Log;

/**
 * A specialized SessionManager to be used with <a href="http://www.terracotta.org">Terracotta</a>.
 * <br />
 * <h3>IMPLEMENTATION NOTES</h3>
 * <h4>Requirements</h4>
 * This implementation of the session management requires J2SE 5 or superior.
 * <h4>Use of Hashtable</h4>
 * In Terracotta, collections classes are
 * <a href="http://www.terracotta.org/web/display/docs/Concept+and+Architecture+Guide">logically managed</a>
 * and we need two levels of locking: a local locking to handle concurrent requests on the same node
 * and a distributed locking to handle concurrent requests on different nodes.
 * Natively synchronized classes such as Hashtable fit better than synchronized wrappers obtained via, for
 * example, {@link Collections#synchronizedMap(Map)}. This is because Terracotta may replay the method call
 * on the inner unsynchronized collection without invoking the external wrapper, so the synchronization will
 * be lost. Natively synchronized collections does not have this problem.
 * <h4>Use of Hashtable as a Set</h4>
 * There is no natively synchronized Set implementation, so we use Hashtable instead, see
 * {@link TerracottaSessionIdManager}.
 * However, we don't map the session id to itself, because Strings are treated specially by Terracotta,
 * causing more traffic to the Terracotta server. Instead we use the same pattern used in the implementation
 * of <code>java.util.HashSet</code>: use a single shared object to indicate the presence of a key.
 * This is necessary since Hashtable does not allow null values.
 * <h4>Sessions expiration map</h4>
 * In order to scavenge expired sessions, we need a way to know if they are expired. This information
 * is normally held in the session itself via the <code>lastAccessedTime</code> property.
 * However, we would need to iterate over all sessions to check if each one is expired, and this migrates
 * all sessions to the node, causing a lot of unneeded traffic between nodes and the Terracotta server.
 * To avoid this, we keep a separate map from session id to expiration time, so we only need to migrate
 * all the expirations times to see if a session is expired or not.
 * <h4>Update of lastAccessedTime</h4>
 * As a performance improvement, the lastAccessedTime is updated only periodically, and not every time
 * a request enters a node. This optimization allows applications that have frequent requests but less
 * frequent accesses to the session to perform better, because the traffic between the node and the
 * Terracotta server is reduced. The update period is the scavenger period, see {@link Session#access(long)}.
 * <h4>Terracotta lock id</h4>
 * The Terracotta lock id is based on the session id, but this alone is not sufficient, as there may be
 * two sessions with the same id for two different contexts. So we need session id and context path.
 * However, this also is not enough, as we may have the rare case of the same webapp mapped to two different
 * virtual hosts, and each virtual host must have a different session object.
 * Therefore the lock id we need to use is a combination of session id, context path and virtual host, see
 * {@link #newLockId(String)}.
 *
 * @see TerracottaSessionIdManager
 */
public class TerracottaSessionManager extends AbstractSessionManager implements Runnable
{
    /**
     * The local cache of session objects.
     */
    private Map<String, Session> _sessions;
    /**
     * The distributed shared SessionData map.
     * Putting objects into the map result in the objects being sent to Terracotta, and any change
     * to the objects are also replicated, recursively.
     * Getting objects from the map result in the objects being fetched from Terracotta.
     */
    private Hashtable<String, SessionData> _sessionDatas;
    /**
     * The distributed shared session expirations map, needed for scavenging.
     * In particular it supports removal of sessions that have been orphaned by nodeA
     * (for example because it crashed) by virtue of scavenging performed by nodeB.
     */
    private Hashtable<String, MutableLong> _sessionExpirations;
    private String _contextPath;
    private String _virtualHost;
    private long _scavengePeriodMs = 30000;
    private ScheduledExecutorService _scheduler;
    private ScheduledFuture<?> _scavenger;

    public void doStart() throws Exception
    {
        super.doStart();

        _contextPath = canonicalize(_context.getContextPath());
        _virtualHost = virtualHostFrom(_context);

        _sessions = Collections.synchronizedMap(new HashMap<String, Session>());
        _sessionDatas = newSharedMap("sessionData:" + _contextPath + ":" + _virtualHost);
        _sessionExpirations = newSharedMap("sessionExpirations:" + _contextPath + ":" + _virtualHost);
        _scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduleScavenging();
    }

    private Hashtable newSharedMap(String name)
    {
        // We want to partition the session data among contexts, so we need to have different roots for
        // different contexts, and each root must have a different name, since roots with the same name are shared.
        Lock.lock(name);
        try
        {
            // We need a synchronized data structure to have node-local synchronization.
            // We use Hashtable because it is a natively synchronized collection that behaves
            // better in Terracotta than synchronized wrappers obtained with Collections.synchronized*().
            Hashtable result = (Hashtable)ManagerUtil.lookupOrCreateRootNoDepth(name, new Hashtable());
            ((Manageable)result).__tc_managed().disableAutoLocking();
            return result;
        }
        finally
        {
            Lock.unlock(name);
        }
    }

    private void scheduleScavenging()
    {
        if (_scavenger != null)
        {
            _scavenger.cancel(false);
            _scavenger = null;
        }
        long scavengePeriod = getScavengePeriodMs();
        if (scavengePeriod > 0 && _scheduler != null)
            _scavenger = _scheduler.scheduleWithFixedDelay(this, scavengePeriod, scavengePeriod, TimeUnit.MILLISECONDS);
    }

    public void doStop() throws Exception
    {
        if (_scavenger != null) _scavenger.cancel(true);
        if (_scheduler != null) _scheduler.shutdownNow();
        super.doStop();
    }

    public void run()
    {
        scavenge();
    }

    public void enter(Request request)
    {
        /**
         * SESSION LOCKING
         * This is an entry point for session locking.
         * We arrive here at the beginning of every request
         */

        String requestedSessionId = request.getRequestedSessionId();
        HttpSession session = request.getSession(false);
        Log.debug("Entering, requested session id {}, session id {}", requestedSessionId, session == null ? null : getClusterId(session));
        if (requestedSessionId == null)
        {
            // The request does not have a session id, do not lock.
            // If the session, later in the request, is created by the user,
            // it will be locked when it will be created
        }
        else
        {
            // We lock anyway with the requested session id.
            // The requested session id may not be a valid one,
            // for example because the session expired.
            // If the user creates a new session, it will have
            // a different session id and that also will be locked.
            enter(getIdManager().getClusterId(requestedSessionId));
        }
    }

    protected void enter(String clusterId)
    {
        Lock.lock(newLockId(clusterId));
        Log.debug("Entered, session id {}", clusterId);
    }

    protected boolean tryEnter(String clusterId)
    {
        return Lock.tryLock(newLockId(clusterId));
    }

    public void exit(Request request)
    {
        /**
         * SESSION LOCKING
         * This is an exit point for session locking.
         * We arrive here at the end of every request
         */

        String requestedSessionId = request.getRequestedSessionId();
        HttpSession session = request.getSession(false);
        Log.debug("Exiting, requested session id {}, session id {}", requestedSessionId, session == null ? null : getClusterId(session));
        if (requestedSessionId == null)
        {
            if (session == null)
            {
                // No session has been created in the request, just return
            }
            else
            {
                // A new session has been created by the user, unlock it
                exit(getClusterId(session));
            }
        }
        else
        {
            // There was a requested session id, and we locked it, so here release it
            String requestedClusterId = getIdManager().getClusterId(requestedSessionId);
            exit(requestedClusterId);

            if (session != null)
            {
                if (!requestedClusterId.equals(getClusterId(session)))
                {
                    // The requested session id was invalid, and a
                    // new session has been created by the user with
                    // a different session id, unlock it
                    exit(getClusterId(session));
                }
            }
        }
    }

    protected void exit(String clusterId)
    {
        Lock.unlock(newLockId(clusterId));
        Log.debug("Exited, session id {}", clusterId);
    }

    protected void addSession(AbstractSessionManager.Session session)
    {
        /**
         * SESSION LOCKING
         * When this method is called, we already hold the session lock.
         * See {@link #newSession(HttpServletRequest)}
         */
        String clusterId = getClusterId(session);
        Session tcSession = (Session)session;
        SessionData sessionData = tcSession.getSessionData();
        _sessionExpirations.put(clusterId, sessionData._expiration);
        _sessionDatas.put(clusterId, sessionData);
        _sessions.put(clusterId, tcSession);
        Log.debug("Added session {} with id {}", tcSession, clusterId);
    }

    @Override
    public Cookie access(HttpSession session, boolean secure)
    {
        Cookie cookie = super.access(session, secure);
        Log.debug("Accessed session {} with id {}", session, session.getId());
        return cookie;
    }

    @Override
    public void complete(HttpSession session)
    {
        super.complete(session);
        Log.debug("Completed session {} with id {}", session, session.getId());
    }

    protected void removeSession(String clusterId)
    {
        /**
         * SESSION LOCKING
         * When this method is called, we already hold the session lock.
         * Either the scavenger acquired it, or the user invalidated
         * the existing session and thus {@link #enter(String)} was called.
         */

        // Remove locally cached session
        Session session = _sessions.remove(clusterId);
        Log.debug("Removed session {} with id {}", session, clusterId);

        // It may happen that one node removes its expired session data,
        // so that when this node does the same, the session data is already gone
        SessionData sessionData = _sessionDatas.remove(clusterId);
        Log.debug("Removed session data {} with id {}", sessionData, clusterId);

        // Remove the expiration entry used in scavenging
        _sessionExpirations.remove(clusterId);
    }

    public void setScavengePeriodMs(long ms)
    {
        ms = ms == 0 ? 60000: ms;
        ms = ms > 60000 ? 60000: ms;
        ms = ms < 1000 ? 1000: ms;
        this._scavengePeriodMs = ms;
        scheduleScavenging();
    }

    public long getScavengePeriodMs()
    {
        return _scavengePeriodMs;
    }

    public AbstractSessionManager.Session getSession(String clusterId)
    {
        Session result = null;

        /**
         * SESSION LOCKING
         * This is an entry point for session locking.
         * We lookup the session given the id, and if it exist we hold the lock.
         * We unlock on end of method, since this method can be called outside
         * an {@link #enter(String)}/{@link #exit(String)} pair.
         */
        enter(clusterId);
        try
        {
            // Need to synchronize because we use a get-then-put that must be atomic
            // on the local session cache
            // Refer to method {@link #scavenge()} for an explanation of synchronization order:
            // first on _sessions, then on _sessionExpirations.
            synchronized (_sessions)
            {
                result = _sessions.get(clusterId);
                if (result == null)
                {
                    Log.debug("Session with id {} --> local cache miss", clusterId);

                    // Lookup the distributed shared sessionData object.
                    // This will migrate the session data to this node from the Terracotta server
                    // We have not grabbed the distributed lock associated with this session yet,
                    // so another node can migrate the session data as well. This is no problem,
                    // since just after this method returns the distributed lock will be grabbed by
                    // one node, the session data will be changed and the lock released.
                    // The second node contending for the distributed lock will then acquire it,
                    // and the session data information will be migrated lazily by Terracotta means.
                    // We are only interested in having a SessionData reference locally.
                    Log.debug("Distributed session data with id {} --> lookup", clusterId);
                    SessionData sessionData = _sessionDatas.get(clusterId);
                    if (sessionData == null)
                    {
                        Log.debug("Distributed session data with id {} --> not found", clusterId);
                    }
                    else
                    {
                        Log.debug("Distributed session data with id {} --> found", clusterId);
                        // Wrap the migrated session data and cache the Session object
                        result = new Session(sessionData);
                        _sessions.put(clusterId, result);
                    }
                }
                else
                {
                    Log.debug("Session with id {} --> local cache hit", clusterId);
                    if (!_sessionExpirations.containsKey(clusterId))
                    {
                        // A session is present in the local cache, but it has been expired
                        // or invalidated on another node, perform local clean up.
                        _sessions.remove(clusterId);
                        result = null;
                        Log.debug("Session with id {} --> local cache stale");
                    }
                }
            }
        }
        finally
        {
            /**
             * SESSION LOCKING
             */
            exit(clusterId);
        }
        return result;
    }

    protected String newLockId(String clusterId)
    {
        StringBuilder builder = new StringBuilder(clusterId);
        builder.append(":").append(_contextPath);
        builder.append(":").append(_virtualHost);
        return builder.toString();
    }

    // TODO: This method is not needed, only used for testing
    public Map getSessionMap()
    {
        return Collections.unmodifiableMap(_sessions);
    }

    // TODO: rename to getSessionsCount()
    // TODO: also, not used if not by superclass for unused statistics data
    public int getSessions()
    {
        return _sessions.size();
    }

    protected Session newSession(HttpServletRequest request)
    {
        /**
         * SESSION LOCKING
         * This is an entry point for session locking.
         * We arrive here when we have to create a new
         * session, for a request.getSession(true) call.
         */
        Session result = new Session(request);

        String requestedSessionId = request.getRequestedSessionId();
        if (requestedSessionId == null)
        {
            // Here the user requested a fresh new session, lock it.
            enter(result.getClusterId());
        }
        else
        {
            if (result.getClusterId().equals(getIdManager().getClusterId(requestedSessionId)))
            {
                // Here we have a cross context dispatch where the same session id
                // is used for two different sessions; we do not lock because the lock
                // has already been acquired in enter(Request), based on the requested
                // session id.
            }
            else
            {
                // Here the requested session id is invalid (the session expired),
                // and a new session is created, lock it.
                enter(result.getClusterId());
            }
        }
        return result;
    }

    protected void invalidateSessions()
    {
        // Do nothing.
        // We don't want to remove and invalidate all the sessions,
        // because this method is called from doStop(), and just
        // because this context is stopping does not mean that we
        // should remove the session from any other node (remember
        // the session map is shared)
    }

    private void scavenge()
    {
        Thread thread = Thread.currentThread();
        ClassLoader old_loader = thread.getContextClassLoader();
        if (_loader != null) thread.setContextClassLoader(_loader);
        try
        {
            long now = System.currentTimeMillis();
            Log.debug(this + " scavenging at {}, scavenge period {}", now, getScavengePeriodMs());

            // Detect the candidates that may have expired already, checking the estimated expiration time.
            Set<String> candidates = new HashSet<String>();
            String lockId = "scavenge:" + _contextPath + ":" + _virtualHost;
            Lock.lock(lockId);
            try
            {
                /**
                 * Synchronize in order, to avoid deadlocks with method {@link #getSession(String)}.
                 * In that method, we first synchronize on _session, then we call _sessionExpirations.containsKey(),
                 * which is synchronized by virtue of being a Collection.synchronizedMap.
                 * Here we must synchronize in the same order to avoid deadlock.
                 */
                synchronized (_sessions)
                {
                    synchronized (_sessionExpirations)
                    {
                        // Do not use iterators that throw ConcurrentModificationException
                        // We do a best effort here, and leave possible imprecisions to the next scavenge
                        Enumeration<String> keys = _sessionExpirations.keys();
                        while (keys.hasMoreElements())
                        {
                            String sessionId = keys.nextElement();
                            MutableLong value = _sessionExpirations.get(sessionId);
                            if (value != null)
                            {
                                long expirationTime = value.value;
                                Log.debug("Estimated expiration time {} for session {}", expirationTime, sessionId);
                                if (expirationTime > 0 && expirationTime < now) candidates.add(sessionId);
                            }
                        }

                        _sessions.keySet().retainAll(Collections.list(_sessionExpirations.keys()));
                    }
                }
            }
            finally
            {
                Lock.unlock(lockId);
            }
            Log.debug("Scavenging detected {} candidate sessions to expire", candidates.size());

            // Now validate that the candidates that do expire are really expired,
            // grabbing the session lock for each candidate
            for (String sessionId : candidates)
            {
                Session candidate = (Session)getSession(sessionId);
                if (candidate == null)
                    continue;
                
                // Here we grab the lock to avoid anyone else interfering
                boolean entered = tryEnter(sessionId);
                if (entered)
                {
                    try
                    {
                        long maxInactiveTime = candidate.getMaxIdlePeriodMs();
                        // Exclude sessions that never expire
                        if (maxInactiveTime > 0)
                        {
                            // The lastAccessedTime is fetched from Terracotta, so we're sure it is up-to-date.
                            long lastAccessedTime = candidate.getLastAccessedTime();
                            // Since we write the shared lastAccessedTime every scavenge period,
                            // take that in account before considering the session expired
                            long expirationTime = lastAccessedTime + maxInactiveTime + getScavengePeriodMs();
                            if (expirationTime < now)
                            {
                                Log.debug("Scavenging expired session {}, expirationTime {}", candidate.getClusterId(), expirationTime);
                                // Calling timeout() result in calling removeSession(), that will clean the data structures
                                candidate.timeout();
                            }
                            else
                            {
                                Log.debug("Scavenging skipping candidate session {}, expirationTime {}", candidate.getClusterId(), expirationTime);
                            }
                        }
                    }
                    finally
                    {
                        exit(sessionId);
                    }
                }
            }

            int sessionCount = getSessions();
            if (sessionCount < _minSessions) _minSessions = sessionCount;
            if (sessionCount > _maxSessions) _maxSessions = sessionCount;
        }
        catch (Throwable x)
        {
            // Must avoid at all costs that the scavenge thread exits, so here we catch and log 
            if(x instanceof ThreadDeath)
                throw (ThreadDeath)x;
            Log.warn("Problem scavenging sessions", x);
        }
        finally
        {
            thread.setContextClassLoader(old_loader);
        }
    }

    private String canonicalize(String contextPath)
    {
        if (contextPath == null) return "";
        return contextPath.replace('/', '_').replace('.', '_').replace('\\', '_');
    }

    private String virtualHostFrom(ContextHandler.SContext context)
    {
        String result = "0.0.0.0";
        if (context == null) return result;

        String[] vhosts = context.getContextHandler().getVirtualHosts();
        if (vhosts == null || vhosts.length == 0 || vhosts[0] == null) return result;

        return vhosts[0];
    }

    class Session extends AbstractSessionManager.Session
    {
        private static final long serialVersionUID = -2134521374206116367L;

        private final SessionData _sessionData;
        private long _lastUpdate;

        protected Session(HttpServletRequest request)
        {
            super(request);
            _sessionData = new SessionData(getClusterId(), _maxIdleMs);
            _lastAccessed = _sessionData.getCreationTime();
        }

        protected Session(SessionData sd)
        {
            super(sd.getCreationTime(), sd.getId());
            _sessionData = sd;
            _lastAccessed = getLastAccessedTime();
            initValues();
        }

        public SessionData getSessionData()
        {
            return _sessionData;
        }

        @Override
        public long getCookieSetTime()
        {
            return _sessionData.getCookieTime();
        }

        @Override
        protected void cookieSet()
        {
            _sessionData.setCookieTime(getLastAccessedTime());
        }
        
        @Override
        public void setMaxInactiveInterval(int secs)
        {
            super.setMaxInactiveInterval(secs);
            if(_maxIdleMs > 0L && _maxIdleMs / 10L < (long)_scavengePeriodMs)
            {
                long newScavengeSecs = (secs + 9) / 10;
                setScavengePeriodMs(1000L * newScavengeSecs);
            }

            // Update the estimated expiration time
            if (secs < 0) {
                this._sessionData._expiration.value = -1L;
            } else {
                this._sessionData._expiration.value = System.currentTimeMillis() + (1000L * secs);
            }
        }

        @Override
        public long getLastAccessedTime()
        {
            if (!isValid()) throw new IllegalStateException();
            return _sessionData.getPreviousAccessTime();
        }

        @Override
        public long getCreationTime() throws IllegalStateException
        {
            if (!isValid()) throw new IllegalStateException();
            return _sessionData.getCreationTime();
        }

        // Overridden for visibility
        @Override
        protected String getClusterId()
        {
            return super.getClusterId();
        }

        protected Map newAttributeMap()
        {
            // It is important to never return a new attribute map here (as other Session implementations do),
            // but always return the shared attributes map, so that a new session created on a different cluster
            // node is immediately filled with the session data from Terracotta.
            return _sessionData.getAttributeMap();
        }

        @Override
        protected void access(long time)
        {
            // The local previous access time is always updated via the super.access() call.
            // If the requests are steady and within the scavenge period, the distributed shared access times
            // are never updated. If only one node gets hits, other nodes reach the expiration time and the
            // scavenging on other nodes will believe the session is expired, since the distributed shared
            // access times have never been updated.
            // Therefore we need to update the distributed shared access times once in a while, no matter what.
            long previousAccessTime = getPreviousAccessTime();
            if (time - previousAccessTime > getScavengePeriodMs())
            {
                Log.debug("Out-of-date update of distributed access times: previous {} - current {}", previousAccessTime, time);
                updateAccessTimes(time);
            }
            else
            {
                if (time - _lastUpdate > getScavengePeriodMs())
                {
                    Log.debug("Periodic update of distributed access times: last update {} - current {}", _lastUpdate, time);
                    updateAccessTimes(time);
                }
                else
                {
                    Log.debug("Skipping update of distributed access times: previous {} - current {}", previousAccessTime, time);
                }
            }
            super.access(time);
        }

        /**
         * Updates the shared distributed access times that need to be updated
         *
         * @param time the update value
         */
        private void updateAccessTimes(long time)
        {
            _sessionData.setPreviousAccessTime(_accessed);
            if (getMaxIdlePeriodMs() > 0) _sessionData.setExpirationTime(time + getMaxIdlePeriodMs());
            _lastUpdate = time;
        }

        // Overridden for visibility
        @Override
        protected void timeout()
        {
            super.timeout();
            Log.debug("Timed out session {} with id {}", this, getClusterId());
        }

        @Override
        public void invalidate()
        {
            super.invalidate();
            Log.debug("Invalidated session {} with id {}", this, getClusterId());
        }

        private long getMaxIdlePeriodMs()
        {
            return _maxIdleMs;
        }

        private long getPreviousAccessTime()
        {
            return super.getLastAccessedTime();
        }
    }

    /**
     * The session data that is distributed to cluster nodes via Terracotta.
     */
    public static class SessionData
    {
        private final String _id;
        private final Map _attributes;
        private final long _creation;
        private final MutableLong _expiration;
        private long _previousAccess;
        private long _cookieTime;

        public SessionData(String sessionId, long maxIdleMs)
        {
            _id = sessionId;
            // Don't need synchronization, as we grab a distributed session id lock
            // when this map is accessed.
            _attributes = new HashMap();
            _creation = System.currentTimeMillis();
            _expiration = new MutableLong();
            _previousAccess = _creation;
            // Set expiration time to negative value if the session never expires
            _expiration.value = maxIdleMs > 0 ? _creation + maxIdleMs : -1L;
        }

        public String getId()
        {
            return _id;
        }

        protected Map getAttributeMap()
        {
            return _attributes;
        }

        public long getCreationTime()
        {
            return _creation;
        }

        public long getExpirationTime()
        {
            return _expiration.value;
        }

        public void setExpirationTime(long time)
        {
            _expiration.value = time;
        }

        public long getCookieTime()
        {
            return _cookieTime;
        }

        public void setCookieTime(long time)
        {
            _cookieTime = time;
        }

        public long getPreviousAccessTime()
        {
            return _previousAccess;
        }

        public void setPreviousAccessTime(long time)
        {
            _previousAccess = time;
        }
    }

    protected static class Lock
    {
        private static final ThreadLocal<Map<String, Integer>> nestings = new ThreadLocal<Map<String, Integer>>()
        {
            @Override
            protected Map<String, Integer> initialValue()
            {
                return new HashMap<String, Integer>();
            }
        };

        private Lock()
        {
        }

        public static void lock(String lockId)
        {
            Integer nestingLevel = nestings.get().get(lockId);
            if (nestingLevel == null) nestingLevel = 0;
            if (nestingLevel < 0)
                throw new AssertionError("Lock(" + lockId + ") nest level = " + nestingLevel + ", thread " + Thread.currentThread() + ": " + getLocks());
            if (nestingLevel == 0)
            {
                ManagerUtil.beginLock(lockId, Manager.LOCK_TYPE_WRITE);
                Log.debug("Lock({}) acquired by thread {}", lockId, Thread.currentThread().getName());
            }
            nestings.get().put(lockId, nestingLevel + 1);
            Log.debug("Lock({}) nestings {}", lockId, getLocks());
        }

        public static boolean tryLock(String lockId)
        {
            boolean result = ManagerUtil.tryBeginLock(lockId, Manager.LOCK_TYPE_WRITE);
            Log.debug("Lock({}) tried and" + (result ? "" : " not") + " acquired by thread {}", lockId, Thread.currentThread().getName());
            if (result)
            {
                Integer nestingLevel = nestings.get().get(lockId);
                if (nestingLevel == null) nestingLevel = 0;
                nestings.get().put(lockId, nestingLevel + 1);
                Log.debug("Lock({}) nestings {}", lockId, getLocks());
            }
            return result;
        }

        public static void unlock(String lockId)
        {
            Integer nestingLevel = nestings.get().get(lockId);
            if (nestingLevel == null) return;
            if (nestingLevel < 1)
                throw new AssertionError("Lock(" + lockId + ") nest level = " + nestingLevel + ", thread " + Thread.currentThread() + ": " + getLocks());
            if (nestingLevel == 1)
            {
                ManagerUtil.commitLock(lockId);
                Log.debug("Lock({}) released by thread {}", lockId, Thread.currentThread().getName());
                nestings.get().remove(lockId);
            }
            else
            {
                nestings.get().put(lockId, nestingLevel - 1);
            }
            Log.debug("Lock({}) nestings {}", lockId, getLocks());
        }

        /**
         * For testing and debugging purposes only.
         * @return the lock ids held by the current thread
         */
        protected static Map<String, Integer> getLocks()
        {
            return Collections.unmodifiableMap(nestings.get());
        }
    }

    private static class MutableLong
    {
        private long value;
    }
}
