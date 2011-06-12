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

package org.mortbay.jetty.servlet;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.SessionIdManager;
import org.mortbay.jetty.servlet.AbstractSessionManager.Session;
import org.mortbay.log.Log;
import org.mortbay.util.MultiMap;

/* ------------------------------------------------------------ */
/**
 * HashSessionIdManager. An in-memory implementation of the session ID manager.
 */
public class HashSessionIdManager extends AbstractLifeCycle implements SessionIdManager
{
    private final static String __NEW_SESSION_ID="org.mortbay.jetty.newSessionId";  

    MultiMap _sessions;
    protected Random _random;
    private boolean _weakRandom;
    private String _workerName;

    /* ------------------------------------------------------------ */
    public HashSessionIdManager()
    {
    }

    /* ------------------------------------------------------------ */
    public HashSessionIdManager(Random random)
    {
        _random=random;
      
    }

    /* ------------------------------------------------------------ */
    /**
     * Get the workname. If set, the workername is dot appended to the session
     * ID and can be used to assist session affinity in a load balancer.
     * 
     * @return String or null
     */
    public String getWorkerName()
    {
        return _workerName;
    }

    /* ------------------------------------------------------------ */
    /**
     * Set the workname. If set, the workername is dot appended to the session
     * ID and can be used to assist session affinity in a load balancer.
     * 
     * @param workerName
     */
    public void setWorkerName(String workerName)
    {
        _workerName=workerName;
    }

    /* ------------------------------------------------------------ */
    /** Get the session ID with any worker ID.
     * 
     * @param request
     * @return sessionId plus any worker ID.
     */
    public String getNodeId(String clusterId,HttpServletRequest request) 
    {
        String worker=request==null?null:(String)request.getAttribute("org.mortbay.http.ajp.JVMRoute");
        if (worker!=null) 
            return clusterId+'.'+worker; 
        
        if (_workerName!=null) 
            return clusterId+'.'+_workerName;
       
        return clusterId;
    }

    /* ------------------------------------------------------------ */
    /** Get the session ID with any worker ID.
     * 
     * @param request
     * @return sessionId plus any worker ID.
     */
    public String getClusterId(String nodeId) 
    {
        int dot=nodeId.lastIndexOf('.');
        return (dot>0)?nodeId.substring(0,dot):nodeId;
    }
    
    /* ------------------------------------------------------------ */
    protected void doStart()
    {
        if (_random==null)
        {      
            try 
            {
                //This operation may block on some systems with low entropy. See this page
                //for workaround suggestions:
                //http://docs.codehaus.org/display/JETTY/Connectors+slow+to+startup
                Log.debug("Init SecureRandom."); 
                _random=new SecureRandom();
            }
            catch (Exception e)
            {
		Log.warn("Could not generate SecureRandom for session-id randomness",e);
		_random=new Random();
		_weakRandom=true;
            }
        }
        _sessions=new MultiMap();
    }

    /* ------------------------------------------------------------ */
    protected void doStop()
    {
        if (_sessions!=null)
            _sessions.clear(); // Maybe invalidate?
        _sessions=null;
    }

    /* ------------------------------------------------------------ */
    /*
     * @see org.mortbay.jetty.SessionManager.MetaManager#idInUse(java.lang.String)
     */
    public boolean idInUse(String id)
    {
        return _sessions.containsKey(id);
    }

    /* ------------------------------------------------------------ */
    /*
     * @see org.mortbay.jetty.SessionManager.MetaManager#addSession(javax.servlet.http.HttpSession)
     */
    public void addSession(HttpSession session)
    {
        synchronized (this)
        {
            _sessions.add(getClusterId(session.getId()),session);
        }
    }

    /* ------------------------------------------------------------ */
    /*
     * @see org.mortbay.jetty.SessionManager.MetaManager#addSession(javax.servlet.http.HttpSession)
     */
    public void removeSession(HttpSession session)
    {
        synchronized (this)
        {
            _sessions.removeValue(getClusterId(session.getId()),session);
        }
    }

    /* ------------------------------------------------------------ */
    /*
     * @see org.mortbay.jetty.SessionManager.MetaManager#invalidateAll(java.lang.String)
     */
    public void invalidateAll(String id)
    {
        while (true)
        {
            Session session=null;
            synchronized (this)
            {
                if (_sessions.containsKey(id))
                {
                    session=(Session)_sessions.getValue(id,0);
                    _sessions.removeValue(id,session);
                }
                else
                    return;
            }
            if (session.isValid())
                session.invalidate();
        }
    }

    /* ------------------------------------------------------------ */
    /*
     * new Session ID. If the request has a requestedSessionID which is unique,
     * that is used. The session ID is created as a unique random long XORed with
     * connection specific information, base 36.
     * @param request 
     * @param created 
     * @return Session ID.
     */
    public String newSessionId(HttpServletRequest request, long created)
    {
        synchronized (this)
        {
            // A requested session ID can only be used if it is in use already.
            String requested_id=request.getRequestedSessionId();

            if (requested_id!=null)
            {
                String cluster_id=getClusterId(requested_id);
                if (idInUse(cluster_id))
                    return cluster_id;
            }

            // Else reuse any new session ID already defined for this request.
            String new_id=(String)request.getAttribute(__NEW_SESSION_ID);
            if (new_id!=null&&idInUse(new_id))
                return new_id;

            // pick a new unique ID!
            String id=null;
            while (id==null||id.length()==0||idInUse(id))
            {
                long r0=_weakRandom
                ?(hashCode()^Runtime.getRuntime().freeMemory()^_random.nextInt()^(((long)request.hashCode())<<32))
                :_random.nextLong();
		long r1=_random.nextLong();
                if (r0<0)
                    r0=-r0;
                if (r1<0)
                    r1=-r1;
                id=Long.toString(r0,36)+Long.toString(r1,36);
            }

            // make the id unique to generating node
            if (_workerName!=null)
                id=_workerName+id;
            
            request.setAttribute(__NEW_SESSION_ID,id);
            return id;
        }
    }

    /* ------------------------------------------------------------ */
    public Random getRandom()
    {
        return _random;
    }

    /* ------------------------------------------------------------ */
    public void setRandom(Random random)
    {
        _random=random;
        _weakRandom=false;
    }

}
