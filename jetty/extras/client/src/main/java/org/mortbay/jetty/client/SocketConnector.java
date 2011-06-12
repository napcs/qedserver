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
import java.io.InterruptedIOException;
import java.net.Socket;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.io.EndPoint;
import org.mortbay.io.bio.SocketEndPoint;
import org.mortbay.log.Log;

class SocketConnector extends AbstractLifeCycle implements HttpClient.Connector
{
    /**
     *
     */
    private final HttpClient _httpClient;

    /**
     * @param httpClient
     */
    SocketConnector(HttpClient httpClient)
    {
        _httpClient = httpClient;
    }

    public void startConnection(final HttpDestination destination) throws IOException
    {
        Socket socket=null;

        if ( destination.isSecure() )
        {
            SSLContext sslContext = _httpClient.getSSLContext();
            socket = sslContext.getSocketFactory().createSocket();
        }
        else
        {
            Log.debug("Using Regular Socket");
            socket = SocketFactory.getDefault().createSocket();
        }

        socket.setSoTimeout(_httpClient.getSoTimeout());
        socket.setTcpNoDelay(true);

        Address address = destination.isProxied() ? destination.getProxy() : destination.getAddress();
        socket.connect(address.toSocketAddress());

        EndPoint endpoint=new SocketEndPoint(socket);

        final HttpConnection connection=new HttpConnection(_httpClient,endpoint,_httpClient.getHeaderBufferSize(),_httpClient.getRequestBufferSize());
        connection.setDestination(destination);
        destination.onNewConnection(connection);
        _httpClient.getThreadPool().dispatch(new Runnable()
        {
            public void run()
            {
                try
                {
                    connection.handle();
                }
                catch (IOException e)
                {
                    if (e instanceof InterruptedIOException)
                        Log.ignore(e);
                    else
                    {
                        Log.warn(e);
                        destination.onException(e);
                    }
                }
            }
        });

    }
}
