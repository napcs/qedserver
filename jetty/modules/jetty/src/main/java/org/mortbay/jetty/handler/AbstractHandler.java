//========================================================================
//$Id: AbstractHandler.java,v 1.4 2005/11/11 22:55:39 gregwilkins Exp $
//Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.handler;


import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.log.Log;


/* ------------------------------------------------------------ */
/** AbstractHandler.
 * @author gregw
 *
 */
public abstract class AbstractHandler extends AbstractLifeCycle implements Handler
{
    protected String _string;
    private Server _server;
    
    /* ------------------------------------------------------------ */
    /**
     * 
     */
    public AbstractHandler()
    {
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.thread.LifeCycle#start()
     */
    protected void doStart() throws Exception
    {
        Log.debug("starting {}",this);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.thread.LifeCycle#stop()
     */
    protected void doStop() throws Exception
    {
        Log.debug("stopping {}",this);
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        if (_string==null)
        {
            _string=super.toString();
            _string=_string.substring(_string.lastIndexOf('.')+1);
        }
        return _string;
    }

    /* ------------------------------------------------------------ */
    public void setServer(Server server)
    {
        Server old_server=_server;
        if (old_server!=null && old_server!=server)
            old_server.getContainer().removeBean(this);
        _server=server;
        if (_server!=null && _server!=old_server)
            _server.getContainer().addBean(this);
    }

    /* ------------------------------------------------------------ */
    public Server getServer()
    {
        return _server;
    }


    /* ------------------------------------------------------------ */
    public void destroy()
    {
        if (!isStopped())
            throw new IllegalStateException("!STOPPED");
        if (_server!=null)
            _server.getContainer().removeBean(this);
    }

}
