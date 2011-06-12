//========================================================================
//$Id: AbstractJettyMojo.java 6194 2010-09-27 07:24:19Z janb $
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
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.plugin.util.ConsoleScanner;
import org.mortbay.jetty.plugin.util.JettyPluginServer;
import org.mortbay.jetty.plugin.util.PluginLog;
import org.mortbay.jetty.plugin.util.SystemProperties;
import org.mortbay.jetty.plugin.util.SystemProperty;
import org.mortbay.util.Scanner;



/**
 * AbstractJettyMojo
 *
 *
 */
public abstract class AbstractJettyMojo extends AbstractMojo
{
    /**
     * The proxy for the Server object
     */
    protected JettyPluginServer server;


    /**
     * The "virtual" webapp created by the plugin
     * @parameter
     */
    protected Jetty6PluginWebAppContext webAppConfig;



    /**
     * The maven project.
     *
     * @parameter expression="${executedProject}"
     * @required
     * @readonly
     */
    protected MavenProject project;



    /**
     * The context path for the webapp. Defaults to the
     * name of the webapp's artifact.
     *
     * @parameter expression="/${project.artifactId}"
     * @required
     */
    protected String contextPath;


    /**
     * The temporary directory to use for the webapp.
     * Defaults to target/jetty-tmp
     *
     * @parameter expression="${project.build.directory}/work"
     * @required
     */
    protected File tmpDirectory;



    /**
     * A webdefault.xml file to use instead
     * of the default for the webapp. Optional.
     *
     * @parameter
     */
    protected File webDefaultXml;


    /**
     * A web.xml file to be applied AFTER
     * the webapp's web.xml file. Useful for
     * applying different build profiles, eg
     * test, production etc. Optional.
     * @parameter
     */
    protected File overrideWebXml;
    
    /**
     * The interval in seconds to scan the webapp for changes 
     * and restart the context if necessary. Ignored if reload
     * is enabled. Disabled by default.
     * 
     * @parameter expression="${jetty.scanIntervalSeconds}" default-value="0"
     * @required
     */
    protected int scanIntervalSeconds;
    
    
    /**
     * reload can be set to either 'automatic' or 'manual'
     *
     * if 'manual' then the context can be reloaded by a linefeed in the console
     * if 'automatic' then traditional reloading on changed files is enabled.
     * 
     * @parameter expression="${jetty.reload}" default-value="automatic"
     */
    protected String reload;
    
    /**
     * File containing system properties to be set before execution
     * 
     * Note that these properties will NOT override System properties
     * that have been set on the command line, by the JVM, or directly 
     * in the POM via systemProperties. Optional.
     * 
     * @parameter expression="${jetty.systemPropertiesFile}"
     */
    protected File systemPropertiesFile;

    /**
     * System properties to set before execution. 
     * Note that these properties will NOT override System properties 
     * that have been set on the command line or by the JVM. They WILL 
     * override System properties that have been set via systemPropertiesFile.
     * Optional.
     * @parameter
     */
    protected SystemProperties systemProperties;
    
    
    
    /**
     * Location of a jetty xml configuration file whose contents 
     * will be applied before any plugin configuration. Optional.
     * @parameter
     */
    protected File jettyConfig;
    
    /**
     * Port to listen to stop jetty on executing -DSTOP.PORT=&lt;stopPort&gt; 
     * -DSTOP.KEY=&lt;stopKey&gt; -jar start.jar --stop
     * @parameter
     */
    protected int stopPort;
    
    /**
     * Key to provide when stopping jetty on executing java -DSTOP.KEY=&lt;stopKey&gt; 
     * -DSTOP.PORT=&lt;stopPort&gt; -jar start.jar --stop
     * @parameter
     */
    protected String stopKey;

    /**
     * <p>
     * Determines whether or not the server blocks when started. The default
     * behavior (daemon = false) will cause the server to pause other processes
     * while it continues to handle web requests. This is useful when starting the
     * server with the intent to work with it interactively.
     * </p><p>
     * Often, it is desirable to let the server start and continue running subsequent
     * processes in an automated build environment. This can be facilitated by setting
     * daemon to true.
     * </p>
     * @parameter expression="${jetty.daemon}" default-value="false"
     */
    protected boolean daemon;
    
    /**
     * <p>
     * If true, execution of the plugin is skipped.
     * </p>
     * @parameter expression="${jetty.skip}" default-value="false"
     */
    protected boolean skip;

    /**
     * A scanner to check for changes to the webapp
     */
    protected Scanner scanner;
    
    /**
     *  List of files and directories to scan
     */
    protected ArrayList scanList;
    
    /**
     * List of Listeners for the scanner
     */
    protected ArrayList scannerListeners;
    
    
    /**
     * A scanner to check ENTER hits on the console
     */
    protected Thread consoleScanner;

    
    public String PORT_SYSPROPERTY = "jetty.port";
    
    /**
     * @return Returns the realms configured in the pom
     */
    public abstract Object[] getConfiguredUserRealms();
    
    /**
     * @return Returns the connectors configured in the pom
     */
    public abstract Object[] getConfiguredConnectors();

