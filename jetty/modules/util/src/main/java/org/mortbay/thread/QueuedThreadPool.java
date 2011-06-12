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

package org.mortbay.thread;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.log.Log;

/* ------------------------------------------------------------ */
/** A pool of threads.
 * <p>
 * Avoids the expense of thread creation by pooling threads after
 * their run methods exit for reuse.
 * <p>
 * If an idle thread is available a job is directly dispatched,
 * otherwise the job is queued.  After queuing a job, if the total
 * number of threads is less than the maximum pool size, a new thread 
 * is spawned.
 * <p>
 *
 * @author Greg Wilkins <gregw@mortbay.com>
 */
public class QueuedThreadPool extends AbstractLifeCycle implements Serializable, ThreadPool
{
    private String _name;
    private Set _threads;
    private List _idle;
    private Runnable[] _jobs;
    private int _nextJob;
    private int _nextJobSlot;
    private int _queued;
    private int _maxQueued;
    
    private boolean _daemon;
    private int _id;

    private final Object _lock = new Lock();
    private final Object _threadsLock = new Lock();
    private final Object _joinLock = new Lock();

    private long _lastShrink;
    private int _maxIdleTimeMs=60000;
    private int _maxThreads=250;
    private int _minThreads=2;
    private boolean _warned=false;
    private int _lowThreads=0;
    private int _priority= Thread.NORM_PRIORITY;
    private int _spawnOrShrinkAt=0;
    private int _maxStopTimeMs;

    
    /* ------------------------------------------------------------------- */
    /* Construct
     */
    public QueuedThreadPool()
    {
        _name="qtp-"+hashCode();
    }
    
    /* ------------------------------------------------------------------- */
    /* Construct
     */
    public QueuedThreadPool(int maxThreads)
    {
        this();
        setMaxThreads(maxThreads);
    }

    /* ------------------------------------------------------------ */
    /** Run job.
     * @return true 
     */
    public boolean dispatch(Runnable job) 
    {  
        if (!isRunning() || job==null)
            return false;

        PoolThread thread=null;
        boolean spawn=false;
            
        synchronized(_lock)
        {
            // Look for an idle thread
            int idle=_idle.size();
            if (idle>0)
                thread=(PoolThread)_idle.remove(idle-1);
            else
            {
                // queue the job
                _queued++;
                if (_queued>_maxQueued)
                    _maxQueued=_queued;
                _jobs[_nextJobSlot++]=job;
                if (_nextJobSlot==_jobs.length)
                    _nextJobSlot=0;
                if (_nextJobSlot==_nextJob)
                {
                    // Grow the job queue
                    Runnable[] jobs= new Runnable[_jobs.length+_maxThreads];
                    int split=_jobs.length-_nextJob;
                    if (split>0)
                        System.arraycopy(_jobs,_nextJob,jobs,0,split);
                    if (_nextJob!=0)
                        System.arraycopy(_jobs,0,jobs,split,_nextJobSlot);
                    
                    _jobs=jobs;
                    _nextJob=0;
                    _nextJobSlot=_queued;
                }
                  
                spawn=_queued>_spawnOrShrinkAt;
            }
        }
        
        if (thread!=null)
        {
            thread.dispatch(job);
        }
        else if (spawn)
        {
            newThread();
        }
        return true;
    }

