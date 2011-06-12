package org.mortbay.jetty.servlet.wadi;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.codehaus.wadi.aop.replication.AOPStackContext;
import org.codehaus.wadi.core.assembler.StackContext;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.manager.SessionAlreadyExistException;
import org.codehaus.wadi.core.manager.SessionMonitor;
import org.codehaus.wadi.core.store.Store;
import org.codehaus.wadi.replication.strategy.BackingStrategyFactory;
import org.codehaus.wadi.replication.strategy.RoundRobinBackingStrategyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.mortbay.jetty.servlet.AbstractSessionManager;
import org.mortbay.jetty.servlet.HashSessionIdManager;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

public class WadiSessionManager extends AbstractSessionManager
{
    private final Map<String, ClusteredSession> _idToSession = new HashMap<String, ClusteredSession>();
    private BackingStrategyFactory _backingStrategyFactory;
    private WadiCluster _wadiCluster;
    private CopyOnWriteArrayList _listeners;
    private Manager _manager;
    private SessionMonitor _sessionMonitor;
    private ServiceSpace _serviceSpace;
    private int _nbReplica;
    private int _numPartitions;
    private int _sweepInterval;
    private final boolean _enableReplication;
    private final boolean _deltaReplication;
    private Store _sharedStore;
    
    public WadiSessionManager(WadiCluster wadiCluster, int numPartitions, int sweepInterval)
    {
        this(wadiCluster, 0, numPartitions, sweepInterval, false, false);
    }

    public WadiSessionManager(WadiCluster wadiCluster, int nbReplica, int numPartitions, int sweepInterval)
    {
        this(wadiCluster, nbReplica, numPartitions, sweepInterval, true, false);
    }
    
    /**
     * Constructs a session manager attached to the provided cluster. 
     * 
     * @param wadiCluster cluster this session manager is attached to.
     * @param nbReplica number of replicas to be maintained for each sessions managed by this session manager. When a 
     * session is created or updated, it is replicated to a configurable number of other nodes.
     * @param numPartitions WADI maintains a distributed and indexed look-up map tracking HttpSession locations. 
     * numPartitions is the number of indexes of this look-up map. A standard number of partitions is 24.
     * @param sweepInterval number of seconds between the execution of HttpSession eviction policies. In other words, 
     * it is how often HttpSessions are scanned to identify and invalidate timed out sessions
     * @param enableReplication if true, then sessions are replicated to other nodes. The number of replicas is 
     * configured by nbReplica.
     */
    public WadiSessionManager(WadiCluster wadiCluster,
            int nbReplica,
            int numPartitions,
            int sweepInterval,
            boolean enableReplication,
            boolean deltaReplication)
    {
        _wadiCluster = wadiCluster;
        _nbReplica = nbReplica;
        _numPartitions = numPartitions;
        _sweepInterval = sweepInterval;
        _enableReplication = enableReplication;
        _deltaReplication = deltaReplication;
        
        _listeners = new CopyOnWriteArrayList();

        HashSessionIdManager sessionIdManager = new HashSessionIdManager();
        sessionIdManager.setWorkerName(wadiCluster.getNodeName());
        setIdManager(sessionIdManager);

        registerListener(new MigrationListener());
    }

    public Store getSharedStore()
    {
        return _sharedStore;
    }

    public void setSharedStore(Store store)
    {
        _sharedStore = store;
    }

    public void doStart() throws Exception
    {
        super.doStart();
        
        _backingStrategyFactory = new RoundRobinBackingStrategyFactory(_nbReplica);
        
        ServiceSpaceName serviceSpaceName = new ServiceSpaceName(new URI(_context.getContextPath() + "/"));
        
        StackContext stackContext;
        if (_deltaReplication) {
            stackContext = new AOPStackContext(Thread.currentThread().getContextClassLoader(),
                serviceSpaceName,
                _wadiCluster.getDispatcher(),
                _dftMaxIdleSecs,
                _numPartitions,
                _sweepInterval,
                _backingStrategyFactory);
        } else {
            stackContext = new StackContext(Thread.currentThread().getContextClassLoader(),
                serviceSpaceName,
                _wadiCluster.getDispatcher(),
                _dftMaxIdleSecs,
                _numPartitions,
                _sweepInterval,
                _backingStrategyFactory);
            
        }
        stackContext.setSharedStore(_sharedStore);
        stackContext.setDisableReplication(!_enableReplication);
        stackContext.build();

        _serviceSpace = stackContext.getServiceSpace();
        _manager = stackContext.getManager();

        _sessionMonitor = stackContext.getSessionMonitor();
        _sessionMonitor.addSessionListener(new SessionListenerAdapter());

        _serviceSpace.start();
    }

    public void doStop() throws Exception
    {
        _serviceSpace.stop();
        super.doStop();
    }

    public Manager getClusteredManager()
    {
        return _manager;
    }

