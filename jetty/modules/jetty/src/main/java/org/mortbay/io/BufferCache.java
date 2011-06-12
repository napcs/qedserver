// ========================================================================
// Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.mortbay.util.StringMap;

/* ------------------------------------------------------------------------------- */
/** 
 * Stores a collection of {@link Buffer} objects.
 * Buffers are stored in an ordered collection and can retreived by index or value
 * @author gregw
 */
public class BufferCache
{
    private HashMap _bufferMap=new HashMap();
    private StringMap _stringMap=new StringMap(StringMap.CASE_INSENSTIVE);
    private ArrayList _index= new ArrayList();

    /* ------------------------------------------------------------------------------- */
    /** Add a buffer to the cache at the specified index.
     * @param value The content of the buffer.
     */
    public CachedBuffer add(String value, int ordinal)
    {
        CachedBuffer buffer= new CachedBuffer(value, ordinal);
        _bufferMap.put(buffer, buffer);
        _stringMap.put(value, buffer);
        while ((ordinal - _index.size()) > 0)
            _index.add(null);
        _index.add(ordinal, buffer);
        return buffer;
    }

    public CachedBuffer get(int ordinal)
    {
        if (ordinal < 0 || ordinal >= _index.size())
            return null;
        return (CachedBuffer)_index.get(ordinal);
    }

    public CachedBuffer get(Buffer buffer)
    {
        return (CachedBuffer)_bufferMap.get(buffer);
    }

    public CachedBuffer get(String value)
    {
        return (CachedBuffer)_stringMap.get(value);
    }

    public Buffer lookup(Buffer buffer)
    {
        Buffer b= get(buffer);
        if (b == null)
        {
            if (buffer instanceof Buffer.CaseInsensitve)
                return buffer;
            return new View.CaseInsensitive(buffer);
        }

        return b;
    }
    
    public CachedBuffer getBest(byte[] value, int offset, int maxLength)
    {
        Entry entry = _stringMap.getBestEntry(value, offset, maxLength);
        if (entry!=null)
            return (CachedBuffer)entry.getValue();
        return null;
    }

    public Buffer lookup(String value)
    {
        Buffer b= get(value);
        if (b == null)
        {
            return new CachedBuffer(value,-1);
        }
        return b;
    }

    public String toString(Buffer buffer)
    {
        return lookup(buffer).toString();
    }

    public int getOrdinal(Buffer buffer)
    {
        if (buffer instanceof CachedBuffer)
            return ((CachedBuffer)buffer).getOrdinal();
        buffer=lookup(buffer);
        if (buffer!=null && buffer instanceof CachedBuffer)
            return ((CachedBuffer)buffer).getOrdinal();
        return -1;
    }
    
    public static class CachedBuffer extends ByteArrayBuffer.CaseInsensitive
    {
        private int _ordinal;
        private HashMap _associateMap=null;
        
        public CachedBuffer(String value, int ordinal)
        {
            super(value);
            _ordinal= ordinal;
        }

        public int getOrdinal()
        {
            return _ordinal;
        }

        public CachedBuffer getAssociate(Object key)
        {
            if (_associateMap==null)
                return null;
            return (CachedBuffer)_associateMap.get(key);
        }
        
        public void setAssociate(Object key, CachedBuffer associate)
        {
            // TODO should be synchronized - but lets try without
            if (_associateMap==null)
                _associateMap=new HashMap();
            _associateMap.put(key,associate);
        }
    }
    
    
    public String toString()
    {
        return "CACHE["+
        	"bufferMap="+_bufferMap+
        	",stringMap="+_stringMap+
        	",index="+_index+
        	"]";
    }
}
