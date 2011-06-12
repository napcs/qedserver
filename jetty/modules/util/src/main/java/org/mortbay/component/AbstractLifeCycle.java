//========================================================================
//$Id: AbstractLifeCycle.java,v 1.3 2005/11/11 22:55:41 gregwilkins Exp $
//Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.component;

import org.mortbay.log.Log;
import org.mortbay.util.LazyList;

/**
 * Basic implementation of the life cycle interface for components.
 * 
 * @author gregw
 */
public abstract class AbstractLifeCycle implements LifeCycle
{
    private Object _lock = new Object();
    private final int FAILED = -1, STOPPED = 0, STARTING = 1, STARTED = 2, STOPPING = 3;
    private volatile int _state = STOPPED;
    protected LifeCycle.Listener[] _listeners;

    protected void doStart() throws Exception
    {
    }

    protected void doStop() throws Exception
    {
    }

    public final void start() throws Exception
    {
        synchronized (_lock)
        {
            try
            {
                if (_state == STARTED || _state == STARTING)
                    return;
                setStarting();
                doStart();
                Log.debug("started {}",this);
                setStarted();
            }
            catch (Exception e)
            {
                setFailed(e);
                throw e;
            }
            catch (Error e)
            {
                setFailed(e);
                throw e;
            }
        }
    }

    public final void stop() throws Exception
    {
        synchronized (_lock)
        {
            try
            {
                if (_state == STOPPING || _state == STOPPED)
                    return;
                setStopping();
                doStop();
                Log.debug("stopped {}",this);
                setStopped();
            }
            catch (Exception e)
            {
                setFailed(e);
                throw e;
            }
            catch (Error e)
            {
                setFailed(e);
                throw e;
            }
        }
    }

    public boolean isRunning()
    {
        return _state == STARTED || _state == STARTING;
    }

    public boolean isStarted()
    {
        return _state == STARTED;
    }

    public boolean isStarting()
    {
        return _state == STARTING;
    }

    public boolean isStopping()
    {
        return _state == STOPPING;
    }

    public boolean isStopped()
    {
        return _state == STOPPED;
    }

    public boolean isFailed()
    {
        return _state == FAILED;
    }

    public void addLifeCycleListener(LifeCycle.Listener listener)
    {
        _listeners = (LifeCycle.Listener[])LazyList.addToArray(_listeners,listener,LifeCycle.Listener.class);
    }

    public void removeLifeCycleListener(LifeCycle.Listener listener)
    {
        _listeners = (LifeCycle.Listener[])LazyList.removeFromArray(_listeners,listener);
    }

    private void setStarted()
    {
        _state = STARTED;
        if (_listeners != null)
        {
            for (int i = 0; i < _listeners.length; i++)
            {
                _listeners[i].lifeCycleStarted(this);
            }
        }
    }

    private void setStarting()
    {
        _state = STARTING;
        if (_listeners != null)
        {
            for (int i = 0; i < _listeners.length; i++)
            {
                _listeners[i].lifeCycleStarting(this);
            }
        }
    }

    private void setStopping()
    {
        _state = STOPPING;
        if (_listeners != null)
        {
            for (int i = 0; i < _listeners.length; i++)
            {
                _listeners[i].lifeCycleStopping(this);
            }
        }
    }

    private void setStopped()
    {
        _state = STOPPED;
        if (_listeners != null)
        {
            for (int i = 0; i < _listeners.length; i++)
            {
                _listeners[i].lifeCycleStopped(this);
            }
        }
    }

    private void setFailed(Throwable th)
    {
        Log.warn("failed "+this+": "+th);
        Log.debug(th);
        _state = FAILED;
        if (_listeners != null)
        {
            for (int i = 0; i < _listeners.length; i++)
            {
                _listeners[i].lifeCycleFailure(this,th);
            }
        }
    }

}
