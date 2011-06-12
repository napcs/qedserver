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

package org.mortbay.jetty;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferCache;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.log.Log;

/**
 * Cached HTTP Header values.
 * This class caches the conversion of common HTTP Header values to and from {@link ByteArrayBuffer} instances.
 * The resource "/org/mortbay/jetty/useragents" is checked for a list of common user agents, so that repeated
 * creation of strings for these agents can be avoided.
 * 
 * @author gregw
 */
public class HttpHeaderValues extends BufferCache
{
    public final static String
        CLOSE="close",
        CHUNKED="chunked",
        GZIP="gzip",
        IDENTITY="identity",
        KEEP_ALIVE="keep-alive",
        CONTINUE="100-continue",
        PROCESSING="102-processing",
        TE="TE",
        BYTES="bytes",
        NO_CACHE="no-cache";

    public final static int
        CLOSE_ORDINAL=1,
        CHUNKED_ORDINAL=2,
        GZIP_ORDINAL=3,
        IDENTITY_ORDINAL=4,
        KEEP_ALIVE_ORDINAL=5,
        CONTINUE_ORDINAL=6,
        PROCESSING_ORDINAL=7,
        TE_ORDINAL=8,
        BYTES_ORDINAL=9,
        NO_CACHE_ORDINAL=10;
    
    public final static HttpHeaderValues CACHE= new HttpHeaderValues();

    public final static Buffer 
        CLOSE_BUFFER=CACHE.add(CLOSE,CLOSE_ORDINAL),
        CHUNKED_BUFFER=CACHE.add(CHUNKED,CHUNKED_ORDINAL),
        GZIP_BUFFER=CACHE.add(GZIP,GZIP_ORDINAL),
        IDENTITY_BUFFER=CACHE.add(IDENTITY,IDENTITY_ORDINAL),
        KEEP_ALIVE_BUFFER=CACHE.add(KEEP_ALIVE,KEEP_ALIVE_ORDINAL),
        CONTINUE_BUFFER=CACHE.add(CONTINUE, CONTINUE_ORDINAL),
        PROCESSING_BUFFER=CACHE.add(PROCESSING, PROCESSING_ORDINAL),
        TE_BUFFER=CACHE.add(TE,TE_ORDINAL),
        BYTES_BUFFER=CACHE.add(BYTES,BYTES_ORDINAL),
        NO_CACHE_BUFFER=CACHE.add(NO_CACHE,NO_CACHE_ORDINAL);
        
    static
    {  
        int index=100;
        CACHE.add("gzip",index++);
        CACHE.add("gzip,deflate",index++);
        CACHE.add("deflate",index++);
        
        try
        {
            InputStream ua = HttpHeaderValues.class.getResourceAsStream("/org/mortbay/jetty/useragents");
            if (ua!=null)
            {
                LineNumberReader in = new LineNumberReader(new InputStreamReader(ua));
                String line = in.readLine();
                while (line!=null)
                {
                    CACHE.add(line,index++);
                    line = in.readLine();
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            Log.ignore(e);
        }
    }
}
