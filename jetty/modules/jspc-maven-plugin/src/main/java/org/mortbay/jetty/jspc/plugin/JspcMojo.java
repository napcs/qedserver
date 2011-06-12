//========================================================================
//$Id: JspcMojo.java 5477 2009-08-27 03:03:38Z janb $
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

package org.mortbay.jetty.jspc.plugin;

import org.apache.jasper.JspC;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.mortbay.util.IO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * This goal will compile jsps for a webapp so that they can be included in a
 * war.
 * </p>
 * <p>
 * At runtime, the plugin will use the jsp2.0 jspc compiler if you are running
 * on a 1.4 or lower jvm. If you are using a 1.5 jvm, then the jsp2.1 compiler
 * will be selected. (this is the same behaviour as the <a
 * href="http://jetty.mortbay.org/maven-plugin">jetty plugin</a> for executing
 * webapps).
 * </p>
 * <p>
 * Note that the same java compiler will be used as for on-the-fly compiled
 * jsps, which will be the Eclipse java compiler.
 * </p>
 * 
 * <p>
 * See <a
 * href="http://docs.codehaus.org/display/JETTY/Maven+Jetty+Jspc+Plugin">Usage
 * Guide</a> for instructions on using this plugin.
 * </p>
 * 
 * @author janb
 * 
 * @goal jspc
 * @phase process-classes
 * @requiresDependencyResolution compile
 * @description Runs jspc compiler to produce .java and .class files
 */
public class JspcMojo extends AbstractMojo
{
    public static final String END_OF_WEBAPP = "</web-app>";


    /**
     * The maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * File into which to generate the &lt;servlet&gt; and
     * &lt;servlet-mapping&gt; tags for the compiled jsps
     * 
     * @parameter expression="${basedir}/target/webfrag.xml"
     */
    private String webXmlFragment;

    /**
     * Optional. A marker string in the src web.xml file which indicates where
     * to merge in the generated web.xml fragment. Note that the marker string
     * will NOT be preserved during the insertion. Can be left blank, in which
     * case the generated fragment is inserted just before the &lt;/web-app&gt;
     * line
     * 
     * @parameter
     */
    private String insertionMarker;

    /**
     * Merge the generated fragment file with the web.xml from
     * webAppSourceDirectory. The merged file will go into the same directory as
     * the webXmlFragment.
     * 
     * @parameter expression="true"
     */
    private boolean mergeFragment;

    /**
     * The destination directory into which to put the compiled jsps.
     * 
     * @parameter expression="${project.build.outputDirectory}"
     */
    private String generatedClasses;

    /**
     * Controls whether or not .java files generated during compilation will be
     * preserved.
     * 
     * @parameter expression="false"
     */
    private boolean keepSources;

    /**
     * Default root package for all generated classes
     * 
     * @parameter expression="jsp"
     */
    private String packageRoot;

    /**
     * Root directory for all html/jsp etc files
     * 
     * @parameter expression="${basedir}/src/main/webapp"
     * @required
     */
    private String webAppSourceDirectory;


    /**
     * The comma separated list of patterns for file extensions to be processed. By default
     * will include all .jsp and .jspx files.
     * 
     * @parameter default-value="**\/*.jsp, **\/*.jspx"
     */
    private String includes;

    /**
     * The comma separated list of file name patters to exclude from compilation.
     * 
     * @parameter default_value="**\/.svn\/**";
     */
    private String excludes;

    /**
     * The location of the compiled classes for the webapp
     * 
     * @parameter expression="${project.build.outputDirectory}"
     */
    private File classesDirectory;

    /**
     * Whether or not to output more verbose messages during compilation.
     * 
     * @parameter expression="false";
     */
    private boolean verbose;

    /**
     * If true, validates tlds when parsing.
     * 
     * @parameter expression="false";
     */
    private boolean validateXml;

    /**
     * The encoding scheme to use.
     * 
     * @parameter expression="UTF-8"
     */
    private String javaEncoding;

    /**
     * Whether or not to generate JSR45 compliant debug info
     * 
     * @parameter expression="true";
     */
    private boolean suppressSmap;

    /**
     * Whether or not to ignore precompilation errors caused by jsp fragments.
     * 
     * @parameter expression="false"
     */
    private boolean ignoreJspFragmentErrors;

