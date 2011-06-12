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
import java.util.Iterator;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.io.Buffers;
import org.mortbay.io.EndPoint;
import org.mortbay.io.Portable;
import org.mortbay.io.BufferCache.CachedBuffer;
import org.mortbay.log.Log;
import org.mortbay.util.QuotedStringTokenizer;

/* ------------------------------------------------------------ */
/**
 * HttpGenerator. Builds HTTP Messages.
 * 
 * @author gregw
 * 
 */
public class HttpGenerator extends AbstractGenerator
{
    // common _content
    private static byte[] LAST_CHUNK =
    { (byte) '0', (byte) '\015', (byte) '\012', (byte) '\015', (byte) '\012'};
    private static byte[] CONTENT_LENGTH_0 = Portable.getBytes("Content-Length: 0\015\012");
    private static byte[] CONNECTION_KEEP_ALIVE = Portable.getBytes("Connection: keep-alive\015\012");
    private static byte[] CONNECTION_CLOSE = Portable.getBytes("Connection: close\015\012");
    private static byte[] CONNECTION_ = Portable.getBytes("Connection: ");
    private static byte[] CRLF = Portable.getBytes("\015\012");
    private static byte[] TRANSFER_ENCODING_CHUNKED = Portable.getBytes("Transfer-Encoding: chunked\015\012");
    private static byte[] SERVER = Portable.getBytes("Server: Jetty(6.0.x)\015\012");

    // other statics
    private static int CHUNK_SPACE = 12;
    
    public static void setServerVersion(String version)
    {
        SERVER=Portable.getBytes("Server: Jetty("+version+")\015\012");
    }

    // data
    private boolean _bypass = false; // True if _content buffer can be written directly to endp and bypass the content buffer
    private boolean _needCRLF = false;
    private boolean _needEOC = false;
    private boolean _bufferChunked = false;

    
    /* ------------------------------------------------------------------------------- */
    /**
     * Constructor.
     * 
     * @param buffers buffer pool
     * @param headerBufferSize Size of the buffer to allocate for HTTP header
     * @param contentBufferSize Size of the buffer to allocate for HTTP content
     */
    public HttpGenerator(Buffers buffers, EndPoint io, int headerBufferSize, int contentBufferSize)
    {
        super(buffers,io,headerBufferSize,contentBufferSize);
    }

    /* ------------------------------------------------------------------------------- */
    public void reset(boolean returnBuffers)
    {
        super.reset(returnBuffers);
        _bypass = false;
        _needCRLF = false;
        _needEOC = false;
        _bufferChunked=false;
        _method=null;
        _uri=null;
        _noContent=false;
    }



