// ========================================================================
// Copyright 2003-2005 Mort Bay Consulting Pty. Ltd.
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

import org.mortbay.log.Log;
import org.mortbay.resource.Resource;
/* ------------------------------------------------------------------------------- */
/**
 * Configure class path from a WEB-INF directory found within a contexts resource base.
 * 
 * @author gregw
 */
public class WebInfConfiguration implements Configuration
{
    protected WebAppContext _context;

    public WebInfConfiguration()
    {
    }

    /* ------------------------------------------------------------------------------- */
    public void setWebAppContext (WebAppContext context)
    {
        _context = context;
    }

    /* ------------------------------------------------------------------------------- */
    public WebAppContext getWebAppContext()
    {
        return _context;
    }

    /* ------------------------------------------------------------------------------- */
    /** Configure ClassPath.
     * This method is called before the context ClassLoader is created.
     * Paths and libraries should be added to the context using the setClassPath,
     * addClassPath and addClassPaths methods.  The default implementation looks
     * for WEB-INF/classes, WEB-INF/lib/*.zip and WEB-INF/lib/*.jar
     * @throws Exception
     */
    public  void configureClassLoader()
    throws Exception
    {
        //cannot configure if the context is already started
        if (_context.isStarted())
        {
            if (Log.isDebugEnabled()){Log.debug("Cannot configure webapp after it is started");}
            return;
        }

        Resource web_inf=_context.getWebInf();

        // Add WEB-INF classes and lib classpaths
        if (web_inf != null && web_inf.isDirectory() && _context.getClassLoader() instanceof WebAppClassLoader)
        {
            // Look for classes directory
            Resource classes= web_inf.addPath("classes/");
            if (classes.exists())
                ((WebAppClassLoader)_context.getClassLoader()).addClassPath(classes.toString());

            // Look for jars
            Resource lib= web_inf.addPath("lib/");
            if (lib.exists() || lib.isDirectory())
                ((WebAppClassLoader)_context.getClassLoader()).addJars(lib);
        }
        
     }

    /* ------------------------------------------------------------------------------- */
    public void configureDefaults() throws Exception
    {
    }

    /* ------------------------------------------------------------------------------- */
    public void configureWebApp() throws Exception
    {
    }

    /* ------------------------------------------------------------------------------- */
    public void deconfigureWebApp() throws Exception
    {
    }

}
