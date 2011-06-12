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

package org.mortbay.jetty.servlet;

import java.security.SecureRandom;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.SessionIdManager;
import org.mortbay.log.Log;

public abstract class AbstractSessionIdManager extends AbstractLifeCycle implements SessionIdManager
{
    private final static String __NEW_SESSION_ID="org.mortbay.jetty.newSessionId";  
    
    protected Random _random;
    protected boolean _weakRandom;
    protected String _workerName;
    protected Server _server;
    
    
    public AbstractSessionIdManager(Server server)
    {
        _server=server;
    }
    
    
    public AbstractSessionIdManager(Server server, Random random)
    {
        _random=random;
        _server=server;
    }

    public String getWorkerName()
    {
        return _workerName;
    }
    
    public void setWorkerName (String name)
    {
        _workerName=name;
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
    /** 
     * Create a new session id if necessary.
     * 
     * @see org.mortbay.jetty.SessionIdManager#newSessionId(javax.servlet.http.HttpServletRequest, long)
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
                if (r0<0)
                    r0=-r0;
                long r1=_weakRandom
                ?(hashCode()^Runtime.getRuntime().freeMemory()^_random.nextInt()^(((long)request.hashCode())<<32))
                :_random.nextLong();
                if (r1<0)
                    r1=-r1;
                id=Long.toString(r0,36)+Long.toString(r1,36);
                
                //add in the id of the node to ensure unique id across cluster
                //NOTE this is different to the node suffix which denotes which node the request was received on
                if (_workerName!=null)
                    id=_workerName + id;
                
            }

            request.setAttribute(__NEW_SESSION_ID,id);
            return id;
        }
    }

    public void doStart()
    {
       initRandom();
    }

    /**
     * Set up a random number generator for the sessionids.
     * 
     * By preference, use a SecureRandom but allow to be injected.
     */
    public void initRandom ()
    {
        if (_random==null)
        {
            try
            {
                _random=new SecureRandom();
                _weakRandom=false;
            }
            catch (Exception e)
            {
                Log.warn("Could not generate SecureRandom for session-id randomness",e);
                _random=new Random();
                _weakRandom=true;
            }
        }
        _random.setSeed(_random.nextLong()^System.currentTimeMillis()^hashCode()^Runtime.getRuntime().freeMemory()); 
    }
}
