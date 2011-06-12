// ========================================================================
// Copyright 2000-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.View;
import org.mortbay.resource.Resource;
import org.mortbay.resource.ResourceFactory;


/* ------------------------------------------------------------ */
/** 
 * @author Greg Wilkins
 */
public class ResourceCache extends AbstractLifeCycle implements Serializable
{   
    private int _maxCachedFileSize =1024*1024;
    private int _maxCachedFiles=2048;
    private int _maxCacheSize =16*1024*1024;
    private MimeTypes _mimeTypes;
    
    protected transient Map _cache;
    protected transient int _cachedSize;
    protected transient int _cachedFiles;
    protected transient Content _mostRecentlyUsed;
    protected transient Content _leastRecentlyUsed;


    /* ------------------------------------------------------------ */
    /** Constructor.
     */
    public ResourceCache(MimeTypes mimeTypes)
    {
        _mimeTypes=mimeTypes;
    }

    /* ------------------------------------------------------------ */
    public int getCachedSize()
    {
        return _cachedSize;
    }
    
    /* ------------------------------------------------------------ */
    public int getCachedFiles()
    {
        return _cachedFiles;
    }
    
    
    /* ------------------------------------------------------------ */
    public int getMaxCachedFileSize()
    {
        return _maxCachedFileSize;
    }

    /* ------------------------------------------------------------ */
    public void setMaxCachedFileSize(int maxCachedFileSize)
    {
        _maxCachedFileSize = maxCachedFileSize;
        flushCache();
    }

    /* ------------------------------------------------------------ */
    public int getMaxCacheSize()
    {
        return _maxCacheSize;
    }

