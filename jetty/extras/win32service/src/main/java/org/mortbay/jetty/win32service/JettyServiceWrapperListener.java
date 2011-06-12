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

import org.mortbay.jetty.Server;
import org.tanukisoftware.wrapper.WrapperManager;
import org.tanukisoftware.wrapper.WrapperListener;
import java.io.File;
public class JettyServiceWrapperListener implements WrapperListener
{
    private static Server __server = null;

    public JettyServiceWrapperListener()
    {
    }

    public void controlEvent(int event)
    {
        if (!WrapperManager.isControlledByNativeWrapper())
        {
            if ((event == WrapperManager.WRAPPER_CTRL_C_EVENT) || (event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT) || (event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT))
            {
                WrapperManager.stop(0);
            }
        }

    }

    public Integer start(String[] args)
    {
        for(int i=0; i<args.length; i++)
        {
            System.out.println("ARG[" + i + "] = " + args[i]);
        }
        org.mortbay.start.Main.main(args);
        return null;
    }

    public int stop(int code)
    {
        try
        {
            System.out.println("JettyServiceWrapperListener: Stopping Jetty 6 Service!!!");
            __server.stop();
            System.out.println("JettyServiceWrapperListener: Jetty 6 Service Stopped!!!");
            return code;
        }
        catch (Exception e)
        {
            System.out.println("Stop Server Error");
            e.printStackTrace();
            return -1;
        }

    }

    public static void setServer(Server server)
    {
        __server = server;
    }

    public static Server getServer()
    {
        return __server;
    }

    public static void main(String[] args)
    {
        String newStrArgs[] = new String[args.length + 1];
        newStrArgs[0] = System.getProperty("jetty.home") + "etc/jetty-win32-service.xml";
        for(int i=0; i<args.length; i++)
        {
            newStrArgs[i+1] = args[i];
        }
        WrapperManager.start(new JettyServiceWrapperListener(), newStrArgs);
    }

}