    public abstract Object getConfiguredRequestLog();
    

    public abstract void checkPomConfiguration() throws MojoExecutionException;
    
    
    
    public abstract void configureScanner () throws MojoExecutionException;
    
    
    public abstract void applyJettyXml () throws Exception;
    
    
    /**
     * create a proxy that wraps a particular jetty version Server object
     * @return
     */
    public abstract JettyPluginServer createServer() throws Exception;
    
    
    public abstract void finishConfigurationBeforeStart() throws Exception;
    
    
    public MavenProject getProject()
    {
        return this.project;
    }
    
    public File getTmpDirectory()
    {
        return this.tmpDirectory;
    }

    
    public File getWebDefaultXml()
    {
        return this.webDefaultXml;
    }
    
    public File getOverrideWebXml()
    {
        return this.overrideWebXml;
    }
    
    /**
     * @return Returns the contextPath.
     */
    public String getContextPath()
    {
        return this.contextPath;
    }

    /**
     * @return Returns the scanIntervalSeconds.
     */
    public int getScanIntervalSeconds()
    {
        return this.scanIntervalSeconds;
    }

    /**
     * @return returns the path to the systemPropertiesFile
     */
    public File getSystemPropertiesFile()
    {
        return this.systemPropertiesFile;
    }
    
    public void setSystemPropertiesFile(File file) throws Exception
    {
        this.systemPropertiesFile = file;
        FileInputStream propFile = new FileInputStream(systemPropertiesFile);
        Properties properties = new Properties();
        properties.load(propFile);
        
        if (this.systemProperties == null )
            this.systemProperties = new SystemProperties();
        
        for (Enumeration keys = properties.keys(); keys.hasMoreElements();  )
        {
            String key = (String)keys.nextElement();
            if ( ! systemProperties.containsSystemProperty(key) )
            {
                SystemProperty prop = new SystemProperty();
                prop.setKey(key);
                prop.setValue(properties.getProperty(key));
                
                this.systemProperties.setSystemProperty(prop);
            }
        }
        
    }
    
    public void setSystemProperties(SystemProperties systemProperties)
    {
        if (this.systemProperties == null)
            this.systemProperties = systemProperties;
        else
        {
            Iterator itor = systemProperties.getSystemProperties().iterator();
            while (itor.hasNext())
            {
                SystemProperty prop = (SystemProperty)itor.next();
                this.systemProperties.setSystemProperty(prop);
            }   
        }
    }

    public File getJettyXmlFile ()
    {
        return this.jettyConfig;
    }


    public JettyPluginServer getServer ()
    {
        return this.server;
    }

    public void setServer (JettyPluginServer server)
    {
        this.server = server;
    }


    public void setScanList (ArrayList list)
    {
        this.scanList = new ArrayList(list);
    }

    public ArrayList getScanList ()
    {
        return this.scanList;
    }


    public void setScannerListeners (ArrayList listeners)
    {
        this.scannerListeners = new ArrayList(listeners);
    }

    public ArrayList getScannerListeners ()
    {
        return this.scannerListeners;
    }

