//========================================================================
//$$Id: JettyHttpServerProvider.java 1647 2009-09-18 09:14:03Z janb $$
//
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

package org.mortbay.jetty.j2se6;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.spi.HttpServerProvider;

/**
 * Jetty implementation of <a href="http://java.sun.com/javase/6/docs/jre/api/net/httpserver/spec/index.html">Java HTTP Server SPI</a>
 * 
 */
public class JettyHttpServerProvider extends HttpServerProvider
{

    @Override
    public HttpServer createHttpServer(InetSocketAddress addr, int backlog)
    throws IOException
    {         
        JettyHttpServer jettyHttpServer = new JettyHttpServer();
        jettyHttpServer.bind(addr, backlog);
        return jettyHttpServer;
    }

    @Override
    public HttpsServer createHttpsServer(InetSocketAddress addr, int backlog)
    throws IOException
    {
        throw new UnsupportedOperationException();
    }

}
