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

import com.sun.grizzly.Context;
import com.sun.grizzly.Controller;
import com.sun.grizzly.ProtocolFilter;
import com.sun.grizzly.SelectorHandler;
import com.sun.grizzly.util.WorkerThread;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import org.mortbay.io.nio.NIOBuffer;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpParser;

/**
 * Delegate the processing of the request to a <code>GrizzlyEndPoint</code>
 *
 * @author Jeanfrancois Arcand
 */
public class HttpProtocolFilter implements ProtocolFilter
{
    private Handler handler;
    
    private boolean keepAlive=true;
    private GrizzlyEndPoint endPoint;
    private GrizzlySocketChannel _channel = new GrizzlySocketChannel();
    private HttpParser parser;
    boolean isError = false;
    
    
    public HttpProtocolFilter()
    {
    }
    
    
    /**
     * Read available bytes and delegate the processing of them to the next
     * ProtocolFilter in the ProtocolChain.
     * @return <tt>true</tt> if the next ProtocolFilter on the ProtocolChain
     *                       need to bve invoked.
     */
    public boolean execute(Context ctx) throws IOException
    {
        ctx.getSelectionKey().attach(null);
        
        isError = false;
        _channel.setSelectionKey(ctx.getSelectionKey());
        _channel.setSocketChannel((SocketChannel)ctx.getSelectionKey().channel());
        endPoint.setChannel(_channel);
        ByteBuffer bb = ((WorkerThread)Thread.currentThread()).getByteBuffer();

        try
        {
            int nRead = 1;
            while (nRead > 0)
            {
                endPoint.handle();
                if (endPoint.isComplete())
                {
                    break;
                }
                else
                {
                    nRead = endPoint.fill(parser.getHeaderBuffer());
                }
            }
        }
        catch(Throwable t)
        {
            isError = true;
            Controller.logger().log(Level.FINE,"endPoint.handler",t);
            return false;
        }
        finally
        {
            if (isError)
            {
                endPoint.getHttpConnection().reset(true);
            }
            parser.reset(true);
            _channel.setSelectionKey(null);
            endPoint.setChannel(null);
        }
        bb.clear();
        if (parser.getBodyBuffer() != null) {
            ByteBuffer bodyBuffer = ((NIOBuffer)parser.getBodyBuffer()).getByteBuffer();
            if (bodyBuffer != null){
                bodyBuffer.clear();
                Thread.currentThread().dumpStack();
            }
        }
                    
        ctx.getSelectionKey().attach(null);
        return true;
    }
    
    /**
     * If no bytes were available, close the connection by cancelling the
     * SelectionKey. If bytes were available, register the SelectionKey
     * for new bytes.
     *
     * @return <tt>true</tt> if the previous ProtocolFilter postExecute method
     *         needs to be invoked.
     */
    public boolean postExecute(Context ctx) throws IOException
    {
        final SelectorHandler selectorHandler =
                ctx.getSelectorHandler();
        final SelectionKey key = ctx.getSelectionKey();
        
        if (!isError && endPoint.keepAlive())
        {
            ctx.setKeyRegistrationState(Context.KeyRegistrationState.REGISTER);
        }
        else
        {
            ctx.setKeyRegistrationState(Context.KeyRegistrationState.CANCEL);
        }
        return true;
    }
    
    public GrizzlyEndPoint getEndPoint()
    {
        return endPoint;
    }
    
    public void setEndPoint(GrizzlyEndPoint endPoint)
    {
        this.endPoint = endPoint;
    }
    
    public HttpParser getParser()
    {
        return parser;
    }
    
    public void setParser(HttpParser parser)
    {
        this.parser = parser;
    }
    
}
