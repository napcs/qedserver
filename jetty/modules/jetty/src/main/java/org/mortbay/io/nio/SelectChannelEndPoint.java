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

package org.mortbay.io.nio;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.mortbay.io.Buffer;
import org.mortbay.io.Connection;
import org.mortbay.io.nio.SelectorManager.SelectSet;
import org.mortbay.jetty.EofException;
import org.mortbay.jetty.HttpException;
import org.mortbay.log.Log;
import org.mortbay.thread.Timeout;

/* ------------------------------------------------------------ */
/**
 * An Endpoint that can be scheduled by {@link SelectorManager}.
 * 
 * @author gregw
 *
 */
public class SelectChannelEndPoint extends ChannelEndPoint implements Runnable
{
    protected SelectorManager _manager;
    protected SelectorManager.SelectSet _selectSet;
    protected boolean _dispatched = false;
    protected boolean _writable = true; 
    protected SelectionKey _key;
    protected int _interestOps;
    protected boolean _readBlocked;
    protected boolean _writeBlocked;
    protected Connection _connection;

    private Timeout.Task _timeoutTask = new IdleTask();

    /* ------------------------------------------------------------ */
    public Connection getConnection()
    {
        return _connection;
    }
    
    /* ------------------------------------------------------------ */
    public SelectChannelEndPoint(SocketChannel channel, SelectSet selectSet, SelectionKey key)
    {
        super(channel);

        _manager = selectSet.getManager();
        _selectSet = selectSet;
        _connection = _manager.newConnection(channel,this);
        
        _manager.endPointOpened(this); // TODO not here!
        
        _key = key;
    }

