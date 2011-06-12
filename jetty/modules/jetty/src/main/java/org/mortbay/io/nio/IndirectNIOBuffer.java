// ========================================================================
// Copyright 2008 Mort Bay Consulting Pty. Ltd.
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
package org.mortbay.io.nio;

import java.nio.ByteBuffer;

import org.mortbay.io.ByteArrayBuffer;

public class IndirectNIOBuffer extends ByteArrayBuffer implements NIOBuffer
{
    protected ByteBuffer _buf;

    /* ------------------------------------------------------------ */
    public IndirectNIOBuffer(int size)
    {
        super(READWRITE,NON_VOLATILE);
        _buf = ByteBuffer.allocate(size);
        _buf.position(0);
        _buf.limit(_buf.capacity());
        _bytes=_buf.array();
    }

    /* ------------------------------------------------------------ */
    public IndirectNIOBuffer(ByteBuffer buffer,boolean immutable)
    {
        super(immutable?IMMUTABLE:READWRITE,NON_VOLATILE);
        if (buffer.isDirect())
            throw new IllegalArgumentException();
        _buf = buffer;
        setGetIndex(buffer.position());
        setPutIndex(buffer.limit());
        _bytes=_buf.array();
    }
    
    /* ------------------------------------------------------------ */
    public ByteBuffer getByteBuffer()
    {
        return _buf;
    }

    /* ------------------------------------------------------------ */
    public boolean isDirect()
    {
        return false;
    }
}
