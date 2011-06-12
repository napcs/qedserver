/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.mortbay.jetty.servlet.wadi;



import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;

public class WADITestServer {
    private final int jettyPort;
    private final String nodeName;
    private Server server;
    private WadiCluster wadiCluster;

    public WADITestServer(int jettyPort, String nodeName) {
        if (jettyPort < 1) {
            throw new IllegalArgumentException("jettyPort must be greater than 1");
        } else if (null == nodeName) {
            throw new IllegalArgumentException("nodeName is required");
        }
        this.jettyPort = jettyPort;
        this.nodeName = nodeName;
    }

    public void setUp() throws Exception {
        server = new Server();
        
        setUpConnector();

        wadiCluster = new WadiCluster("CLUSTER", nodeName, "http://localhost:" + jettyPort + "/test");
        
        setUpHandlers();

        startCluster();

        startServer();
    }

    public void startServer() throws Exception {
        server.start();
        server.join();
    }

    public void startCluster() throws Exception {
        wadiCluster.doStart();
    }
    
    protected void setUpHandlers() {
        HandlerCollection handlerCollection = new HandlerCollection();

        Handler handler = newCounterHandler();
        handlerCollection.addHandler(handler);

        handler = newExitHandler();
        handlerCollection.addHandler(handler);
        
        server.setHandler(handlerCollection);
    }
    
    protected ContextHandler newExitHandler() {
        ContextHandler ctx = new ContextHandler();
        ctx.setContextPath("/exit");
        ctx.setHandler(new AbstractHandler() {
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
                    throws IOException, ServletException {
                System.exit(1);
            }
        });
        return ctx;
    }

    protected ContextHandler newCounterHandler() {
        WadiSessionManager wadiManager = new WadiSessionManager(wadiCluster, 2, 24, 10);
        wadiManager.setMaxInactiveInterval(120);

        WadiSessionHandler wadiHandler = new WadiSessionHandler(wadiManager);
        wadiHandler.setHandler(new CounterHandler());

        ContextHandler ctx = new ContextHandler();
        ctx.setContextPath("/counter");
        ctx.setHandler(wadiHandler);

        return ctx;
    }

    protected void setUpConnector() {
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(jettyPort);
        server.setConnectors(new Connector[] { connector });
    }

    public static void main(String args[]) throws Exception {
        String port = System.getProperty("jetty.port", "8080");
        String node = System.getProperty("node.name", "red");
        
        new WADITestServer(Integer.parseInt(port), node).setUp();
    }
    
}
