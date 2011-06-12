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

package org.mortbay.jetty;

import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;
import org.mortbay.util.URIUtil;

public class Main
{

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** Construct server from command line arguments.
     * @param args 
     */
    public static void main(String[] args)
    {
        if (args.length<1 || args.length>3)
        {
            System.err.println("Usage - java org.mortbay.jetty.Main [<addr>:]<port>");
            System.err.println("Usage - java org.mortbay.jetty.Main [<addr>:]<port> docroot");
            System.err.println("Usage - java org.mortbay.jetty.Main [<addr>:]<port> -webapp myapp.war");
            System.err.println("Usage - java org.mortbay.jetty.Main [<addr>:]<port> -webapps webapps");
            System.err.println("Usage - java -jar jetty-x.x.x-standalone.jar [<addr>:]<port>");
            System.err.println("Usage - java -jar jetty-x.x.x-standalone.jar [<addr>:]<port> docroot");
            System.err.println("Usage - java -jar jetty-x.x.x-standalone.jar [<addr>:]<port> -webapp myapp.war");
            System.err.println("Usage - java -jar jetty-x.x.x-standalone.jar [<addr>:]<port> -webapps webapps");
            System.exit(1);
        }
        
        try{
            
            // Create the server
            Server server = new Server();
            ContextHandlerCollection contexts = new ContextHandlerCollection();
            server.setHandler(contexts);
            
            SocketConnector connector = new SocketConnector();
            String address = args[0];
            int colon = address.lastIndexOf(':');
            if (colon<0)
                connector.setPort(Integer.parseInt(address));
            else
            {
                connector.setHost(address.substring(0,colon));
                connector.setPort(Integer.parseInt(address.substring(colon+1)));
            }
            server.setConnectors(new Connector[]{connector});
            
            if (args.length<3)
            {
                ContextHandler context = new ContextHandler();
                context.setContextPath(URIUtil.SLASH);
                context.setResourceBase(args.length==1?".":args[1]);
                ServletHandler servlet = new ServletHandler();
                servlet.addServletWithMapping("org.mortbay.jetty.servlet.DefaultServlet", URIUtil.SLASH);
                context.setHandler(servlet);
                contexts.addHandler(context);
            }
            else if ("-webapps".equals(args[1]))
            {
                WebAppContext.addWebApplications(server, args[2], WebAppContext.WEB_DEFAULTS_XML, true, true);
            }
            else if ("-webapp".equals(args[1]))
            {
                WebAppContext webapp = new WebAppContext();
                webapp.setWar(args[2]);
                webapp.setContextPath(URIUtil.SLASH);
                contexts.addHandler(webapp);
            }
                
            server.start();
            
        }
        catch (Exception e)
        {
            Log.warn(Log.EXCEPTION,e);
        }
    }
}
