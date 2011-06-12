//========================================================================
//$Id: HttpGenerator.java,v 1.7 2005/11/25 21:17:12 gregwilkins Exp $
//Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.io.Buffer;
import org.mortbay.io.Buffers;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.EndPoint;
import org.mortbay.io.View;
import org.mortbay.log.Log;
import org.mortbay.util.ByteArrayOutputStream2;
import org.mortbay.util.StringUtil;
import org.mortbay.util.TypeUtil;

/* ------------------------------------------------------------ */
/**
 * Abstract Generator. Builds HTTP Messages.
 * 
 * Currently this class uses a system parameter "jetty.direct.writers" to control
 * two optional writer to byte conversions. buffer.writers=true will probably be 
 * faster, but will consume more memory.   This option is just for testing and tuning.
 * 
 * @author gregw
 * 
 */
public abstract class AbstractGenerator implements Generator
{
    // states
    public final static int STATE_HEADER = 0;
    public final static int STATE_CONTENT = 2;
    public final static int STATE_FLUSHING = 3;
    public final static int STATE_END = 4;
    
    private static final byte[] NO_BYTES = {};
    private static int MAX_OUTPUT_CHARS = 512; 

    private static Buffer[] __reasons = new Buffer[505];
    static
    {
        Field[] fields = HttpServletResponse.class.getDeclaredFields();
        for (int i=0;i<fields.length;i++)
        {
            if ((fields[i].getModifiers()&Modifier.STATIC)!=0 &&
                 fields[i].getName().startsWith("SC_"))
            {
                try
                {
                    int code = fields[i].getInt(null);
                    if (code<__reasons.length)
                        __reasons[code]=new ByteArrayBuffer(fields[i].getName().substring(3));
                }
                catch(IllegalAccessException e)
                {}
            }    
        }
    }
    
    protected static Buffer getReasonBuffer(int code)
    {
        Buffer reason=(code<__reasons.length)?__reasons[code]:null;
        return reason==null?null:reason;
    }
    
    public static String getReason(int code)
    {
        Buffer reason=(code<__reasons.length)?__reasons[code]:null;
        return reason==null?TypeUtil.toString(code):reason.toString();
    }

    // data
    protected int _state = STATE_HEADER;
    
    protected int _status = 0;
    protected int _version = HttpVersions.HTTP_1_1_ORDINAL;
    protected  Buffer _reason;
    protected  Buffer _method;
    protected  String _uri;

    protected long _contentWritten = 0;
    protected long _contentLength = HttpTokens.UNKNOWN_CONTENT;
    protected boolean _last = false;
    protected boolean _head = false;
    protected boolean _noContent = false;
    protected boolean _close = false;

    protected Buffers _buffers; // source of buffers
    protected EndPoint _endp;

    protected int _headerBufferSize;
    protected int _contentBufferSize;
    
    protected Buffer _header; // Buffer for HTTP header (and maybe small _content)
    protected Buffer _buffer; // Buffer for copy of passed _content
    protected Buffer _content; // Buffer passed to addContent
    
    private boolean _sendServerVersion;

    
    /* ------------------------------------------------------------------------------- */
    /**
     * Constructor.
     * 
     * @param buffers buffer pool
     * @param headerBufferSize Size of the buffer to allocate for HTTP header
     * @param contentBufferSize Size of the buffer to allocate for HTTP content
     */
    public AbstractGenerator(Buffers buffers, EndPoint io, int headerBufferSize, int contentBufferSize)
    {
        this._buffers = buffers;
        this._endp = io;
        _headerBufferSize=headerBufferSize;
        _contentBufferSize=contentBufferSize;
    }

    /* ------------------------------------------------------------------------------- */
    public void reset(boolean returnBuffers)
    {
        _state = STATE_HEADER;
        _status = 0;
        _version = HttpVersions.HTTP_1_1_ORDINAL;
        _reason = null;
        _last = false;
        _head = false;
        _noContent=false;
        _close = false;
        _contentWritten = 0;
        _contentLength = HttpTokens.UNKNOWN_CONTENT;

        synchronized(this)
        {
            if (returnBuffers)
            {
                if (_header != null) 
                    _buffers.returnBuffer(_header);
                _header = null;
                if (_buffer != null) 
                    _buffers.returnBuffer(_buffer);
                _buffer = null;
            }
            else
            {
                if (_header != null) 
                    _header.clear();

                if (_buffer != null)
                {
                    _buffers.returnBuffer(_buffer);
                    _buffer = null;
                }
            }
        }
        _content = null;
        _method=null;
    }