    /* ------------------------------------------------------------ */
    /**
     * Add content.
     * 
     * @param content
     * @param last
     * @throws IllegalArgumentException if <code>content</code> is {@link Buffer#isImmutable immutable}.
     * @throws IllegalStateException If the request is not expecting any more content,
     *   or if the buffers are full and cannot be flushed.
     * @throws IOException if there is a problem flushing the buffers.
     */
    public void addContent(Buffer content, boolean last) throws IOException
    {
        if (_noContent)
            throw new IllegalStateException("NO CONTENT");

        if (_last || _state==STATE_END) 
        {
            Log.debug("Ignoring extra content {}",content);
            content.clear();
            return;
        }
        _last = last;

        // Handle any unfinished business?
        if (_content!=null && _content.length()>0 || _bufferChunked)
        {
            if (!_endp.isOpen())
                throw new EofException();
            flush();
            if (_content != null && _content.length()>0 || _bufferChunked) 
                throw new IllegalStateException("FULL");
        }

        _content = content;
        _contentWritten += content.length();

        // Handle the _content
        if (_head)
        {
            content.clear();
            _content=null;
        }
        else if (_endp != null && _buffer == null && content.length() > 0 && _last)
        {
            // TODO - use bypass in more cases.
            // Make _content a direct buffer
            _bypass = true;
        }
        else
        {
            // Yes - so we better check we have a buffer
            if (_buffer == null) 
                _buffer = _buffers.getBuffer(_contentBufferSize);

            // Copy _content to buffer;
            int len=_buffer.put(_content);
            _content.skip(len);
            if (_content.length() == 0) 
                _content = null;
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * send complete response.
     * 
     * @param response
     */
    public void sendResponse(Buffer response) throws IOException
    {
        if (_noContent || _state!=STATE_HEADER || _content!=null && _content.length()>0 || _bufferChunked || _head )
            throw new IllegalStateException();

        _last = true;

        _content = response;
        _bypass = true;
        _state = STATE_FLUSHING;

        // TODO this is not exactly right, but should do.
        _contentLength =_contentWritten = response.length();
        
    }

    /* ------------------------------------------------------------ */
    /**
     * Add content.
     * 
     * @param b byte
     * @return true if the buffers are full
     * @throws IOException
     */
    public boolean addContent(byte b) throws IOException
    {
        if (_noContent)
            throw new IllegalStateException("NO CONTENT");
        
        if (_last || _state==STATE_END) 
        {
            Log.debug("Ignoring extra content {}",new Byte(b));
            return false;
        }

        // Handle any unfinished business?
        if (_content != null && _content.length()>0 || _bufferChunked)
        {
            flush();
            if (_content != null && _content.length()>0 || _bufferChunked) 
                throw new IllegalStateException("FULL");
        }

        _contentWritten++;
        
        // Handle the _content
        if (_head)
            return false;
        
        // we better check we have a buffer
        if (_buffer == null) 
            _buffer = _buffers.getBuffer(_contentBufferSize);
        
        // Copy _content to buffer;
        _buffer.put(b);
        
        return _buffer.space()<=(_contentLength == HttpTokens.CHUNKED_CONTENT?CHUNK_SPACE:0);
    }

    /* ------------------------------------------------------------ */
    /** Prepare buffer for unchecked writes.
     * Prepare the generator buffer to receive unchecked writes
     * @return the available space in the buffer.
     * @throws IOException
     */
    protected int prepareUncheckedAddContent() throws IOException
    {
        if (_noContent)
            return -1;
        
        if (_last || _state==STATE_END) 
            return -1;

        // Handle any unfinished business?
        Buffer content = _content;
        if (content != null && content.length()>0 || _bufferChunked)
        {
            flush();
            if (content != null && content.length()>0 || _bufferChunked) 
                throw new IllegalStateException("FULL");
        }

        // we better check we have a buffer
        if (_buffer == null) 
            _buffer = _buffers.getBuffer(_contentBufferSize);

        _contentWritten-=_buffer.length();
        
        // Handle the _content
        if (_head)
            return Integer.MAX_VALUE;
        
        return _buffer.space()-(_contentLength == HttpTokens.CHUNKED_CONTENT?CHUNK_SPACE:0);
    }
    
    /* ------------------------------------------------------------ */
    public boolean isBufferFull()
    {
        // Should we flush the buffers?
        boolean full = super.isBufferFull() || _bufferChunked || _bypass  || (_contentLength == HttpTokens.CHUNKED_CONTENT && _buffer != null && _buffer.space() < CHUNK_SPACE);
        return full;
    }
    
    /* ------------------------------------------------------------ */
    public void completeHeader(HttpFields fields, boolean allContentAdded) throws IOException
    {
        if (_state != STATE_HEADER) 
            return;
        
        // handle a reset 
        if (_method==null && _status==0)
            throw new EofException();

        if (_last && !allContentAdded) 
            throw new IllegalStateException("last?");
        _last = _last | allContentAdded;

        // get a header buffer
        if (_header == null) 
            _header = _buffers.getBuffer(_headerBufferSize);
        
        boolean has_server = false;
        
        if (_method!=null)
        {
            _close = false;
            // Request
            if (_version == HttpVersions.HTTP_0_9_ORDINAL)
            {
                _contentLength = HttpTokens.NO_CONTENT;
                _header.put(_method);
                _header.put((byte)' ');
                _header.put(_uri.getBytes("utf-8")); // TODO WRONG!
                _header.put(HttpTokens.CRLF);
                _state = STATE_FLUSHING;
                _noContent=true;
                return;
            }
            else
            {
                _header.put(_method);
                _header.put((byte)' ');
                _header.put(_uri.getBytes("utf-8")); // TODO WRONG!
                _header.put((byte)' ');
                _header.put(_version==HttpVersions.HTTP_1_0_ORDINAL?HttpVersions.HTTP_1_0_BUFFER:HttpVersions.HTTP_1_1_BUFFER);
                _header.put(HttpTokens.CRLF);
            }
        }
        else
        {
            // Response
            if (_version == HttpVersions.HTTP_0_9_ORDINAL)
            {
                _close = true;
                _contentLength = HttpTokens.EOF_CONTENT;
                _state = STATE_CONTENT;
                return;
            }
            else
            {
                if (_version == HttpVersions.HTTP_1_0_ORDINAL) 
                    _close = true;

                // add response line
                Buffer line = HttpStatus.getResponseLine(_status);

                
                if (line==null)
                {
                    if (_reason==null)
                        _reason=getReasonBuffer(_status);

                    _header.put(HttpVersions.HTTP_1_1_BUFFER);
                    _header.put((byte) ' ');
                    _header.put((byte) ('0' + _status / 100));
                    _header.put((byte) ('0' + (_status % 100) / 10));
                    _header.put((byte) ('0' + (_status % 10)));
                    _header.put((byte) ' ');
                    if (_reason==null)
                    {
                        _header.put((byte) ('0' + _status / 100));
                        _header.put((byte) ('0' + (_status % 100) / 10));
                        _header.put((byte) ('0' + (_status % 10)));
                    }
                    else
                        _header.put(_reason);
                    _header.put(HttpTokens.CRLF);
                }
                else
                {
                    if (_reason==null)
                        _header.put(line);
                    else
                    {
                        _header.put(line.array(), 0, HttpVersions.HTTP_1_1_BUFFER.length() + 5);
                        _header.put(_reason);
                        _header.put(HttpTokens.CRLF);
                    }
                }

                if (_status<200 && _status>=100 )
                {
                    _noContent=true;
                    _content=null;
                    if (_buffer!=null)
                        _buffer.clear();
                    // end the header.
                    _header.put(HttpTokens.CRLF);
                    _state = STATE_CONTENT;
                    return;
                }

                if (_status==204 || _status==304)
                {
                    _noContent=true;
                    _content=null;
                    if (_buffer!=null)
                        _buffer.clear();
                }
            }
        }
        
        // Add headers

        // key field values
        HttpFields.Field content_length = null;
        HttpFields.Field transfer_encoding = null;
        boolean keep_alive = false;
        boolean close=false;
        boolean content_type =false;
        StringBuffer connection = null;

        if (fields != null)
        {
            Iterator iter = fields.getFields();

            while (iter.hasNext())
            {
                HttpFields.Field field = (HttpFields.Field) iter.next();

                switch (field.getNameOrdinal())
                {
                    case HttpHeaders.CONTENT_LENGTH_ORDINAL:
                        content_length = field;
                        _contentLength = field.getLongValue();

                        if (_contentLength < _contentWritten || _last && _contentLength != _contentWritten)
                            content_length = null;

                        // write the field to the header buffer
                        field.put(_header);
                        break;

                    case HttpHeaders.CONTENT_TYPE_ORDINAL:
                        if (BufferUtil.isPrefix(MimeTypes.MULTIPART_BYTERANGES_BUFFER, field.getValueBuffer())) 
                            _contentLength = HttpTokens.SELF_DEFINING_CONTENT;
                        content_type=true;
                        // write the field to the header buffer
                        field.put(_header);
                        break;

                    case HttpHeaders.TRANSFER_ENCODING_ORDINAL:
                        if (_version == HttpVersions.HTTP_1_1_ORDINAL) transfer_encoding = field;
                        // Do NOT add yet!
                        break;

                    case HttpHeaders.CONNECTION_ORDINAL:
                        if (_method!=null)
                            field.put(_header);
                        
                        int connection_value = field.getValueOrdinal();
                        switch (connection_value)
                        {
                            case -1:
                            { 

                                QuotedStringTokenizer tok = new QuotedStringTokenizer(field.getValue(), ",");
                                while(tok.hasMoreTokens())
                                {
                                	String token=tok.nextToken().trim();
                                    CachedBuffer cb = HttpHeaderValues.CACHE.get(token);
                                    if (cb!=null)
                                    {
                                        switch(cb.getOrdinal())
                                        {
                                            case HttpHeaderValues.CLOSE_ORDINAL:
                                                close=true;
                                                if (_method==null)
                                                    _close=true;
                                                keep_alive=false;
                                                if (_close && _method==null && _contentLength == HttpTokens.UNKNOWN_CONTENT) 
                                                    _contentLength = HttpTokens.EOF_CONTENT;
                                                break;

                                            case HttpHeaderValues.KEEP_ALIVE_ORDINAL:
                                                if (_version == HttpVersions.HTTP_1_0_ORDINAL)
                                                {
                                                    keep_alive = true;
                                                    if (_method==null) 
                                                        _close = false;
                                                }
                                                break;
                                            
                                            default:
                                                if (connection==null)
                                                    connection=new StringBuffer();
                                                else
                                                    connection.append(',');
                                                connection.append(token);
                                        }
                                    }
                                    else
                                    {
                                        if (connection==null)
                                            connection=new StringBuffer();
                                        else
                                            connection.append(',');
                                        connection.append(token);
                                    }
                                }
                                
                                break;
                            }
                            case HttpHeaderValues.CLOSE_ORDINAL:
                            {
                                close=true;
                                if (_method==null)
                                    _close=true;
                                if (_close && _method==null && _contentLength == HttpTokens.UNKNOWN_CONTENT) 
                                    _contentLength = HttpTokens.EOF_CONTENT;
                                break;
                            }
                            case HttpHeaderValues.KEEP_ALIVE_ORDINAL:
                            {
                                if (_version == HttpVersions.HTTP_1_0_ORDINAL)
                                {
                                    keep_alive = true;
                                    if (_method==null) 
                                        _close = false;
                                }
                                break;
                            }
                            default:
                            {
                                if (connection==null)
                                    connection=new StringBuffer();
                                else
                                    connection.append(',');
                                connection.append(field.getValue());
                            }
                        }

                        // Do NOT add yet!
                        break;

                    case HttpHeaders.SERVER_ORDINAL:
                        if (getSendServerVersion()) 
                        {
                            has_server=true;
                            field.put(_header);
                        }
                        break;

                    default:
                        // write the field to the header buffer
                        field.put(_header);
                }
            }
        }

        // Calculate how to end _content and connection, _content length and transfer encoding
        // settings.
        // From RFC 2616 4.4:
        // 1. No body for 1xx, 204, 304 & HEAD response
        // 2. Force _content-length?
        // 3. If Transfer-Encoding!=identity && HTTP/1.1 && !HttpConnection==close then chunk
        // 4. Content-Length
        // 5. multipart/byteranges
        // 6. close
        switch ((int) _contentLength)
        {
            case HttpTokens.UNKNOWN_CONTENT:
                // It may be that we have no _content, or perhaps _content just has not been
                // written yet?

                // Response known not to have a body
                if (_contentWritten == 0 && _method==null && (_status < 200 || _status == 204 || _status == 304))
                    _contentLength = HttpTokens.NO_CONTENT;
                else if (_last)
                {
                    // we have seen all the _content there is
                    _contentLength = _contentWritten;
                    if (content_length == null && (_method==null || content_type || _contentLength>0))
                    {
                        // known length but not actually set.
                        _header.put(HttpHeaders.CONTENT_LENGTH_BUFFER);
                        _header.put(HttpTokens.COLON);
                        _header.put((byte) ' ');
                        BufferUtil.putDecLong(_header, _contentLength);
                        _header.put(HttpTokens.CRLF);
                    }
                }
                else
                {
                    // No idea, so we must assume that a body is coming
                    _contentLength = (_close || _version < HttpVersions.HTTP_1_1_ORDINAL ) ? HttpTokens.EOF_CONTENT : HttpTokens.CHUNKED_CONTENT;
                    if (_method!=null && _contentLength==HttpTokens.EOF_CONTENT)
                    {
                        _contentLength=HttpTokens.NO_CONTENT;
                        _noContent=true;
                    }
                }
                break;

            case HttpTokens.NO_CONTENT:
                if (content_length == null && _method==null && _status >= 200 && _status != 204 && _status != 304) 
                    _header.put(CONTENT_LENGTH_0);
                break;

            case HttpTokens.EOF_CONTENT:
                _close = _method==null;
                break;

            case HttpTokens.CHUNKED_CONTENT:
                break;

            default:
                // TODO - maybe allow forced chunking by setting te ???
                break;
        }

        // Add transfer_encoding if needed
        if (_contentLength == HttpTokens.CHUNKED_CONTENT)
        {
            // try to use user supplied encoding as it may have other values.
            if (transfer_encoding != null && HttpHeaderValues.CHUNKED_ORDINAL != transfer_encoding.getValueOrdinal())
            {
                String c = transfer_encoding.getValue();
                if (c.endsWith(HttpHeaderValues.CHUNKED))
                    transfer_encoding.put(_header);
                else
                    throw new IllegalArgumentException("BAD TE");
            }
            else
                _header.put(TRANSFER_ENCODING_CHUNKED);
        }

        // Handle connection if need be
        if (_contentLength==HttpTokens.EOF_CONTENT)
        {
            keep_alive=false;
            _close=true;
        }
               
        if (_method==null)
        {
            if (_close && (close || _version > HttpVersions.HTTP_1_0_ORDINAL))
            {
                _header.put(CONNECTION_CLOSE);
                if (connection!=null)
                {
                    _header.setPutIndex(_header.putIndex()-2);
                    _header.put((byte)',');
                    _header.put(connection.toString().getBytes());
                    _header.put(CRLF);
                }
            }
            else if (keep_alive)
            {
                _header.put(CONNECTION_KEEP_ALIVE);
                if (connection!=null)
                {
                    _header.setPutIndex(_header.putIndex()-2);
                    _header.put((byte)',');
                    _header.put(connection.toString().getBytes());
                    _header.put(CRLF);
                }
            }
            else if (connection!=null)
            {
                _header.put(CONNECTION_);
                _header.put(connection.toString().getBytes());
                _header.put(CRLF);
            }
        }
        
        if (!has_server && _status>100 && getSendServerVersion())
            _header.put(SERVER);

        // end the header.
        _header.put(HttpTokens.CRLF);

        _state = STATE_CONTENT;

    }

    /* ------------------------------------------------------------ */
    /**
     * Complete the message.
     * 
     * @throws IOException
     */
    public void complete() throws IOException
    {
        if (_state == STATE_END) 
            return;
        
        super.complete();
        
        if (_state < STATE_FLUSHING)
        {
            _state = STATE_FLUSHING;
            if (_contentLength == HttpTokens.CHUNKED_CONTENT) 
                _needEOC = true;
        }
        
        flush();
    }

    /* ------------------------------------------------------------ */
    public long flush() throws IOException
    {
        try
        {   
            if (_state == STATE_HEADER) 
                throw new IllegalStateException("State==HEADER");
            
            prepareBuffers();
            
            if (_endp == null)
            {
                if (_needCRLF && _buffer!=null) 
                    _buffer.put(HttpTokens.CRLF);
                if (_needEOC && _buffer!=null && !_head) 
                    _buffer.put(LAST_CHUNK);
                _needCRLF=false;
                _needEOC=false;
                return 0;
            }
            
            // Keep flushing while there is something to flush (except break below)
            int total= 0;
            long last_len = -1;
            Flushing: while (true)
            {
                int len = -1;
                int to_flush = ((_header != null && _header.length() > 0)?4:0) | ((_buffer != null && _buffer.length() > 0)?2:0) | ((_bypass && _content != null && _content.length() > 0)?1:0);
                switch (to_flush)
                {
                    case 7:
                        throw new IllegalStateException(); // should never happen!
                    case 6:
                        len = _endp.flush(_header, _buffer, null);
                        break;
                    case 5:
                        len = _endp.flush(_header, _content, null);
                        break;
                    case 4:
                        len = _endp.flush(_header);
                        break;
                    case 3:
                        throw new IllegalStateException(); // should never happen!
                    case 2:
                        len = _endp.flush(_buffer);
                        break;
                    case 1:
                        len = _endp.flush(_content);
                        break;
                    case 0:
                    {
                        // Nothing more we can write now.
                        if (_header != null) 
                            _header.clear();
                        
                        _bypass = false;
                        _bufferChunked = false;
                        
                        if (_buffer != null)
                        {
                            _buffer.clear();
                            if (_contentLength == HttpTokens.CHUNKED_CONTENT)
                            {
                                // reserve some space for the chunk header
                                _buffer.setPutIndex(CHUNK_SPACE);
                                _buffer.setGetIndex(CHUNK_SPACE);
                                
                                // Special case handling for small left over buffer from
                                // an addContent that caused a buffer flush.
                                if (_content != null && _content.length() < _buffer.space() && _state != STATE_FLUSHING)
                                {
                                    _buffer.put(_content);
                                    _content.clear();
                                    _content = null;
                                    break Flushing;
                                }
                            }
                        }
                        
                        // Are we completely finished for now?
                        if (!_needCRLF && !_needEOC && (_content == null || _content.length() == 0))
                        {
                            if (_state == STATE_FLUSHING)
                                _state = STATE_END;
                            if (_state==STATE_END && _close && _status!=100) 
                                _endp.shutdownOutput();
                            
                            break Flushing;
                        }
                        
                        // Try to prepare more to write.
                        prepareBuffers();
                    }
                }
                
                
                // break If we failed to flush
                if (len > 0)
                    total+=len;
                else 
                    break Flushing;
          
                last_len = len;
            }
            
            return total;
        }
        catch (IOException e)
        {
            Log.ignore(e);
            throw (e instanceof EofException) ? e:new EofException(e);
        }
    }

    /* ------------------------------------------------------------ */
    private void prepareBuffers()
    {
        // if we are not flushing an existing chunk
        if (!_bufferChunked)
        {
            // Refill buffer if possible
            if (_content != null && _content.length() > 0 && _buffer != null && _buffer.space() > 0)
            {
                int len = _buffer.put(_content);
                _content.skip(len);
                if (_content.length() == 0) 
                    _content = null;
            }

            // Chunk buffer if need be
            if (_contentLength == HttpTokens.CHUNKED_CONTENT)
            {
                int size = _buffer == null ? 0 : _buffer.length();
                if (size > 0)
                {
                    // Prepare a chunk!
                    _bufferChunked = true;

                    // Did we leave space at the start of the buffer.
                    if (_buffer.getIndex() == CHUNK_SPACE)
                    {
                        // Oh yes, goodie! let's use it then!
                        _buffer.poke(_buffer.getIndex() - 2, HttpTokens.CRLF, 0, 2);
                        _buffer.setGetIndex(_buffer.getIndex() - 2);
                        BufferUtil.prependHexInt(_buffer, size);

                        if (_needCRLF)
                        {
                            _buffer.poke(_buffer.getIndex() - 2, HttpTokens.CRLF, 0, 2);
                            _buffer.setGetIndex(_buffer.getIndex() - 2);
                            _needCRLF = false;
                        }
                    }
                    else
                    {
                        // No space so lets use the header buffer.
                        if (_needCRLF)
                        {
                            if (_header.length() > 0) throw new IllegalStateException("EOC");
                            _header.put(HttpTokens.CRLF);
                            _needCRLF = false;
                        }
                        BufferUtil.putHexInt(_header, size);
                        _header.put(HttpTokens.CRLF);
                    }

                    // Add end chunk trailer.
                    if (_buffer.space() >= 2)
                        _buffer.put(HttpTokens.CRLF);
                    else
                        _needCRLF = true;
                }

                // If we need EOC and everything written
                if (_needEOC && (_content == null || _content.length() == 0))
                {
                    if (_needCRLF)
                    {
                        if (_buffer == null && _header.space() >= 2)
                        {
                            _header.put(HttpTokens.CRLF);
                            _needCRLF = false;
                        }
                        else if (_buffer!=null && _buffer.space() >= 2)
                        {
                            _buffer.put(HttpTokens.CRLF);
                            _needCRLF = false;
                        }
                    }

                    if (!_needCRLF && _needEOC)
                    {
                        if (_buffer == null && _header.space() >= LAST_CHUNK.length)
                        {
                            if (!_head)
                            {
                                _header.put(LAST_CHUNK);
                                _bufferChunked=true;
                            }
                            _needEOC = false;
                        }
                        else if (_buffer!=null && _buffer.space() >= LAST_CHUNK.length)
                        {
                            if (!_head)
                            {
                                _buffer.put(LAST_CHUNK);
                                _bufferChunked=true;
                            }
                            _needEOC = false;
                        }
                    }
                }
            }
        }

        if (_content != null && _content.length() == 0) 
            _content = null;

    }

    public int getBytesBuffered()
    {
        return(_header==null?0:_header.length())+
        (_buffer==null?0:_buffer.length())+
        (_content==null?0:_content.length());
    }

    public boolean isEmpty()
    {
        return (_header==null||_header.length()==0) &&
        (_buffer==null||_buffer.length()==0) &&
        (_content==null||_content.length()==0);
    }
    
    public String toString()
    {
        return "HttpGenerator s="+_state+
        " h="+(_header==null?"null":(""+_header.length()))+
        " b="+(_buffer==null?"null":(""+_buffer.length()))+
        " c="+(_content==null?"null":(""+_content.length()));
    }
}
