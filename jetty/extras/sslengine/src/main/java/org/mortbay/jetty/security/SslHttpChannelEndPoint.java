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

package org.mortbay.jetty.security;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import org.mortbay.io.Buffer;
import org.mortbay.io.Buffers;
import org.mortbay.io.nio.NIOBuffer;
import org.mortbay.io.nio.SelectChannelEndPoint;
import org.mortbay.io.nio.SelectorManager;
import org.mortbay.jetty.EofException;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.log.Log;

/* ------------------------------------------------------------ */
/**
 * SslHttpChannelEndPoint.
 * 
 * @author Nik Gonzalez <ngonzalez@exist.com>
 * @author Greg Wilkins <gregw@mortbay.com>
 */
public class SslHttpChannelEndPoint extends SelectChannelConnector.ConnectorEndPoint implements Runnable
{
    private static final ByteBuffer[] __NO_BUFFERS={};

    private Buffers _buffers;
    
    private SSLEngine _engine;
    private ByteBuffer _inBuffer;
    private NIOBuffer _inNIOBuffer;
    private ByteBuffer _outBuffer;
    private NIOBuffer _outNIOBuffer;
    
    private ByteBuffer[] _gather=new ByteBuffer[2];

    private boolean _closing=false;
    private SSLEngineResult _result;
    
    private boolean _handshook=false;
    private boolean _allowRenegotiate=false;


    // ssl
    protected SSLSession _session;
    
    /* ------------------------------------------------------------ */
    public SslHttpChannelEndPoint(Buffers buffers,SocketChannel channel, SelectorManager.SelectSet selectSet, SelectionKey key, SSLEngine engine)
            throws SSLException, IOException
    {
        super(channel,selectSet,key);
        _buffers=buffers;
        
        // ssl
        _engine=engine;
        _session=engine.getSession();

        // TODO pool buffers and use only when needed.
        _outNIOBuffer=(NIOBuffer)buffers.getBuffer(_session.getPacketBufferSize());
        _outBuffer=_outNIOBuffer.getByteBuffer();
        _inNIOBuffer=(NIOBuffer)buffers.getBuffer(_session.getPacketBufferSize());
        _inBuffer=_inNIOBuffer.getByteBuffer();
    }


    /* ------------------------------------------------------------ */
    /**
     * @return True if SSL re-negotiation is allowed (default false)
     */
    public boolean isAllowRenegotiate()
    {
        return _allowRenegotiate;
    }

    /* ------------------------------------------------------------ */
    /**
     * Set if SSL re-negotiation is allowed. CVE-2009-3555 discovered
     * a vulnerability in SSL/TLS with re-negotiation.  If your JVM
     * does not have CVE-2009-3555 fixed, then re-negotiation should 
     * not be allowed.
     * @param allowRenegotiate true if re-negotiation is allowed (default false)
     */
    public void setAllowRenegotiate(boolean allowRenegotiate)
    {
        _allowRenegotiate = allowRenegotiate;
    }

    /* ------------------------------------------------------------ */
    // TODO get rid of these dumps
    public void dump()
    {
        System.err.println(_result);
        // System.err.println(h.toString());
        // System.err.println("--");
    }
    
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.io.nio.SelectChannelEndPoint#idleExpired()
     */
    protected void idleExpired()
    {
        try
        {
            _selectSet.getManager().dispatch(new Runnable()
            {
                public void run() 
                { 
                    doIdleExpired();
                }
            });
        }
        catch(Exception e)
        {
            Log.ignore(e);
        }
    }
    
    /* ------------------------------------------------------------ */
    protected void doIdleExpired()
    {
        super.idleExpired();
    }

    /* ------------------------------------------------------------ */
    public void shutdownOutput() throws IOException
    {
        close();
    }
    
