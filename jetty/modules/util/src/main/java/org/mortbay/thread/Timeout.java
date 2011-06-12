//========================================================================
//$Id: Timeout.java,v 1.3 2005/11/11 22:55:41 gregwilkins Exp $
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

package org.mortbay.thread;

import org.mortbay.log.Log;


/* ------------------------------------------------------------ */
/** Timeout queue.
 * This class implements a timeout queue for timers that are at least as likely to be cancelled as they are to expire.
 * Unlike the util timeout class, the duration of the timeouts is shared by all scheduled tasks and if the duration 
 * is changed, this affects all scheduled tasks.
 * <p>
 * The nested class Task should be extended by users of this class to obtain call back notification of 
 * expiries. 
 * 
 * @author gregw
 *
 */
public class Timeout
{
    private Object _lock;
    private long _duration;
    private volatile long _now=System.currentTimeMillis();
    private Task _head=new Task();

    /* ------------------------------------------------------------ */
    public Timeout()
    {
        _lock=new Object();
        _head._timeout=this;
    }

    /* ------------------------------------------------------------ */
    public Timeout(Object lock)
    {
        _lock=lock;
        _head._timeout=this;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the duration.
     */
    public long getDuration()
    {
        return _duration;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param duration The duration to set.
     */
    public void setDuration(long duration)
    {
        _duration = duration;
    }

    /* ------------------------------------------------------------ */
    public long setNow()
    {
        _now=System.currentTimeMillis();
        return _now; 
    }
    
    /* ------------------------------------------------------------ */
    public long getNow()
    {
        return _now;
    }

    /* ------------------------------------------------------------ */
    public void setNow(long now)
    {
        _now=now;
    }

    /* ------------------------------------------------------------ */
    /** Get an expired tasks.
     * This is called instead of {@link #tick()} to obtain the next
     * expired Task, but without calling it's {@link Task#expire()} or
     * {@link Task#expired()} methods.
     * 
     * @returns the next expired task or null.
     */
    public Task expired()
    {
        long now=_now;
        synchronized (_lock)
        {
            long _expiry = now-_duration;

            if (_head._next!=_head)
            {
                Task task = _head._next;
                if (task._timestamp>_expiry)
                    return null;

                task.unlink();
                task._expired=true;
                return task;
            }
            return null;
        }
    }

    /* ------------------------------------------------------------ */
    public void tick()
    {
        final long expiry = _now-_duration;
        
        Task task=null;
        while (true)
        {
            try
            {
                synchronized (_lock)
                {
                    task= _head._next;
                    if (task==_head || task._timestamp>expiry)
                        break;
                    task.unlink();
                    task._expired=true;
                    task.expire();
                }
                
                task.expired();
            }
            catch(Throwable th)
            {
                Log.warn(Log.EXCEPTION,th);
            }
        }
    }

    /* ------------------------------------------------------------ */
    public void tick(long now)
    {
        _now=now;
        tick();
    }

    /* ------------------------------------------------------------ */
    public void schedule(Task task)
    {
        schedule(task,0L);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param task
     * @param delay A delay in addition to the default duration of the timeout
     */
    public void schedule(Task task,long delay)
    {
        synchronized (_lock)
        {
            if (task._timestamp!=0)
            {
                task.unlink();
                task._timestamp=0;
            }
            task._timeout=this;
            task._expired=false;
            task._delay=delay;
            task._timestamp = _now+delay;

            Task last=_head._prev;
            while (last!=_head)
            {
                if (last._timestamp <= task._timestamp)
                    break;
                last=last._prev;
            }
            last.link(task);
        }
    }


    /* ------------------------------------------------------------ */
    public void cancelAll()
    {
        synchronized (_lock)
        {
            _head._next=_head._prev=_head;
        }
    }

    /* ------------------------------------------------------------ */
    public boolean isEmpty()
    {
        synchronized (_lock)
        {
            return _head._next==_head;
        }
    }

    /* ------------------------------------------------------------ */
    public long getTimeToNext()
    {
        synchronized (_lock)
        {
            if (_head._next==_head)
                return -1;
            long to_next = _duration+_head._next._timestamp-_now;
            return to_next<0?0:to_next;
        }
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(super.toString());
        
        Task task = _head._next;
        while (task!=_head)
        {
            buf.append("-->");
            buf.append(task);
            task=task._next;
        }
        
        return buf.toString();
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** Task.
     * The base class for scheduled timeouts.  This class should be
     * extended to implement the expire() method, which is called if the
     * timeout expires.
     * 
     * @author gregw
     *
     */
    public static class Task
    {
        Task _next;
        Task _prev;
        Timeout _timeout;
        long _delay;
        long _timestamp=0;
        boolean _expired=false;

        /* ------------------------------------------------------------ */
        public Task()
        {
            _next=_prev=this;
        }

        /* ------------------------------------------------------------ */
        public long getTimestamp()
        {
            return _timestamp;
        }

        /* ------------------------------------------------------------ */
        public long getAge()
        {
            final Timeout t = _timeout;
            if (t!=null)
            {
                final long now=t._now;
                if (now!=0 && _timestamp!=0)
                    return now-_timestamp;
            }
            return 0;
        }

        /* ------------------------------------------------------------ */
        private void unlink()
        {
            _next._prev=_prev;
            _prev._next=_next;
            _next=_prev=this;
            _expired=false;
        }

        /* ------------------------------------------------------------ */
        private void link(Task task)
        {
            Task next_next = _next;
            _next._prev=task;
            _next=task;
            _next._next=next_next;
            _next._prev=this;   
        }
        
        /* ------------------------------------------------------------ */
        /** Schedule the task on the given timeout.
         * The task exiry will be called after the timeout duration.
         * @param timer
         */
        public void schedule(Timeout timer)
        {
            timer.schedule(this);
        }
        
        /* ------------------------------------------------------------ */
        /** Schedule the task on the given timeout.
         * The task exiry will be called after the timeout duration.
         * @param timer
         */
        public void schedule(Timeout timer, long delay)
        {
            timer.schedule(this,delay);
        }
        
        /* ------------------------------------------------------------ */
        /** Reschedule the task on the current timeout.
         * The task timeout is rescheduled as if it had been cancelled and
         * scheduled on the current timeout.
         */
        public void reschedule()
        {
            Timeout timeout = _timeout;
            if (timeout!=null)
                timeout.schedule(this,_delay);
        }
        
        /* ------------------------------------------------------------ */
        /** Cancel the task.
         * Remove the task from the timeout.
         */
        public void cancel()
        {
            Timeout timeout = _timeout;
            if (timeout!=null)
            {
                synchronized (timeout._lock)
                {
                    unlink();
                    _timestamp=0;
                }
            }
        }
        
        /* ------------------------------------------------------------ */
        public boolean isExpired() { return _expired; }

        /* ------------------------------------------------------------ */
	public boolean isScheduled() { return _next!=this; }
        
        /* ------------------------------------------------------------ */
        /** Expire task.
         * This method is called when the timeout expires. It is called
         * in the scope of the synchronize block (on this) that sets 
         * the {@link #isExpired()} state to true.
         * @see #expired() For an unsynchronized callback.
         */
        public void expire(){}

        /* ------------------------------------------------------------ */
        /** Expire task.
         * This method is called when the timeout expires. It is called 
         * outside of any synchronization scope and may be delayed. 
         * 
         */
        public void expired(){}

    }

}
