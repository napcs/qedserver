// ========================================================================
// Copyright 2008 Mort Bay Consulting Pty. Ltd.
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


package org.mortbay.jetty.openspaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.log.Log;
import org.mortbay.util.LazyList;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.UrlSpaceConfigurer;
import org.openspaces.core.space.cache.LocalCacheSpaceConfigurer;

import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceProperty;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.gigaspaces.annotation.pojo.SpaceProperty.IndexType;
import com.j_spaces.core.client.Query;
import com.j_spaces.core.client.SQLQuery;



/**
 * GigaspacesSessionManager
 *
 * A Jetty SessionManager where the session data is stored in a 
 * data grid "cloud".
 * 
 * On each request, the session data is looked up in the "cloud"
 * and brought into the local space cache if doesn't already exist,
 * and an entry put into the managers map of sessions. When the request
 * exists, any changes, including changes to the access time of the session
 * are written back out to the grid.
 * 
 * TODO if we follow this strategy of having the grid store the session
 * data for us, and it is relatively cheap to access, then we could dispense
 * with the in-memory _sessions map.
 */
public class GigaSessionManager extends org.mortbay.jetty.servlet.AbstractSessionManager
{    
    private static int __id; //for identifying the scavenger thread
    private ConcurrentHashMap _sessions;
    private GigaSpace _space;
    private String _spaceUrl;
    private long _waitMsec = 5000L; //wait up to 5secs for requested objects to appear
    protected Timer _timer; //scavenge timer
    protected TimerTask _task; //scavenge task
    protected int _scavengePeriodMs = 1000 * 60 * 10; //10mins
    protected int _scavengeCount = 0;
    protected int _savePeriodMs = 60 * 1000; //60 sec
    protected SQLQuery _query;
    
    /**
     * SessionData
     *
     * Data about a session.
     * 
     * NOTE: we let gigaspaces assign a globally unique identifier for
     * a SessionData object, although we could compose our own, based on:
     *  canonicalized(contextPath) + virtualhost[0] + sessionid
     */
    public static class SessionData implements Serializable
    {
        private String _uid; //unique id
        private String _id;
        private long _accessed=-1;
        private long _lastAccessed=-1;
        private long _lastSaved=-1;
        private long _maxIdleMs=-1;
        private long _cookieSet=-1;
        private long _created=-1;
        private ConcurrentHashMap _attributes=null;
        private String _contextPath;
        private long _expiryTime=-1;
        private String _virtualHost;

        
        public SessionData ()
        {
        }

        
        public SessionData (String sessionId)
        {
            _id=sessionId;
            _created=System.currentTimeMillis();
            _accessed = _created;
            _lastAccessed = 0;
            _lastSaved = 0;
        }
        
        
        @SpaceId(autoGenerate=true)
        public synchronized String getUid ()
        {
            return _uid;
        }
        
        public synchronized void setUid (String uid)
        {
            _uid=uid;
        }
        
        public synchronized void setId (String id)
        {
            _id=id;
        }
        
        @SpaceProperty(index=IndexType.BASIC)
        @SpaceRouting
        public synchronized String getId ()
        {
            return _id;
        }
        
        @SpaceProperty(nullValue="-1")
        public synchronized long getCreated ()
        {
            return _created;
        }
        
        public synchronized void setCreated (long ms)
        {
            _created = ms;
        }
        
        @SpaceProperty(nullValue="-1")
        public synchronized long getAccessed ()
        {
            return _accessed;
        }
        
        public synchronized void setAccessed (long ms)
        {
            _accessed = ms;
        }
        
        public synchronized void setLastSaved (long ms)
        {
            _lastSaved = ms;
        }
        
        @SpaceProperty(nullValue="-1")
        public synchronized long getLastSaved ()
        {
            return _lastSaved;
        }
        
        public synchronized void setMaxIdleMs (long ms)
        {
            _maxIdleMs = ms;
        }
        
