package org.mortbay.jetty.nio;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;

import org.mortbay.log.Log;

/**
 * An implementation of the SelectChannelConnector which first tries to  
 * inherit from a channel provided by the system. If there is no inherited
 * channel available, or if the inherited channel provided not usable, then 
 * it will fall back upon normal ServerSocketChannel creation.
 * <p> 
 * Note that System.inheritedChannel() is only available from Java 1.5 onwards.
 * Trying to use this class under Java 1.4 will be the same as using a normal
 * SelectChannelConnector. 
 * <p> 
 * Use it with xinetd/inetd, to launch an instance of Jetty on demand. The port
 * used to access pages on the Jetty instance is the same as the port used to
 * launch Jetty. 
 * 
 * @author athena
 */
public class InheritedChannelConnector extends SelectChannelConnector
{
    /* ------------------------------------------------------------ */
    public void open() throws IOException
    {
        synchronized(this)
        {
            try 
            {
                Method m = System.class.getMethod("inheritedChannel",null);
                if (m!=null)
                {
                    Channel channel = (Channel)m.invoke(null,null);
                    if ( channel instanceof ServerSocketChannel )
                        _acceptChannel = (ServerSocketChannel)channel;
                }
                
                if (_acceptChannel!=null)
                    _acceptChannel.configureBlocking(false);
            }
            catch(Exception e)
            {
                Log.warn(e);
            }

            if (_acceptChannel == null)
                super.open();
            else
                throw new IOException("No System.inheritedChannel()");
        }
    }

}