    public WadiSession createSession(String sessionId) throws SessionAlreadyExistsException
    {
        org.codehaus.wadi.core.session.Session session;
        try
        {
            session = _manager.createWithName(sessionId);
        }
        catch (SessionAlreadyExistException e)
        {
            throw new SessionAlreadyExistsException(sessionId);
        }
        return new WadiSessionAdaptor(session);
    }

    public void registerListener(SessionListener listener)
    {
        _listeners.add(listener);
    }

    public void unregisterListener(SessionListener listener)
    {
        _listeners.remove(listener);
    }

    private void notifyInboundSessionMigration(org.codehaus.wadi.core.session.Session session)
    {
        for (Iterator iter = _listeners.iterator(); iter.hasNext();)
        {
            SessionListener listener = (SessionListener) iter.next();
            listener.notifyInboundSessionMigration(new WadiSessionAdaptor(session));
        }
    }

    private void notifyOutboundSessionMigration(org.codehaus.wadi.core.session.Session session)
    {
        for (Iterator iter = _listeners.iterator(); iter.hasNext();)
        {
            SessionListener listener = (SessionListener) iter.next();
            listener.notifyOutboundSessionMigration(new WadiSessionAdaptor(session));
        }
    }

    private void notifySessionDestruction(org.codehaus.wadi.core.session.Session session)
    {
        for (Iterator iter = _listeners.iterator(); iter.hasNext();)
        {
            SessionListener listener = (SessionListener) iter.next();
            listener.notifySessionDestruction(new WadiSessionAdaptor(session));
        }
    }

    private class SessionListenerAdapter implements org.codehaus.wadi.core.manager.SessionListener
    {

        public void onSessionCreation(org.codehaus.wadi.core.session.Session session) {
        }

        public void onSessionDestruction(org.codehaus.wadi.core.session.Session session) {
            notifySessionDestruction(session);
        }

        public void onInboundSessionMigration(org.codehaus.wadi.core.session.Session session) {
            notifyInboundSessionMigration(session);
        }
        
        public void onOutbountSessionMigration(org.codehaus.wadi.core.session.Session session) {
            notifyOutboundSessionMigration(session);
        }
        
    }

    protected Session newSession(HttpServletRequest request)
    {
        return new ClusteredSession(request);
    }

    public void complete(HttpSession session)
    {
        ClusteredSession clusteredSession = (ClusteredSession) session;
        clusteredSession.session.onEndAccess();
    }

    protected void addSession(Session session)
    {
        ClusteredSession clusteredSession = (ClusteredSession) session;
        synchronized (_idToSession)
        {
            _idToSession.put(clusteredSession.getClusterId(), clusteredSession);
        }
    }

    protected void removeSession(String idInCluster)
    {
        // Let MigrationListener handle session removal
    }

    public Session getSession(String idInCluster)
    {
        synchronized (_idToSession)
        {
            return _idToSession.get(idInCluster);
        }
    }

    public int getSessions()
    {
        synchronized (_idToSession)
        {
            return _idToSession.size();
        }
    }

    public Map getSessionMap()
    {
        throw new AssertionError("getSessionMap is never used.");
    }

    protected void invalidateSessions()
    {
    }

    private class MigrationListener implements SessionListener
    {

        public void notifyInboundSessionMigration(WadiSession session)
        {
            addSession(new ClusteredSession(session), false);
        }

        public void notifyOutboundSessionMigration(WadiSession session)
        {
            ClusteredSession clusteredSession = getClusteredSession(session);
            removeSession(clusteredSession, false);
        }

        public void notifySessionDestruction(WadiSession session)
        {
            ClusteredSession clusteredSession = getClusteredSession(session);
            removeSession(clusteredSession, true);
        }

        private ClusteredSession getClusteredSession(WadiSession session) throws AssertionError
        {
            ClusteredSession clusteredSession;
            synchronized (_idToSession)
            {
                clusteredSession = _idToSession.remove(session.getSessionId());
            }
            if (null == clusteredSession)
            {
                throw new AssertionError("Session [" + session + "] is undefined");
            }
            return clusteredSession;
        }

    }

    public class ClusteredSession extends Session
    {
        private final WadiSession session;

        protected ClusteredSession(HttpServletRequest request)
        {
            super(request);
            try
            {
                this.session = createSession(getClusterId());
            }
            catch (SessionAlreadyExistsException e)
            {
                throw (IllegalStateException) new IllegalStateException().initCause(e);
            }
            initValues();
        }

        protected ClusteredSession(WadiSession session)
        {
            super(System.currentTimeMillis(), session.getSessionId());
            this.session = session;
            initValues();
        }

        protected Map newAttributeMap()
        {
            return session.getState();
        }

        protected String getClusterId()
        {
            return super.getClusterId();
        }

        public void invalidate() throws IllegalStateException
        {
            super.doInvalidate();
            session.release();
        }
    }

}