        @SpaceProperty(nullValue="-1")
        public synchronized long getMaxIdleMs()
        {
            return _maxIdleMs;
        }

        public synchronized void setLastAccessed (long ms)
        {
            _lastAccessed = ms;
        }
        
        @SpaceProperty(nullValue="-1")
        public synchronized long getLastAccessed()
        {
            return _lastAccessed;
        }

        public void setCookieSet (long ms)
        {
            _cookieSet = ms;
        }
        
        @SpaceProperty(nullValue="-1")
        public synchronized long getCookieSet ()
        {
            return _cookieSet;
        }
        
        @SpaceProperty
        protected synchronized ConcurrentHashMap getAttributeMap ()
        {
            return _attributes;
        }
        
        protected synchronized void setAttributeMap (ConcurrentHashMap map)
        {
            _attributes = map;
        } 

        public synchronized void setContextPath(String str)
        {
            _contextPath=str;
        }
        
        @SpaceProperty(index=IndexType.BASIC)
        public synchronized String getContextPath ()
        {
            return _contextPath;
        }

        public synchronized void setExpiryTime (long time)
        {
            _expiryTime=time;
        }
        
        @SpaceProperty(nullValue="-1")
        public synchronized long getExpiryTime ()
        {
            return _expiryTime;
        }
        
        public synchronized void setVirtualHost (String vhost)
        {
            _virtualHost=vhost;
        }
        
        public synchronized String getVirtualHost ()
        {
            return _virtualHost;
        }
        
        public String toString ()
        {
            return "Session uid="+_uid+", id="+_id+
                   ", contextpath="+_contextPath+
                   ", virtualHost="+_virtualHost+
                   ",created="+_created+",accessed="+_accessed+
                   ",lastAccessed="+_lastAccessed+
                   ",cookieSet="+_cookieSet+
                   ",expiryTime="+_expiryTime;
        }
        
        public String toStringExtended ()
        {
            return toString()+"values="+_attributes;
        }
    }
    
    
    /**
     * Session
     *
     * A session in memory of a Context. Adds behaviour around SessionData.
     */
    public class Session extends org.mortbay.jetty.servlet.AbstractSessionManager.Session
    {
        private SessionData _data;
        private boolean _dirty=false;

        /**
         * Session from a request.
         * 
         * @param request
         */
        protected Session (HttpServletRequest request)
        {
         
            super(request);   
            _data = new SessionData(_clusterId);
            _data.setMaxIdleMs(_dftMaxIdleSecs*1000);
            _data.setContextPath(_context.getContextPath());
            _data.setVirtualHost(getVirtualHost(_context));
            _data.setExpiryTime(_maxIdleMs < 0 ? 0 : (System.currentTimeMillis() + _maxIdleMs));
            _data.setCookieSet(0);
            if (_data.getAttributeMap()==null)
                newAttributeMap();
            _values=_data.getAttributeMap();
            if (Log.isDebugEnabled()) Log.debug("New Session from request, "+_data.toStringExtended());
        }

        /**
         * Session restored in database.
         * @param row
         */
        protected Session (SessionData data)
        {
            super(data.getCreated(), data.getId());
            _data=data;
            _values = data.getAttributeMap();
            if (Log.isDebugEnabled()) Log.debug("New Session from existing session data "+_data.toStringExtended());
        }


        protected void cookieSet()
        {
            _data.setCookieSet(_data.getAccessed());
        }

        
        protected Map newAttributeMap()
        {
            if (_data.getAttributeMap()==null)
            {
                _data.setAttributeMap(new ConcurrentHashMap());
            }
            return _data.getAttributeMap();
        }
        
        
        public void setAttribute (String name, Object value)
        {
            super.setAttribute(name, value);
            _dirty=true;
        }

        public void removeAttribute (String name)
        {
            super.removeAttribute(name); 
            _dirty=true;
        }