    /* ------------------------------------------------------------ */
    public void close() throws IOException
    {
        // TODO - this really should not be done in a loop here - but with async callbacks.

        _closing=true;
        long end=System.currentTimeMillis()+((SocketChannel)_channel).socket().getSoTimeout();
        try
        {   
            if (isBufferingOutput())
            {
                flush();
                while (isOpen() && isBufferingOutput() && System.currentTimeMillis()<end)
                {
                    Thread.sleep(100); // TODO non blocking
                    flush();
                }
            }

            _engine.closeOutbound();

            loop: while (isOpen() && !(_engine.isInboundDone() && _engine.isOutboundDone()) && System.currentTimeMillis()<end)
            {   
                if (isBufferingOutput())
                {
                    flush();
                    while (isOpen() && isBufferingOutput() && System.currentTimeMillis()<end)
                    {
                        Thread.sleep(100); // TODO non blocking
                        flush();
                    }
                }

                // System.err.println(_channel+" close "+_engine.getHandshakeStatus()+" "+_closing);
                switch(_engine.getHandshakeStatus())
                {
                    case FINISHED:
                    case NOT_HANDSHAKING:
                        _handshook=true;
                        break loop;
                        
                    case NEED_UNWRAP:
                        Buffer buffer =_buffers.getBuffer(_engine.getSession().getApplicationBufferSize());
                        try
                        {
                            ByteBuffer bbuffer = ((NIOBuffer)buffer).getByteBuffer();
                            if (!unwrap(bbuffer) && _engine.getHandshakeStatus()==HandshakeStatus.NEED_UNWRAP)
                            {
                                break loop;
                            }
                        }
                        catch(SSLException e)
                        {
                            Log.ignore(e);
                        }
                        finally
                        {
                            _buffers.returnBuffer(buffer);
                        }
                        break;
                        
                    case NEED_TASK:
                    {
                        Runnable task;
                        while ((task=_engine.getDelegatedTask())!=null)
                        {
                            task.run();
                        }
                        break;
                    }
                        
                    case NEED_WRAP:
                    {
                        try
                        {
                            _outNIOBuffer.compact();
                            int put=_outNIOBuffer.putIndex();
                            _outBuffer.position(put);
                            _result=null;
                            _result=_engine.wrap(__NO_BUFFERS,_outBuffer);
                            _outNIOBuffer.setPutIndex(put+_result.bytesProduced());
                        }
                        finally
                        {
                            _outBuffer.position(0);
                        }
                        
                        break;
                    }
                }
            }
        }
        catch(IOException e)
        {
            Log.ignore(e);
        }
        catch (InterruptedException e)
        {
            Log.ignore(e);
        }
        finally
        {
            super.close();
            
            if (_inNIOBuffer!=null)
                _buffers.returnBuffer(_inNIOBuffer);
            if (_outNIOBuffer!=null)
                _buffers.returnBuffer(_outNIOBuffer);
        }   
    }


    
    /* ------------------------------------------------------------ */
    /* 
     */
    public int fill(Buffer buffer) throws IOException
    {
        ByteBuffer bbuf=extractInputBuffer(buffer);
        int size=buffer.length();
        HandshakeStatus initialStatus = _engine.getHandshakeStatus();
        synchronized (bbuf)
        {
            try
            {
                unwrap(bbuf);

                int wraps=0;
                loop: while (true)
                {
                    if (isBufferingOutput())
                    {
                        flush();
                        if (isBufferingOutput())
                            break loop;
                    }

                    // System.err.println(_channel+" fill  "+_engine.getHandshakeStatus()+" "+_closing);
                    switch(_engine.getHandshakeStatus())
                    {
                        case FINISHED:
                        case NOT_HANDSHAKING:
                            if (_closing)
                                return -1;
                            break loop;

                        case NEED_UNWRAP:
                            checkRenegotiate();
                            if (!unwrap(bbuf) && _engine.getHandshakeStatus()==HandshakeStatus.NEED_UNWRAP)
                            {
                                break loop;
                            }
                            break;

                        case NEED_TASK:
                        {
                            Runnable task;
                            while ((task=_engine.getDelegatedTask())!=null)
                            {
                                task.run();
                            }
                            
                            if(initialStatus==HandshakeStatus.NOT_HANDSHAKING && 
                               _engine.getHandshakeStatus()==HandshakeStatus.NEED_UNWRAP && wraps==0)
                            {
                                // This should be NEED_WRAP
                                // The fix simply detects the signature of the bug and then close the connection (fail-fast) so that ff3 will delegate to using SSL instead of TLS.
                                // This is a jvm bug on java1.6 where the SSLEngine expects more data from the initial handshake when the client(ff3-tls) already had given it.
                                // See http://jira.codehaus.org/browse/JETTY-567 for more details
                                return -1;
                            }
                            break;
                        }

                        case NEED_WRAP:
                        {
                            checkRenegotiate();
                            wraps++;
                            synchronized(_outBuffer)
                            {
                                try
                                {
                                    _outNIOBuffer.compact();
                                    int put=_outNIOBuffer.putIndex();
                                    _outBuffer.position();
                                    _result=null;
                                    _result=_engine.wrap(__NO_BUFFERS,_outBuffer);
                                    switch(_result.getStatus())
                                    {
                                        case BUFFER_OVERFLOW:
                                        case BUFFER_UNDERFLOW:
                                            Log.warn("wrap {}",_result);
                                        case CLOSED:
                                            _closing=true;
                                    }
                                    
                                    _outNIOBuffer.setPutIndex(put+_result.bytesProduced());
                                }
                                finally
                                {
                                    _outBuffer.position(0);
                                }
                            }

                            flush();

                            break;
                        }
                    }
                }
            }
            catch(SSLException e)
            {
                Log.warn(e.toString());
                Log.debug(e);
                throw e;
            }
            finally
            {
                buffer.setPutIndex(bbuf.position());
                bbuf.position(0);
            }
            
            int filled=buffer.length()-size; 
            if (filled>0)
                _handshook=true;
            return filled; 
        }
        
    }

