//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.win32service;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.Server;
import org.tanukisoftware.wrapper.WrapperManager;


public class Win32Service extends AbstractLifeCycle implements Runnable
{
    private Server server;
    public void doStart()
    {
        
        
        JettyServiceWrapperListener.setServer(server);
         
    }
    
    public void doStop()
    {
        System.out.println("Listener is stopping Jetty Service Instance!!!");
        
    }
    
    public void run()
    {
        doStop();
        
    }

    public void stopServer()
    {
        try
        {
            System.out.println("Thread Test Stopper!!!");
            server.stop();
            //WrapperManager.stop(0);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    
    public Server getServer()
    {
        return server;
    }

    public void setServer(Server server)
    {
        this.server = server;
    }
    
   

   
    
    
    
}