    /* ------------------------------------------------------------ */
    void dispatch() throws IOException
    {
        boolean dispatch_done = true;
        try
        {
            if (dispatch(_manager.isDelaySelectKeyUpdate()))
            {
                dispatch_done= false;
                dispatch_done = _manager.dispatch((Runnable)this);
            }
        }
        finally
        {
            if (!dispatch_done)
            {
                Log.warn("dispatch failed!");
                undispatch();
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Put the endpoint into the dispatched state.
     * A blocked thread may be woken up by this call, or the endpoint placed in a state ready
     * for a dispatch to a threadpool.
     * @param assumeShortDispatch If true, the interested ops are not modified.
     * @return True if the endpoint should be dispatched to a thread pool.
     * @throws IOException
     */
    public boolean dispatch(boolean assumeShortDispatch) throws IOException
    {
        // If threads are blocked on this
        synchronized (this)
        {
            if (_key == null || !_key.isValid())
            {
                _readBlocked=false;
                _writeBlocked=false;
                this.notifyAll();
                return false;
            }
            
            if (_readBlocked || _writeBlocked)
            {
                if (_readBlocked && _key.isReadable())
                    _readBlocked=false;
                if (_writeBlocked && _key.isWritable())
                    _writeBlocked=false;

                // wake them up is as good as a dispatched.
                this.notifyAll();
                
                // we are not interested in further selecting
                _key.interestOps(0);
                return false;
            }

            if (!assumeShortDispatch)
                _key.interestOps(0);

            // Otherwise if we are still dispatched
            if (_dispatched)
            {
                // we are not interested in further selecting
                _key.interestOps(0);
                return false;
            }

            // Remove writeable op
            if ((_key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE && (_key.interestOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE)
            {
                // Remove writeable op
                _interestOps = _key.interestOps() & ~SelectionKey.OP_WRITE;
                _key.interestOps(_interestOps);
                _writable = true; // Once writable is in ops, only removed with dispatch.
            }

            _dispatched = true;
        }
        return true;
    }

    /* ------------------------------------------------------------ */
    public void scheduleIdle()
    {
        _selectSet.scheduleIdle(_timeoutTask);
    }

    /* ------------------------------------------------------------ */
    public void cancelIdle()
    {
        _selectSet.cancelIdle(_timeoutTask);
    }


    /* ------------------------------------------------------------ */
    protected void idleExpired()
    {
        try
        {
            close();
        }
        catch (IOException e)
        {
            Log.ignore(e);
        }
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Called when a dispatched thread is no longer handling the endpoint. The selection key
     * operations are updated.
     */
    public void undispatch()
    {
        synchronized (this)
        {
            try
            {
                _dispatched = false;
                updateKey();
            }
            catch (Exception e)
            {
                // TODO investigate if this actually is a problem?
                Log.ignore(e);
                _interestOps = -1;
                _selectSet.addChange(this);
            }
        }
    }

    /* ------------------------------------------------------------ */
    /*
     */
    public int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException
    {
        int l = super.flush(header, buffer, trailer);
        _writable = l > 0;
        return l;
    }

    /* ------------------------------------------------------------ */
    /*
     */
    public int flush(Buffer buffer) throws IOException
    {
        int l = super.flush(buffer);
        _writable = l > 0;
        return l;
    }

    /* ------------------------------------------------------------ */
    /*
     * Allows thread to block waiting for further events.
     */
    public boolean blockReadable(long timeoutMs) throws IOException
    {
        synchronized (this)
        {
            long start=_selectSet.getNow();
            try
            {   
                _readBlocked=true;
                while (isOpen() && _readBlocked)
                {
                    try
                    {
                        updateKey();
                        this.wait(timeoutMs);

                        if (_readBlocked && timeoutMs<(_selectSet.getNow()-start))
                            return false;
                    }
                    catch (InterruptedException e)
                    {
                        Log.warn(e);
                    }
                }
            }
            finally
            {
                _readBlocked=false;
            }
        }
        return true;
    }

    /* ------------------------------------------------------------ */
    /*
     * Allows thread to block waiting for further events.
     */
    public boolean blockWritable(long timeoutMs) throws IOException
    {
        synchronized (this)
        {
            long start=_selectSet.getNow();
            try
            {   
                _writeBlocked=true;
                while (isOpen() && _writeBlocked)
                {
                    try
                    {
                        updateKey();
                        this.wait(timeoutMs);

                        if (_writeBlocked && timeoutMs<(_selectSet.getNow()-start))
                            return false;
                    }
                    catch (InterruptedException e)
                    {
                        Log.warn(e);
                    }
                }
            }
            finally
            {
                _writeBlocked=false;
                scheduleIdle();
            }
        }
        return true;
    }

    /* ------------------------------------------------------------ */
    public void setWritable(boolean writable)
    {
        _writable=writable;
    }
    
    /* ------------------------------------------------------------ */
    public void scheduleWrite()
    {
        _writable=false;
        updateKey();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Updates selection key. Adds operations types to the selection key as needed. No operations
     * are removed as this is only done during dispatch. This method records the new key and
     * schedules a call to doUpdateKey to do the keyChange
     */
    private void updateKey()
    {
        synchronized (this)
        {
            int ops=-1;
            if (getChannel().isOpen())
            {
                ops = ((_key!=null && _key.isValid())?_key.interestOps():-1);
                _interestOps = 
                    ((!_dispatched || _readBlocked)  ? SelectionKey.OP_READ  : 0) 
                |   ((!_writable   || _writeBlocked) ? SelectionKey.OP_WRITE : 0);
            }
            if(_interestOps == ops && getChannel().isOpen())
                return;
            
        }
        _selectSet.addChange(this);
        _selectSet.wakeup();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Synchronize the interestOps with the actual key. Call is scheduled by a call to updateKey
     */
    void doUpdateKey()
    {
        synchronized (this)
        {
            if (getChannel().isOpen())
            {
                if (_interestOps>0)
                {
                    if (_key==null || !_key.isValid())
                    {
                        SelectableChannel sc = (SelectableChannel)getChannel();
                        if (sc.isRegistered())
                        {
                            updateKey();   
                        }
                        else
                        {
                            try
                            {
                                _key=((SelectableChannel)getChannel()).register(_selectSet.getSelector(),_interestOps,this);
                            }
                            catch (Exception e)
                            {
                                Log.ignore(e);
                                if (_key!=null && _key.isValid())
                                {
                                    _key.cancel();
                                }
                                cancelIdle();
                                _manager.endPointClosed(this);
                                _key = null;
                            }
                        }
                    }
                    else
                    {
                        _key.interestOps(_interestOps);
                    }
                }
                else
                {
                    if (_key!=null && _key.isValid())
                        _key.interestOps(0);
                    else
                        _key=null;
                }
            }
            else    
            {
                if (_key!=null && _key.isValid())
                {
                    _key.interestOps(0);
                    _key.cancel(); 
                }
                cancelIdle();
                _manager.endPointClosed(this);
                _key = null;
            }
        }
    }

    /* ------------------------------------------------------------ */
    /* 
     */
    public void run()
    {
        try
        {
            _connection.handle();
        }
        catch (ClosedChannelException e)
        {
            Log.ignore(e);
        }
        catch (EofException e)
        {
            Log.debug("EOF", e);
            try{close();}
            catch(IOException e2){Log.ignore(e2);}
        }
        catch (HttpException e)
        {
            Log.debug("BAD", e);
            try{close();}
            catch(IOException e2){Log.ignore(e2);}
        }
        catch (Throwable e)
        {
            Log.warn("handle failed", e);
            try{close();}
            catch(IOException e2){Log.ignore(e2);}
        }
        finally
        {
            undispatch();
        }
    }

    /* ------------------------------------------------------------ */
    /*
     * @see org.mortbay.io.nio.ChannelEndPoint#close()
     */
    public void close() throws IOException
    {
        try
        {
            super.close();
        }
        catch (IOException e)
        {
            Log.ignore(e);
        }   
        finally
        {
            updateKey();
        }
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "SCEP@" + hashCode() + "[d=" + _dispatched + ",io=" + _interestOps + ",w=" + _writable + ",b=" + _readBlocked + "|" + _writeBlocked + "]";
    }

    /* ------------------------------------------------------------ */
    public Timeout.Task getTimeoutTask()
    {
        return _timeoutTask;
    }

    /* ------------------------------------------------------------ */
    public SelectSet getSelectSet()
    {
        return _selectSet;
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public class IdleTask extends Timeout.Task 
    {
        /* ------------------------------------------------------------ */
        /*
         * @see org.mortbay.thread.Timeout.Task#expired()
         */
        public void expired()
        {
            idleExpired();
        }

        public String toString()
        {
            return "TimeoutTask:" + SelectChannelEndPoint.this.toString();
        }

    }

}
