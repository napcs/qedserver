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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/* ------------------------------------------------------------------------------- */
/**
 * @author gregw
 */
public class ByteArrayBuffer extends AbstractBuffer
{
    protected byte[] _bytes;

    protected ByteArrayBuffer(int access, boolean isVolatile)
    {
        super(access, isVolatile);
    }
    
    public ByteArrayBuffer(byte[] bytes)
    {
        this(bytes, 0, bytes.length, READWRITE);
    }

    public ByteArrayBuffer(byte[] bytes, int index, int length)
    {
        this(bytes, index, length, READWRITE);
    }

    public ByteArrayBuffer(byte[] bytes, int index, int length, int access)
    {
        super(READWRITE, NON_VOLATILE);
        _bytes = bytes;
        setPutIndex(index + length);
        setGetIndex(index);
        _access = access;
    }

    public ByteArrayBuffer(byte[] bytes, int index, int length, int access, boolean isVolatile)
    {
        super(READWRITE, isVolatile);
        _bytes = bytes;
        setPutIndex(index + length);
        setGetIndex(index);
        _access = access;
    }

    public ByteArrayBuffer(int size)
    {
        this(new byte[size], 0, size, READWRITE);
        setPutIndex(0);
    }

    public ByteArrayBuffer(String value)
    {
        super(READWRITE,NON_VOLATILE);
        _bytes = Portable.getBytes(value);
        setGetIndex(0);
        setPutIndex(_bytes.length);
        _access=IMMUTABLE;
        _string = value;
    }

    public ByteArrayBuffer(String value,String encoding) throws UnsupportedEncodingException
    {
        super(READWRITE,NON_VOLATILE);
        _bytes = value.getBytes(encoding);
        setGetIndex(0);
        setPutIndex(_bytes.length);
        _access=IMMUTABLE;
        _string = value;
    }

    public byte[] array()
    {
        return _bytes;
    }

    public int capacity()
    {
        return _bytes.length;
    }
    
    public void compact()
    {
        if (isReadOnly()) 
            throw new IllegalStateException(__READONLY);
        int s = markIndex() >= 0 ? markIndex() : getIndex();
        if (s > 0)
        {
            int length = putIndex() - s;
            if (length > 0)
            {
                Portable.arraycopy(_bytes, s,_bytes, 0, length);
            }
            if (markIndex() > 0) setMarkIndex(markIndex() - s);
            setGetIndex(getIndex() - s);
            setPutIndex(putIndex() - s);
        }
    }


    public boolean equals(Object obj)
    {
        if (obj==this)
            return true;

        if (obj == null || !(obj instanceof Buffer)) 
            return false;
        
        if (obj instanceof Buffer.CaseInsensitve)
            return equalsIgnoreCase((Buffer)obj);
        

        Buffer b = (Buffer) obj;
        
        // reject different lengths
        if (b.length() != length()) 
            return false;

        // reject AbstractBuffer with different hash value
        if (_hash != 0 && obj instanceof AbstractBuffer)
        {
            AbstractBuffer ab = (AbstractBuffer) obj;
            if (ab._hash != 0 && _hash != ab._hash) 
                return false;
        }

        // Nothing for it but to do the hard grind.
        int get=getIndex();
        int bi=b.putIndex();
        for (int i = putIndex(); i-->get;)
        {
            byte b1 = _bytes[i];
            byte b2 = b.peek(--bi);
            if (b1 != b2) return false;
        }
        return true;
    }


    public boolean equalsIgnoreCase(Buffer b)
    {
        if (b==this)
            return true;
        
        // reject different lengths
        if (b==null || b.length() != length()) 
            return false;

        // reject AbstractBuffer with different hash value
        if (_hash != 0 && b instanceof AbstractBuffer)
        {
            AbstractBuffer ab = (AbstractBuffer) b;
            if (ab._hash != 0 && _hash != ab._hash) return false;
        }

        // Nothing for it but to do the hard grind.
        int get=getIndex();
        int bi=b.putIndex();
        byte[] barray=b.array();
        if (barray==null)
        {
            for (int i = putIndex(); i-->get;)
            {
                byte b1 = _bytes[i];
                byte b2 = b.peek(--bi);
                if (b1 != b2)
                {
                    if ('a' <= b1 && b1 <= 'z') b1 = (byte) (b1 - 'a' + 'A');
                    if ('a' <= b2 && b2 <= 'z') b2 = (byte) (b2 - 'a' + 'A');
                    if (b1 != b2) return false;
                }
            }
        }
        else
        {
            for (int i = putIndex(); i-->get;)
            {
                byte b1 = _bytes[i];
                byte b2 = barray[--bi];
                if (b1 != b2)
                {
                    if ('a' <= b1 && b1 <= 'z') b1 = (byte) (b1 - 'a' + 'A');
                    if ('a' <= b2 && b2 <= 'z') b2 = (byte) (b2 - 'a' + 'A');
                    if (b1 != b2) return false;
                }
            }
        }
        return true;
    }

