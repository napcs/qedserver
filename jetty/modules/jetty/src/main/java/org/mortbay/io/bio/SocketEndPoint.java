//========================================================================
//$Id: SocketEndPoint.java,v 1.1 2005/10/05 14:09:39 janb Exp $
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

package org.mortbay.io.bio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.mortbay.io.Portable;
import org.mortbay.log.Log;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SocketEndPoint extends StreamEndPoint
{
    final Socket _socket;
    final InetSocketAddress _local;
    final InetSocketAddress _remote;

    /**
     * 
     */
    public SocketEndPoint(Socket socket)
    	throws IOException	
    {
        super(socket.getInputStream(),socket.getOutputStream());
        _socket=socket;
        _local=(InetSocketAddress)_socket.getLocalSocketAddress();
        _remote=(InetSocketAddress)_socket.getRemoteSocketAddress();
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.BufferIO#isClosed()
     */
    public boolean isOpen()
    {
        return super.isOpen() && _socket!=null && !_socket.isClosed() && !_socket.isInputShutdown() && !_socket.isOutputShutdown();
    }

    /* ------------------------------------------------------------ */
    /*
     * @see org.eclipse.jetty.io.bio.StreamEndPoint#shutdownOutput()
     */
    public void shutdownOutput() throws IOException
    {    
        if (!_socket.isClosed() && !_socket.isOutputShutdown())
            _socket.shutdownOutput();
    }
    
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.eclipse.io.BufferIO#close()
     */
    public void close() throws IOException
    {
        _socket.close();
        _in=null;
        _out=null;
    }
    

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#getLocalAddr()
     */
    public String getLocalAddr()
    {
       if (_local==null || _local.getAddress()==null || _local.getAddress().isAnyLocalAddress())
           return Portable.ALL_INTERFACES;
        
        return _local.getAddress().getHostAddress();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#getLocalHost()
     */
    public String getLocalHost()
    {
       if (_local==null || _local.getAddress()==null || _local.getAddress().isAnyLocalAddress())
           return Portable.ALL_INTERFACES;
        
        return _local.getAddress().getCanonicalHostName();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#getLocalPort()
     */
    public int getLocalPort()
    {
        if (_local==null)
            return -1;
        return _local.getPort();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#getRemoteAddr()
     */
    public String getRemoteAddr()
    {
        if (_remote==null)
            return null;
        InetAddress addr = _remote.getAddress();
        return ( addr == null ? null : addr.getHostAddress() );
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#getRemoteHost()
     */
    public String getRemoteHost()
    {
        if (_remote==null)
            return null;
        return _remote.getAddress().getCanonicalHostName();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#getRemotePort()
     */
    public int getRemotePort()
    {
        if (_remote==null)
            return -1;
        return _remote.getPort();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#getConnection()
     */
    public Object getTransport()
    {
        return _socket;
    }
}