       /** 
        * Entry to session.
        * Called by SessionHandler on inbound request and the session already exists in this node's memory.
        * 
        * @see org.mortbay.jetty.servlet.AbstractSessionManager.Session#access(long)
        */
       protected void access(long time)
       {
           super.access(time);
           _data.setLastAccessed(_data.getAccessed());
           _data.setAccessed(time);
           _data.setExpiryTime(_maxIdleMs < 0 ? 0 : (time + _maxIdleMs));
       }

       /** 
        * Exit from session
        * 
        * If the session attributes changed then always write the session 
        * to the cloud.
        * 
        * If just the session access time changed, we don't always write out the
        * session, because the gigaspace will serialize the unchanged sesssion
        * attributes. To save on serialization overheads, we only write out the
        * session when only the access time has changed if the time at which we
        * last saved the session exceeds the chosen save interval.
        * 
        * @see org.mortbay.jetty.servlet.AbstractSessionManager.Session#complete()
        */
       protected void complete()
       {
           super.complete();
           try
           {
               if (_dirty || (_data._accessed - _data._lastSaved) >= (_savePeriodMs))
               {
                   _data.setLastSaved(System.currentTimeMillis());
                   willPassivate();   
                   update(_data);
                   didActivate();
                   if (Log.isDebugEnabled()) Log.debug("Dirty="+_dirty+", accessed-saved="+_data._accessed +"-"+ _data._lastSaved+", savePeriodMs="+_savePeriodMs);
               }
           }
           catch (Exception e)
           {
               Log.warn("Problem persisting changed session data id="+getId(), e);
           }
           finally
           {
               _dirty=false;
           }
       }
       
        protected void timeout() throws IllegalStateException
        {
            if (Log.isDebugEnabled()) Log.debug("Timing out session id="+getClusterId());
            super.timeout();
        }
        
        protected void willPassivate ()
        {
            super.willPassivate();
        }
        
        protected void didActivate ()
        {
            super.didActivate();
        }
        
        public String getClusterId()
        {
            return super.getClusterId();
        }
        
        public String getNodeId()
        {
            return super.getNodeId();
        }
    }
    
  
    /** 
     * Start the session manager.
     * 
     * @see org.mortbay.jetty.servlet.AbstractSessionManager#doStart()
     */
    public void doStart() throws Exception
    {
        if (_sessionIdManager==null)
            throw new IllegalStateException("No session id manager defined");
     
        _sessions = new ConcurrentHashMap();
        
        if (_space==null)
            initSpace(); 
        
        super.doStart();
        
        _timer=new Timer("GigaspaceSessionScavenger_"+(++__id), true);
        setScavengePeriod(getScavengePeriod());
    }
    
    
    /** 
     * Stop the session manager.
     * 
     * @see org.mortbay.jetty.servlet.AbstractSessionManager#doStop()
     */
    public void doStop() throws Exception
    {
        // stop the scavenger
        synchronized(this)
        {
            if (_task!=null)
                _task.cancel();
            if (_timer!=null)
                _timer.cancel();
            _timer=null;
        }
        
        _sessions.clear();
        _sessions = null;
        
        _space = null;
        
        super.doStop();
    }
    
    public int getSavePeriod ()
    {
        return _savePeriodMs/1000;
    }
    
    public void setSavePeriod (int seconds)
    {
        if (seconds <= 0)
            seconds=60;
        
        _savePeriodMs = seconds*1000;
    }
    
    
    public int getScavengePeriod()
    {
        return _scavengePeriodMs/1000;
    }

    public void setScavengePeriod(int seconds)
    {
        if (seconds<=0)
            seconds=60;

        int old_period=_scavengePeriodMs;
        int period=seconds*1000;
      
        _scavengePeriodMs=period;
        
        //add a bit of variability into the scavenge time so that not all
        //contexts with the same scavenge time sync up
        int tenPercent = _scavengePeriodMs/10;
        if ((System.currentTimeMillis()%2) == 0)
            _scavengePeriodMs += tenPercent;
        
        if (Log.isDebugEnabled()) Log.debug("GigspacesSessionScavenger scavenging every "+_scavengePeriodMs+" ms");
        if (_timer!=null && (period!=old_period || _task==null))
        {
            synchronized (this)
            {
                if (_task!=null)
                    _task.cancel();
                _task = new TimerTask()
                {
                    public void run()
                    {
                        scavenge();
                    }   
                };
                _timer.schedule(_task,_scavengePeriodMs,_scavengePeriodMs);
            }
        }
    }
    