    public Scanner getScanner ()
    {
        return scanner;
    }

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        getLog().info("Configuring Jetty for project: " + getProject().getName());
        if (skip)
        {
            getLog().info("Skipping jetty: jetty.skip==true");
            return;
        }
        PluginLog.setLog(getLog());
        checkPomConfiguration();
        startJetty();
    }


    public void startJetty () throws MojoExecutionException
    {
        try
        {
            getLog().debug("Starting Jetty Server ...");

            printSystemProperties();
            setServer(createServer());

            //apply any config from a jetty.xml file first which is able to
            //be overwritten by config in the pom.xml
            applyJettyXml ();

            JettyPluginServer plugin=getServer();


            // if the user hasn't configured their project's pom to use a
            // different set of connectors,
            // use the default
            Object[] configuredConnectors = getConfiguredConnectors();

            plugin.setConnectors(configuredConnectors);
            Object[] connectors = plugin.getConnectors();

            if (connectors == null|| connectors.length == 0)
            {
                //if a SystemProperty -Djetty.port=<portnum> has been supplied, use that as the default port
                configuredConnectors = new Object[] { plugin.createDefaultConnector(System.getProperty(PORT_SYSPROPERTY, null)) };
                plugin.setConnectors(configuredConnectors);
            }


            //set up a RequestLog if one is provided
            if (getConfiguredRequestLog() != null)
                getServer().setRequestLog(getConfiguredRequestLog());

            //set up the webapp and any context provided
            getServer().configureHandlers();
            configureWebApplication();
            getServer().addWebApplication(webAppConfig);


            // set up security realms
            Object[] configuredRealms = getConfiguredUserRealms();
            for (int i = 0; (configuredRealms != null) && i < configuredRealms.length; i++)
                getLog().debug(configuredRealms[i].getClass().getName() + ": "+ configuredRealms[i].toString());

            plugin.setUserRealms(configuredRealms);

            //do any other configuration required by the
            //particular Jetty version
            finishConfigurationBeforeStart();

            // start Jetty
            server.start();

            getLog().info("Started Jetty Server");
            
            if(stopPort>0 && stopKey!=null)
            {
                org.mortbay.jetty.plugin.util.Monitor monitor = new org.mortbay.jetty.plugin.util.Monitor(stopPort, stopKey, new Server[]{(Server)server.getProxiedObject()}, !daemon);
                monitor.start();
            }
            
            // start the scanner thread (if necessary) on the main webapp
            configureScanner ();
            startScanner();
            
            // start the new line scanner thread if necessary
            startConsoleScanner();

            // keep the thread going if not in daemon mode
            if (!daemon)
            {
                server.join();
            }
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Failure", e);
        }
        finally
        {
            if (!daemon)
            {
                getLog().info("Jetty server exiting.");
            }            
        }
        
    }
    
    
    public abstract void restartWebApp(boolean reconfigureScanner) throws Exception;

    /**
     * Subclasses should invoke this to setup basic info
     * on the webapp
     * 
     * @throws MojoExecutionException
     */
    public void configureWebApplication () throws Exception
    {
        //use EITHER a <webAppConfig> element or the now deprecated <contextPath>, <tmpDirectory>, <webDefaultXml>, <overrideWebXml>
        //way of doing things
        if (webAppConfig == null)
        {
            webAppConfig = new Jetty6PluginWebAppContext();
            webAppConfig.setContextPath((getContextPath().startsWith("/") ? getContextPath() : "/"+ getContextPath()));
            if (getTmpDirectory() != null)
                webAppConfig.setTempDirectory(getTmpDirectory());
            if (getWebDefaultXml() != null)
                webAppConfig.setDefaultsDescriptor(getWebDefaultXml().getCanonicalPath());
            if (getOverrideWebXml() != null)
                webAppConfig.setOverrideDescriptor(getOverrideWebXml().getCanonicalPath());
        }

        if (webAppConfig.getContextPath() == null)
        {
            webAppConfig.setContextPath((getContextPath().startsWith("/") ? getContextPath() : "/"+ getContextPath()));
        }

        getLog().info("Context path = " + webAppConfig.getContextPath());
        getLog().info("Tmp directory = "+ " determined at runtime");
        getLog().info("Web defaults = "+(webAppConfig.getDefaultsDescriptor()==null?" jetty default":webAppConfig.getDefaultsDescriptor()));
        getLog().info("Web overrides = "+(webAppConfig.getOverrideDescriptor()==null?" none":webAppConfig.getOverrideDescriptor()));

    }

    /**
     * Run a scanner thread on the given list of files and directories, calling
     * stop/start on the given list of LifeCycle objects if any of the watched
     * files change.
     *
     */
    private void startScanner()
    {

        // check if scanning is enabled
        if (getScanIntervalSeconds() <= 0) return;

        // check if reload is manual. It disables file scanning
        if ( "manual".equalsIgnoreCase( reload ) )
        {
            // issue a warning if both scanIntervalSeconds and reload
            // are enabled
            getLog().warn("scanIntervalSeconds is set to " + scanIntervalSeconds + " but will be IGNORED due to manual reloading");
            return;
        }

        scanner = new Scanner();
        scanner.setReportExistingFilesOnStartup(false);
        scanner.setScanInterval(getScanIntervalSeconds());
        scanner.setScanDirs(getScanList());
        scanner.setRecursive(true);
        List listeners = getScannerListeners();
        Iterator itor = (listeners==null?null:listeners.iterator());
        while (itor!=null && itor.hasNext())
            scanner.addListener((Scanner.Listener)itor.next());
        getLog().info("Starting scanner at interval of " + getScanIntervalSeconds()+ " seconds.");
        scanner.start();
    }
    
    /**
     * Run a thread that monitors the console input to detect ENTER hits.
     */
    protected void startConsoleScanner() 
    {
        if ( "manual".equalsIgnoreCase( reload ) )
        {
            getLog().info("Console reloading is ENABLED. Hit ENTER on the console to restart the context.");
            consoleScanner = new ConsoleScanner(this);
            consoleScanner.start();
        }
        
    }

    private void printSystemProperties ()
    {
        // print out which system properties were set up
        if (getLog().isDebugEnabled())
        {
            if (systemProperties != null)
            {
                Iterator itor = systemProperties.getSystemProperties().iterator();
                while (itor.hasNext())
                {
                    SystemProperty prop = (SystemProperty)itor.next();
                    getLog().debug("Property "+prop.getName()+"="+prop.getValue()+" was "+ (prop.isSet() ? "set" : "skipped"));
                }
            }
        }
    }

    /**
     * Try and find a jetty-web.xml file, using some
     * historical naming conventions if necessary.
     * @param webInfDir
     * @return
     */
    public File findJettyWebXmlFile (File webInfDir)
    {
        if (webInfDir == null)
            return null;
        if (!webInfDir.exists())
            return null;

        File f = new File (webInfDir, "jetty-web.xml");
        if (f.exists())
            return f;

        //try some historical alternatives
        f = new File (webInfDir, "web-jetty.xml");
        if (f.exists())
            return f;
        f = new File (webInfDir, "jetty6-web.xml");
        if (f.exists())
            return f;
        
        return null;
    }
}
