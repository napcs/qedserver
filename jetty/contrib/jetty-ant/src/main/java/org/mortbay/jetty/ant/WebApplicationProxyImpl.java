// ========================================================================
// Copyright 2006-2007 Sabre Holdings.
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

package org.mortbay.jetty.ant;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;

import org.mortbay.jetty.ant.types.FileMatchingConfiguration;
import org.mortbay.jetty.ant.utils.TaskLog;
import org.mortbay.jetty.ant.utils.WebApplicationProxy;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.plus.webapp.EnvConfiguration;
import org.mortbay.jetty.webapp.Configuration;
import org.mortbay.jetty.webapp.JettyWebXmlConfiguration;
import org.mortbay.jetty.webapp.TagLibConfiguration;
import org.mortbay.jetty.webapp.WebAppClassLoader;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.jetty.webapp.WebInfConfiguration;

/**
 * An abstraction layer over Jetty WebAppContext.
 *
 * @author Jakub Pawlowicz
 */
public class WebApplicationProxyImpl implements WebApplicationProxy
{

    /** Common root temp directory for all web applications. */
    static File baseTempDirectory = new File(".");

    /** Name of this web application. */
    private String name;

    /** Location of WAR file (either expanded or not). */
    private File warFile;

    /** Application context path. */
    private String contextPath;

    /** Location of web.xml file. */
    private File webXmlFile;

    /** Location of jetty-env.xml file. */
    private File jettyEnvXml;

    /** List of classpath files. */
    private List classPathFiles;

    /** Jetty6 Web Application Context. */
    private WebAppContext webAppContext;

    /** Extra scan targets. */
    private FileMatchingConfiguration extraScanTargetsConfiguration;

    /** Extra context handlers. */
    private List contextHandlers;

    Configuration[] configurations;

    private FileMatchingConfiguration librariesConfiguration;

    public static void setBaseTempDirectory(File tempDirectory)
    {
        baseTempDirectory = tempDirectory;
    }

    /**
     * Default constructor. Takes application name as an argument.
     *
     * @param name web application name.
     */
    public WebApplicationProxyImpl(String name) throws Exception
    {
        this.name = name;
        TaskLog.log("\nConfiguring Jetty for web application: " + name);

				this.configurations = new Configuration[] { new WebInfConfiguration(),
		            new EnvConfiguration(), new JettyWebAppConfiguration(), new JettyWebXmlConfiguration(),
		            new TagLibConfiguration() };
    }

    public List getClassPathFiles()
    {
        return classPathFiles;
    }

    public String getContextPath()
    {
        return contextPath;
    }

    public String getName()
    {
        return name;
    }

    public File getSourceDirectory()
    {
        return warFile;
    }

    public File getWebXmlFile()
    {
        return webXmlFile;
    }

    public void setSourceDirectory(File warFile)
    {
        this.warFile = warFile;
        TaskLog.log("Webapp source directory = " + warFile);
    }

    public void setContextPath(String contextPath)
    {
        if (!contextPath.startsWith("/"))
        {
            contextPath = "/" + contextPath;
        }
        this.contextPath = contextPath;
        TaskLog.log("Context path = " + contextPath);

    }

    public void setWebXml(File webXmlFile)
    {
        this.webXmlFile = webXmlFile;
    }

    public void setJettyEnvXml(File jettyEnvXml)
    {
        this.jettyEnvXml = jettyEnvXml;
        if (this.jettyEnvXml != null)
        {
            TaskLog.log("jetty-env.xml file: = " + jettyEnvXml.getAbsolutePath());
        }
    }

    public void setClassPathFiles(List classPathFiles)
    {
        this.classPathFiles = classPathFiles;
        TaskLog.log("Classpath = " + classPathFiles);
    }

    /**
     * Checks if a given file is scanned according to the internal
     * configuration. This may be difficult due to use of 'includes' and
     * 'excludes' statements.
     *
     * @param pathToFile a fully qualified path to file.
     * @return true if file is being scanned, false otherwise.
     */
    public boolean isFileScanned(String pathToFile)
    {
        return librariesConfiguration.isIncluded(pathToFile)
                || extraScanTargetsConfiguration.isIncluded(pathToFile);
    }

    public void setLibrariesConfiguration(FileMatchingConfiguration classesConfiguration)
    {
        TaskLog.log("Default scanned paths = " + classesConfiguration.getBaseDirectories());
        this.librariesConfiguration = classesConfiguration;
    }

    public List getLibraries()
    {
        return librariesConfiguration.getBaseDirectories();
    }

    public void setExtraScanTargetsConfiguration(
            FileMatchingConfiguration extraScanTargetsConfiguration)
    {
        this.extraScanTargetsConfiguration = extraScanTargetsConfiguration;
        TaskLog.log("Extra scan targets = " + extraScanTargetsConfiguration.getBaseDirectories());
    }

