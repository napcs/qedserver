// ========================================================================
// Copyright 2006 Mort Bay Consulting Pty. Ltd.
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
package org.mortbay.jetty.handler.rewrite;

import junit.framework.TestCase;

import org.mortbay.io.bio.StringEndPoint;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.LocalConnector;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.Server;

public abstract class AbstractRuleTestCase extends TestCase
{
    protected Server _server=new Server();
    protected LocalConnector _connector;
    protected StringEndPoint _endpoint=new StringEndPoint();
    protected HttpConnection _connection;
    
    protected Request _request;
    protected Response _response;
    
    protected boolean _isSecure = false;
    
    public void setUp() throws Exception
    {
        start();
    }
    
    public void tearDown() throws Exception
    {
        stop();
    }
    
    public void start() throws Exception
    {
        _connector = new LocalConnector() {
            public boolean isConfidential(Request request)
            {
                return _isSecure;
            }
        };
        
        _server.setConnectors(new Connector[]{_connector});
        _server.start();
        reset();
    }
    
    public void stop() throws Exception
    {
        _server.stop();
        _request = null;
        _response = null;
    }
    
    public void reset()
    {
        _connection = new HttpConnection(_connector,_endpoint,_server);
        _request = new Request(_connection);
        _response = new Response(_connection);
        
        _request.setRequestURI("/test/");
    }
}