    public void setSpace (GigaSpace space)
    {
        _space=space;
    }
    
    public GigaSpace getSpace ()
    {
        return _space;
    }
    
    
    /** 
     * Get a session matching the id.
     * 
     * Look in the grid to see if such a session exists, as it may have moved from
     * another node.
     * 
     * @see org.mortbay.jetty.servlet.AbstractSessionManager#getSession(java.lang.String)
     */
    public Session getSession(String idInCluster)
    {  
        synchronized (this)
        {        
            try
            {               
                //Ask the space for the session. This might incur serialization:
                //if we have no localcache, OR the localcache has to fetch the session
                //because of a cache miss OR the localcache is set to pull mode (where it
                //checks for changes to an object when that object is requested).
                //Alternatively, if the localcache is set to push mode, the cloud will
                //keep the localcache up-to-date with object changes in the background,
                //so serialization is occuring beyond our control.
                //TODO consider using the jdbc approach, were we only ask the cloud
                //intermittently for the session.
                SessionData template = new SessionData();
                template.setId(idInCluster);
                template.setContextPath(_context.getContextPath());
                template.setVirtualHost(getVirtualHost(_context));
                SessionData data = fetch (template);
                
                Session session = null;

                if (data == null)
                {
                    //No session in cloud with matching id and context path.
                    session=null;
                    if (Log.isDebugEnabled()) Log.debug("No session matching id="+idInCluster);
                }
                else
                {
                    Session oldSession = (Session)_sessions.get(idInCluster);
         

                    //if we had no prior session, or the session from the cloud has been
                    //more recently updated than our copy in memory, we should use it
                    //instead
                    if ((oldSession == null) || (data.getAccessed() > oldSession._data.getAccessed()))
                    {
                        session = new Session(data);
                        _sessions.put(idInCluster, session);
                        session.didActivate();
                        if (Log.isDebugEnabled()) Log.debug("Refreshed in-memory Session with "+data.toStringExtended());
                    }
                    else
                    {
                        if (Log.isDebugEnabled()) Log.debug("Not updating session "+idInCluster+", in-memory session is as fresh or fresher");
                        session = oldSession;
                    }
                }

                return session;
            }
            catch (Exception e)
            {
                Log.warn("Unable to load session from database", e);
                return null;
            }
        }
    }

    
    public void setSpaceUrl (String url)
    {
        _spaceUrl=url;
    }
    
    public String getSpaceUrl ()
    {
        return _spaceUrl;
    }
    
    public void setWaitMs (long msec)
    {
        _waitMsec=msec;
    }
   
    
    public long getWaitMs ()
    {
        return _waitMsec;
    }
    
    
    public Map getSessionMap()
    {
        return Collections.unmodifiableMap(_sessions);
    }

 
    public int getSessions()
    {
        int size = 0;
        synchronized (this)
        {
            size = _sessions.size();
        }
        return size;
    }

   
    protected void invalidateSessions()
    {
        //Do nothing - we don't want to remove and
        //invalidate all the sessions because this
        //method is called from doStop(), and just
        //because this context is stopping does not
        //mean that we should remove the session from
        //any other nodes
    }

