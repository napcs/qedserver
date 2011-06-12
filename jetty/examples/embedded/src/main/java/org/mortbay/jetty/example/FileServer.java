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

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.log.Log;

/* ------------------------------------------------------------ */
/** File server
 * Usage - java org.mortbay.jetty.example.FileServer [ port [ docroot ]]
 * @author gregw
 *
 */
public class FileServer
{
    public static void main(String[] args)
        throws Exception
    {
        int port=8080;
        if (args.length>=1)
            port=Integer.parseInt(args[0]);
        Server server = new Server(port);
        
        ResourceHandler resource_handler=new ResourceHandler();
        resource_handler.setWelcomeFiles(new String[]{"index.html"});
        resource_handler.setResourceBase(args.length==2?args[1]:".");
        Log.info("serving "+resource_handler.getBaseResource());
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{resource_handler,new DefaultHandler()});
        server.setHandler(handlers);
        
        server.start();
        server.join();
    }

}
