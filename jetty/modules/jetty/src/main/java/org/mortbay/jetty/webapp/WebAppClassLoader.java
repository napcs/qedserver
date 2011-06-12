// ========================================================================
// Copyright 1999-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.webapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.log.Log;
import org.mortbay.resource.Resource;
import org.mortbay.util.IO;
import org.mortbay.util.LazyList;
import org.mortbay.util.StringUtil;


/* ------------------------------------------------------------ */
/** ClassLoader for HttpContext.
 * Specializes URLClassLoader with some utility and file mapping
 * methods.
 *
 * This loader defaults to the 2.3 servlet spec behaviour where non
 * system classes are loaded from the classpath in preference to the
 * parent loader.  Java2 compliant loading, where the parent loader
 * always has priority, can be selected with the 
 * {@link org.mortbay.jetty.webapp.WebAppContext#setParentLoaderPriority(boolean)} method.
 *
 * If no parent class loader is provided, then the current thread context classloader will
 * be used.  If that is null then the classloader that loaded this class is used as the parent.
 * 
 * @author Greg Wilkins (gregw)
 */
public class WebAppClassLoader extends URLClassLoader 
{
    private String _name;
    private WebAppContext _context;
    private ClassLoader _parent;
    private HashSet _extensions;
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     */
    public WebAppClassLoader(WebAppContext context)
        throws IOException
    {
        this(null,context);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     */
    public WebAppClassLoader(ClassLoader parent, WebAppContext context)
        throws IOException
    {
        super(new URL[]{},parent!=null?parent
                :(Thread.currentThread().getContextClassLoader()!=null?Thread.currentThread().getContextClassLoader()
                        :(WebAppClassLoader.class.getClassLoader()!=null?WebAppClassLoader.class.getClassLoader()
                                :ClassLoader.getSystemClassLoader())));
        _parent=getParent();
        _context=context;
        if (_parent==null)
            throw new IllegalArgumentException("no parent classloader!");
        
        _extensions = new HashSet();
        _extensions.add(".jar");
        _extensions.add(".zip");
        
        String extensions = System.getProperty(WebAppClassLoader.class.getName() + ".extensions");
        if(extensions!=null)
        {
            StringTokenizer tokenizer = new StringTokenizer(extensions, ",;");
            while(tokenizer.hasMoreTokens())
                _extensions.add(tokenizer.nextToken().trim());
        }
        
        if (context.getExtraClasspath()!=null)
            addClassPath(context.getExtraClasspath());
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return the name of the classloader
     */
    public String getName()
    {
        return _name;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param name the name of the classloader
     */
    public void setName(String name)
    {
        _name=name;
    }
    

    /* ------------------------------------------------------------ */
    public ContextHandler getContext()
    {
        return _context;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param classPath Comma or semicolon separated path of filenames or URLs
     * pointing to directories or jar files. Directories should end
     * with '/'.
     */
    public void addClassPath(String classPath)
    	throws IOException
    {
        if (classPath == null)
            return;
            
        StringTokenizer tokenizer= new StringTokenizer(classPath, ",;");
        while (tokenizer.hasMoreTokens())
        {
            Resource resource= Resource.newResource(tokenizer.nextToken());
            if (Log.isDebugEnabled())
                Log.debug("Path resource=" + resource);

            // Resolve file path if possible
            File file= resource.getFile();
            if (file != null)
            {
                URL url= resource.getURL();
                addURL(url);
            }
            else
            {
                // Add resource or expand jar/
                if (!resource.isDirectory() && file == null)
                {
                    InputStream in= resource.getInputStream();
                    File tmp_dir=_context.getTempDirectory();
                    if (tmp_dir==null)
                    {
                        tmp_dir = File.createTempFile("jetty.cl.lib",null);
                        tmp_dir.mkdir();
                        tmp_dir.deleteOnExit();
                    }
                    File lib= new File(tmp_dir, "lib");
                    if (!lib.exists())
                    {
                        lib.mkdir();
                        lib.deleteOnExit();
                    }
                    File jar= File.createTempFile("Jetty-", ".jar", lib);
                    
                    jar.deleteOnExit();
                    if (Log.isDebugEnabled())
                        Log.debug("Extract " + resource + " to " + jar);
                    FileOutputStream out = null;
                    try
                    {
                        out= new FileOutputStream(jar);
                        IO.copy(in, out);
                    }
                    finally
                    {
                        IO.close(out);
                    }
                    
                    URL url= jar.toURL();
                    addURL(url);
                }
                else
                {
                    URL url= resource.getURL();
                    addURL(url);
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * @param file Checks if this file type can be added to the classpath.
     */
    private boolean isFileSupported(String file)
    {
        int dot = file.lastIndexOf('.');
        return dot!=-1 && _extensions.contains(file.substring(dot));
    }
    
    /* ------------------------------------------------------------ */
    /** Add elements to the class path for the context from the jar and zip files found
     *  in the specified resource.
     * @param lib the resource that contains the jar and/or zip files.
     * @param append true if the classpath entries are to be appended to any
     * existing classpath, or false if they replace the existing classpath.
     * @see #setClassPath(String)
     */
    public void addJars(Resource lib)
    {
        if (lib.exists() && lib.isDirectory())
        {
            String[] files=lib.list();
            for (int f=0;files!=null && f<files.length;f++)
            {
                try {
                    Resource fn=lib.addPath(files[f]);
                    String fnlc=fn.getName().toLowerCase();
                    if (isFileSupported(fnlc))
                    {
                    	String jar=fn.toString();
                    	jar=StringUtil.replace(jar, ",", "%2C");
                    	jar=StringUtil.replace(jar, ";", "%3B");
                        addClassPath(jar);
                    }
                }
                catch (Exception ex)
                {
                    Log.warn(Log.EXCEPTION,ex);
                }
            }
        }
    }
    /* ------------------------------------------------------------ */
    public void destroy()
    {
        this._parent=null;
    }
    

    /* ------------------------------------------------------------ */
    public PermissionCollection getPermissions(CodeSource cs)
    {
        // TODO check CodeSource
        PermissionCollection permissions=_context.getPermissions();
        PermissionCollection pc= (permissions == null) ? super.getPermissions(cs) : permissions;
        return pc;
    }

    /* ------------------------------------------------------------ */
    public URL getResource(String name)
    {
        URL url= null;
        boolean tried_parent= false;
        if (_context.isParentLoaderPriority() || isSystemPath(name))
        {
            tried_parent= true;
            
            if (_parent!=null)
                url= _parent.getResource(name);
        }

        if (url == null)
        {
            url= this.findResource(name);

            if (url == null && name.startsWith("/"))
            {
                if (Log.isDebugEnabled())
                    Log.debug("HACK leading / off " + name);
                url= this.findResource(name.substring(1));
            }
        }

        if (url == null && !tried_parent)
        {
            if (_parent!=null)
                url= _parent.getResource(name);
        }

        if (url != null)
            if (Log.isDebugEnabled())
                Log.debug("getResource("+name+")=" + url);

        return url;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isServerPath(String name)
    {
        name=name.replace('/','.');
        while(name.startsWith("."))
            name=name.substring(1);

        String[] server_classes = _context.getServerClasses();
        if (server_classes!=null)
        {
            for (int i=0;i<server_classes.length;i++)
            {
                boolean result=true;
                String c=server_classes[i];
                if (c.startsWith("-"))
                {
                    c=c.substring(1); // TODO cache
                    result=false;
                }
                
                if (c.endsWith("."))
                {
                    if (name.startsWith(c))
                        return result;
                }
                else if (name.equals(c))
                    return result;
            }
        }
        return false;
    }

    /* ------------------------------------------------------------ */
    public boolean isSystemPath(String name)
    {
        name=name.replace('/','.');
        while(name.startsWith("."))
            name=name.substring(1);
        String[] system_classes = _context.getSystemClasses();
        if (system_classes!=null)
        {
            for (int i=0;i<system_classes.length;i++)
            {
                boolean result=true;
                String c=system_classes[i];
                
                if (c.startsWith("-"))
                {
                    c=c.substring(1); // TODO cache
                    result=false;
                }
                
                if (c.endsWith("."))
                {
                    if (name.startsWith(c))
                        return result;
                }
                else if (name.equals(c))
                    return result;
            }
        }
        
        return false;
        
    }

    /* ------------------------------------------------------------ */
    public Class loadClass(String name) throws ClassNotFoundException
    {
        return loadClass(name, false);
    }

    /* ------------------------------------------------------------ */
    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        Class c= findLoadedClass(name);
        ClassNotFoundException ex= null;
        boolean tried_parent= false;
        
        if (c == null && _parent!=null && (_context.isParentLoaderPriority() || isSystemPath(name)) )
        {
            tried_parent= true;
            try
            {
                c= _parent.loadClass(name);
                if (Log.isDebugEnabled())
                    Log.debug("loaded " + c);
            }
            catch (ClassNotFoundException e)
            {
                ex= e;
            }
        }

        if (c == null)
        {
            try
            {
                c= this.findClass(name);
            }
            catch (ClassNotFoundException e)
            {
                ex= e;
            }
        }

        if (c == null && _parent!=null && !tried_parent && !isServerPath(name) )
            c= _parent.loadClass(name);

        if (c == null)
            throw ex;

        if (resolve)
            resolveClass(c);

        if (Log.isDebugEnabled())
            Log.debug("loaded " + c+ " from "+c.getClassLoader());
        
        return c;
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        if (Log.isDebugEnabled())
            return "ContextLoader@" + _name + "(" + LazyList.array2List(getURLs()) + ") / " + _parent;
        return "ContextLoader@" + _name;
    }
    
}
