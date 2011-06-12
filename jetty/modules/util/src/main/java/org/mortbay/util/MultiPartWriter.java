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

package org.mortbay.util;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;


/* ================================================================ */
/** Handle a multipart MIME response.
 *
 * @author Greg Wilkins
 * @author Jim Crossley
*/
public class MultiPartWriter extends FilterWriter
{
    /* ------------------------------------------------------------ */
    private final static String __CRLF="\015\012";
    private final static String __DASHDASH="--";
    
    public static String MULTIPART_MIXED=MultiPartOutputStream.MULTIPART_MIXED;
    public static String MULTIPART_X_MIXED_REPLACE=MultiPartOutputStream.MULTIPART_X_MIXED_REPLACE;
    
    /* ------------------------------------------------------------ */
    private String boundary;

    /* ------------------------------------------------------------ */
    private boolean inPart=false;    
    
    /* ------------------------------------------------------------ */
    public MultiPartWriter(Writer out)
         throws IOException
    {
        super(out);
        boundary = "jetty"+System.identityHashCode(this)+
        Long.toString(System.currentTimeMillis(),36);
        
        inPart=false;
    }

    /* ------------------------------------------------------------ */
    /** End the current part.
     * @exception IOException IOException
     */
    public void close()
         throws IOException
    {
        if (inPart)
            out.write(__CRLF);
        out.write(__DASHDASH);
        out.write(boundary);
        out.write(__DASHDASH);
        out.write(__CRLF);
        inPart=false;
        super.close();
    }
    
    /* ------------------------------------------------------------ */
    public String getBoundary()
    {
        return boundary;
    }
    
    /* ------------------------------------------------------------ */
    /** Start creation of the next Content.
     */
    public void startPart(String contentType)
         throws IOException
    {
        if (inPart)
            out.write(__CRLF);
        out.write(__DASHDASH);
        out.write(boundary);
        out.write(__CRLF);
        out.write("Content-Type: ");
        out.write(contentType);
        out.write(__CRLF);
        out.write(__CRLF);
        inPart=true;
    }
    
    /* ------------------------------------------------------------ */
    /** end creation of the next Content.
     */
    public void endPart()
         throws IOException
    {
        if (inPart)
            out.write(__CRLF);
        inPart=false;
    }
        
    /* ------------------------------------------------------------ */
    /** Start creation of the next Content.
     */
    public void startPart(String contentType, String[] headers)
         throws IOException
    {
        if (inPart)
            out.write(__CRLF);
        out.write(__DASHDASH);
        out.write(boundary);
        out.write(__CRLF);
        out.write("Content-Type: ");
        out.write(contentType);
        out.write(__CRLF);
        for (int i=0;headers!=null && i<headers.length;i++)
        {
            out.write(headers[i]);
            out.write(__CRLF);
        }
        out.write(__CRLF);
        inPart=true;
    }
    
}