    protected Session newSession(HttpServletRequest request)
    {
        return new Session(request);
    }

    
    protected void removeSession(String idInCluster)
    {
        synchronized (this)
        {
           try
           {
               Session session = (Session)_sessions.remove(idInCluster);
               delete(session._data);
           }
           catch (Exception e)
           {
               Log.warn("Problem deleting session id="+idInCluster, e);
           }
        }
    }

    
    public void removeSession(org.mortbay.jetty.servlet.AbstractSessionManager.Session abstractSession, boolean invalidate)
    {        
        if (! (abstractSession instanceof GigaSessionManager.Session))
            throw new IllegalStateException("Session is not a GigaspacesSessionManager.Session "+abstractSession);
        
        GigaSessionManager.Session session = (GigaSessionManager.Session)abstractSession;
        
        synchronized (_sessionIdManager)
        {
            boolean removed = false;
            
            synchronized (this)
            {
                //take this session out of the map of sessions for this context
                if (_sessions.get(getClusterId(session)) != null)
                {
                    removed = true;
                    removeSession(getClusterId(session));
                }
            }   
            
            if (removed)
            {
                // Remove session from all context and global id maps
                _sessionIdManager.removeSession(session);
                if (invalidate)
                    _sessionIdManager.invalidateAll(getClusterId(session));
            }
        }
        
        if (invalidate && _sessionListeners!=null)
        {
            HttpSessionEvent event=new HttpSessionEvent(session);
            for (int i=LazyList.size(_sessionListeners); i-->0;)
                ((HttpSessionListener)LazyList.get(_sessionListeners,i)).sessionDestroyed(event);
        }
        if (!invalidate)
        {
            session.willPassivate();
        }
    }

    
    public void invalidateSession(String idInCluster)
    {
        synchronized (this)
        {
            Session session = (Session)_sessions.get(idInCluster);
            if (session != null)
            {
                session.invalidate();
            }
        }
    }

  
    protected void addSession(org.mortbay.jetty.servlet.AbstractSessionManager.Session abstractSession)
    {
        if (abstractSession==null)
            return;
        
        if (!(abstractSession instanceof GigaSessionManager.Session))
                throw new IllegalStateException("Not a GigaspacesSessionManager.Session "+abstractSession);
        
        synchronized (this)
        {
            GigaSessionManager.Session session = (GigaSessionManager.Session)abstractSession;

            try
            {
                _sessions.put(getClusterId(session), session);
                add(session._data);
            }
            catch (Exception e)
            {
                Log.warn("Problem writing new SessionData to space ", e);
            }
        } 
    }
    
    
    /**
     * Look for expired sessions that we know about in our
     * session map, and double check with the grid that
     * it has really expired, or already been removed.
     */
    protected void scavenge ()
    {
        //don't attempt to scavenge if we are shutting down
        if (isStopping() || isStopped())
            return;

        Thread thread=Thread.currentThread();
        ClassLoader old_loader=thread.getContextClassLoader();
        _scavengeCount++;

        try
        {
            if (_loader!=null)
                thread.setContextClassLoader(_loader);
            long now = System.currentTimeMillis();
            if (Log.isDebugEnabled()) Log.debug("Scavenger running at "+now+" for context = "+_context.getContextPath());
            
            //go through in-memory map of Sessions, pick out any that are candidates for removal
            //due to expiry time being reached or passed.
            synchronized (this)
            {
                ArrayList removalCandidates = new ArrayList();
                Iterator itor = _sessions.values().iterator();

                while (itor.hasNext())
                {
                    Session session = (Session)itor.next();
                    if (session._data._expiryTime < now)
                        removalCandidates.add(session);
                }

                //for each candidate, check the session data in the cloud to ensure that some other
                //node hasn't been updating it's access time. If it's still expired, then delete it
                //locally and in the cloud.
                itor = removalCandidates.listIterator();
                while (itor.hasNext())
                {
                    Session candidate = (Session)itor.next();
                    SessionData template = new SessionData();
                    template.setUid(candidate._data._uid);
                    template.setId(candidate.getId());
                    try
                    {
                        SessionData currentSessionData = fetch(template);
                        if (currentSessionData==null)
                        {
                            //it's no longer in the cloud - either some other node has
                            //expired it or invalidated it
                            _sessions.remove(candidate.getId());
                            if (Log.isDebugEnabled()) Log.debug("Dropped non-existant session "+candidate._data);
                        }
                        else if (currentSessionData._expiryTime < now)
                        {
                            //its expired, run all the listeners etc
                            candidate.timeout();
                            itor.remove();
                            if (Log.isDebugEnabled()) Log.debug("Timed out session "+candidate._data);
                        }
                    }
                    catch (Exception e)
                    {
                        Log.warn("Problem checking current state of session "+candidate._data, e);
                    }
                }
                
                //every so often do a bigger sweep for very old sessions in
                //the cloud. A very old session is one that is defined to have 
                //expired at least 2 sweeps of the scavenger ago. TODO make
                //this configurable
                if ((_scavengeCount % 2) == 0)
                {
                    if (Log.isDebugEnabled()) Log.debug("Scavenging old sessions, expiring before: "+(now - (2 * _scavengePeriodMs)));
                    Object[] expiredSessions = findExpiredSessions((now - (2 * _scavengePeriodMs)));
                    for (int i = 0; i < expiredSessions.length; i++) 
                    {
                        if (Log.isDebugEnabled()) Log.debug("Timing out expired sesson " + expiredSessions[i]);
                        GigaSessionManager.Session expiredSession = new GigaSessionManager.Session((SessionData)expiredSessions[i]);
                        _sessions.put(expiredSession.getClusterId(), expiredSession); //needs to be in there so removeSession test will succeed and remove it
                        expiredSession.timeout();
                        if (Log.isDebugEnabled()) Log.debug("Expiring old session "+expiredSession._data);
                    }
                }
                
                int count = this._sessions.size();
                if (count < this._minSessions)
                    this._minSessions=count;
            }
        }
        catch (Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw ((ThreadDeath)t);
            else
                Log.warn("Problem scavenging sessions", t);
        }
        finally
        {
            thread.setContextClassLoader(old_loader);
        }
    }
    
    
    