    /**
     * Allows a prefix to be appended to the standard schema locations so that
     * they can be loaded from elsewhere.
     * 
     * @parameter
     */
    private String schemaResourcePrefix;
    

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (getLog().isDebugEnabled())
        {
            getLog().info("verbose=" + verbose);
            getLog().info("webAppSourceDirectory=" + webAppSourceDirectory);
            getLog().info("generatedClasses=" + generatedClasses);
            getLog().info("webXmlFragment=" + webXmlFragment);
            getLog().info("validateXml=" + validateXml);
            getLog().info("packageRoot=" + packageRoot);
            getLog().info("javaEncoding=" + javaEncoding);
            getLog().info("insertionMarker="+ (insertionMarker == null || insertionMarker.equals("") ? END_OF_WEBAPP : insertionMarker));
            getLog().info("keepSources=" + keepSources);
            getLog().info("mergeFragment=" + mergeFragment);
            getLog().info("suppressSmap=" + suppressSmap);
            getLog().info("ignoreJspFragmentErrors=" + ignoreJspFragmentErrors);
            getLog().info("schemaResourcePrefix=" + schemaResourcePrefix);
        }
        try
        {
            prepare();
            compile();
            cleanupSrcs();
            mergeWebXml();
        }
        catch (Exception e)
        {
            throw new MojoFailureException(e, "Failure processing jsps","Failure processing jsps");
        }
    }

    public void compile() throws Exception
    {
        ClassLoader currentClassLoader = Thread.currentThread()
        .getContextClassLoader();

        ArrayList urls = new ArrayList();
        setUpClassPath(urls);
        URLClassLoader ucl = new URLClassLoader((URL[]) urls.toArray(new URL[0]), currentClassLoader);
        StringBuffer classpathStr = new StringBuffer();

        for (int i = 0; i < urls.size(); i++)
        {
            if (getLog().isDebugEnabled())
                getLog().debug("webappclassloader contains: " + urls.get(i));
            classpathStr.append(((URL) urls.get(i)).getFile());
            if (getLog().isDebugEnabled())
                getLog().debug(
                        "added to classpath: " + ((URL) urls.get(i)).getFile());
            classpathStr.append(System.getProperty("path.separator"));
        }

        Thread.currentThread().setContextClassLoader(ucl);

        JspC jspc = new JspC();
        jspc.setWebXmlFragment(webXmlFragment);
        jspc.setUriroot(webAppSourceDirectory);
        jspc.setPackage(packageRoot);
        jspc.setOutputDir(generatedClasses);
        jspc.setValidateXml(validateXml);
        jspc.setClassPath(classpathStr.toString());
        jspc.setCompile(true);
        jspc.setSmapSuppressed(suppressSmap);
        jspc.setSmapDumped(!suppressSmap);
        jspc.setJavaEncoding(javaEncoding);

        // JspC#setExtensions() does not exist, so 
        // always set concrete list of files that will be processed.
        String jspFiles = getJspFiles(webAppSourceDirectory);
        System.err.println("Compiling "+jspFiles);
        System.err.println("Includes="+includes);
        System.err.println("Excludes="+excludes);
        jspc.setJspFiles(jspFiles);
        if (verbose)
        {
            getLog().info("Files selected to precompile: " + jspFiles);
        }
        

        try
        {
            jspc.setIgnoreJspFragmentErrors(ignoreJspFragmentErrors);
        }
        catch (NoSuchMethodError e)
        {
            getLog().debug("Tomcat Jasper does not support configuration option 'ignoreJspFragmentErrors': ignored");
        }

        try
        {
            if (schemaResourcePrefix != null)
                jspc.setSchemaResourcePrefix(schemaResourcePrefix);
        }
        catch (NoSuchMethodError e)
        {
            getLog().debug("Tomcat Jasper does not support configuration option 'schemaResourcePrefix': ignored");
        }
        if (verbose)
            jspc.setVerbose(99);
        else
            jspc.setVerbose(0);

        jspc.execute();

        Thread.currentThread().setContextClassLoader(currentClassLoader);
    }

    private String getJspFiles(String webAppSourceDirectory)
    throws Exception
    {
        List fileNames =  FileUtils.getFileNames(new File(webAppSourceDirectory),includes, excludes, false);
        return StringUtils.join(fileNames.toArray(new String[0]), ",");

    }

    /**
     * Until Jasper supports the option to generate the srcs in a different dir
     * than the classes, this is the best we can do.
     * 
     * @throws Exception
     */
    public void cleanupSrcs() throws Exception
    {
        // delete the .java files - depending on keepGenerated setting
        if (!keepSources)
        {
            File generatedClassesDir = new File(generatedClasses);

            if(generatedClassesDir.exists() && generatedClassesDir.isDirectory())
            {
                delete(generatedClassesDir, new FileFilter()
                {
                    public boolean accept(File f)
                    {
                        return f.isDirectory() || f.getName().endsWith(".java");
                    }                
                });
            }
        }
    }
    
    static void delete(File dir, FileFilter filter)
    {
        File[] files = dir.listFiles(filter);
        for(int i=0; i<files.length; i++)
        {
            File f = files[i];
            if(f.isDirectory())
                delete(f, filter);
            else
                f.delete();
        }
    }

    /**
     * Take the web fragment and put it inside a copy of the web.xml file from
     * the webAppSourceDirectory.
     * 
     * You can specify the insertion point by specifying the string in the
     * insertionMarker configuration entry.
     * 
     * If you dont specify the insertionMarker, then the fragment will be
     * inserted at the end of the file just before the &lt;/webapp&gt;
     * 
     * @throws Exception
     */
    public void mergeWebXml() throws Exception
    {
        if (mergeFragment)
        {
            // open the src web.xml
            File webXml = new File(webAppSourceDirectory + "/WEB-INF/web.xml");
            if (!webXml.exists())
            {
                getLog()
                .info(
                        webAppSourceDirectory
                        + "/WEB-INF/web.xml does not exist, cannot merge with generated fragment");
                return;
            }

            File fragmentWebXml = new File(webXmlFragment);
            if (!fragmentWebXml.exists())
            {
                getLog().info("No fragment web.xml file generated");
            }
            File mergedWebXml = new File(fragmentWebXml.getParentFile(),
            "web.xml");
            BufferedReader webXmlReader = new BufferedReader(new FileReader(
                    webXml));
            PrintWriter mergedWebXmlWriter = new PrintWriter(new FileWriter(
                    mergedWebXml));

            // read up to the insertion marker or the </webapp> if there is no
            // marker
            boolean atInsertPoint = false;
            boolean atEOF = false;
            String marker = (insertionMarker == null
                    || insertionMarker.equals("") ? END_OF_WEBAPP : insertionMarker);
            while (!atInsertPoint && !atEOF)
            {
                String line = webXmlReader.readLine();
                if (line == null)
                    atEOF = true;
                else if (line.indexOf(marker) >= 0)
                {
                    atInsertPoint = true;
                }
                else
                {
                    mergedWebXmlWriter.println(line);
                }
            }

            // put in the generated fragment
            BufferedReader fragmentWebXmlReader = new BufferedReader(
                    new FileReader(fragmentWebXml));
            IO.copy(fragmentWebXmlReader, mergedWebXmlWriter);

            // if we inserted just before the </web-app>, put it back in
            if (marker.equals(END_OF_WEBAPP))
                mergedWebXmlWriter.println(END_OF_WEBAPP);

            // copy in the rest of the original web.xml file
            IO.copy(webXmlReader, mergedWebXmlWriter);

            webXmlReader.close();
            mergedWebXmlWriter.close();
            fragmentWebXmlReader.close();
        }
    }

    private void prepare() throws Exception
    {
        // For some reason JspC doesn't like it if the dir doesn't
        // already exist and refuses to create the web.xml fragment
        File generatedSourceDirectoryFile = new File(generatedClasses);
        if (!generatedSourceDirectoryFile.exists())
            generatedSourceDirectoryFile.mkdirs();
    }

    /**
     * Set up the execution classpath for Jasper.
     * 
     * Put everything in the classesDirectory and all of the dependencies on the
     * classpath.
     * 
     * @param urls a list to which to add the urls of the dependencies
     * @throws Exception
     */
    private void setUpClassPath(List urls) throws Exception
    {
        String classesDir = classesDirectory.getCanonicalPath();
        classesDir = classesDir
        + (classesDir.endsWith(File.pathSeparator) ? "" : File.separator);
        urls.add(new File(classesDir).toURL());

        if (getLog().isDebugEnabled())
            getLog().debug("Adding to classpath classes dir: " + classesDir);

        for (Iterator iter = project.getArtifacts().iterator(); iter.hasNext();)
        {
            Artifact artifact = (Artifact) iter.next();

            // Include runtime and compile time libraries
            if (!Artifact.SCOPE_TEST.equals(artifact.getScope()))
            {
                String filePath = artifact.getFile().getCanonicalPath();
                if (getLog().isDebugEnabled())
                    getLog().debug(
                            "Adding to classpath dependency file: " + filePath);

                urls.add(artifact.getFile().toURL());
            }
        }
    }
}
