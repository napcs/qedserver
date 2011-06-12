//========================================================================
// Parts Copyright 2006 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//========================================================================

package org.mortbay.jetty.grizzly;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import org.mortbay.io.Buffer;

import org.mortbay.io.nio.ChannelEndPoint;
import org.mortbay.io.nio.NIOBuffer;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpParser;
import org.mortbay.jetty.grizzly.GrizzlySocketChannel;
import org.mortbay.log.Log;
import org.mortbay.util.ajax.Continuation;

public class GrizzlyEndPoint extends ChannelEndPoint
{
    protected HttpConnection _connection;
    
    public GrizzlyEndPoint(GrizzlyConnector connector,ByteChannel channel)
    throws IOException
    {
        // TODO: Needs an empty constructor?
        super(channel);
        
        //System.err.println("\nnew GrizzlyEndPoint channel="+channel);
        _connection = new HttpConnection(connector,this,connector.getServer());
    }
    
    public void handle() throws IOException
    {
        //System.err.println("GrizzlyEndPoint.handle "+this);
        
        try
        {
            //System.err.println("handle  "+this);
            _connection.handle();
        }
        finally
        {
            //System.err.println("handled "+this);
            Continuation continuation =  _connection.getRequest().getContinuation();
            if (continuation != null && continuation.isPending())
            {
                // We have a continuation
                // TODO something!
            }
            else
            {
                // something else... normally re-enable this connection is the selectset with the latest interested ops
            }
        }
        
    }
    
    /* (non-Javadoc)
     * @see org.mortbay.io.EndPoint#fill(org.mortbay.io.Buffer)
     */
    public int fill(Buffer buffer) throws IOException
    {
        Buffer buf = buffer.buffer();
        int len=0;

        NIOBuffer nbuf = (NIOBuffer)buf;
        ByteBuffer bbuf=nbuf.getByteBuffer();

        synchronized(nbuf)
        {
            try
            {
                if (bbuf.position() == 0)
                {
                    bbuf.position(buffer.putIndex());
                    len=_channel.read(bbuf);
                }
            }
            finally
            {
                buffer.setPutIndex(bbuf.position());
                bbuf.position(0);
            }
        }

        return len;
    }
    
    
    public boolean keepAlive()
    {
        return _connection.getGenerator().isPersistent();
    }
    
    public boolean isComplete()
    {
        return _connection.getGenerator().isComplete();
    }
    
    public boolean isBlocking()
    {
        return false;
    }
    
    public void setChannel(ByteChannel channel)
    {
        _channel = channel;
    }
    
    public void recycle()
    {
        _connection.destroy();
    }
    
    public HttpConnection getHttpConnection()
    {
        return _connection;
    }
    
    
}