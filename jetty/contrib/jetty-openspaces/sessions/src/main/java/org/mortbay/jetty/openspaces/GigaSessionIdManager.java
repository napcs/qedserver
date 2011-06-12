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

import java.util.HashSet;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.AbstractSessionIdManager;
import org.mortbay.jetty.servlet.AbstractSessionManager;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.UrlSpaceConfigurer;
import org.openspaces.core.space.cache.LocalCacheSpaceConfigurer;

import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;

/**
 * GigaspacesSessionIdManager
 *
 * A Jetty SessionIDManager where the in-use session ids are stored
 * in a data grid "cloud".
 */
public class GigaSessionIdManager extends AbstractSessionIdManager
{
    protected HashSet<Id> _sessionIds = new HashSet<Id>();
    protected long _scavengeIntervalSec = 60 * 10; //10mins - TODO could move back to the SessionManager?
    protected String _spaceUrl;
    protected GigaSpace _space;
    protected long _waitMsec = 5000L; //time in msec to wait on read operations
 
    
    /**
     * Id
     *
     * Class to hold the session id. This is really only needed
     * for gigaspaces so that we can annotate routing information etc.
     */
    public static class Id
    {
        private String _id;
        
        public Id()
        {}
        
        public Id(String id)
        {
            _id=id;
        }
        
        public void setId(String id)
        {
            _id=id;
        }
        
        @SpaceId
        @SpaceRouting
        public String getId()
        {
            return _id;
        }
        
        public boolean equals(Object o)
        {
            if (o == null)
                return false;
            Id targetId = (Id)o;
            
            if (targetId.getId() == _id)
                return true;
            
            if (targetId.getId().equals(_id))
                return true;
            
            return false;
        }
    }
    
    public GigaSessionIdManager(Server server)
    {
       super(server);
    }
    
    public GigaSessionIdManager(Server server, Random random)
    {
       super(server, random);
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
    
    public void addSession(HttpSession session)
    {
        if (session == null)
            return;
        
        synchronized (_sessionIds)
        {
            if (session instanceof GigaSessionManager.Session)
            {
                String id = ((GigaSessionManager.Session)session).getClusterId();            
                try
                {
                    Id theId = new Id(id);
                    add(theId);
                    _sessionIds.add(theId);
                    if (Log.isDebugEnabled()) Log.debug("Added id "+id);
                }
                catch (Exception e)
                {
                    Log.warn("Problem storing session id="+id, e);
                }
            }
            else
                throw new IllegalStateException ("Session is not a Gigaspaces session");
        }
    }

    public String getClusterId(String nodeId)
    {
        int dot=nodeId.lastIndexOf('.');
        return (dot>0)?nodeId.substring(0,dot):nodeId;
    }

    public String getNodeId(String clusterId, HttpServletRequest request)
    {
        if (_workerName!=null)
            return clusterId+'.'+_workerName;

        return clusterId;
    }

    public boolean idInUse(String id)
    {
        if (id == null)
            return false;
        
        String clusterId = getClusterId(id);
        Id theId = new Id(clusterId);
        synchronized (_sessionIds)
        {
            if (_sessionIds.contains(theId))
                return true; //optimisation - if this session is one we've been managing, we can check locally
            
            //otherwise, we need to go to the space to check
            try
            {
                return exists(theId);
            }
            catch (Exception e)
            {
                Log.warn("Problem checking inUse for id="+clusterId, e);
                return false;
            }
        }
    }

    public void invalidateAll(String id)
    {
        //take the id out of the list of known sessionids for this node
        removeSession(id);
        
        synchronized (_sessionIds)
        {
            //tell all contexts that may have a session object with this id to
            //get rid of them
            Handler[] contexts = _server.getChildHandlersByClass(WebAppContext.class);
            for (int i=0; contexts!=null && i<contexts.length; i++)
            {
                AbstractSessionManager manager = ((AbstractSessionManager)((WebAppContext)contexts[i]).getSessionHandler().getSessionManager());
                if (manager instanceof GigaSessionManager)
                {
                    ((GigaSessionManager)manager).invalidateSession(id);
                }
            }
        }
    }

    public void removeSession(HttpSession session)
    {
        if (session == null)
            return;
        
        removeSession(((GigaSessionManager.Session)session).getClusterId());
    }
    
    public void removeSession (String id)
    {

        if (id == null)
            return;
        
        synchronized (_sessionIds)
        {  
            if (Log.isDebugEnabled())
                Log.debug("Removing session id="+id);
            try
            {               
                Id theId = new Id(id);
                _sessionIds.remove(theId);
                delete(theId);
            }
            catch (Exception e)
            {
                Log.warn("Problem removing session id="+id, e);
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
     * Start up the id manager.
     * 
     * @see org.mortbay.jetty.servlet.AbstractSessionIdManager#doStart()
     */
    public void doStart()
    {
        try
        { 
            if (_space==null)
                initSpace();
            super.doStart();
        }
        catch (Exception e)
        {
            Log.warn("Problem initialising session ids", e);
            throw new IllegalStateException (e);
        }
    }
    
    /** 
     * Stop the scavenger.
     * 
     * @see org.mortbay.component.AbstractLifeCycle#doStop()
     */
    public void doStop () 
    throws Exception
    {
        super.doStop();
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
    }
    
    protected void add (Id id)
    throws Exception
    {
        _space.write(id);
    }
    
    
    protected void delete (Id id)
    throws Exception
    {
        _space.takeIfExists(id, getWaitMs());
        if (Log.isDebugEnabled()) Log.debug ("Deleted id from space: id="+id);
    }
    
    protected boolean exists (Id id)
    throws Exception
    {
        Id idFromSpace = (Id)_space.readIfExists(id, getWaitMs());
        if (Log.isDebugEnabled()) Log.debug("Id="+id+(idFromSpace==null?"does not exist":"exists"));
        if (idFromSpace==null)
            return false;
        return true;
    }
}