    public List getExtraScanTargets()
    {
        return extraScanTargetsConfiguration.getBaseDirectories();
    }

    public List getContextHandlers()
    {
        return contextHandlers;
    }

    public void setContextHandlers(List contextHandlers)
    {
        this.contextHandlers = contextHandlers;
    }

    /**
     * @see com.sabre.ant.jetty.WebApplicationProxy#getProxiedObject()
     */
    public Object getProxiedObject()
    {
        return webAppContext;
    }

    /**
     * @see com.sabre.ant.jetty.WebApplicationProxy#start()
     */
    public void start()
    {
        try
        {
            TaskLog.logWithTimestamp("Starting web application " + name + " ...\n");
            webAppContext.setShutdown(false);
            webAppContext.start();
        }
        catch (Exception e)
        {
            TaskLog.log(e.toString());
        }
    }

    /**
     * @see com.sabre.ant.jetty.WebApplicationProxy#stop()
     */
    public void stop()
    {
        try
        {
            TaskLog.logWithTimestamp("Stopping web application " + name + " ...");
            webAppContext.setShutdown(true);
            Thread.currentThread().sleep(500L);
            webAppContext.stop();
        }
        catch (InterruptedException e)
        {
            TaskLog.log(e.toString());
        }
        catch (Exception e)
        {
            TaskLog.log(e.toString());
        }
    }

    /**
     * @see com.sabre.ant.jetty.WebApplicationProxy#createApplicationContext(org.mortbay.jetty.handler.ContextHandlerCollection)
     */
    public void createApplicationContext(ContextHandlerCollection contexts)
    {
        webAppContext = new WebAppContext(contexts, warFile.getAbsolutePath(), contextPath);
        webAppContext.setDisplayName(name);

        configurePaths();
        configureHandlers(contexts);

        applyConfiguration();
    }

    private void configureHandlers(ContextHandlerCollection contexts)
    {
        // adding extra context handlers
        Iterator handlersIterator = contextHandlers.iterator();
        while (handlersIterator.hasNext())
        {
            ContextHandler contextHandler = (ContextHandler) handlersIterator.next();
            contexts.addHandler(contextHandler);
        }
    }

    private void configurePaths()
    {
        // configuring temp directory
        File tempDir = new File(baseTempDirectory, contextPath);
        if (!tempDir.exists())
        {
            tempDir.mkdirs();
        }
        webAppContext.setTempDirectory(tempDir);
        tempDir.deleteOnExit();
        TaskLog.log("Temp directory = " + tempDir.getAbsolutePath());

        // configuring WAR directory for packaged web applications
        if (warFile.isFile())
        {
            warFile = new File(tempDir, "webapp");
            webXmlFile = new File(new File(warFile, "WEB-INF"), "web.xml");
        }
    }

    /**
     * Applies web application configuration at the end of configuration process
     * or after application restart.
     */
    void applyConfiguration()
    {
        for (int i = 0; i < configurations.length; i++)
        {
            if (configurations[i] instanceof EnvConfiguration)
            {
                try
                {
                    if (jettyEnvXml != null && jettyEnvXml.exists())
                    {
                        ((EnvConfiguration) configurations[i]).setJettyEnvXml(jettyEnvXml.toURL());
                    }
                }
                catch (MalformedURLException e)
                {
                    throw new RuntimeException(e);
                }
            }
            else if (configurations[i] instanceof JettyWebAppConfiguration)
            {
                ((JettyWebAppConfiguration) configurations[i]).setClassPathFiles(classPathFiles);
                ((JettyWebAppConfiguration) configurations[i]).setWebAppBaseDir(warFile);
                ((JettyWebAppConfiguration) configurations[i]).setWebXmlFile(webXmlFile);
                ((JettyWebAppConfiguration) configurations[i]).setWebDefaultXmlFile(webDefaultXmlFile);
            }
        }

        try
        {
            ClassLoader loader = new WebAppClassLoader(this.getClass().getClassLoader(),
                    webAppContext);
            webAppContext.setParentLoaderPriority(true);
            webAppContext.setClassLoader(loader);
            if (webDefaultXmlFile != null)
                webAppContext.setDefaultsDescriptor(webDefaultXmlFile.getCanonicalPath());

        }
        catch (IOException e)
        {
            TaskLog.log(e.toString());
        }

        webAppContext.setConfigurations(configurations);
    }

    private File webDefaultXmlFile;

    public File getWebDefaultXmlFile()
    {
        return this.webDefaultXmlFile;
    }

    public void setWebDefaultXmlFile(File webDefaultXmlfile)
    {
        this.webDefaultXmlFile = webDefaultXmlfile;
    }
}
