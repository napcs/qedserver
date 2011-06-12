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

package org.mortbay.jetty;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.mortbay.io.Buffer;
import org.mortbay.io.View;
import org.mortbay.io.nio.IndirectNIOBuffer;
import org.mortbay.io.nio.NIOBuffer;
import org.mortbay.io.nio.SelectChannelEndPoint;
import org.mortbay.io.nio.SelectorManager.SelectSet;
import org.mortbay.jetty.nio.SelectChannelConnector;

/**
 * HttpServer Tester.
 */
public class BusySelectChannelServerTest extends HttpServerTestBase
{
    public BusySelectChannelServerTest()
    {
        super(new SelectChannelConnector()
        {
            /* ------------------------------------------------------------ */
            /* (non-Javadoc)
             * @see org.mortbay.jetty.nio.SelectChannelConnector#newEndPoint(java.nio.channels.SocketChannel, org.mortbay.io.nio.SelectorManager.SelectSet, java.nio.channels.SelectionKey)
             */
            protected SelectChannelEndPoint newEndPoint(SocketChannel channel, SelectSet selectSet, SelectionKey key) throws IOException
            {
                return new ConnectorEndPoint(channel,selectSet,key)
                {
                    int write;
                    int read;
                    NIOBuffer one = new IndirectNIOBuffer(1);
                    NIOBuffer two = new IndirectNIOBuffer(2);
                    NIOBuffer three = new IndirectNIOBuffer(3);
                    
                    /* ------------------------------------------------------------ */
                    /* (non-Javadoc)
                     * @see org.mortbay.io.nio.SelectChannelEndPoint#flush(org.mortbay.io.Buffer, org.mortbay.io.Buffer, org.mortbay.io.Buffer)
                     */
                    public int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException
                    {
                        int x=write++&0xff;
                        if (x<16)
                            return 0;
                        if (x<128)
                            return flush(header);
                        return super.flush(header,buffer,trailer);
                    }

                    /* ------------------------------------------------------------ */
                    /* (non-Javadoc)
                     * @see org.mortbay.io.nio.SelectChannelEndPoint#flush(org.mortbay.io.Buffer)
                     */
                    public int flush(Buffer buffer) throws IOException
                    {
                        int x=write++&0xff;
                        if (x<16)
                            return 0;
                        if (x<96)
                        {
                            View v = new View(buffer);
                            v.setPutIndex(v.getIndex()+1);
                            int l=super.flush(v);
                            if (l>0)
                                buffer.skip(l);
                            return l;
                        }
                        return super.flush(buffer);
                    }

                    /* ------------------------------------------------------------ */
                    /* (non-Javadoc)
                     * @see org.mortbay.io.nio.ChannelEndPoint#fill(org.mortbay.io.Buffer)
                     */
                    public int fill(Buffer buffer) throws IOException
                    {
                        int x=read++&0xff;
                        if (x<32)
                            return 0;
                        
                        if (x<64 & buffer.space()>0)
                        {
                            one.clear();
                            int l=super.fill(one);
                            if (l>0)
                                buffer.put(one.peek(0));
                            return l;
                        }
                        
                        if (x<96 & buffer.space()>0)
                        {
                            two.clear();
                            int l=super.fill(two);
                            if (l>0)
                                buffer.put(two.peek(0));
                            if (l>1)
                                buffer.put(two.peek(1));
                            return l;
                        }
                        
                        if (x<128 & buffer.space()>0)
                        {
                            three.clear();
                            int l=super.fill(three);
                            if (l>0)
                                buffer.put(three.peek(0));
                            if (l>1)
                                buffer.put(three.peek(1));
                            if (l>2)
                                buffer.put(three.peek(2));
                            return l;
                        }
                        
                        return super.fill(buffer);
                    }
                    
                };
            }
            
        });
    }   
}