    protected void add (SessionData data)
    throws Exception
    {
        _space.write(data);
    }

    protected void delete (SessionData data)
    throws Exception
    {
        SessionData sd = new SessionData();
        sd.setUid(data.getUid());
        sd.setId(data.getId());
         _space.takeIfExists(sd, getWaitMs());
    }
    
    
    protected void update (SessionData data)
    throws Exception
    {
        _space.write(data);
        if (Log.isDebugEnabled()) Log.debug("Wrote session "+data.toStringExtended());
    }
    
    protected SessionData fetch (SessionData template)
    throws Exception
    {
        SessionData obj = (SessionData)_space.readIfExists(template, getWaitMs());
        return obj;
    }
    
    protected Object[] findExpiredSessions (long timestamp)
    throws Exception
    {
        _query.setParameter(1, new Long(timestamp));
        Object[] sessions = _space.takeMultiple(_query, Integer.MAX_VALUE);
        return sessions;
    }
    
    protected void initSpace ()
    throws Exception
    {
        if (_spaceUrl==null)
            throw new IllegalStateException ("No url for space");
        
        UrlSpaceConfigurer usc = new UrlSpaceConfigurer(_spaceUrl);
        LocalCacheSpaceConfigurer lcsc = new LocalCacheSpaceConfigurer(usc.space()); 
        GigaSpaceConfigurer gigaSpaceConfigurer = new GigaSpaceConfigurer(usc.space());
        _space = gigaSpaceConfigurer.gigaSpace();     
        _query = new SQLQuery(SessionData.class, "expiryTime < ?");
    }
    
    /**
     * Get the first virtual host for the context.
     * 
     * Used to help identify the exact session/contextPath.
     * 
     * @return 0.0.0.0 if no virtual host is defined
     */
    private String getVirtualHost (ContextHandler.SContext context)
    {
        String vhost = "0.0.0.0";
        
        if (context==null)
            return vhost;
        
        String [] vhosts = context.getContextHandler().getVirtualHosts();
        if (vhosts==null || vhosts.length==0 || vhosts[0]==null)
            return vhost;
        
        return vhosts[0];
    }
}
