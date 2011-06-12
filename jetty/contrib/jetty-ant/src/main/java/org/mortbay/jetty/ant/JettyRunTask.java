//========================================================================
//Copyright 2006-2007 Sabre Holdings.
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

package org.mortbay.jetty.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Property;
import org.mortbay.jetty.RequestLog;
import org.mortbay.jetty.ant.types.Connectors;
import org.mortbay.jetty.ant.types.SystemProperties;
import org.mortbay.jetty.ant.types.UserRealms;
import org.mortbay.jetty.ant.types.WebApp;
import org.mortbay.jetty.ant.utils.ServerProxy;
import org.mortbay.jetty.ant.utils.TaskLog;
import org.mortbay.util.Scanner;

/**
 * Ant task for running a Jetty server.
 *
 * @author Jakub Pawlowicz
 */
public class JettyRunTask extends Task
{

    /** Temporary files directory. */
    private File tempDirectory;

    /** List of web applications to be deployed. */
    private List webapps = new ArrayList();

    /** Location of jetty.xml file. */
    private File jettyXml;

    /** List of server connectors. */
    private Connectors connectors = null;

    /** Server request logger object. */
    private RequestLog requestLog;

    /** List of user realms. */
    private UserRealms userRealms;

    /** List of system properties to be set. */
    private SystemProperties systemProperties;

    /**
     * Creates a new <code>WebApp</code> Ant object.
     *
     * @return a WebApp object.
     */
    public void addWebApp(WebApp webapp)
    {
        webapps.add(webapp);
    }

    /**
     * Adds a new Ant's connector tag object if it have not been created yet.
     */
    public void addConnectors(Connectors connectors)
    {
        if (this.connectors != null)
        {
            throw new BuildException("Only one <connectors> tag is allowed!");
        }

        this.connectors = connectors;
    }

    /**
     * @return a new Ant's connector tag object if it have not been created yet.
     */
    public void addUserRealms(UserRealms realms)
    {
        if (this.userRealms != null)
        {
            throw new BuildException("Only one <userRealms> tag is allowed!");
        }

        this.userRealms = realms;
    }

    public void addSystemProperties(SystemProperties systemProperties)
    {
        if (this.systemProperties != null)
        {
            throw new BuildException("Only one <systemProperties> tag is allowed!");
        }

        this.systemProperties = systemProperties;
    }

    public File getTempDirectory()
    {
        return tempDirectory;
    }

    public void setTempDirectory(File tempDirectory)
    {
        this.tempDirectory = tempDirectory;
    }

    public File getJettyXml()
    {
        return jettyXml;
    }

    public void setJettyXml(File jettyXml)
    {
        this.jettyXml = jettyXml;
    }

    public void setRequestLog(String className)
    {
        try
        {
            this.requestLog = (RequestLog) Class.forName(className).newInstance();
        }
        catch (InstantiationException e)
        {
            throw new BuildException("Request logger instantiation exception: " + e);
        }
        catch (IllegalAccessException e)
        {
            throw new BuildException("Request logger instantiation exception: " + e);
        }
        catch (ClassNotFoundException e)
        {
            throw new BuildException("Unknown request logger class: " + className);
        }
    }

    public String getRequestLog()
    {
        if (requestLog != null)
        {
            return requestLog.getClass().getName();
        }

        return "";
    }

    /**
     * Executes this Ant task. The build flow is being stopped until Jetty
     * server stops.
     *
     * @throws BuildException
     */
    public void execute() throws BuildException
    {
        TaskLog.setTask(this);
        TaskLog.log("Configuring Jetty for project: " + getProject().getName());
        WebApplicationProxyImpl.setBaseTempDirectory(tempDirectory);
        setSystemProperties();

        List connectorsList = (connectors != null ? connectors.getConnectors()
                : Connectors.DEFAULT_CONNECTORS);
        List userRealmsList = (userRealms != null ? userRealms.getUserRealms() : new ArrayList());
        ServerProxy server = new ServerProxyImpl(connectorsList, userRealmsList, requestLog,
                jettyXml);

				try 
				{
	        Iterator iterator = webapps.iterator();
	        while (iterator.hasNext())
	        {
	            WebApp webAppConfiguration = (WebApp) iterator.next();
	            WebApplicationProxyImpl webApp = new WebApplicationProxyImpl(webAppConfiguration
	                    .getName());
	            webApp.setSourceDirectory(webAppConfiguration.getWarFile());
	            webApp.setContextPath(webAppConfiguration.getContextPath());
	            webApp.setWebXml(webAppConfiguration.getWebXmlFile());
	            webApp.setJettyEnvXml(webAppConfiguration.getJettyEnvXml());
	            webApp.setClassPathFiles(webAppConfiguration.getClassPathFiles());
	            webApp.setLibrariesConfiguration(webAppConfiguration.getLibrariesConfiguration());
	            webApp.setExtraScanTargetsConfiguration(webAppConfiguration
	                    .getScanTargetsConfiguration());
	            webApp.setContextHandlers(webAppConfiguration.getContextHandlers());
	            webApp.setWebDefaultXmlFile(webAppConfiguration.getWebDefaultXmlFile());

	            server.addWebApplication(webApp, webAppConfiguration.getScanIntervalSeconds());
	        }
				} 
				catch (Exception e) {
					throw new BuildException(e);
				}
				
        server.start();
    }

    /**
     * Starts a new thread which scans project files and automatically reloads a
     * container on any changes.
     *
     * @param scanIntervalSeconds
     *
     * @param webapp
     * @param appContext
     */
    static void startScanner(final WebApplicationProxyImpl webApp, int scanIntervalSeconds)
    {
        List scanList = new ArrayList();
        scanList.add(webApp.getWebXmlFile());
        scanList.addAll(webApp.getLibraries());
        scanList.addAll(webApp.getExtraScanTargets());

        Scanner.Listener changeListener = new Scanner.BulkListener()
        {

            public void filesChanged(List changedFiles)
            {
                if (hasAnyFileChanged(changedFiles))
                {
                    try
                    {
                        webApp.stop();
                        webApp.applyConfiguration();
                        webApp.start();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            /**
             * Checks if any file in this particular application has changed.
             * This is not that easy, because some applications may use the same
             * class'es directory.
             *
             * @param changedFiles list of changed files.
             * @return true if any of passed files has changed, false otherwise.
             */
            private boolean hasAnyFileChanged(List changedFiles)
            {
                Iterator changes = changedFiles.iterator();
                while (changes.hasNext())
                {
                    String className = (String) changes.next();
                    if (webApp.isFileScanned(className))
                    {
                        return true;
                    }
                }

                return false;
            }
        };

        TaskLog.log("Web application '" + webApp.getName() + "': starting scanner at interval of "
                + scanIntervalSeconds + " seconds.");

        Scanner scanner = new Scanner();
        scanner.setScanInterval(scanIntervalSeconds);
        scanner.addListener(changeListener);
        scanner.setScanDirs(scanList);
        scanner.setReportExistingFilesOnStartup(false);
        scanner.start();
    }

    /**
     * Sets the system properties.
     */
    private void setSystemProperties()
    {
        if (systemProperties != null)
        {
            Iterator propertiesIterator = systemProperties.getSystemProperties().iterator();
            while (propertiesIterator.hasNext())
            {
                Property property = ((Property) propertiesIterator.next());
                SystemProperties.setIfNotSetAlready(property);
            }
        }
    }
}
