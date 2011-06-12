// ========================================================================
// Copyright 2003-2005 Mort Bay Consulting Pty. Ltd.
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
 
package org.mortbay.jetty.bio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.EndPoint;
import org.mortbay.io.bio.SocketEndPoint;
import org.mortbay.jetty.AbstractConnector;
import org.mortbay.jetty.EofException;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpException;
import org.mortbay.jetty.Request;
import org.mortbay.log.Log;


/* ------------------------------------------------------------------------------- */
/**  Socket Connector.
 * This connector implements a traditional blocking IO and threading model.
 * Normal JRE sockets are used and a thread is allocated per connection.
 * Buffers are managed so that large buffers are only allocated to active connections.
 * 
 * This Connector should only be used if NIO is not available.
 * 
 * @org.apache.xbean.XBean element="bioConnector" description="Creates a BIO based socket connector"
 * 
 * @author gregw
 */
public class SocketConnector extends AbstractConnector
{
    protected ServerSocket _serverSocket;
    protected Set _connections;
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     * 
     */
    public SocketConnector()
    {
    }

    /* ------------------------------------------------------------ */
    public Object getConnection()
    {
        return _serverSocket;
    }
    
    /* ------------------------------------------------------------ */
    public void open() throws IOException
    {
        // Create a new server socket and set to non blocking mode
        if (_serverSocket==null || _serverSocket.isClosed())
        _serverSocket= newServerSocket(getHost(),getPort(),getAcceptQueueSize());
        _serverSocket.setReuseAddress(getReuseAddress());
    }

    /* ------------------------------------------------------------ */
    protected ServerSocket newServerSocket(String host, int port,int backlog) throws IOException
    {
        ServerSocket ss= host==null?
            new ServerSocket(port,backlog):
            new ServerSocket(port,backlog,InetAddress.getByName(host));
       
        return ss;
    }
    
    /* ------------------------------------------------------------ */
    public void close() throws IOException
    {
        if (_serverSocket!=null)
            _serverSocket.close();
        _serverSocket=null;
    }

    /* ------------------------------------------------------------ */
    public void accept(int acceptorID)
    	throws IOException, InterruptedException
    {   
        Socket socket = _serverSocket.accept();
        configure(socket);
        
        Connection connection=new Connection(socket);
        connection.dispatch();
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * Allows subclass to override Conection if required.
     */
    protected HttpConnection newHttpConnection(EndPoint endpoint) 
    {
        return new HttpConnection(this, endpoint, getServer());
    }

    /* ------------------------------------------------------------------------------- */
    protected Buffer newBuffer(int size)
    {
        return new ByteArrayBuffer(size);
    }

    /* ------------------------------------------------------------------------------- */
    public void customize(EndPoint endpoint, Request request)
        throws IOException
    {
        Connection connection = (Connection)endpoint;
        if (connection._sotimeout!=_maxIdleTime)
        {
            connection._sotimeout=_maxIdleTime;
            ((Socket)endpoint.getTransport()).setSoTimeout(_maxIdleTime);
        }
              
        super.customize(endpoint, request);
    }

    /* ------------------------------------------------------------------------------- */
    public int getLocalPort()
    {
        if (_serverSocket==null || _serverSocket.isClosed())
            return -1;
        return _serverSocket.getLocalPort();
    }

    /* ------------------------------------------------------------------------------- */
    protected void doStart() throws Exception
    {
        _connections=new HashSet();
        super.doStart();
    }

    /* ------------------------------------------------------------------------------- */
    protected void doStop() throws Exception
    {
        super.doStop();
        Set set=null;

        synchronized(_connections)
        {
            set= new HashSet(_connections);
        }
        
        Iterator iter=set.iterator();
        while(iter.hasNext())
        {
            Connection connection = (Connection)iter.next();
            connection.close();
        }
    }

    /* ------------------------------------------------------------------------------- */
    /* ------------------------------------------------------------------------------- */
    /* ------------------------------------------------------------------------------- */
    protected class Connection extends SocketEndPoint implements Runnable
    {
        boolean _dispatched=false;
        HttpConnection _connection;
        int _sotimeout;
        protected Socket _socket;
        
        public Connection(Socket socket) throws IOException
        {
            super(socket);
            _connection = newHttpConnection(this);
            _sotimeout=socket.getSoTimeout();
            _socket=socket;
        }
        
        public void dispatch() throws InterruptedException, IOException
        {
            if (getThreadPool()==null || !getThreadPool().dispatch(this))
            {
                Log.warn("dispatch failed for {}",_connection);
                close();
            }
        }
        
        public int fill(Buffer buffer) throws IOException
        {
            int l = super.fill(buffer);
            if (l<0)
                close();
            return l;
        }
        
        public void run()
        {
            try
            {
                connectionOpened(_connection);
                synchronized(_connections)
                {
                    _connections.add(this);
                }
                
                while (isStarted() && !isClosed())
                {
                    if (_connection.isIdle())
                    {
                        if (getServer().getThreadPool().isLowOnThreads())
                        {
                            int lrmit = getLowResourceMaxIdleTime();
                            if (lrmit>=0 && _sotimeout!= lrmit)
                            {
                                _sotimeout=lrmit;
                                _socket.setSoTimeout(_sotimeout);
                            }
                        }
                    }                    
                    _connection.handle();
                }
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
            catch(Throwable e)
            {
                Log.warn("handle failed",e);
                try{close();}
                catch(IOException e2){Log.ignore(e2);}
            }
            finally
            { 
                connectionClosed(_connection);
                synchronized(_connections)
                {
                    _connections.remove(this);
                }
            }
        }
    }
}
