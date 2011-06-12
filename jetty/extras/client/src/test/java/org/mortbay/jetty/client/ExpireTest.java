package org.mortbay.jetty.client;

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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.client.ContentExchange;
import org.mortbay.jetty.client.HttpClient;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.log.Log;

/* Test expiring connections
 * 
 * Test contributed by: Michiel Thuys for JETTY-806
 */
public class ExpireTest extends TestCase
{
    HttpClient client;

    Server server;

    AtomicInteger expireCount = new AtomicInteger();

    final String host = "localhost";

    int _port;

    @Override
    protected void setUp() throws Exception
    {
        client = new HttpClient();
        client.setConnectorType( HttpClient.CONNECTOR_SELECT_CHANNEL );
        client.setTimeout( 200 );
        client.setMaxRetries( 0 );
        client.setMaxConnectionsPerAddress(100);
        try
        {
            client.start();
        }
        catch ( Exception e )
        {
            throw new Error( "Cannot start HTTP client: " + e );
        }

        // Create server
        server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setHost( host );
        connector.setPort( 0 );
        server.setConnectors( new Connector[] { connector } );
        server.setHandler( new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest servletRequest, HttpServletResponse response,
                                int dispatch ) throws IOException,
                ServletException
            {
                Request request = (Request) servletRequest;
                try
                {
                    Thread.sleep( 2000 );
                }
                catch ( InterruptedException e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                request.setHandled( true );
            }
        } );
        try
        {
            server.start();
            _port = connector.getLocalPort();
        }
        catch ( Exception e )
        {
            Log.warn( "Cannot create server: " + e );
        }
    }

    @Override
    protected void tearDown() throws Exception
    {
        client.stop();
        server.stop();
    }

    public void testExpire() throws IOException
    {
        String baseUrl = "http://" + host + ":" + _port + "/";

        int count = 200;
        expireCount.set( 0 );
        Log.info( "Starting test on " + baseUrl );

        for (int i=0;i<count;i++)
        {
            if (i%10==0)
                System.err.print('.');
            expireCount.incrementAndGet();
            final ContentExchange ex = new ContentExchange()
            {
                protected void onExpire()
                {
                    expireCount.decrementAndGet();
                }
            };
            ex.setMethod( "GET" );
            ex.setURL( baseUrl );

            client.send( ex );
            try
            {
                Thread.sleep( 50 );
            }
            catch ( InterruptedException e )
            {
                break;
            }
        }
        // Log.info("Test done");
        // Wait to be sure that all exchanges have expired
        try
        {
            Thread.sleep( 2000 );
            int loops = 0;
            while ( expireCount.get()>0 && loops < 10 ) // max out at 30 seconds
            {
                Log.info( "waiting for test to complete: "+expireCount.get()+" of "+count );
                ++loops;
                Thread.sleep( 2000 );
            }
            Thread.sleep( 2000 );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
        System.err.println('!');

        assertEquals( 0, expireCount.get() );
    }
}