    /* ------------------------------------------------------------ */
    public void setMaxCacheSize(int maxCacheSize)
    {
        _maxCacheSize = maxCacheSize;
        flushCache();
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the maxCachedFiles.
     */
    public int getMaxCachedFiles()
    {
        return _maxCachedFiles;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param maxCachedFiles The maxCachedFiles to set.
     */
    public void setMaxCachedFiles(int maxCachedFiles)
    {
        _maxCachedFiles = maxCachedFiles;
    }
    
    /* ------------------------------------------------------------ */
    public void flushCache()
    {
        if (_cache!=null)
        {
            synchronized(this)
            {
                ArrayList values=new ArrayList(_cache.values());
                Iterator iter = values.iterator();
                while(iter.hasNext())
                {
                    Content content = (Content)iter.next();
                    content.invalidate();
                }
                
                _cache.clear();
                _cachedSize=0;
                _cachedFiles=0;
                _mostRecentlyUsed=null;
                _leastRecentlyUsed=null;
            }
        }
    }

    /* ------------------------------------------------------------ */
    /** Get a Entry from the cache.
     * Get either a valid entry object or create a new one if possible.
     *
     * @param pathInContext The key into the cache
     * @param factory If no matching entry is found, this {@link ResourceFactory} will be used to create the {@link Resource} 
     *                for the new enry that is created.
     * @return The entry matching <code>pathInContext</code>, or a new entry if no matching entry was found
     */
    public Content lookup(String pathInContext, ResourceFactory factory)
        throws IOException
    {
        Content content=null;
        
        // Look up cache operations
        synchronized(_cache)
        {
            // Look for it in the cache
            content = (Content)_cache.get(pathInContext);
        
            if (content!=null && content.isValid())
            {
                return content;
            }    
        }
        Resource resource=factory.getResource(pathInContext);
        return load(pathInContext,resource);
    }
    
    public Content lookup(String pathInContext, Resource resource)
        throws IOException
    {
        Content content=null;
        
        // Look up cache operations
        synchronized(_cache)
        {
            // Look for it in the cache
            content = (Content)_cache.get(pathInContext);
        
            if (content!=null && content.isValid())
            {
                return content;
            }    
        }
        return load(pathInContext,resource);
    }

    private Content load(String pathInContext, Resource resource)
        throws IOException
    {
        Content content=null;
        if (resource!=null && resource.exists() && !resource.isDirectory())
        {
            long len = resource.length();
            if (len>0 && len<_maxCachedFileSize && len<_maxCacheSize)
            {
                content = new Content(resource);
                fill(content);

                synchronized(_cache)
                {
                    // check that somebody else did not fill this spot.
                    Content content2 =(Content)_cache.get(pathInContext);
                    if (content2!=null)
                    {
                        content.release();
                        return content2;
                    }

                    int must_be_smaller_than=_maxCacheSize-(int)len;
                    while(_cachedSize>must_be_smaller_than || (_maxCachedFiles>0 && _cachedFiles>=_maxCachedFiles))
                        _leastRecentlyUsed.invalidate();
                    content.cache(pathInContext);
                    
                    return content;
                }
            }
        }

        return null; 
    }


    /* ------------------------------------------------------------ */
    public synchronized void doStart()
        throws Exception
    {
        _cache=new HashMap();
        _cachedSize=0;
        _cachedFiles=0;
    }

    /* ------------------------------------------------------------ */
    /** Stop the context.
     */
    public void doStop()
        throws InterruptedException
    {
        flushCache();
    }

    /* ------------------------------------------------------------ */
    protected void fill(Content content)
        throws IOException
    {
        try
        {
            InputStream in = content.getResource().getInputStream();
            int len=(int)content.getResource().length();
            Buffer buffer = new ByteArrayBuffer(len);
            buffer.readFrom(in,len);
            in.close();
            content.setBuffer(buffer);
        }
        finally
        {
            content.getResource().release();
        }
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** MetaData associated with a context Resource.
     */
    public class Content implements HttpContent
    {
        String _key;
        Resource _resource;
        long _lastModified;
        Content _prev;
        Content _next;
        
        Buffer _lastModifiedBytes;
        Buffer _contentType;
        Buffer _buffer;

        /* ------------------------------------------------------------ */
        Content(Resource resource)
        {
            _resource=resource;

            _next=this;
            _prev=this;
            _contentType=_mimeTypes.getMimeByExtension(_resource.toString());
            
            _lastModified=resource.lastModified();
        }

        /* ------------------------------------------------------------ */
        void cache(String pathInContext)
        {
            _key=pathInContext;
            _next=_mostRecentlyUsed;
            _mostRecentlyUsed=this;
            if (_next!=null)
                _next._prev=this;
            _prev=null;
            if (_leastRecentlyUsed==null)
                _leastRecentlyUsed=this;

            _cache.put(_key,this);
            _cachedSize+=_buffer.length();
            _cachedFiles++;
            if (_lastModified!=-1)
                _lastModifiedBytes=new ByteArrayBuffer(HttpFields.formatDate(_lastModified,false));
        }

        /* ------------------------------------------------------------ */
        public String getKey()
        {
            return _key;
        }

        /* ------------------------------------------------------------ */
        public boolean isCached()
        {
            return _key!=null;
        }

        /* ------------------------------------------------------------ */
        public Resource getResource()
        {
            return _resource;
        }
        
        /* ------------------------------------------------------------ */
        boolean isValid()
        {
            if (_lastModified==_resource.lastModified())
            {
                if (_mostRecentlyUsed!=this)
                {
                    Content tp = _prev;
                    Content tn = _next;

                    _next=_mostRecentlyUsed;
                    _mostRecentlyUsed=this;
                    if (_next!=null)
                        _next._prev=this;
                    _prev=null;

                    if (tp!=null)
                        tp._next=tn;
                    if (tn!=null)
                        tn._prev=tp;

                    if (_leastRecentlyUsed==this && tp!=null)
                        _leastRecentlyUsed=tp;
                }
                return true;
            }

            invalidate();
            return false;
        }

        /* ------------------------------------------------------------ */
        public void invalidate()
        {
            synchronized(this)
            {
                // Invalidate it
                _cache.remove(_key);
                _key=null;
                _cachedSize=_cachedSize-(int)_buffer.length();
                _cachedFiles--;
                
                if (_mostRecentlyUsed==this)
                    _mostRecentlyUsed=_next;
                else
                    _prev._next=_next;
                
                if (_leastRecentlyUsed==this)
                    _leastRecentlyUsed=_prev;
                else
                    _next._prev=_prev;
                
                _prev=null;
                _next=null;
                if (_resource!=null)
                    _resource.release();
                _resource=null;
                
            }
        }

        /* ------------------------------------------------------------ */
        public Buffer getLastModified()
        {
            return _lastModifiedBytes;
        }

        /* ------------------------------------------------------------ */
        public Buffer getContentType()
        {
            return _contentType;
        }

        /* ------------------------------------------------------------ */
        public void setContentType(Buffer type)
        {
            _contentType=type;
        }

        /* ------------------------------------------------------------ */
        public void release()
        {
        }

        /* ------------------------------------------------------------ */
        public Buffer getBuffer()
        {
            if (_buffer==null)
                return null;
            return new View(_buffer);
        }
        
        /* ------------------------------------------------------------ */
        public void setBuffer(Buffer buffer)
        {
            _buffer=buffer;
        }

        /* ------------------------------------------------------------ */
        public long getContentLength()
        {
            if (_buffer==null)
            {
                if (_resource!=null)
                    return _resource.length();
                return -1;
            }
            return _buffer.length();
        }

        /* ------------------------------------------------------------ */
        public InputStream getInputStream() throws IOException
        {
            return _resource.getInputStream();
        }
        
    }

}