    /* ------------------------------------------------------------ */
    public int flush(Buffer buffer) throws IOException
    {
        return flush(buffer,null,null);
    }


    /* ------------------------------------------------------------ */
    /*     
     */
    public int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException
    {   
        int consumed=0;
        int available=header.length();
        if (buffer!=null)
            available+=buffer.length();
        
        int tries=0;
        loop: while (true)
        {   
            if (_outNIOBuffer.length()>0)
            {
                flush();
                if (isBufferingOutput())
                    break loop;
            }

            // System.err.println(_channel+" flush "+_engine.getHandshakeStatus()+" "+_closing);
            switch(_engine.getHandshakeStatus())
            {
                case FINISHED:
                case NOT_HANDSHAKING:
                    if (_closing || available==0)
                    {
                        if (consumed==0)
                            consumed= -1;
                        break loop;
                    }
                        
                    int c;
                    if (header!=null && header.length()>0)
                    {
                        if (buffer!=null && buffer.length()>0)
                            c=wrap(header,buffer);
                        else
                            c=wrap(header);
                    }
                    else 
                        c=wrap(buffer);
                    
                    if (c>0)
                    {
                        _handshook=true;
                        consumed+=c;
                        available-=c;
                    }
                    else if (c<0)
                    {
                        if (consumed==0)
                            consumed=-1;
                        break loop;
                    }
                    
                    break;

                case NEED_UNWRAP:
                    checkRenegotiate();
                    Buffer buf =_buffers.getBuffer(_engine.getSession().getApplicationBufferSize());
                    try
                    {
                        ByteBuffer bbuf = ((NIOBuffer)buf).getByteBuffer();
                        if (!unwrap(bbuf) && _engine.getHandshakeStatus()==HandshakeStatus.NEED_UNWRAP)
                        {
                            break loop;
                        }
                    }
                    finally
                    {
                        _buffers.returnBuffer(buf);
                    }
                    
                    break;

                case NEED_TASK:
                {
                    Runnable task;
                    while ((task=_engine.getDelegatedTask())!=null)
                    {
                        task.run();
                    }
                    break;
                }

                case NEED_WRAP:
                {
                    checkRenegotiate();
                    synchronized(_outBuffer)
                    {
                        try
                        {
                            _outNIOBuffer.compact();
                            int put=_outNIOBuffer.putIndex();
                            _outBuffer.position();
                            _result=null;
                            _result=_engine.wrap(__NO_BUFFERS,_outBuffer);
                            switch(_result.getStatus())
                            {
                                case BUFFER_OVERFLOW:
                                case BUFFER_UNDERFLOW:
                                    Log.warn("wrap {}",_result);
                                case CLOSED:
                                    _closing=true;
                            }
                            _outNIOBuffer.setPutIndex(put+_result.bytesProduced());
                        }
                        finally
                        {
                            _outBuffer.position(0);
                        }
                    }

                    flush();
                    if (isBufferingOutput())
                        break loop;

                    break;
                }
            }
        }
        
        return consumed;
    }
    
