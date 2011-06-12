//========================================================================
//$Id: TagLibConfiguration.java,v 1.4 2005/08/13 00:01:27 gregwilkins Exp $
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.webapp;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.mortbay.log.Log;
import org.mortbay.resource.Resource;
import org.mortbay.util.Loader;
import org.mortbay.xml.XmlParser;

/* ------------------------------------------------------------ */
/** TagLibConfiguration.
 * 
 * The class searches for TLD descriptors found in web.xml, in WEB-INF/*.tld files of the web app
 * or *.tld files withing jars found in WEB-INF/lib of the webapp.   Any listeners defined in these
 * tld's are added to the context.
 * 
 * The "org.mortbay.jetty.webapp.NoTLDJarPattern" context init parameter, if set, is used as a 
 * regular expression to match commonly known jar files known not to contain TLD files (and 
 * thus not needed to be scanned).
 * 
 * &lt;bile&gt;Scanning for TLDs is total rubbish special case for JSPs! If there was a general use-case for web app
 * frameworks to register listeners directly, then a generic mechanism could have been added to the servlet
 * spec.  Instead some special purpose JSP support is required that breaks all sorts of encapsualtion rules as
 * the servlet container must go searching for and then parsing the descriptors for one particular framework.
 * It only appears to be used by JSF.
 * &lt;/bile&gt;
 * 
 * @author gregw
 *
 */
public class TagLibConfiguration implements Configuration
{
    WebAppContext _context;
    
    /* ------------------------------------------------------------ */
    public void setWebAppContext(WebAppContext context)
    {
        _context=context;
    }

    /* ------------------------------------------------------------ */
    public WebAppContext getWebAppContext()
    {
        return _context;
    }

