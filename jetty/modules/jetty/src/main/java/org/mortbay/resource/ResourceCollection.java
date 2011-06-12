//========================================================================
//Copyright 2007 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.mortbay.util.URIUtil;

/**
 * A collection of resources (dirs).
 * Allows webapps to have multiple (static) sources.
 * The first resource in the collection is the main resource.
 * If a resource is not found in the main resource, it looks it up in 
 * the order the resources were constructed.
 * 
 * @author dyu
 *
 */
public class ResourceCollection extends Resource
{
    
    private Resource[] _resources;
    
    public ResourceCollection()
    {
        
    }
    
    /* ------------------------------------------------------------ */
    public ResourceCollection(Resource[] resources)
    {
        setResources(resources);
    }
    
    /* ------------------------------------------------------------ */
    public ResourceCollection(String[] resources)
    {
        setResources(resources);
    }
    
    /* ------------------------------------------------------------ */
    public ResourceCollection(String csvResources)
    {
        setResources(csvResources);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * 
     * @param resources Resource array
     */
    public void setResources(Resource[] resources)
    {
        if(_resources!=null)
            throw new IllegalStateException("*resources* already set.");
        
        if(resources==null)
            throw new IllegalArgumentException("*resources* must not be null.");
        
        if(resources.length==0)
            throw new IllegalArgumentException("arg *resources* must be one or more resources.");
        
        _resources = resources;
        for(int i=0; i<_resources.length; i++)
        {
            Resource r = _resources[i];
            if(!r.exists() || !r.isDirectory())
                throw new IllegalArgumentException(r + " is not an existing directory.");
        }
    }
    
    /* ------------------------------------------------------------ */
    /**
     * 
     * @param resources String array
     */
    public void setResources(String[] resources)
    {
        if(_resources!=null)
            throw new IllegalStateException("*resources* already set.");
        
        if(resources==null)
            throw new IllegalArgumentException("*resources* must not be null.");
        
        if(resources.length==0)
            throw new IllegalArgumentException("arg *resources* must be one or more resources.");
        
        _resources = new Resource[resources.length];
        try
        {
            for(int i=0; i<resources.length; i++)
            {
                _resources[i] = Resource.newResource(resources[i]);
                if(!_resources[i].exists() || !_resources[i].isDirectory())
                    throw new IllegalArgumentException(_resources[i] + " is not an existing directory.");
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /* ------------------------------------------------------------ */
    /**
     * 
     * @param csvResources Comma separated values
     */
    public void setResources(String csvResources)
    {
        if(_resources!=null)
            throw new IllegalStateException("*resources* already set.");
        
        if(csvResources==null)
            throw new IllegalArgumentException("*csvResources* must not be null.");
        
        StringTokenizer tokenizer = new StringTokenizer(csvResources, ",;");
        int len = tokenizer.countTokens();
        if(len==0)
            throw new IllegalArgumentException("arg *resources* must be one or more resources.");
        
        _resources = new Resource[len];
        try
        {            
            for(int i=0; tokenizer.hasMoreTokens(); i++)
            {
                _resources[i] = Resource.newResource(tokenizer.nextToken().trim());
                if(!_resources[i].exists() || !_resources[i].isDirectory())
                    throw new IllegalArgumentException(_resources[i] + " is not an existing directory.");
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /* ------------------------------------------------------------ */
    /**
     * 
     * @param csvResources Comma separated values
     */
    public void setResourcesAsCSV(String csvResources)
    {
        setResources(csvResources);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * 
     * @return the resource array
     */
    public Resource[] getResources()
    {
        return _resources;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param path The path segment to add
     * @return The contained resource (found first) in the collection of resources
     */
    public Resource addPath(String path) throws IOException, MalformedURLException
    {
        if(_resources==null)
            throw new IllegalStateException("*resources* not set.");
        
        if(path==null)
            throw new MalformedURLException();
        
        if(path.length()==0 || URIUtil.SLASH.equals(path))
            return this;
        
        Resource resource=null;
        ArrayList resources = null;
        int i=0;
        for(; i<_resources.length; i++)
        {
            resource = _resources[i].addPath(path);  
            if (resource.exists())
            {
                if (resource.isDirectory())
                    break;       
                return resource;
            }
        }  

        for(i++; i<_resources.length; i++)
        {
            Resource r = _resources[i].addPath(path); 
            if (r.exists() && r.isDirectory())
            {
                if (resource!=null)
                {
                    resources = new ArrayList();
                    resources.add(resource);
                    resource=null;
                }
                resources.add(r);
            }
        }

        if (resource!=null)
            return resource;
        if (resources!=null)
            return new ResourceCollection((Resource[])resources.toArray(new Resource[resources.size()]));
        return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param path
     * @return the resource(file) if found, returns a list of resource dirs if its a dir, else null.
     * @throws IOException
     * @throws MalformedURLException
     */
    protected Object findResource(String path) throws IOException, MalformedURLException
    {        
        Resource resource=null;
        ArrayList resources = null;
        int i=0;
        for(; i<_resources.length; i++)
        {
            resource = _resources[i].addPath(path);  
            if (resource.exists())
            {
                if (resource.isDirectory())
                    break;
               
                return resource;
            }
        }  

        for(i++; i<_resources.length; i++)
        {
            Resource r = _resources[i].addPath(path); 
            if (r.exists() && r.isDirectory())
            {
                if (resource!=null)
                {
                    resources = new ArrayList();
                    resources.add(resource);
                }
                resources.add(r);
            }
        }
        
        if (resource!=null)
            return resource;
        if (resources!=null)
            return resources;
        return null;
    }
    
    /* ------------------------------------------------------------ */
    public boolean delete() throws SecurityException
    {
        throw new UnsupportedOperationException();
    }
    
    /* ------------------------------------------------------------ */
    public boolean exists()
    {
        if(_resources==null)
            throw new IllegalStateException("*resources* not set.");
        
        return true;
    }
    
    /* ------------------------------------------------------------ */
    public File getFile() throws IOException
    {
        if(_resources==null)
            throw new IllegalStateException("*resources* not set.");
        
        for(int i=0; i<_resources.length; i++)
        {
            File f = _resources[i].getFile();
            if(f!=null)
                return f;
        }
        return null;
    }
    
    /* ------------------------------------------------------------ */
    public InputStream getInputStream() throws IOException
    {
        if(_resources==null)
            throw new IllegalStateException("*resources* not set.");
        
        for(int i=0; i<_resources.length; i++)
        {
            InputStream is = _resources[i].getInputStream();
            if(is!=null)
                return is;
        }
        return null;
    }
    
    /* ------------------------------------------------------------ */
    public String getName()
    {
        if(_resources==null)
            throw new IllegalStateException("*resources* not set.");
        
        for(int i=0; i<_resources.length; i++)
        {
            String name = _resources[i].getName();
            if(name!=null)
                return name;
        }
        return null;
    }
    
    /* ------------------------------------------------------------ */
    public OutputStream getOutputStream() throws IOException, SecurityException
    {
        if(_resources==null)
            throw new IllegalStateException("*resources* not set.");
        
        for(int i=0; i<_resources.length; i++)
        {
            OutputStream os = _resources[i].getOutputStream();
            if(os!=null)
                return os;
        }
        return null;
    }
    
    /* ------------------------------------------------------------ */
    public URL getURL()
    {
        if(_resources==null)
            throw new IllegalStateException("*resources* not set.");
        
        for(int i=0; i<_resources.length; i++)
        {
            URL url = _resources[i].getURL();
            if(url!=null)
                return url;
        }
        return null;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isDirectory()
    {
        if(_resources==null)
            throw new IllegalStateException("*resources* not set.");
        
        return true;
    }
    
    /* ------------------------------------------------------------ */
    public long lastModified()
    {
        if(_resources==null)
            throw new IllegalStateException("*resources* not set.");
        
        for(int i=0; i<_resources.length; i++)
        {
            long lm = _resources[i].lastModified();
            if (lm!=-1)
                return lm;
        }
        return -1;
    }
    
    /* ------------------------------------------------------------ */
    public long length()
    {
        return -1;
    }    
    
    /* ------------------------------------------------------------ */
    /**
     * @return The list of resource names(merged) contained in the collection of resources.
     */    
    public String[] list()
    {
        if(_resources==null)
            throw new IllegalStateException("*resources* not set.");
        
        HashSet set = new HashSet();
        for(int i=0; i<_resources.length; i++)
        {
            String[] list = _resources[i].list();
            for(int j=0; j<list.length; j++)
                set.add(list[j]);
        }
        return (String[])set.toArray(new String[set.size()]);
    }
    
    /* ------------------------------------------------------------ */
    public void release()
    {
        if(_resources==null)
            throw new IllegalStateException("*resources* not set.");
        
        for(int i=0; i<_resources.length; i++)
            _resources[i].release();
    }
    
    /* ------------------------------------------------------------ */
    public boolean renameTo(Resource dest) throws SecurityException
    {
        throw new UnsupportedOperationException();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return the list of resources separated by a path separator
     */
    public String toString()
    {
        if(_resources==null)
            return "";
        
        StringBuffer buffer = new StringBuffer();
        for(int i=0; i<_resources.length; i++)
            buffer.append(_resources[i].toString()).append(';');
        return buffer.toString();
    }

}
