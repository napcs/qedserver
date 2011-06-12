//========================================================================
//Copyright 2006-2007 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.util.ByteArrayOutputStream2;
import org.mortbay.util.StringUtil;

/* ------------------------------------------------------------ */
/**
 * A CachedExchange that retains response content for later use.
 *
 */
public class ContentExchange extends CachedExchange
{
    protected int _responseStatus;
    protected int _contentLength = -1;
    protected String _encoding = "utf-8";
    protected ByteArrayOutputStream2 _responseContent;

    protected File _fileForUpload;

    /* ------------------------------------------------------------ */
    public ContentExchange()
    {
        super(false);
    }

    /* ------------------------------------------------------------ */
    public ContentExchange(boolean keepHeaders)
    {
        super(keepHeaders);
    }

    /* ------------------------------------------------------------ */
    public int getResponseStatus()
    {
        if (getStatus() < HttpExchange.STATUS_PARSING_HEADERS)
            throw new IllegalStateException("Response not received");
        return _responseStatus;
    }

    
    /* ------------------------------------------------------------ */
    /**
     * @return The response content as a String
     * @throws UnsupportedEncodingException
     */
    public String getResponseContent() throws UnsupportedEncodingException
    {
        if (_responseContent != null)
            return _responseContent.toString(_encoding);
        return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return The response content as a byte array;
     */
    public byte[] getResponseBytes()
    {
        if (_responseContent != null)
        {
            if (_contentLength>=0 && _responseContent.getBuf().length==_contentLength)
                return _responseContent.getBuf();
            return _responseContent.toByteArray();
        }
        return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param out An output stream to write the content to.
     * @throws IOException
     */
    public void writeResponseBytesTo(OutputStream out) throws IOException
    {
        if (_responseContent != null)
            out.write(_responseContent.getBuf(),0,_responseContent.getCount());
    }

    /* ------------------------------------------------------------ */
    protected void onResponseStatus(Buffer version, int status, Buffer reason) throws IOException
    {
        if (_responseContent!=null)
            _responseContent.reset();
        _responseStatus = status;
        super.onResponseStatus(version,status,reason);
    }


    /* ------------------------------------------------------------ */
    protected void onResponseHeader(Buffer name, Buffer value) throws IOException
    {
        super.onResponseHeader(name,value);
        int header = HttpHeaders.CACHE.getOrdinal(name);
        switch (header)
        {
            case HttpHeaders.CONTENT_LENGTH_ORDINAL:
                _contentLength = BufferUtil.toInt(value);
                break;
            case HttpHeaders.CONTENT_TYPE_ORDINAL:
                String mime = StringUtil.asciiToLowerCase(value.toString());
                int i = mime.indexOf("charset=");
                if (i > 0)
                    _encoding = mime.substring(i + 8);
                break;
        }
    }

    /* ------------------------------------------------------------ */
    protected void onResponseContent(Buffer content) throws IOException
    {
        super.onResponseContent( content );
        if (_responseContent == null)
            _responseContent = (_contentLength>=0)?new ByteArrayOutputStream2(_contentLength):new ByteArrayOutputStream2();
        
        content.writeTo(_responseContent);
    }

    /* ------------------------------------------------------------ */
    protected void onRetry() throws IOException
    {
        if (_fileForUpload != null)
        {
            setRequestContent(null);
            setRequestContentSource(getInputStream());
        }
        else
            super.onRetry();
    }

    /* ------------------------------------------------------------ */
    private InputStream getInputStream() throws IOException
    {
        return new FileInputStream( _fileForUpload );
    }

    /* ------------------------------------------------------------ */
    public File getFileForUpload()
    {
        return _fileForUpload;
    }

    /* ------------------------------------------------------------ */
    public void setFileForUpload(File fileForUpload) throws IOException
    {
        this._fileForUpload = fileForUpload;
        _requestContentSource = getInputStream();
    }
}