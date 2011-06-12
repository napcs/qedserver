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

package org.mortbay.cometd.session;

import java.io.ByteArrayInputStream;
import java.util.Map;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.http.HttpServletRequest;

import org.mortbay.cometd.AbstractBayeux;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.security.B64Code;
import org.mortbay.jetty.servlet.AbstractSessionManager;
import org.mortbay.jetty.servlet.HashSessionManager;
import org.mortbay.log.Log;
import org.mortbay.util.ByteArrayOutputStream2;

import org.cometd.Bayeux;
import org.cometd.Client;
import org.cometd.Extension;
import org.cometd.Message;

/* ------------------------------------------------------------ */
/**
 * This is an experimental session manager that uses Bayeux to send
 * replicated session data to the client, that can be made available if the client
 * switches nodes in a cluster.
 * 
 * Care must be taken when handling requests that do not have sessions, so that
 * new sessions are not created.  Session should be created/restored by the bayeux handshake.
 * 
 * The client side needs to add in the dojox.cometd.session extension.
 * 
 * @author gregw
 *
 */
public class BayeuxSessionManager extends HashSessionManager
{
    public final static String BAYEUX_SESSION=Bayeux.SERVICE+"/ext/session";
    AbstractBayeux _bayeux;
    private String _secret="Really Private";

    /* ------------------------------------------------------------ */
    protected void initialize(Bayeux bayeux)
    {
        Log.info("Bayeux Session Manager initialized for "+_context.getContextPath());
        _bayeux=(AbstractBayeux)bayeux;
        _bayeux.setRequestAvailable(true);
        _bayeux.addExtension(new SessionExt());
    }
    
    /* ------------------------------------------------------------ */
    protected AbstractSessionManager.Session newSession(HttpServletRequest request)
    {
        return new BayeuxSession(request);
    }

    /* ------------------------------------------------------------ */
    protected AbstractSessionManager.Session newSession(long created, String clusterId)
    {
        return new BayeuxSession(created,clusterId);
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.jetty.servlet.HashSessionManager#doStart()
     */
    public void doStart() throws Exception
    {
        super.doStart();
        
        _context.getContextHandler().addEventListener(new ServletContextAttributeListener(){

            public void attributeAdded(ServletContextAttributeEvent scab)
            {
                if (scab.getName().equals(Bayeux.DOJOX_COMETD_BAYEUX))
                {
                    Bayeux bayeux=(Bayeux)scab.getValue();
                    initialize(bayeux);
                }
            }

            public void attributeRemoved(ServletContextAttributeEvent scab)
            {
            }

            public void attributeReplaced(ServletContextAttributeEvent scab)
            {
            }
        });
        
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    protected class SessionExt implements Extension
    {

        /* -------------------------------------------------------- */
        public Message rcv(Message message)
        {
            return message;
        }

        /* -------------------------------------------------------- */
        public Message rcvMeta(Message message)
        {
            if (message.getChannel().equals(Bayeux.META_HANDSHAKE))
            {
                Map<String,?> ext = (Map<String,?>)message.get(Bayeux.EXT_FIELD);
                String session=ext==null?null:(String)ext.get("session");
                if (session!=null)
                {
                    byte[] buf = B64Code.decode(session.toCharArray());
                    // TODO decrypt
                    ByteArrayInputStream bin=new ByteArrayInputStream(buf);
                    try
                    {
                        Session s =restoreSession(bin);
                        ((BayeuxSession)s).access(System.currentTimeMillis());
                        ((Request)_bayeux.getCurrentRequest()).setSession(s);
                        String cid=message.getClientId();
                        Client client = _bayeux.getClient(cid);
                        ((BayeuxSession)s).setClient(client);
                        addSession(s);
                        Log.info("Restored session "+s.getId()+" for bayeux client "+cid);
                    }
                    catch(Exception e)
                    {
                        Log.warn(e);
                    }
                }
            }
            return message;
        }

        /* -------------------------------------------------------- */
        public Message send(Message message)
        {
            return message;
        }

        /* -------------------------------------------------------- */
        public Message sendMeta(Message message)
        {
            if (message.getChannel().equals(Bayeux.META_HANDSHAKE))
            {
                String cid=message.getClientId();
                Client client = _bayeux.getClient(cid);
                ((BayeuxSession)_bayeux.getCurrentRequest().getSession(true)).setClient(client);
            }
            return message;
        }
        
    }

    /* ------------------------------------------------------------ */
    private Object encode(Object o, String valueSecret)
    {
        return null;
    }

    /* ------------------------------------------------------------ */
    private Object decode(String value, String valueSecret)
    {
        return null;
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    protected class BayeuxSession extends HashSessionManager.Session
    {
        Client _client;
        boolean _dirty;
        
        /* ------------------------------------------------------------ */
        protected BayeuxSession(HttpServletRequest request)
        {
            super(request);
        }

        /* ------------------------------------------------------------ */
        public BayeuxSession(long created, String clusterId)
        {
            super(created,clusterId);
            _dirty=true;
        }
        
        /* ------------------------------------------------------------ */
        public void setClient(Client client)
        {
            _client=client;
        }


        /* ------------------------------------------------------------ */
        /* (non-Javadoc)
         */
        protected void access(long time)
        {
            super.access(time);
        }

        /* ------------------------------------------------------------ */
        /* (non-Javadoc)
         * @see org.mortbay.jetty.servlet.HashSessionManager.Session#invalidate()
         */
        public void invalidate() throws IllegalStateException
        {
            super.invalidate();
            _dirty=true;
        }

        /* ------------------------------------------------------------ */
        /* (non-Javadoc)
         * @see org.mortbay.jetty.servlet.AbstractSessionManager.Session#removeAttribute(java.lang.String)
         */
        public synchronized void removeAttribute(String name)
        {
            super.removeAttribute(name);
            _dirty=true;
        }

        /* ------------------------------------------------------------ */
        /* (non-Javadoc)
         * @see org.mortbay.jetty.servlet.AbstractSessionManager.Session#setAttribute(java.lang.String, java.lang.Object)
         */
        public synchronized void setAttribute(String name, Object value)
        {
            super.setAttribute(name,value);
            _dirty=true;
        }

        /* ------------------------------------------------------------ */
        /* (non-Javadoc)
         * @see org.mortbay.jetty.servlet.AbstractSessionManager.Session#complete()
         */
        protected void complete()
        {
            super.complete();
            
            if (_dirty && _client!=null)
            {
                _dirty=false;
                Message message=_bayeux.newMessage();
                
                ByteArrayOutputStream2 bout = new ByteArrayOutputStream2();
                try
                {
                    save(bout);
                    
                    // TODO real encryption....  
                    byte[] buf = bout.toByteArray();
                    String encoded=new String(B64Code.encode(buf));
                    message.put(Bayeux.DATA_FIELD,encoded);
                }
                catch(Exception e)
                {
                    Log.warn(e);
                }
                
                _bayeux.deliver(_client,_client,BAYEUX_SESSION,message);
            }
        }
    }
}
