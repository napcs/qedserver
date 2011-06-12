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

package org.mortbay.jetty.example;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.NCSARequestLog;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.deployer.ContextDeployer;
import org.mortbay.jetty.deployer.WebAppDeployer;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SslSelectChannelConnector;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.thread.BoundedThreadPool;
import org.mortbay.thread.QueuedThreadPool;

public class LikeJettyXml
{
    public static void main(String[] args)
        throws Exception
    {
        String jetty_default=new java.io.File("./start.jar").exists()?".":"../..";;
        String jetty_home = System.getProperty("jetty.home",jetty_default);
        
        Server server = new Server();
        
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(100);
        server.setThreadPool(threadPool);
             
        SelectChannelConnector connector=new SelectChannelConnector();
        connector.setPort(8080);
        connector.setMaxIdleTime(30000);
        connector.setConfidentialPort(8443);
        server.setConnectors(new Connector[]{connector});

        SslSelectChannelConnector ssl_connector=new SslSelectChannelConnector();
        ssl_connector.setPort(8443);
        ssl_connector.setKeystore(jetty_home+"/etc/keystore");
        ssl_connector.setPassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
        ssl_connector.setKeyPassword("OBF:1u2u1wml1z7s1z7a1wnl1u2g");
        ssl_connector.setTruststore(jetty_home+"/etc/keystore");
        ssl_connector.setTrustPassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
        server.addConnector(ssl_connector);  
        
        SslSocketConnector ssl_b_connector=new SslSocketConnector();
        ssl_b_connector.setPort(8444);
        ssl_b_connector.setKeystore(jetty_home+"/etc/keystore");
        ssl_b_connector.setPassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
        ssl_b_connector.setKeyPassword("OBF:1u2u1wml1z7s1z7a1wnl1u2g");
        ssl_b_connector.setTruststore(jetty_home+"/etc/keystore");
        ssl_b_connector.setTrustPassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
        server.addConnector(ssl_b_connector);  
        
        HandlerCollection handlers = new HandlerCollection();
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        handlers.setHandlers(new Handler[]{contexts,new DefaultHandler(),requestLogHandler});
        server.setHandler(handlers);
        
        ContextDeployer deployer0 = new ContextDeployer();
        deployer0.setContexts(contexts);
        deployer0.setConfigurationDir(jetty_home+"/contexts");
        deployer0.setScanInterval(1);
        server.addLifeCycle(deployer0);   
        
        WebAppDeployer deployer1 = new WebAppDeployer();
        deployer1.setContexts(contexts);
        deployer1.setWebAppDir(jetty_home+"/webapps");
        deployer1.setParentLoaderPriority(false);
        deployer1.setExtract(true);
        deployer1.setAllowDuplicates(false);
        deployer1.setDefaultsDescriptor(jetty_home+"/etc/webdefault.xml");
        server.addLifeCycle(deployer1);
          
        HashUserRealm userRealm = new HashUserRealm();
        userRealm.setName("Test Realm");
        userRealm.setConfig(jetty_home+"/etc/realm.properties");
        server.setUserRealms(new UserRealm[]{userRealm});
        
        NCSARequestLog requestLog = new NCSARequestLog(jetty_home+"/logs/jetty-yyyy_mm_dd.log");
        requestLog.setExtended(false);
        requestLogHandler.setRequestLog(requestLog);
        
        server.setStopAtShutdown(true);
        server.setSendServerVersion(true);
        
        server.start();
        server.join();
    }
    
}