    public byte get()
    {
        return _bytes[_get++];
    }

    public int hashCode()
    {
        if (_hash == 0 || _hashGet!=_get || _hashPut!=_put) 
        {
            int get=getIndex();
            for (int i = putIndex(); i-- >get;)
            {
                byte b = _bytes[i];
                if ('a' <= b && b <= 'z') 
                    b = (byte) (b - 'a' + 'A');
                _hash = 31 * _hash + b;
            }
            if (_hash == 0) 
                _hash = -1;
            _hashGet=_get;
            _hashPut=_put;
        }
        return _hash;
    }
    
    
    public byte peek(int index)
    {
        return _bytes[index];
    }
    
    public int peek(int index, byte[] b, int offset, int length)
    {
        int l = length;
        if (index + l > capacity())
        {
            l = capacity() - index;
            if (l==0)
                return -1;
        }
        
        if (l < 0) 
            return -1;
        
        Portable.arraycopy(_bytes, index, b, offset, l);
        return l;
    }

    public void poke(int index, byte b)
    {
        /* 
        if (isReadOnly()) 
            throw new IllegalStateException(__READONLY);
        
        if (index < 0) 
            throw new IllegalArgumentException("index<0: " + index + "<0");
        if (index > capacity())
                throw new IllegalArgumentException("index>capacity(): " + index + ">" + capacity());
        */
        _bytes[index] = b;
    }
    
    public int poke(int index, Buffer src)
    {
        _hash=0;
        
        /* 
        if (isReadOnly()) 
            throw new IllegalStateException(__READONLY);
        if (index < 0) 
            throw new IllegalArgumentException("index<0: " + index + "<0");
        */
        
        int length=src.length();
        if (index + length > capacity())
        {
            length=capacity()-index;
            /*
            if (length<0)
                throw new IllegalArgumentException("index>capacity(): " + index + ">" + capacity());
            */
        }
        
        byte[] src_array = src.array();
        if (src_array != null)
            Portable.arraycopy(src_array, src.getIndex(), _bytes, index, length);
        else if (src_array != null)
        {
            int s=src.getIndex();
            for (int i=0;i<length;i++)
                poke(index++,src_array[s++]);
        }
        else 
        {
            int s=src.getIndex();
            for (int i=0;i<length;i++)
                _bytes[index++]=src.peek(s++);
        }
        
        return length;
    }
    

    public int poke(int index, byte[] b, int offset, int length)
    {
        _hash=0;
        /*
        if (isReadOnly()) 
            throw new IllegalStateException(__READONLY);
        if (index < 0) 
            throw new IllegalArgumentException("index<0: " + index + "<0");
        */
        
        if (index + length > capacity())
        {
            length=capacity()-index;
            /* if (length<0)
                throw new IllegalArgumentException("index>capacity(): " + index + ">" + capacity());
            */
        }
        
        Portable.arraycopy(b, offset, _bytes, index, length);
        
        return length;
    }

    /* ------------------------------------------------------------ */
    /** Wrap a byte array.
     * @param b
     * @param off
     * @param len
     */
    public void wrap(byte[] b, int off, int len)
    {
        if (isReadOnly()) throw new IllegalStateException(__READONLY);
        if (isImmutable()) throw new IllegalStateException(__IMMUTABLE);
        _bytes=b;
        clear();
        setGetIndex(off);
        setPutIndex(off+len);
    }

    /* ------------------------------------------------------------ */
    /** Wrap a byte array
     * @param b
     */
    public void wrap(byte[] b)
    {
        if (isReadOnly()) throw new IllegalStateException(__READONLY);
        if (isImmutable()) throw new IllegalStateException(__IMMUTABLE);
        _bytes=b;
        setGetIndex(0);
        setPutIndex(b.length);
    }

    /* ------------------------------------------------------------ */
    public void writeTo(OutputStream out)
        throws IOException
    {
        out.write(_bytes,getIndex(),length());
        clear();
    }
    
    /* ------------------------------------------------------------ */
    public int readFrom(InputStream in,int max) throws IOException
    {
        if (max<0||max>space())
            max=space();
        int p = putIndex();
        
        int len=0, total=0, available=max;
        while (total<max) 
        {
            len=in.read(_bytes,p,available);
            if (len<0)
                break;
            else if (len>0)
            {
                p += len;
                total += len;
                available -= len;
                setPutIndex(p);
            }
            if (in.available()<=0)
                break;
        }
        if (len<0 && total==0)
            return -1;
        return total;
    }

    /* ------------------------------------------------------------ */
    public int space()
    {
        return _bytes.length - _put;
    }

    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public static class CaseInsensitive extends ByteArrayBuffer implements Buffer.CaseInsensitve
    {
        public CaseInsensitive(String s)
        {
            super(s);
        }

        public CaseInsensitive(byte[] b, int o, int l, int rw)
        {
            super(b,o,l,rw);
        }

        public boolean equals(Object obj)
        {
            return equalsIgnoreCase((Buffer)obj);
        }
        
    }
}
