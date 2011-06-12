// ========================================================================
// Copyright 2008 Mort Bay Consulting Pty. Ltd.
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


package org.mortbay.jetty.opensources;



import org.mortbay.jetty.openspaces.GigaSessionIdManager;
import org.mortbay.jetty.openspaces.GigaSessionManager;
import org.mortbay.jetty.servlet.AbstractSessionTest;
import org.mortbay.jetty.servlet.SessionTestServer;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.UrlSpaceConfigurer;



public class GigaIntegrationTest extends AbstractSessionTest
{

    private GigaSpace _space;
    
    
    public class GigaSessionTestServer extends SessionTestServer
    {
        
        public GigaSessionTestServer(int port, String workerName)
        {
            super(port, workerName);
        }
        
        public void configureEnvironment ()
        {
            UrlSpaceConfigurer uscA = new UrlSpaceConfigurer("/./space"); 
            GigaSpaceConfigurer gigaSpaceConfigurer = new GigaSpaceConfigurer(uscA.space());
            _space = gigaSpaceConfigurer.gigaSpace();
        }

        public void configureIdManager()
        {
            GigaSessionIdManager idMgr = new GigaSessionIdManager(this);
            idMgr.setWorkerName(_workerName);
            idMgr.setSpace(_space);
            _sessionIdMgr = idMgr;
        }

        public void configureSessionManager1()
        {
            GigaSessionManager sessionMgr1 = new GigaSessionManager();
            sessionMgr1.setIdManager(_sessionIdMgr);
            sessionMgr1.setSpace(_space);
            _sessionMgr1 = sessionMgr1;
        }

        public void configureSessionManager2()
        {
            GigaSessionManager sessionMgr2 = new GigaSessionManager();
            sessionMgr2.setIdManager(_sessionIdMgr);
            sessionMgr2.setSpace(_space);
            _sessionMgr2 = sessionMgr2;
        }    
    }
    
    


    public SessionTestServer newServer1()
    {
        return new GigaSessionTestServer(Integer.parseInt(__port1), "fred");
    }


    public SessionTestServer newServer2()
    {
        return new GigaSessionTestServer(Integer.parseInt(__port2), "mabel");
    }
}