    /* ------------------------------------------------------------------------------- */
    public void resetBuffer()
    {                   
        if(_state>=STATE_FLUSHING)
            throw new IllegalStateException("Flushed");
        
        _last = false;
        _close = false;
        _contentWritten = 0;
        _contentLength = HttpTokens.UNKNOWN_CONTENT;
        _content=null;
        if (_buffer!=null)
            _buffer.clear();  
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the contentBufferSize.
     */
    public int getContentBufferSize()
    {
        return _contentBufferSize;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param contentBufferSize The contentBufferSize to set.
     */
    public void increaseContentBufferSize(int contentBufferSize)
    {
        if (contentBufferSize > _contentBufferSize)
        {
            _contentBufferSize = contentBufferSize;
            if (_buffer != null)
            {
                Buffer nb = _buffers.getBuffer(_contentBufferSize);
                nb.put(_buffer);
                _buffers.returnBuffer(_buffer);
                _buffer = nb;
            }
        }
    }
    
    /* ------------------------------------------------------------ */    
    public Buffer getUncheckedBuffer()
    {
        return _buffer;
    }
    
    /* ------------------------------------------------------------ */    
    public boolean getSendServerVersion ()
    {
        return _sendServerVersion;
    }
    
    /* ------------------------------------------------------------ */    
    public void setSendServerVersion (boolean sendServerVersion)
    {
        _sendServerVersion = sendServerVersion;
    }
    
    /* ------------------------------------------------------------ */
    public int getState()
    {
        return _state;
    }

    /* ------------------------------------------------------------ */
    public boolean isState(int state)
    {
        return _state == state;
    }

    /* ------------------------------------------------------------ */
    public boolean isComplete()
    {
        return _state == STATE_END;
    }

    /* ------------------------------------------------------------ */
    public boolean isIdle()
    {
        return _state == STATE_HEADER && _method==null && _status==0;
    }

    /* ------------------------------------------------------------ */
    public boolean isCommitted()
    {
        return _state != STATE_HEADER;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the head.
     */
    public boolean isHead()
    {
        return _head;
    }

    /* ------------------------------------------------------------ */
    public void setContentLength(long value)
    {
        if (value<0)
            _contentLength=HttpTokens.UNKNOWN_CONTENT;
        else
            _contentLength=value;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param head The head to set.
     */
    public void setHead(boolean head)
    {
        _head = head;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return <code>false</code> if the connection should be closed after a request has been read,
     * <code>true</code> if it should be used for additional requests.
     */
    public boolean isPersistent()
    {
        return !_close;
    }

    /* ------------------------------------------------------------ */
    public void setPersistent(boolean persistent)
    {
        _close=!persistent;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param version The version of the client the response is being sent to (NB. Not the version
     *            in the response, which is the version of the server).
     */
    public void setVersion(int version)
    {
        if (_state != STATE_HEADER) throw new IllegalStateException("STATE!=START");
        _version = version;
        if (_version==HttpVersions.HTTP_0_9_ORDINAL && _method!=null)
            _noContent=true;
    }

    /* ------------------------------------------------------------ */
    public int getVersion()
    {
        return _version;
    }
    
    /* ------------------------------------------------------------ */
    /**
     */
    public void setRequest(String method, String uri)
    {
        if (method==null || HttpMethods.GET.equals(method) )
            _method=HttpMethods.GET_BUFFER;
        else
            _method=HttpMethods.CACHE.lookup(method);
        _uri=uri;
        if (_version==HttpVersions.HTTP_0_9_ORDINAL)
            _noContent=true;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param status The status code to send.
     * @param reason the status message to send.
     */
    public void setResponse(int status, String reason)
    {
        if (_state != STATE_HEADER) throw new IllegalStateException("STATE!=START");

        _status = status;
        if (reason!=null)
        {
            int len=reason.length();
            if (len>_headerBufferSize/2)
                len=_headerBufferSize/2;
            _reason=new ByteArrayBuffer(len);
            for (int i=0;i<len;i++)
            {
                char ch = reason.charAt(i);
                if (ch!='\r'&&ch!='\n')
                    _reason.put((byte)ch);
                else
                    _reason.put((byte)' ');
            }
        }
    }

    /* ------------------------------------------------------------ */
    /** Prepare buffer for unchecked writes.
     * Prepare the generator buffer to receive unchecked writes
     * @return the available space in the buffer.
     * @throws IOException
     */
    protected abstract int prepareUncheckedAddContent() throws IOException;

    /* ------------------------------------------------------------ */
    void uncheckedAddContent(int b)
    {
        _buffer.put((byte)b);
    }

    /* ------------------------------------------------------------ */
    void completeUncheckedAddContent()
    {
        if (_noContent)
        {
            if(_buffer!=null)
                _buffer.clear();
            return;
        }
        else 
        {
            _contentWritten+=_buffer.length();
            if (_head)
                _buffer.clear();
        }
    }
    
    /* ------------------------------------------------------------ */
    public boolean isBufferFull()
    {
        if (_buffer != null && _buffer.space()==0)
        {
            if (_buffer.length()==0 && !_buffer.isImmutable())
                _buffer.compact();
            return _buffer.space()==0;
        }

        return _content!=null && _content.length()>0;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isContentWritten()
    {
        return _contentLength>=0 && _contentWritten>=_contentLength;
    }
    
    /* ------------------------------------------------------------ */
    public abstract void completeHeader(HttpFields fields, boolean allContentAdded) throws IOException;
    
    /* ------------------------------------------------------------ */
    /**
     * Complete the message.
     * 
     * @throws IOException
     */
    public void complete() throws IOException
    {
        if (_state == STATE_HEADER)
        {
            throw new IllegalStateException("State==HEADER");
        }

        if (_contentLength >= 0 && _contentLength != _contentWritten && !_head)
        {
            if (Log.isDebugEnabled())
                Log.debug("ContentLength written=="+_contentWritten+" != contentLength=="+_contentLength);
            _close = true;
        }
    }

    /* ------------------------------------------------------------ */
    public abstract long flush() throws IOException;
    

    /* ------------------------------------------------------------ */
    /**
     * Utility method to send an error response. If the builder is not committed, this call is
     * equivalent to a setResponse, addcontent and complete call.
     * 
     * @param code
     * @param reason
     * @param content
     * @param close
     * @throws IOException
     */
    public void sendError(int code, String reason, String content, boolean close) throws IOException
    {
        if (close)
            _close = close;
        if (!isCommitted())
        {
            setResponse(code, reason);
            completeHeader(null, false);
            if (content != null) 
                addContent(new View(new ByteArrayBuffer(content)), Generator.LAST);
            complete();
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the contentWritten.
     */
    public long getContentWritten()
    {
        return _contentWritten;
    }


    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** Output.
     * 
     * <p>
     * Implements  {@link javax.servlet.ServletOutputStream} from the {@link javax.servlet} package.   
     * </p>
     * A {@link ServletOutputStream} implementation that writes content
     * to a {@link AbstractGenerator}.   The class is designed to be reused
     * and can be reopened after a close.
     */
    public static class Output extends ServletOutputStream 
    {
        protected AbstractGenerator _generator;
        protected long _maxIdleTime;
        protected ByteArrayBuffer _buf = new ByteArrayBuffer(NO_BYTES);
        protected boolean _closed;
        
        // These are held here for reuse by Writer
        String _characterEncoding;
        Writer _converter;
        char[] _chars;
        ByteArrayOutputStream2 _bytes;
        

        /* ------------------------------------------------------------ */
        public Output(AbstractGenerator generator, long maxIdleTime)
        {
            _generator=generator;
            _maxIdleTime=maxIdleTime;
        }
        
        /* ------------------------------------------------------------ */
        /*
         * @see java.io.OutputStream#close()
         */
        public void close() throws IOException
        {
            _closed=true;
        }

        /* ------------------------------------------------------------ */
        void  blockForOutput() throws IOException
        {
            if (_generator._endp.isBlocking())
            {
                try
                {
                    flush();
                }
                catch(IOException e)
                {
                    _generator._endp.close();
                    throw e;
                }
            }
            else
            {
                if (!_generator._endp.blockWritable(_maxIdleTime))
                {
                    _generator._endp.close();
                    throw new EofException("timeout");
                }
                
                _generator.flush();
            }
        }
        
        /* ------------------------------------------------------------ */
        void reopen()
        {
            _closed=false;
        }
        
        /* ------------------------------------------------------------ */
        public void flush() throws IOException
        {
            // block until everything is flushed
            Buffer content = _generator._content;
            Buffer buffer = _generator._buffer;
            if (content!=null && content.length()>0 || buffer!=null && buffer.length()>0 || _generator.isBufferFull())
            {
                _generator.flush();
                
                while ((content!=null && content.length()>0 ||buffer!=null && buffer.length()>0) && _generator._endp.isOpen())
                    blockForOutput();
            }
        }

        /* ------------------------------------------------------------ */
        public void write(byte[] b, int off, int len) throws IOException
        {
            _buf.wrap(b, off, len);
            write(_buf);
            _buf.wrap(NO_BYTES);
        }

        /* ------------------------------------------------------------ */
        /*
         * @see java.io.OutputStream#write(byte[])
         */
        public void write(byte[] b) throws IOException
        {
            _buf.wrap(b);
            write(_buf);
            _buf.wrap(NO_BYTES);
        }

        /* ------------------------------------------------------------ */
        /*
         * @see java.io.OutputStream#write(int)
         */
        public void write(int b) throws IOException
        {
            if (_closed)
                throw new IOException("Closed");
            if (!_generator._endp.isOpen())
                throw new EofException();
            
            // Block until we can add _content.
            while (_generator.isBufferFull())
            {
                blockForOutput();
                if (_closed)
                    throw new IOException("Closed");
                if (!_generator._endp.isOpen())
                    throw new EofException();
            }

            // Add the _content
            if (_generator.addContent((byte)b))
                // Buffers are full so flush.
                flush();
           
            if (_generator.isContentWritten())
            {
                flush();
                close();
            }
        }

        /* ------------------------------------------------------------ */
        private void write(Buffer buffer) throws IOException
        {
            if (_closed)
                throw new IOException("Closed");
            if (!_generator._endp.isOpen())
                throw new EofException();
            
            // Block until we can add _content.
            while (_generator.isBufferFull())
            {
                blockForOutput();
                if (_closed)
                    throw new IOException("Closed");
                if (!_generator._endp.isOpen())
                    throw new EofException();
            }

            // Add the _content
            _generator.addContent(buffer, Generator.MORE);

            // Have to flush and complete headers?
            if (_generator.isBufferFull())
                flush();
            
            if (_generator.isContentWritten())
            {
                flush();
                close();
            }

            // Block until our buffer is free
            while (buffer.length() > 0 && _generator._endp.isOpen())
                blockForOutput();
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletOutputStream#print(java.lang.String)
         */
        public void print(String s) throws IOException
        {
            write(s.getBytes());
        }
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** OutputWriter.
     * A writer that can wrap a {@link Output} stream and provide
     * character encodings.
     *
     * The UTF-8 encoding is done by this class and no additional 
     * buffers or Writers are used.
     * The UTF-8 code was inspired by http://javolution.org
     */
    public static class OutputWriter extends Writer
    {
        private static final int WRITE_CONV = 0;
        private static final int WRITE_ISO1 = 1;
        private static final int WRITE_UTF8 = 2;
        
        Output _out;
        AbstractGenerator _generator;
        int _writeMode;
        int _surrogate;

        /* ------------------------------------------------------------ */
        public OutputWriter(Output out)
        {
            _out=out;
            _generator=_out._generator;
             
        }

        /* ------------------------------------------------------------ */
        public void setCharacterEncoding(String encoding)
        {
            if (encoding == null || StringUtil.__ISO_8859_1.equalsIgnoreCase(encoding))
            {
                _writeMode = WRITE_ISO1;
            }
            else if (StringUtil.__UTF8.equalsIgnoreCase(encoding))
            {
                _writeMode = WRITE_UTF8;
            }
            else
            {
                _writeMode = WRITE_CONV;
                if (_out._characterEncoding == null || !_out._characterEncoding.equalsIgnoreCase(encoding))
                    _out._converter = null; // Set lazily in getConverter()
            }
            
            _out._characterEncoding = encoding;
            if (_out._bytes==null)
                _out._bytes = new ByteArrayOutputStream2(MAX_OUTPUT_CHARS);
        }

        /* ------------------------------------------------------------ */
        public void close() throws IOException
        {
            _out.close();
        }

        /* ------------------------------------------------------------ */
        public void flush() throws IOException
        {
            _out.flush();
        }

        /* ------------------------------------------------------------ */
        public void write (String s,int offset, int length) throws IOException
        {   
            while (length > MAX_OUTPUT_CHARS)
            {
                write(s, offset, MAX_OUTPUT_CHARS);
                offset += MAX_OUTPUT_CHARS;
                length -= MAX_OUTPUT_CHARS;
            }

            if (_out._chars==null)
            {
                _out._chars = new char[MAX_OUTPUT_CHARS]; 
            }
            char[] chars = _out._chars;
            s.getChars(offset, offset + length, chars, 0);
            write(chars, 0, length);
        }

        /* ------------------------------------------------------------ */
        public void write (char[] s,int offset, int length) throws IOException
        {              
            Output out = _out; 
            
            while (length > 0)
            {  
                out._bytes.reset();
                int chars = length>MAX_OUTPUT_CHARS?MAX_OUTPUT_CHARS:length;

                switch (_writeMode)
                {
                    case WRITE_CONV:
                    {
                        Writer converter=getConverter();
                        converter.write(s, offset, chars);
                        converter.flush();
                    }
                    break;

                    case WRITE_ISO1:
                    {
                        byte[] buffer=out._bytes.getBuf();
                        int bytes=out._bytes.getCount();
                        
                        if (chars>buffer.length-bytes)
                            chars=buffer.length-bytes;

                        for (int i = 0; i < chars; i++)
                        {
                            int c = s[offset+i];
                            buffer[bytes++]=(byte)(c<256?c:'?'); // ISO-1 and UTF-8 match for 0 - 255
                        }
                        if (bytes>=0)
                            out._bytes.setCount(bytes);

                        break;
                    }

                    case WRITE_UTF8:
                    {
                        byte[] buffer=out._bytes.getBuf();
                        int bytes=out._bytes.getCount();
         
                        if (bytes+chars>buffer.length)
                            chars=buffer.length-bytes;
                        
                        for (int i = 0; i < chars; i++)
                        {
                            int code = s[offset+i];

                            if ((code & 0xffffff80) == 0) 
                            {
                                // 1b
                                if (bytes+1>buffer.length)
                                {
                                    chars=i;
                                    break;
                                }
                                buffer[bytes++]=(byte)(code);
                            }
                            else
			    {
				if((code&0xfffff800)==0)
				{
				    // 2b
				    if (bytes+2>buffer.length)
				    {
					chars=i;
					break;
				    }
				    buffer[bytes++]=(byte)(0xc0|(code>>6));
				    buffer[bytes++]=(byte)(0x80|(code&0x3f));
				}
				else if((code&0xffff0000)==0)
				{
				    // 3b
				    if (bytes+3>buffer.length)
				    {
					chars=i;
					break;
				    }
				    buffer[bytes++]=(byte)(0xe0|(code>>12));
				    buffer[bytes++]=(byte)(0x80|((code>>6)&0x3f));
				    buffer[bytes++]=(byte)(0x80|(code&0x3f));
				}
				else if((code&0xff200000)==0)
				{
				    // 4b
				    if (bytes+4>buffer.length)
				    {
					chars=i;
					break;
				    }
				    buffer[bytes++]=(byte)(0xf0|(code>>18));
				    buffer[bytes++]=(byte)(0x80|((code>>12)&0x3f));
				    buffer[bytes++]=(byte)(0x80|((code>>6)&0x3f));
				    buffer[bytes++]=(byte)(0x80|(code&0x3f));
				}
				else if((code&0xf4000000)==0)
				{
				    // 5b
				    if (bytes+5>buffer.length)
				    {
					chars=i;
					break;
				    }
				    buffer[bytes++]=(byte)(0xf8|(code>>24));
				    buffer[bytes++]=(byte)(0x80|((code>>18)&0x3f));
				    buffer[bytes++]=(byte)(0x80|((code>>12)&0x3f));
				    buffer[bytes++]=(byte)(0x80|((code>>6)&0x3f));
				    buffer[bytes++]=(byte)(0x80|(code&0x3f));
				}
				else if((code&0x80000000)==0)
				{
				    // 6b
				    if (bytes+6>buffer.length)
				    {
					chars=i;
					break;
				    }
				    buffer[bytes++]=(byte)(0xfc|(code>>30));
				    buffer[bytes++]=(byte)(0x80|((code>>24)&0x3f));
				    buffer[bytes++]=(byte)(0x80|((code>>18)&0x3f));
				    buffer[bytes++]=(byte)(0x80|((code>>12)&0x3f));
				    buffer[bytes++]=(byte)(0x80|((code>>6)&0x3f));
				    buffer[bytes++]=(byte)(0x80|(code&0x3f));
				}
				else
				{
				    buffer[bytes++]=(byte)('?');
				}
				if (bytes==buffer.length)
				{
				    chars=i+1;
				    break;
				}
			    }
                        }
                        out._bytes.setCount(bytes);
                        break;
                    }
                    default:
                        throw new IllegalStateException();
                }
                
                out._bytes.writeTo(out);
                length-=chars;
                offset+=chars;
            }
        }

        /* ------------------------------------------------------------ */
        private Writer getConverter() throws IOException
        {
            if (_out._converter == null)
                _out._converter = new OutputStreamWriter(_out._bytes, _out._characterEncoding);
            return _out._converter;
        }   
    }
}
