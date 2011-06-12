// ========================================================================
// Copyright 1996-2005 Mort Bay Consulting Pty. Ltd.
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
package org.mortbay.resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.mortbay.log.Log;
import org.mortbay.util.IO;
import org.mortbay.util.URIUtil;


/* ------------------------------------------------------------ */
public class JarResource extends URLResource
{

    protected transient JarURLConnection _jarConnection;
    
    /* -------------------------------------------------------- */
    JarResource(URL url)
    {
        super(url,null);
    }

    /* ------------------------------------------------------------ */
    JarResource(URL url, boolean useCaches)
    {
        super(url, null, useCaches);
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void release()
    {
        _jarConnection=null;
        super.release();
    }
    
    /* ------------------------------------------------------------ */
    protected boolean checkConnection()
    {
        super.checkConnection();
        try
        {
            if (_jarConnection!=_connection)
                newConnection();
        }
        catch(IOException e)
        {
            Log.ignore(e);
            _jarConnection=null;
        }
        
        return _jarConnection!=null;
    }

    /* ------------------------------------------------------------ */
    /**
     * @throws IOException Sub-classes of <code>JarResource</code> may throw an IOException (or subclass) 
     */
    protected void newConnection() throws IOException
    {
        _jarConnection=(JarURLConnection)_connection;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns true if the respresenetd resource exists.
     */
    public boolean exists()
    {
        if (_urlString.endsWith("!/"))
            return checkConnection();
        else
            return super.exists();
    }    

    /* ------------------------------------------------------------ */
    public File getFile()
        throws IOException
    {
        return null;
    }
    
    /* ------------------------------------------------------------ */
    public InputStream getInputStream()
        throws java.io.IOException
    {     
        checkConnection();
        if (!_urlString.endsWith("!/"))
            return new FilterInputStream(super.getInputStream()) 
            {
                public void close() throws IOException {this.in=IO.getClosedStream();}
            };

        URL url = new URL(_urlString.substring(4,_urlString.length()-2));      
        InputStream is = url.openStream();
        return is;
    }
    
    /* ------------------------------------------------------------ */
    public static void extract(Resource resource, File directory, boolean deleteOnExit)
        throws IOException
    {
        if(Log.isDebugEnabled())Log.debug("Extract "+resource+" to "+directory);
        
        
        String urlString = resource.getURL().toExternalForm().trim();
        int endOfJarUrl = urlString.indexOf("!/");
        int startOfJarUrl = (endOfJarUrl >= 0?4:0);
        
        if (endOfJarUrl < 0)
            throw new IOException("Not a valid jar url: "+urlString);
        
        URL jarFileURL = new URL(urlString.substring(startOfJarUrl, endOfJarUrl));
        String subEntryName = (endOfJarUrl+2 < urlString.length() ? urlString.substring(endOfJarUrl + 2) : null);
        boolean subEntryIsDir = (subEntryName != null && subEntryName.endsWith("/")?true:false);
      
        if (Log.isDebugEnabled()) Log.debug("Extracting entry = "+subEntryName+" from jar "+jarFileURL);
        
        InputStream is = jarFileURL.openConnection().getInputStream();
        JarInputStream jin = new JarInputStream(is);
        JarEntry entry;
        boolean shouldExtract;
        String directoryCanonicalPath = directory.getCanonicalPath()+"/";
        while((entry=jin.getNextJarEntry())!=null)
        {
            String entryName = entry.getName();
            if ((subEntryName != null) && (entryName.startsWith(subEntryName)))
            { 
                //if there is a particular subEntry that we are looking for, only
                //extract it.
                if (subEntryIsDir)
                {
                    //if it is a subdirectory we are looking for, then we
                    //are looking to extract its contents into the target
                    //directory. Remove the name of the subdirectory so
                    //that we don't wind up creating it too.
                    entryName = entryName.substring(subEntryName.length());
                    if (!entryName.equals(""))
                    {
                        //the entry is 
                        shouldExtract = true;                   
                    }
                    else
                        shouldExtract = false;
                }
                else
                    shouldExtract = true;
            }
            else if ((subEntryName != null) && (!entryName.startsWith(subEntryName)))
            {
                //there is a particular entry we are looking for, and this one
                //isn't it
                shouldExtract = false;
            }
            else
            {
                //we are extracting everything
                shouldExtract =  true;
            }
                
            
            if (!shouldExtract)
            {
                if (Log.isDebugEnabled()) Log.debug("Skipping entry: "+entryName);
                continue;
            }
                
            String dotCheck = entryName.replace('\\', '/');   
            dotCheck = URIUtil.canonicalPath(dotCheck);
            if (dotCheck == null)
            {
                if (Log.isDebugEnabled()) Log.debug("Invalid entry: "+entryName);
                continue;
            }
            
            File file=new File(directory,entryName);
          
            if (entry.isDirectory())
            {
                // Make directory
                if (!file.exists())
                    file.mkdirs();
            }
            else
            {
                // make directory (some jars don't list dirs)
                File dir = new File(file.getParent());
                if (!dir.exists())
                    dir.mkdirs();

                // Make file
                FileOutputStream fout = null;
                try
                {
                    fout = new FileOutputStream(file);
                    IO.copy(jin,fout);
                }
                finally
                {
                    IO.close(fout);
                }

                // touch the file.
                if (entry.getTime()>=0)
                    file.setLastModified(entry.getTime());
            }
            if (deleteOnExit)
                file.deleteOnExit();
        }
        
        if ((subEntryName == null) || (subEntryName != null && subEntryName.equalsIgnoreCase("META-INF/MANIFEST.MF")))
        {
            Manifest manifest = jin.getManifest();
            if (manifest != null)
            {
                File metaInf = new File (directory, "META-INF");
                metaInf.mkdir();
                File f = new File(metaInf, "MANIFEST.MF");
                FileOutputStream fout = new FileOutputStream(f);
                manifest.write(fout);
                fout.close();   
            }
        }
        IO.close(jin);
    }
    
    /* ------------------------------------------------------------ */
    public void extract(File directory, boolean deleteOnExit)
        throws IOException
    {
        extract(this,directory,deleteOnExit);
    }   
}
