//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

/**
 * 
 */
package org.mortbay.jetty.nio;

import org.mortbay.io.Buffer;
import org.mortbay.io.nio.DirectNIOBuffer;
import org.mortbay.io.nio.IndirectNIOBuffer;
import org.mortbay.io.nio.NIOBuffer;
import org.mortbay.jetty.AbstractConnector;

/* ------------------------------------------------------------ */
/**
 * @author gregw
 *
 */
public abstract class AbstractNIOConnector extends AbstractConnector implements NIOConnector
{
    private boolean _useDirectBuffers=true;
 
    /* ------------------------------------------------------------------------------- */
    public boolean getUseDirectBuffers()
    {
        return _useDirectBuffers;
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * @param direct If True (the default), the connector can use NIO direct buffers.
     * Some JVMs have memory management issues (bugs) with direct buffers.
     */
    public void setUseDirectBuffers(boolean direct)
    {
        _useDirectBuffers=direct;
    }

    /* ------------------------------------------------------------------------------- */
    protected Buffer newBuffer(int size)
    {
        // TODO
        // Header buffers always byte array buffers (efficiency of random access)
        // There are lots of things to consider here... DIRECT buffers are faster to
        // send but more expensive to build and access! so we have choices to make...
        // + headers are constructed bit by bit and parsed bit by bit, so INDiRECT looks
        // good for them.   
        // + but will a gather write of an INDIRECT header with a DIRECT body be any good?
        // this needs to be benchmarked.
        // + Will it be possible to get a DIRECT header buffer just for the gather writes of
        // content from file mapped buffers?  
        // + Are gather writes worth the effort?  Maybe they will work well with two INDIRECT
        // buffers being copied into a single kernel buffer?
        // 
        Buffer buf = null;
        if (size==getHeaderBufferSize())
            buf= new IndirectNIOBuffer(size);
        else
            buf = _useDirectBuffers
                ?(NIOBuffer)new DirectNIOBuffer(size)
                :(NIOBuffer)new IndirectNIOBuffer(size);
        return buf;
    }
    

}
