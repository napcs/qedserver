//========================================================================
//$Id: WrappedHandler.java,v 1.2 2005/11/11 22:55:39 gregwilkins Exp $
//Copyright 2004-2006 Mort Bay Consulting Pty. Ltd.
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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.component.LifeCycle;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HandlerContainer;
import org.mortbay.jetty.Server;

/* ------------------------------------------------------------ */
/** A <code>HandlerWrapper</code> acts as a {@link Handler} but delegates the {@link Handler#handle handle} method and
 * {@link LifeCycle life cycle} events to a delegate. This is primarily used to implement the <i>Decorator</i> pattern.
 * @author gregw
 */
public class HandlerWrapper extends AbstractHandlerContainer
{
    private Handler _handler;

    /* ------------------------------------------------------------ */
    /**
     * 
     */
    public HandlerWrapper()
    {
        super();
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the handlers.
     */
    public Handler getHandler()
    {
        return _handler;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param handler Set the {@link Handler} which should be wrapped.
     */
    public void setHandler(Handler handler)
    {
        try
        {
            Handler old_handler = _handler;
            
            if (getServer()!=null)
                getServer().getContainer().update(this, old_handler, handler, "handler");
            
            if (handler!=null)
            {
                handler.setServer(getServer());
            }
            
            _handler = handler;
            
            if (old_handler!=null)
            {
                if (old_handler.isStarted())
                    old_handler.stop();
            }
        }
        catch(Exception e)
        {
            IllegalStateException ise= new IllegalStateException();
            ise.initCause(e);
            throw ise;
        }
    }

    /* ------------------------------------------------------------ */
    /** Add a handler.
     * This implementation of addHandler calls setHandler with the 
     * passed handler.  If this HandlerWrapper had a previous wrapped
     * handler, then it is passed to a call to addHandler on the passed
     * handler.  Thus this call can add a handler in a chain of 
     * wrapped handlers.
     * 
     * @param handler
     */
    public void addHandler(Handler handler)
    {
        Handler old = getHandler();
        if (old!=null && !(handler instanceof HandlerContainer))
            throw new IllegalArgumentException("Cannot add");
        setHandler(handler);
        if (old!=null)
            ((HandlerContainer)handler).addHandler(old);
    }
    
    
    public void removeHandler (Handler handler)
    {
        Handler old = getHandler();
        if (old!=null && (old instanceof HandlerContainer))
            ((HandlerContainer)old).removeHandler(handler);
        else if (old!=null && handler.equals(old))
            setHandler(null);
        else
            throw new IllegalStateException("Cannot remove");
    }
    
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.thread.AbstractLifeCycle#doStart()
     */
    protected void doStart() throws Exception
    {
        if (_handler!=null)
            _handler.start();
        super.doStart();
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.thread.AbstractLifeCycle#doStop()
     */
    protected void doStop() throws Exception
    {
        super.doStop();
        if (_handler!=null)
            _handler.stop();
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.EventHandler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
    {
        if (_handler!=null && isStarted())
            _handler.handle(target,request, response, dispatch);
    }
    

    /* ------------------------------------------------------------ */
    public void setServer(Server server)
    {
        Server old_server=getServer();
        
        super.setServer(server);
        
        Handler h=getHandler();
        if (h!=null)
            h.setServer(server);
        
        if (server!=null && server!=old_server)
            server.getContainer().update(this, null,_handler, "handler");
    }
    

    /* ------------------------------------------------------------ */
    protected Object expandChildren(Object list, Class byClass)
    {
        return expandHandler(_handler,list,byClass);
    }

   
}
