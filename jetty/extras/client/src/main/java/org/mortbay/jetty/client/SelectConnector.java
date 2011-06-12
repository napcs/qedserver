//========================================================================
//Copyright 2006-2007 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.client;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.io.Buffer;
import org.mortbay.io.Connection;
import org.mortbay.io.nio.IndirectNIOBuffer;
import org.mortbay.io.nio.SelectChannelEndPoint;
import org.mortbay.io.nio.SelectorManager;
import org.mortbay.jetty.AbstractBuffers;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.HttpVersions;
import org.mortbay.jetty.security.SslHttpChannelEndPoint;
import org.mortbay.log.Log;

class SelectConnector extends AbstractLifeCycle implements HttpClient.Connector, Runnable
{
    private final HttpClient _httpClient;
    private SSLContext _sslContext;
    private AbstractBuffers _sslBuffers;

    SelectorManager _selectorManager=new Manager();

    /**
     * @param httpClient
     */
    SelectConnector(HttpClient httpClient)
    {
        _httpClient = httpClient;
    }

    protected void doStart() throws Exception
    {
        _selectorManager.start();
        _httpClient._threadPool.dispatch(this);
    }

    protected void doStop() throws Exception
    {
        _selectorManager.stop();
    }

    public void startConnection( HttpDestination destination )
        throws IOException
    {
        SocketChannel channel = SocketChannel.open();
        Address address = destination.isProxied() ? destination.getProxy() : destination.getAddress();
        channel.configureBlocking( false );
        channel.socket().setTcpNoDelay(true);
        channel.connect(address.toSocketAddress());
        _selectorManager.register( channel, destination );
    }

    public void run()
    {
        while (_httpClient.isRunning())
        {
            try
            {
                _selectorManager.doSelect(0);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    class Manager extends SelectorManager
    {
        protected SocketChannel acceptChannel(SelectionKey key) throws IOException
        {
            throw new IllegalStateException();
        }

        public boolean dispatch(Runnable task)
        {
            return SelectConnector.this._httpClient._threadPool.dispatch(task);
        }

        protected void endPointOpened(SelectChannelEndPoint endpoint)
        {
        }

        protected void endPointClosed(SelectChannelEndPoint endpoint)
        {
        }

        protected Connection newConnection(SocketChannel channel, SelectChannelEndPoint endpoint)
        {
            if (endpoint instanceof SslHttpChannelEndPoint)
                return new HttpConnection(_sslBuffers,endpoint,_sslBuffers.getHeaderBufferSize(),_sslBuffers.getRequestBufferSize());
            return new HttpConnection(_httpClient,endpoint,SelectConnector.this._httpClient.getHeaderBufferSize(),SelectConnector.this._httpClient.getRequestBufferSize());
        }

        protected SelectChannelEndPoint newEndPoint(SocketChannel channel, SelectSet selectSet, SelectionKey key) throws IOException
        {
            // key should have destination at this point (will be replaced by endpoint after this call)
            HttpDestination dest=(HttpDestination)key.attachment();


            SelectChannelEndPoint ep=null;

            if (dest.isSecure())
            {
                if (dest.isProxied())
                {
                    String connect = HttpMethods.CONNECT+" "+dest.getAddress()+HttpVersions.HTTP_1_0+"\r\n\r\n";
                    // TODO need to send this over channel unencrypted and setup endpoint to ignore the 200 OK response.

                    throw new IllegalStateException("Not Implemented");
                }

                SSLEngine engine=newSslEngine();
                ep = new SslHttpChannelEndPoint(_sslBuffers,channel,selectSet,key,engine);
            }
            else
            {
                ep=new SelectChannelEndPoint(channel,selectSet,key);
            }

            HttpConnection connection=(HttpConnection)ep.getConnection();
            connection.setDestination(dest);
            dest.onNewConnection(connection);
            return ep;
        }

        private synchronized SSLEngine newSslEngine() throws IOException
        {
            if (_sslContext==null)
            {
                _sslContext = SelectConnector.this._httpClient.getSSLContext();
            }

            SSLEngine sslEngine = _sslContext.createSSLEngine();
            sslEngine.setUseClientMode(true);
            sslEngine.beginHandshake();

            if (_sslBuffers==null)
            {
                AbstractBuffers buffers = new AbstractBuffers()
                {
                    protected Buffer newBuffer( int size )
                    {
                        return new IndirectNIOBuffer( size);
                    }
                };

                buffers.setHeaderBufferSize( sslEngine.getSession().getApplicationBufferSize());
                buffers.setRequestBufferSize( sslEngine.getSession().getApplicationBufferSize());
                buffers.setResponseBufferSize(sslEngine.getSession().getApplicationBufferSize());

                try
                {
                    buffers.start();
                }
                catch(Exception e)
                {
                    throw new IllegalStateException(e);
                }
                _sslBuffers=buffers;
            }

            return sslEngine;
        }

        /* ------------------------------------------------------------ */
        /* (non-Javadoc)
         * @see org.mortbay.io.nio.SelectorManager#connectionFailed(java.nio.channels.SocketChannel, java.lang.Throwable, java.lang.Object)
         */
        protected void connectionFailed(SocketChannel channel, Throwable ex, Object attachment)
        {
            if (attachment instanceof HttpDestination)
                ((HttpDestination)attachment).onConnectionFailed(ex);
            else
                Log.warn(ex);
        }

    }

}