    /* ------------------------------------------------------------ */
    public void configureClassLoader() throws Exception
    {
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.servlet.WebAppContext.Configuration#configureDefaults()
     */
    public void configureDefaults() throws Exception
    {
    }

    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.servlet.WebAppContext.Configuration#configureWebApp()
     */
    public void configureWebApp() throws Exception
    {   
        Set tlds = new HashSet();
        Set jars = new HashSet();
        
        // Find tld's from web.xml
        // When the XMLConfigurator (or other configurator) parsed the web.xml,
        // It should have created aliases for all TLDs.  So search resources aliases
        // for aliases ending in tld
        if (_context.getResourceAliases()!=null && 
            _context.getBaseResource()!=null && 
            _context.getBaseResource().exists())
        {
            Iterator iter=_context.getResourceAliases().values().iterator();
            while(iter.hasNext())
            {
                String location = (String)iter.next();
                if (location!=null && location.toLowerCase().endsWith(".tld"))
                {
                    if (!location.startsWith("/"))
                        location="/WEB-INF/"+location;
                    Resource l=_context.getBaseResource().addPath(location);
                    tlds.add(l);
                }
            }
        }
        
        // Look for any tlds in WEB-INF directly.
        Resource web_inf = _context.getWebInf();
        if (web_inf!=null)
        {
            String[] contents = web_inf.list();
            for (int i=0;contents!=null && i<contents.length;i++)
            {
                if (contents[i]!=null && contents[i].toLowerCase().endsWith(".tld"))
                {
                    Resource l=_context.getWebInf().addPath(contents[i]);
                    tlds.add(l);
                }
                
            }
        }
        
        // Get the pattern for noTLDJars
        String no_TLD_attr = _context.getInitParameter("org.mortbay.jetty.webapp.NoTLDJarPattern");
        Pattern no_TLD_pattern = no_TLD_attr==null?null:Pattern.compile(no_TLD_attr);
        
        // Look for tlds in any jars
 
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        boolean parent=false;
        
        while (loader!=null)
        {
            if (loader instanceof URLClassLoader)
            {
                URL[] urls = ((URLClassLoader)loader).getURLs();

                if (urls!=null)
                {
                    for (int i=0;i<urls.length;i++)
                    {   
                        if (urls[i].toString().toLowerCase().endsWith(".jar"))
                        {

                            String jar = urls[i].toString();
                            int slash=jar.lastIndexOf('/');
                            jar=jar.substring(slash+1);

                            if (parent && ( 
                                    (!_context.isParentLoaderPriority() && jars.contains(jar)) || 
                                    (no_TLD_pattern!=null && no_TLD_pattern.matcher(jar).matches())))
                                continue;
                            jars.add(jar);
                            
                            Log.debug("TLD search of {}",urls[i]);
                            
                            File file=Resource.newResource(urls[i]).getFile();
                            if (file==null || !file.exists() || !file.canRead())
                                continue;
                            
                            JarFile jarfile = null;
                            try
                            {
                                jarfile = new JarFile(file);
                                Enumeration e = jarfile.entries();
                                while (e.hasMoreElements())
                                {
                                    ZipEntry entry = (ZipEntry)e.nextElement();
                                    String name = entry.getName();
                                    if (name.startsWith("META-INF/") && name.toLowerCase().endsWith(".tld"))
                                    {
                                        Resource tld=Resource.newResource("jar:"+urls[i]+"!/"+name);
                                        tlds.add(tld);
                                        Log.debug("TLD found {}",tld);
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                Log.warn("Failed to read file: " + file, e); 
                            }
                            finally
                            {
                                if (jarfile != null)
                                {
                                    jarfile.close();
                                }
                            }   
                        }
                    }
                }
            }

            loader=loader.getParent();
            parent=true;
            
        }
        
        // Create a TLD parser
        XmlParser parser = new XmlParser(false);
        parser.redirectEntity("web-jsptaglib_1_1.dtd",Loader.getResource(TagLibConfiguration.class,"javax/servlet/jsp/resources/web-jsptaglibrary_1_1.dtd", false));
        parser.redirectEntity("web-jsptaglib_1_2.dtd",Loader.getResource(TagLibConfiguration.class,"javax/servlet/jsp/resources/web-jsptaglibrary_1_2.dtd", false));
        parser.redirectEntity("web-jsptaglib_2_0.xsd",Loader.getResource(TagLibConfiguration.class,"javax/servlet/jsp/resources/web-jsptaglibrary_2_0.xsd", false));
        parser.redirectEntity("web-jsptaglibrary_1_1.dtd",Loader.getResource(TagLibConfiguration.class,"javax/servlet/jsp/resources/web-jsptaglibrary_1_1.dtd", false));
        parser.redirectEntity("web-jsptaglibrary_1_2.dtd",Loader.getResource(TagLibConfiguration.class,"javax/servlet/jsp/resources/web-jsptaglibrary_1_2.dtd", false));
        parser.redirectEntity("web-jsptaglibrary_2_0.xsd",Loader.getResource(TagLibConfiguration.class,"javax/servlet/jsp/resources/web-jsptaglibrary_2_0.xsd", false));
        parser.setXpath("/taglib/listener/listener-class");
        // Parse all the discovered TLDs
        Iterator iter = tlds.iterator();
        while (iter.hasNext())
        {
            try
            {
                Resource tld = (Resource)iter.next();
                if (Log.isDebugEnabled()) Log.debug("TLD="+tld);
                
                XmlParser.Node root;
                
                try
                {
                    //xerces on apple appears to sometimes close the zip file instead
                    //of the inputstream, so try opening the input stream, but if
                    //that doesn't work, fallback to opening a new url
                    root = parser.parse(tld.getInputStream());
                }
                catch (Exception e)
                {
                    root = parser.parse(tld.getURL().toString());
                }

		if (root==null)
		{
		    Log.warn("No TLD root in {}",tld);
		    continue;
		}
                
                for (int i=0;i<root.size();i++)
                {
                    Object o=root.get(i);
                    if (o instanceof XmlParser.Node)
                    {
                        XmlParser.Node node = (XmlParser.Node)o;
                        if ("listener".equals(node.getTag()))
                        {
                            String className=node.getString("listener-class",false,true);
                            if (Log.isDebugEnabled()) Log.debug("listener="+className);
                            
                            try
                            {
                                Class listenerClass=getWebAppContext().loadClass(className);
                                EventListener l=(EventListener)listenerClass.newInstance();
                                _context.addEventListener(l);
                            }
                            catch(Exception e)
                            {
                                Log.warn("Could not instantiate listener "+className+": "+e);
                                Log.debug(e);
                            }
                            catch(Error e)
                            {
                                Log.warn("Could not instantiate listener "+className+": "+e);
                                Log.debug(e);
                            }
                        }
                    }
                }
            }
            catch(Exception e)
            {
                Log.warn(e);
            }
        }
    }


    public void deconfigureWebApp() throws Exception
    {
    }
    

}
