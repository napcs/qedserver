//========================================================================
//$Id: JettyStopMojo.java 5222 2009-05-29 07:34:32Z dyu $
//Copyright 2000-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plugin;

import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * JettyStopMojo - stops a running instance of jetty.
 * The ff are required:
 * -DstopKey=someKey
 * -DstopPort=somePort
 * 
 * @author David Yu
 * 
 * @goal stop
 * @description Stops jetty that is configured with &lt;stopKey&gt; and &lt;stopPort&gt;.
 */

public class JettyStopMojo extends AbstractMojo
{
    
    /**
     * Port to listen to stop jetty on sending stop command
     * @parameter
     * @required
     */
    protected int stopPort;
    
    /**
     * Key to provide when stopping jetty on executing java -DSTOP.KEY=&lt;stopKey&gt; 
     * -DSTOP.PORT=&lt;stopPort&gt; -jar start.jar --stop
     * @parameter
     * @required
     */
    protected String stopKey;

    public void execute() throws MojoExecutionException, MojoFailureException 
    {
        if (stopPort <= 0)
            throw new MojoExecutionException("Please specify a valid port"); 
        if (stopKey == null)
            throw new MojoExecutionException("Please specify a valid stopKey");  

        try
        {        
            Socket s=new Socket(InetAddress.getByName("127.0.0.1"),stopPort);
            s.setSoLinger(false, 0);
            
            OutputStream out=s.getOutputStream();
            out.write((stopKey+"\r\nstop\r\n").getBytes());
            out.flush();
            s.close();
        }
        catch (ConnectException e)
        {
            getLog().info("Jetty not running!");
        }
        catch (Exception e)
        {
            getLog().error(e);
        }
    }

}