    /* ------------------------------------------------------------ */
    /** Get the number of idle threads in the pool.
     * @see #getThreads
     * @return Number of threads
     */
    public int getIdleThreads()
    {
        return _idle==null?0:_idle.size();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return low resource threads threshhold
     */
    public int getLowThreads()
    {
        return _lowThreads;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return maximum queue size
     */
    public int getMaxQueued()
    {
        return _maxQueued;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the maximum thread idle time.
     * Delegated to the named or anonymous Pool.
     * @see #setMaxIdleTimeMs
     * @return Max idle time in ms.
     */
    public int getMaxIdleTimeMs()
    {
        return _maxIdleTimeMs;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the maximum number of threads.
     * Delegated to the named or anonymous Pool.
     * @see #setMaxThreads
     * @return maximum number of threads.
     */
    public int getMaxThreads()
    {
        return _maxThreads;
    }

    /* ------------------------------------------------------------ */
    /** Get the minimum number of threads.
     * Delegated to the named or anonymous Pool.
     * @see #setMinThreads
     * @return minimum number of threads.
     */
    public int getMinThreads()
    {
        return _minThreads;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return The name of the BoundedThreadPool.
     */
    public String getName()
    {
        return _name;
    }

    /* ------------------------------------------------------------ */
    /** Get the number of threads in the pool.
     * @see #getIdleThreads
     * @return Number of threads
     */
    public int getThreads()
    {
        return _threads.size();
    }

    /* ------------------------------------------------------------ */
    /** Get the priority of the pool threads.
     *  @return the priority of the pool threads.
     */
    public int getThreadsPriority()
    {
        return _priority;
    }

    /* ------------------------------------------------------------ */
    public int getQueueSize()
    {
        return _queued;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return the spawnOrShrinkAt  The number of queued jobs (or idle threads) needed 
     * before the thread pool is grown (or shrunk)
     */
    public int getSpawnOrShrinkAt()
    {
        return _spawnOrShrinkAt;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param spawnOrShrinkAt The number of queued jobs (or idle threads) needed 
     * before the thread pool is grown (or shrunk)
     */
    public void setSpawnOrShrinkAt(int spawnOrShrinkAt)
    {
        _spawnOrShrinkAt=spawnOrShrinkAt;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return maximum total time that stop() will wait for threads to die.
     */
    public int getMaxStopTimeMs()
    {
        return _maxStopTimeMs;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param stopTimeMs maximum total time that stop() will wait for threads to die.
     */
    public void setMaxStopTimeMs(int stopTimeMs)
    {
        _maxStopTimeMs = stopTimeMs;
    }

    /* ------------------------------------------------------------ */
    /** 
     * Delegated to the named or anonymous Pool.
     */
    public boolean isDaemon()
    {
        return _daemon;
    }

    /* ------------------------------------------------------------ */
    public boolean isLowOnThreads()
    {
        return _queued>_lowThreads;
    }

    /* ------------------------------------------------------------ */
    public void join() throws InterruptedException
    {
        synchronized (_joinLock)
        {
            while (isRunning())
                _joinLock.wait();
        }
        
        // TODO remove this semi busy loop!
        while (isStopping())
            Thread.sleep(100);
    }

    /* ------------------------------------------------------------ */
    /** 
     * Delegated to the named or anonymous Pool.
     */
    public void setDaemon(boolean daemon)
    {
        _daemon=daemon;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param lowThreads low resource threads threshhold
     */
    public void setLowThreads(int lowThreads)
    {
        _lowThreads = lowThreads;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the maximum thread idle time.
     * Threads that are idle for longer than this period may be
     * stopped.
     * Delegated to the named or anonymous Pool.
     * @see #getMaxIdleTimeMs
     * @param maxIdleTimeMs Max idle time in ms.
     */
    public void setMaxIdleTimeMs(int maxIdleTimeMs)
    {
        _maxIdleTimeMs=maxIdleTimeMs;
    }

    /* ------------------------------------------------------------ */
    /** Set the maximum number of threads.
     * Delegated to the named or anonymous Pool.
     * @see #getMaxThreads
     * @param maxThreads maximum number of threads.
     */
    public void setMaxThreads(int maxThreads)
    {
        if (isStarted() && maxThreads<_minThreads)
            throw new IllegalArgumentException("!minThreads<maxThreads");
        _maxThreads=maxThreads;
    }

    /* ------------------------------------------------------------ */
    /** Set the minimum number of threads.
     * Delegated to the named or anonymous Pool.
     * @see #getMinThreads
     * @param minThreads minimum number of threads
     */
    public void setMinThreads(int minThreads)
    {
        if (isStarted() && (minThreads<=0 || minThreads>_maxThreads))
            throw new IllegalArgumentException("!0<=minThreads<maxThreads");
        _minThreads=minThreads;
        synchronized (_threadsLock)
        {
            while (isStarted() && _threads.size()<_minThreads)
            {
                newThread();   
            }
        }
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param name Name of the BoundedThreadPool to use when naming Threads.
     */
    public void setName(String name)
    {
        _name= name;
    }

    /* ------------------------------------------------------------ */
    /** Set the priority of the pool threads.
     *  @param priority the new thread priority.
     */
    public void setThreadsPriority(int priority)
    {
        _priority=priority;
    }

    /* ------------------------------------------------------------ */
    /* Start the BoundedThreadPool.
     * Construct the minimum number of threads.
     */
    protected void doStart() throws Exception
    {
        if (_maxThreads<_minThreads || _minThreads<=0)
            throw new IllegalArgumentException("!0<minThreads<maxThreads");
        
        _threads=new HashSet();
        _idle=new ArrayList();
        _jobs=new Runnable[_maxThreads];
        
        for (int i=0;i<_minThreads;i++)
        {
            newThread();
        }   
    }

    /* ------------------------------------------------------------ */
    /** Stop the BoundedThreadPool.
     * New jobs are no longer accepted,idle threads are interrupted
     * and stopJob is called on active threads.
     * The method then waits 
     * min(getMaxStopTimeMs(),getMaxIdleTimeMs()), for all jobs to
     * stop, at which time killJob is called.
     */
    protected void doStop() throws Exception
    {   
        super.doStop();
        
        long start=System.currentTimeMillis();
        for (int i=0;i<100;i++)
        {
            synchronized (_threadsLock)
            {
                Iterator iter = _threads.iterator();
                while (iter.hasNext())
                    ((Thread)iter.next()).interrupt();
            }
            
            Thread.yield();
            if (_threads.size()==0 || (_maxStopTimeMs>0 && _maxStopTimeMs < (System.currentTimeMillis()-start)))
               break;
            
            try
            {
                Thread.sleep(i*100);
            }
            catch(InterruptedException e){}
            
            
        }

        // TODO perhaps force stops
        if (_threads.size()>0)
            Log.warn(_threads.size()+" threads could not be stopped");
        
        synchronized (_joinLock)
        {
            _joinLock.notifyAll();
        }
    }

    /* ------------------------------------------------------------ */
    protected void newThread()
    {
        synchronized (_threadsLock)
        {
            if (_threads.size()<_maxThreads)
            {
                PoolThread thread =new PoolThread();
                _threads.add(thread);
                thread.setName(thread.hashCode()+"@"+_name+"-"+_id++);
                thread.start(); 
            }
            else if (!_warned)    
            {
                _warned=true;
                Log.debug("Max threads for {}",this);
            }
        }
    }

    /* ------------------------------------------------------------ */
    /** Stop a Job.
     * This method is called by the Pool if a job needs to be stopped.
     * The default implementation does nothing and should be extended by a
     * derived thread pool class if special action is required.
     * @param thread The thread allocated to the job, or null if no thread allocated.
     * @param job The job object passed to run.
     */
    protected void stopJob(Thread thread, Object job)
    {
        thread.interrupt();
    }
    

    /* ------------------------------------------------------------ */
    public String dump()
    {
        StringBuffer buf = new StringBuffer();

        synchronized (_threadsLock)
        {
            for (Iterator i=_threads.iterator();i.hasNext();)
            {
                Thread thread = (Thread)i.next();
                buf.append(thread.getName()).append(" ").append(thread.toString()).append('\n');
            }
        }
        
        return buf.toString();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param name The thread name to stop.
     * @return true if the thread was found and stopped.
     * @Deprecated Use {@link #interruptThread(long)} in preference
     */
    public boolean stopThread(String name)
    {
        synchronized (_threadsLock)
        {
            for (Iterator i=_threads.iterator();i.hasNext();)
            {
                Thread thread = (Thread)i.next();
                if (name.equals(thread.getName()))
                {
                    thread.stop();
                    return true;
                }
            }
        }
        return false;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param name The thread name to interrupt.
     * @return true if the thread was found and interrupted.
     */
    public boolean interruptThread(String name)
    {
        synchronized (_threadsLock)
        {
            for (Iterator i=_threads.iterator();i.hasNext();)
            {
                Thread thread = (Thread)i.next();
                if (name.equals(thread.getName()))
                {
                    thread.interrupt();
                    return true;
                }
            }
        }
        return false;
    }

    /* ------------------------------------------------------------ */
    /** Pool Thread class.
     * The PoolThread allows the threads job to be
     * retrieved and active status to be indicated.
     */
    public class PoolThread extends Thread 
    {
        Runnable _job=null;

        /* ------------------------------------------------------------ */
        PoolThread()
        {
            setDaemon(_daemon);
            setPriority(_priority);
        }
        
        /* ------------------------------------------------------------ */
        /** BoundedThreadPool run.
         * Loop getting jobs and handling them until idle or stopped.
         */
        public void run()
        {
            boolean idle=false;
            Runnable job=null;
            try
            {
                while (isRunning())
                {   
                    // Run any job that we have.
                    if (job!=null)
                    {
                        final Runnable todo=job;
                        job=null;
                        idle=false;
                        todo.run();
                    }
                    
                    synchronized(_lock)
                    {
                        // is there a queued job?
                        if (_queued>0)
                        {
                            _queued--;
                            job=_jobs[_nextJob];
                            _jobs[_nextJob++]=null;
                            if (_nextJob==_jobs.length)
                                _nextJob=0;
                            continue;
                        }

                        // Should we shrink?
                        final int threads=_threads.size();
                        if (threads>_minThreads && 
                            (threads>_maxThreads || 
                             _idle.size()>_spawnOrShrinkAt))   
                        {
                            long now = System.currentTimeMillis();
                            if ((now-_lastShrink)>getMaxIdleTimeMs())
                            {
                                _lastShrink=now;
                                _idle.remove(this);
                                return;
                            }
                        }

                        if (!idle)
                        {   
                            // Add ourselves to the idle set.
                            _idle.add(this);
                            idle=true;
                        }
                    }

                    // We are idle
                    // wait for a dispatched job
                    synchronized (this)
                    {
                        if (_job==null)
                            this.wait(getMaxIdleTimeMs());
                        job=_job;
                        _job=null;
                    }
                }
            }
            catch (InterruptedException e)
            {
                Log.ignore(e);
            }
            finally
            {
                synchronized (_lock)
                {
                    _idle.remove(this);
                }
                synchronized (_threadsLock)
                {
                    _threads.remove(this);
                }
                synchronized (this)
                {
                    job=_job;
                }
                
                // we died with a job! reschedule it
                if (job!=null)
                {
                    QueuedThreadPool.this.dispatch(job);
                }
            }
        }
        
        /* ------------------------------------------------------------ */
        void dispatch(Runnable job)
        {
            synchronized (this)
            {
                _job=job;
                this.notify();
            }
        }
    }

    private class Lock{}
}
