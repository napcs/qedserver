//========================================================================
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.ajp;

import java.io.IOException;

import org.mortbay.io.EndPoint;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpSchemes;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;

import org.mortbay.jetty.deployer.WebAppDeployer;

/**
 * @author Greg Wilkins
 * @author Markus Kobler markus(at)inquisitive-mind.com
 * 
 */
public class Ajp13SocketConnector extends SocketConnector
{
    static String __secretWord = null;
    static boolean __allowShutdown = false;
    public Ajp13SocketConnector()
    {
        super.setHeaderBufferSize(Ajp13Packet.MAX_DATA_SIZE);
        super.setRequestBufferSize(Ajp13Packet.MAX_DATA_SIZE);
        super.setResponseBufferSize(Ajp13Packet.MAX_DATA_SIZE);
        // IN AJP protocol the socket stay open, so
        // by default the time out is set to 900 seconds
        super.setMaxIdleTime(900000);
    }

    protected void doStart() throws Exception
    {
        super.doStart();
        Log.info("AJP13 is not a secure protocol. Please protect port {}",Integer.toString(getLocalPort()));
    }
    
    

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.jetty.bio.SocketConnector#customize(org.mortbay.io.EndPoint, org.mortbay.jetty.Request)
     */
    public void customize(EndPoint endpoint, Request request) throws IOException
    {
        super.customize(endpoint,request);
        if (request.isSecure())
            request.setScheme(HttpSchemes.HTTPS);
    }

    /* ------------------------------------------------------------ */
    protected HttpConnection newHttpConnection(EndPoint endpoint)
    {
        return new Ajp13Connection(this,endpoint,getServer());
    }

    /* ------------------------------------------------------------ */
    // Secured on a packet by packet bases not by connection
    public boolean isConfidential(Request request)
    {
        return ((Ajp13Request) request).isSslSecure();
    }

    /* ------------------------------------------------------------ */
    // Secured on a packet by packet bases not by connection
    public boolean isIntegral(Request request)
    {
        return ((Ajp13Request) request).isSslSecure();
    }

    /* ------------------------------------------------------------ */
    public void setHeaderBufferSize(int headerBufferSize)
    {
        Log.debug(Log.IGNORED);
    }

    /* ------------------------------------------------------------ */
    public void setRequestBufferSize(int requestBufferSize)
    {
        Log.debug(Log.IGNORED);
    }

    /* ------------------------------------------------------------ */
    public void setResponseBufferSize(int responseBufferSize)
    {
        Log.debug(Log.IGNORED);
    }

    /* ------------------------------------------------------------ */
    public void setAllowShutdown(boolean allowShutdown)
    {
        Log.warn("AJP13: Shutdown Request is: " + allowShutdown);
        __allowShutdown = allowShutdown;
    }

    /* ------------------------------------------------------------ */
    public void setSecretWord(String secretWord)
    {
        Log.warn("AJP13: Shutdown Request secret word is : " + secretWord);
        __secretWord = secretWord;
    }

}