    /* ------------------------------------------------------------ */
    public void flush() throws IOException
    {
        int len=_outNIOBuffer.length();
        if (len>0)
        {
            int flushed=super.flush(_outNIOBuffer);
            len=_outNIOBuffer.length();
            
            if (len>0)
            {
                Thread.yield();
                flushed=super.flush(_outNIOBuffer);
                len=_outNIOBuffer.length();
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    private void checkRenegotiate() throws IOException
    {
        if (_handshook && !_allowRenegotiate && _channel!=null && _channel.isOpen())
        {
            Log.warn("SSL renegotiate denied: "+_channel);
            close();
        }   
    }

    /* ------------------------------------------------------------ */
    private ByteBuffer extractInputBuffer(Buffer buffer)
    {
        assert buffer instanceof NIOBuffer;
        NIOBuffer nbuf=(NIOBuffer)buffer;
        ByteBuffer bbuf=nbuf.getByteBuffer();
        bbuf.position(buffer.putIndex());
        return bbuf;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return true if progress is made
     */
    private boolean unwrap(ByteBuffer buffer) throws IOException
    {
        if (_inNIOBuffer.hasContent())
            _inNIOBuffer.compact();
        else 
            _inNIOBuffer.clear();

        int total_filled=0;
        
        while (_inNIOBuffer.space()>0 && super.isOpen())
        {
            try
            {
                int filled=super.fill(_inNIOBuffer);
                if (filled<=0)
                    break;
                total_filled+=filled;
            }
            catch(IOException e)
            {
                if (_inNIOBuffer.length()==0)
                {
                    _outNIOBuffer.clear();
                    throw e;
                }
                break;
            }
        }
        
        if (total_filled==0 && _inNIOBuffer.length()==0)
        {
            if(!isOpen())
            {
                _outNIOBuffer.clear();
                throw new EofException();
            }
            return false;
        }

        try
        {
            _inBuffer.position(_inNIOBuffer.getIndex());
            _inBuffer.limit(_inNIOBuffer.putIndex());
            _result=null;
            _result=_engine.unwrap(_inBuffer,buffer);
            _inNIOBuffer.skip(_result.bytesConsumed());
        }
        finally
        {
            _inBuffer.position(0);
            _inBuffer.limit(_inBuffer.capacity());
        }
        
        switch(_result.getStatus())
        {
            case BUFFER_OVERFLOW:
                throw new IllegalStateException(_result.toString());                        
                
            case BUFFER_UNDERFLOW:
                if (Log.isDebugEnabled()) 
                    Log.debug("unwrap {}",_result);
                if(!isOpen())
                {
                    _inNIOBuffer.clear();
                    _outNIOBuffer.clear();
                    throw new EofException();
                }
                return (total_filled > 0);
                
            case CLOSED:
                _closing=true;
                
            case OK:
                boolean progress=total_filled>0 ||_result.bytesConsumed()>0 || _result.bytesProduced()>0; 
                return progress;
            default:
                Log.warn("unwrap "+_result);
                throw new IOException(_result.toString());
        }
    }

    /* ------------------------------------------------------------ */
    private ByteBuffer extractOutputBuffer(Buffer buffer,int n)
    {
        if (buffer.buffer() instanceof NIOBuffer)
            return ((NIOBuffer)buffer.buffer()).getByteBuffer();
        
        return ByteBuffer.wrap(buffer.array());
    }

    /* ------------------------------------------------------------ */
    private int wrap(Buffer header, Buffer buffer) throws IOException
    {
        _gather[0]=extractOutputBuffer(header,0);
        synchronized(_gather[0])
        {
            _gather[0].position(header.getIndex());
            _gather[0].limit(header.putIndex());

            _gather[1]=extractOutputBuffer(buffer,1);

            synchronized(_gather[1])
            {
                _gather[1].position(buffer.getIndex());
                _gather[1].limit(buffer.putIndex());

                synchronized(_outBuffer)
                {
                    int consumed=0;
                    try
                    {
                        _outNIOBuffer.clear();
                        _outBuffer.position(0);
                        _outBuffer.limit(_outBuffer.capacity());

                        _result=null;
                        _result=_engine.wrap(_gather,_outBuffer);
                        _outNIOBuffer.setGetIndex(0);
                        _outNIOBuffer.setPutIndex(_result.bytesProduced());
                        consumed=_result.bytesConsumed();
                    }
                    finally
                    {
                        _outBuffer.position(0);

                        if (consumed>0 && header!=null)
                        {
                            int len=consumed<header.length()?consumed:header.length();
                            header.skip(len);
                            consumed-=len;
                            _gather[0].position(0);
                            _gather[0].limit(_gather[0].capacity());
                        }
                        if (consumed>0 && buffer!=null)
                        {
                            int len=consumed<buffer.length()?consumed:buffer.length();
                            buffer.skip(len);
                            consumed-=len;
                            _gather[1].position(0);
                            _gather[1].limit(_gather[1].capacity());
                        }
                        assert consumed==0;
                    }
                }
            }
        }
        

        switch(_result.getStatus())
        {
            case BUFFER_OVERFLOW:
            case BUFFER_UNDERFLOW:
                Log.warn("wrap {}",_result);
                
            case OK:
                return _result.bytesConsumed();
            case CLOSED:
                _closing=true;
                return _result.bytesConsumed()>0?_result.bytesConsumed():-1;

            default:
                Log.warn("wrap "+_result);
            throw new IOException(_result.toString());
        }
    }

    /* ------------------------------------------------------------ */
    private int wrap(Buffer buffer) throws IOException
    {
        _gather[0]=extractOutputBuffer(buffer,0);
        synchronized(_gather[0])
        {
            _gather[0].position(buffer.getIndex());
            _gather[0].limit(buffer.putIndex());

            int consumed=0;
            synchronized(_outBuffer)
            {
                try
                {
                    _outNIOBuffer.clear();
                    _outBuffer.position(0);
                    _outBuffer.limit(_outBuffer.capacity());
                    _result=null;
                    _result=_engine.wrap(_gather[0],_outBuffer);
                    _outNIOBuffer.setGetIndex(0);
                    _outNIOBuffer.setPutIndex(_result.bytesProduced());
                    consumed=_result.bytesConsumed();
                }
                finally
                {
                    _outBuffer.position(0);

                    if (consumed>0 && buffer!=null)
                    {
                        int len=consumed<buffer.length()?consumed:buffer.length();
                        buffer.skip(len);
                        consumed-=len;
                        _gather[0].position(0);
                        _gather[0].limit(_gather[0].capacity());
                    }
                    assert consumed==0;
                }
            }
        }
        switch(_result.getStatus())
        {
            case BUFFER_OVERFLOW:
            case BUFFER_UNDERFLOW:
                Log.warn("wrap {}",_result);
                
            case OK:
                return _result.bytesConsumed();
            case CLOSED:
                _closing=true;
                return _result.bytesConsumed()>0?_result.bytesConsumed():-1;

            default:
                Log.warn("wrap "+_result);
            throw new IOException(_result.toString());
        }
    }

    /* ------------------------------------------------------------ */
    public boolean isBufferingInput()
    {
        return _inNIOBuffer.hasContent();
    }

    /* ------------------------------------------------------------ */
    public boolean isBufferingOutput()
    {
        return _outNIOBuffer.hasContent();
    }

    /* ------------------------------------------------------------ */
    public boolean isBufferred()
    {
        return true;
    }

    /* ------------------------------------------------------------ */
    public SSLEngine getSSLEngine()
    {
        return _engine;
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return super.toString()+","+_engine.getHandshakeStatus()+", in/out="+_inNIOBuffer.length()+"/"+_outNIOBuffer.length()+" "+_result;
    }
}
