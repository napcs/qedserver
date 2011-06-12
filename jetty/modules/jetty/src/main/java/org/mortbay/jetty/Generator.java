//========================================================================
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
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

import org.mortbay.io.Buffer;

public interface Generator
{
    public static final boolean LAST=true;
    public static final boolean MORE=false;

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
    void addContent(Buffer content, boolean last) throws IOException;

    /* ------------------------------------------------------------ */
    /**
     * Add content.
     * 
     * @param b byte
     * @return true if the buffers are full
     * @throws IOException
     */
    boolean addContent(byte b) throws IOException;

    void complete() throws IOException;

    void completeHeader(HttpFields responseFields, boolean last) throws IOException;

    long flush() throws IOException;

    int getContentBufferSize();

    long getContentWritten();
    
    boolean isContentWritten();

    void increaseContentBufferSize(int size);
    
    boolean isBufferFull();

    boolean isCommitted();

    boolean isComplete();

    boolean isPersistent();

    void reset(boolean returnBuffers);

    void resetBuffer();

    void sendError(int code, String reason, String content, boolean close) throws IOException;
    
    void setHead(boolean head);

    void setRequest(String method, String uri);

    void setResponse(int status, String reason);


    void setSendServerVersion(boolean sendServerVersion);
 
    void setVersion(int version);

    boolean isIdle();

    void setContentLength(long length);
    
    void setPersistent(boolean persistent);
    

}
