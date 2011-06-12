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

import com.sun.grizzly.util.OutputWriter;
import com.sun.grizzly.util.SelectorFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Jeanfrancois Arcand
 */
public class GrizzlySocketChannel implements ByteChannel
{
    
    private SocketChannel socketChannel;
    
    private SelectionKey key;
    
    private long readTimeout=30*1000;
    
    private long writeTimeout=30*1000;
    
    public GrizzlySocketChannel()
    {
    }
    
    public int read(ByteBuffer dst) throws IOException
    {
        //System.err.println("GrizzlySocketChannel.read");
        
        if (key==null)
            return -1;
        
        int count=1;
        int byteRead=0;
        Selector readSelector=null;
        SelectionKey tmpKey=null;
        
        try
        {
            SocketChannel socketChannel=(SocketChannel)key.channel();
            while (count>0)
            {
                count=socketChannel.read(dst);
                if (count>0)
                    byteRead+=count;
            }
            
            if (byteRead==0)
            {
                readSelector=SelectorFactory.getSelector();
                
                if (readSelector==null)
                {
                    return 0;
                }
                count=1;
                tmpKey=socketChannel.register(readSelector,SelectionKey.OP_READ);
                tmpKey.interestOps(tmpKey.interestOps()|SelectionKey.OP_READ);
                int code=readSelector.select(readTimeout);
                tmpKey.interestOps(tmpKey.interestOps()&(~SelectionKey.OP_READ));
                while (count>0)
                {
                    count=socketChannel.read(dst);
                    if (count>0)
                        byteRead+=count;
                }
            }
        }
        finally
        {
            if (tmpKey!=null)
                tmpKey.cancel();
            
            if (readSelector!=null)
            {
                // Bug 6403933
                try
                {
                    readSelector.selectNow();
                }
                catch (IOException ex)
                {
                    ;
                }
                SelectorFactory.returnSelector(readSelector);
            }
        }
        return byteRead;
    }
    
    public boolean isOpen()
    {
        return (socketChannel!=null?socketChannel.isOpen():false);
    }
    
    public void close() throws IOException
    {
        socketChannel.close();
    }
    
    public int write(ByteBuffer src) throws IOException
    {
        return (int)OutputWriter.flushChannel(socketChannel,src, writeTimeout);
    }
    
    public int write(ByteBuffer[] src) throws IOException
    {
        return (int)OutputWriter.flushChannel(socketChannel,src, writeTimeout);
    }
    
    public SocketChannel getSocketChannel()
    {
        return socketChannel;
    }
    
    public void setSocketChannel(SocketChannel socketChannel)
    {
        this.socketChannel=socketChannel;
    }
    
    public SelectionKey getSelectionKey()
    {
        return key;
    }
    
    public void setSelectionKey(SelectionKey key)
    {
        this.key=key;
    }
    
    public long getReadTimeout()
    {
        return readTimeout;
    }
    
    public void setReadTimeout(long readTimeout)
    {
        this.readTimeout=readTimeout;
    }
    
    public long getWriteTimeout()
    {
        return writeTimeout;
    }
    
    public void setWriteTimeout(long writeTimeout)
    {
        this.writeTimeout = writeTimeout;
    }
    
}
