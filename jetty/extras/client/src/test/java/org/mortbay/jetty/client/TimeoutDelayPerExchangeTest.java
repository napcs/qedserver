// ========================================================================
// Copyright 2006-2007 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.HttpStatus;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;

public class TimeoutDelayPerExchangeTest extends TestCase
{

    protected HttpClient _httpClient;
    protected int _maxConnectionsPerAddress = 2;
    protected String _scheme = "http://";
    protected Server _server;
    protected int _port;
    protected Connector _connector;

    public void setUp() throws Exception
    {
        startServer();
        createClient();
    }

    public void tearDown() throws Exception
    {
        _httpClient.stop();
        Thread.sleep(500);
        stopServer();
    }

    private void startServer() throws Exception
    {
        _server = new Server();
        _server.setGracefulShutdown(500);
        _connector = new SelectChannelConnector();

        _connector.setPort(0);
        _server.setConnectors(new Connector[]
        { _connector });

        Handler handler = new AbstractHandler()
        {
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
            {
                try
                {
                    // let's sleep for 0.7 sec as the default timeout is 0.5 sec
                    Thread.sleep(700);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("<h1>Hello</h1>");
                ((Request)request).setHandled(true);
            }
        };

        _server.setHandler(handler);

        _server.start();
        _port = _connector.getLocalPort();
    }

    private void stopServer() throws Exception
    {
        if (_server != null)
        {
            _server.stop();
            _server = null;
        }
    }

    private void createClient() throws Exception
    {
        _httpClient = new HttpClient();
        _httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        _httpClient.setMaxConnectionsPerAddress(_maxConnectionsPerAddress);
        // default timeout = 500 ms
        _httpClient.setTimeout(500);
        _httpClient.start();
    }

    public void testTimeouts() throws Exception
    {

        CustomContentExchange httpExchange = new CustomContentExchange();
        httpExchange.setURL(_scheme + "localhost:" + _port);
        httpExchange.setMethod(HttpMethods.POST);
        httpExchange.setRequestContent(new ByteArrayBuffer("<h1>??</h1>"));

        // let's use the default timeout - the one set on the HttpClient
        // (500 ms)
        _httpClient.send(httpExchange);

        httpExchange.getDoneLatch().await(900,TimeUnit.MILLISECONDS);
        // we should get a timeout - the server sleeps for 700 ms
        Assert.assertTrue(httpExchange.isTimeoutOccurred());
        Assert.assertFalse(httpExchange.isResponseReceived());

        // let's do it again - with a custom timeout
        httpExchange = new CustomContentExchange();
        httpExchange.setURL(_scheme + "localhost:" + _port);
        httpExchange.setMethod(HttpMethods.POST);
        httpExchange.setRequestContent(new ByteArrayBuffer("<h1>??</h1>"));
        httpExchange.setTimeout(500);

        // let's use a custom timeout - 500 ms (the default one) + 500 ms
        // delay = 1000 ms
        _httpClient.send(httpExchange);

        httpExchange.getDoneLatch().await(1100,TimeUnit.MILLISECONDS);
        // we should not get a timeout - the server sleeps for 700 ms
        // while we wait for 1000 ms
        Assert.assertFalse(httpExchange.isTimeoutOccurred());
        Assert.assertTrue(httpExchange.isResponseReceived());
    }

    class CustomContentExchange extends ContentExchange
    {

        protected final CountDownLatch _doneLatch = new CountDownLatch(1);
        protected boolean _errorOccurred = false;
        protected boolean _timeoutOccurred = false;
        protected boolean _responseReceived = false;

        public boolean isErrorOccurred()
        {
            return _errorOccurred;
        }

        public boolean isTimeoutOccurred()
        {
            return _timeoutOccurred;
        }

        public boolean isResponseReceived()
        {
            return _responseReceived;
        }

        public CustomContentExchange()
        {
            super(true);
        }

        @Override
        protected void onRequestComplete() throws IOException
        {
            // close the input stream when its not needed anymore
            InputStream is = getRequestContentSource();
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onResponseComplete() throws IOException
        {
            try
            {
                super.onResponseComplete();
            }
            finally
            {
                doTaskCompleted();
            }
        }

        @Override
        protected void onExpire()
        {
            try
            {
                super.onExpire();
            }
            finally
            {
                doTaskCompleted();
            }
        }

        @Override
        protected void onException(Throwable ex)
        {
            try
            {
                super.onException(ex);
            }
            finally
            {
                doTaskCompleted(ex);
            }
        }

        @Override
        protected void onConnectionFailed(Throwable ex)
        {
            try
            {
                super.onConnectionFailed(ex);
            }
            finally
            {
                doTaskCompleted(ex);
            }
        }

        public String getBody() throws UnsupportedEncodingException
        {
            return super.getResponseContent();
        }

        public String getUrl()
        {
            String params = getRequestFields().getStringField(HttpHeaders.CONTENT_ENCODING);
            return getScheme() + "//" + getAddress().toString() + getURI() + (params != null?"?" + params:"");
        }

        protected void doTaskCompleted()
        {

            int exchangeState = getStatus();

            try
            {
                if (exchangeState == HttpExchange.STATUS_COMPLETED)
                {
                    // process the response as the state is ok
                    try
                    {
                        int responseCode = getResponseStatus();

                        if (responseCode >= HttpStatus.ORDINAL_100_Continue && responseCode < HttpStatus.ORDINAL_300_Multiple_Choices)
                        {
                            _responseReceived = true;
                        }
                        else
                        {
                            _errorOccurred = true;
                        }
                    }
                    catch (Exception e)
                    {
                        _errorOccurred = true;
                        e.printStackTrace();
                    }
                }
                else if (exchangeState == HttpExchange.STATUS_EXPIRED)
                {
                    _timeoutOccurred = true;
                }
                else
                {
                    _errorOccurred = true;
                }
            }
            finally
            {
                // make sure to lower the latch
                getDoneLatch().countDown();
            }
        }

        protected void doTaskCompleted(Throwable ex)
        {
            try
            {
                _errorOccurred = true;
            }
            finally
            {
                // make sure to lower the latch
                getDoneLatch().countDown();
            }
        }

        public CountDownLatch getDoneLatch()
        {
            return _doneLatch;
        }
    }

}
