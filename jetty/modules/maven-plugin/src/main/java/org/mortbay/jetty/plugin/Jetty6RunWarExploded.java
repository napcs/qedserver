//========================================================================
//$Id: Jetty6RunWarExploded.java 3591 2008-09-03 21:31:12Z jesse $
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.mortbay.util.Scanner;

/**
 * 
 *  <p>
 *  This goal is used to assemble your webapp into an exploded war and automatically deploy it to Jetty.
 *  </p>
 *  <p>
 *  Once invoked, the plugin can be configured to run continuously, scanning for changes in the pom.xml and 
 *  to WEB-INF/web.xml, WEB-INF/classes or WEB-INF/lib and hot redeploy when a change is detected. 
 *  </p>
 *  <p>
 *  You may also specify the location of a jetty.xml file whose contents will be applied before any plugin configuration.
 *  This can be used, for example, to deploy a static webapp that is not part of your maven build. 
 *  </p>
 *  <p>
 *  There is a <a href="run-exploded-mojo.html">reference guide</a> to the configuration parameters for this plugin, and more detailed information
 *  with examples in the <a href="http://docs.codehaus.org/display/JETTY/Maven+Jetty+Plugin">Configuration Guide</a>.
 *  </p>
 *
 *@goal run-exploded
 *@execute phase=package
 */
public class Jetty6RunWarExploded extends AbstractJetty6Mojo
{

    
    
    /**
     * The location of the war file.
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private File webApp;

    
   
  
   

    /**
     * 
     * @see org.mortbay.jetty.plugin.AbstractJettyMojo#checkPomConfiguration()
     */
    public void checkPomConfiguration() throws MojoExecutionException
    {
        return;
    }

    /**
     * @see org.mortbay.jetty.plugin.AbstractJettyMojo#configureScanner()
     */
    public void configureScanner() throws MojoExecutionException
    {
        final ArrayList scanList = new ArrayList();
        scanList.add(getProject().getFile());
        File webInfDir = new File(webApp,"WEB-INF");
        scanList.add(new File(webInfDir, "web.xml"));
        File jettyWebXmlFile = findJettyWebXmlFile(webInfDir);
        if (jettyWebXmlFile != null)
            scanList.add(jettyWebXmlFile);
        File jettyEnvXmlFile = new File(webInfDir, "jetty-env.xml");
        if (jettyEnvXmlFile.exists())
            scanList.add(jettyEnvXmlFile);
        scanList.add(new File(webInfDir, "classes"));
        scanList.add(new File(webInfDir, "lib"));
        setScanList(scanList);
        
        ArrayList listeners = new ArrayList();
        listeners.add(new Scanner.BulkListener()
        {
            public void filesChanged(List changes)
            {
                try
                {
                    boolean reconfigure = changes.contains(getProject().getFile().getCanonicalPath());
                    restartWebApp(reconfigure);
                }
                catch (Exception e)
                {
                    getLog().error("Error reconfiguring/restarting webapp after change in watched files",e);
                }
            }
        });
        setScannerListeners(listeners);
    }

    
    
    
    public void restartWebApp(boolean reconfigureScanner) throws Exception 
    {
        getLog().info("Restarting webapp");
        getLog().debug("Stopping webapp ...");
        webAppConfig.stop();
        getLog().debug("Reconfiguring webapp ...");

        checkPomConfiguration();

        // check if we need to reconfigure the scanner,
        // which is if the pom changes
        if (reconfigureScanner)
        {
            getLog().info("Reconfiguring scanner after change to pom.xml ...");
            ArrayList scanList = getScanList();
            scanList.clear();
            scanList.add(getProject().getFile());
            File webInfDir = new File(webApp,"WEB-INF");
            scanList.add(new File(webInfDir, "web.xml"));
            File jettyWebXmlFile = findJettyWebXmlFile(webInfDir);
            if (jettyWebXmlFile != null)
                scanList.add(jettyWebXmlFile);
            File jettyEnvXmlFile = new File(webInfDir, "jetty-env.xml");
            if (jettyEnvXmlFile.exists())
                scanList.add(jettyEnvXmlFile);
            scanList.add(new File(webInfDir, "classes"));
            scanList.add(new File(webInfDir, "lib"));
            setScanList(scanList);
            getScanner().setScanDirs(scanList);
        }

        getLog().debug("Restarting webapp ...");
        webAppConfig.start();
        getLog().info("Restart completed.");
    }

        
    /* (non-Javadoc)
     * @see org.mortbay.jetty.plugin.util.AbstractJettyMojo#finishConfigurationBeforeStart()
     */
    public void finishConfigurationBeforeStart() throws Exception
    {
        return;
    }

    
    
    public void configureWebApplication () throws Exception
    {
        super.configureWebApplication();        
        webAppConfig.setWar(webApp.getCanonicalPath());
        webAppConfig.configure();
    }
    
    public void execute () throws MojoExecutionException, MojoFailureException
    {
        super.execute();
    }
    
}
