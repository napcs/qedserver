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
import java.util.Iterator;
import java.util.List;

import org.mortbay.jetty.ant.utils.TaskLog;
import org.mortbay.jetty.plus.webapp.Configuration;
import org.mortbay.jetty.webapp.WebAppClassLoader;

/**
 * This configuration object provides additional way to inject application
 * properties into the configured web application. The list of classpath files,
 * the application base directory and web.xml file could be specified in this
 * way.
 *
 * @author Jakub Pawlowicz
 */
public class JettyWebAppConfiguration extends Configuration
{

    /** List of classpath files. */
    private List classPathFiles;

    /** Web application root directory. */
    private File webAppBaseDir;

    /** Web application web.xml file. */
    private File webXmlFile;

    private File webDefaultXmlFile;

    public JettyWebAppConfiguration() throws ClassNotFoundException
    {
    }

    public File getWebDefaultXmlFile()
    {
        return this.webDefaultXmlFile;
    }

    public void setWebDefaultXmlFile(File webDefaultXmlfile)
    {
        this.webDefaultXmlFile = webDefaultXmlfile;
    }

    public void setClassPathFiles(List classPathFiles)
    {
        this.classPathFiles = classPathFiles;
    }

    public void setWebAppBaseDir(File webAppBaseDir)
    {
        this.webAppBaseDir = webAppBaseDir;
    }

    public void setWebXmlFile(File webXmlFile)
    {
        this.webXmlFile = webXmlFile;

        if (webXmlFile.exists())
        {
            TaskLog.log("web.xml file = " + webXmlFile);
        }
    }

    /**
     * Adds classpath files into web application classloader.
     *
     * @see Configuration#configureClassLoader()
     */
    public void configureClassLoader() throws Exception
    {
        Iterator filesIterator = classPathFiles.iterator();

        while (filesIterator.hasNext())
        {
            File classPathFile = (File) filesIterator.next();
            if (classPathFile.exists())
            {
                ((WebAppClassLoader) getWebAppContext().getClassLoader())
                        .addClassPath(classPathFile.getCanonicalPath());
            }
        }
    }

    /**
     * Sets web.xml and base directory for the configured web application.
     *
     * @see Configuration#configureWebApp()
     */
    public void configureWebApp() throws Exception
    {
        if (getWebAppContext().isStarted())
        {
            TaskLog.log("Cannot configure webapp after it is started");
            return;
        }

        getWebAppContext().setResourceBase(webAppBaseDir.getAbsolutePath());

        if (webXmlFile.exists())
        {
            configure(webXmlFile.toURL().toString());
        }

        bindUserTransaction();
        lockCompEnv();
    }
}
