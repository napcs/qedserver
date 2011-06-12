// ========================================================================
// $Id: ContextFactory.java 1327 2006-11-27 18:40:14Z janb $
// Copyright 1999-2006 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.naming;


import java.util.Hashtable;
import java.util.WeakHashMap;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.log.Log;



/**
 * ContextFactory.java
 *
 * This is an object factory that produces a jndi naming
 * context based on a classloader. 
 * 
 *  It is used for the java:comp context.
 *  
 *  This object factory is bound at java:comp. When a
 *  lookup arrives for java:comp,  this object factory
 *  is invoked and will return a context specific to
 *  the caller's environment (so producing the java:comp/env
 *  specific to a webapp).
 *  
 *  The context selected is based on classloaders. First
 *  we try looking in at the classloader that is associated
 *  with the current webapp context (if there is one). If
 *  not, we use the thread context classloader.
 * 
 * Created: Fri Jun 27 09:26:40 2003
 *
 * @author <a href="mailto:janb@mortbay.com">Jan Bartel</a>
 * 
 */
public class ContextFactory implements ObjectFactory
{
    /**
     * Map of classloaders to contexts.
     */
    private static WeakHashMap _contextMap;
    
    /**
     * Threadlocal for injecting a context to use
     * instead of looking up the map.
     */
    private static ThreadLocal _threadContext;

    static
    {
        _contextMap = new WeakHashMap();
        _threadContext = new ThreadLocal();
    }
    
  

    /** 
     * Find or create a context which pertains to a classloader.
     * 
     * We use either the classloader for the current ContextHandler if
     * we are handling a request, OR we use the thread context classloader
     * if we are not processing a request.
     * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object, javax.naming.Name, javax.naming.Context, java.util.Hashtable)
     */
    public Object getObjectInstance (Object obj,
                                     Name name,
                                     Context nameCtx,
                                     Hashtable env)
        throws Exception
    {
        //First, see if we have had a context injected into us to use.
        Context ctx = (Context)_threadContext.get();
        if (ctx != null) 
        {
            if(Log.isDebugEnabled()) Log.debug("Using the Context that is bound on the thread");
            return ctx;
        }
        
        // Next, see if we are in a webapp context, if we are, use
        // the classloader of the webapp to find the right jndi comp context
        ClassLoader loader = null;
        if (ContextHandler.getCurrentContext() != null)
        {
            loader = ContextHandler.getCurrentContext().getContextHandler().getClassLoader();
        }
        
        
        if (loader != null)
        {
            if (Log.isDebugEnabled()) Log.debug("Using classloader of current org.mortbay.jetty.handler.ContextHandler");
        }
        else
        {
            //Not already in a webapp context, in that case, we try the
            //curren't thread's classloader instead
            loader = Thread.currentThread().getContextClassLoader();
            if (Log.isDebugEnabled()) Log.debug("Using thread context classloader");
        }
        
        //Get the context matching the classloader
        ctx = (Context)_contextMap.get(loader);
        
        //The map does not contain an entry for this classloader
        if (ctx == null)
        {
            //Check if a parent classloader has created the context
            ctx = getParentClassLoaderContext(loader);

            //Didn't find a context to match any of the ancestors
            //of the classloader, so make a context
            if (ctx == null)
            {
                Reference ref = (Reference)obj;
                StringRefAddr parserAddr = (StringRefAddr)ref.get("parser");
                String parserClassName = (parserAddr==null?null:(String)parserAddr.getContent());
                NameParser parser = (NameParser)(parserClassName==null?null:loader.loadClass(parserClassName).newInstance());
                
                ctx = new NamingContext (env,
                                         name.get(0),
                                         nameCtx,
                                         parser);
                if(Log.isDebugEnabled())Log.debug("No entry for classloader: "+loader);
                _contextMap.put (loader, ctx);
            }
        }

        return ctx;
    }

    /**
     * Keep trying ancestors of the given classloader to find one to which
     * the context is bound.
     * @param loader
     * @return
     */
    public Context getParentClassLoaderContext (ClassLoader loader)
    {
        Context ctx = null;
        ClassLoader cl = loader;
        for (cl = cl.getParent(); (cl != null) && (ctx == null); cl = cl.getParent())
        {
            ctx = (Context)_contextMap.get(cl);
        }

        return ctx;
    }
    

    /**
     * Associate the given Context with the current thread.
     * resetComponentContext method should be called to reset the context.
     * @param ctx the context to associate to the current thread.
     * @return the previous context associated on the thread (can be null)
     */
    public static Context setComponentContext(final Context ctx) 
    {
        Context previous = (Context)_threadContext.get();
        _threadContext.set(ctx);
        return previous;
    }

    /**
     * Set back the context with the given value.
     * Don't return the previous context, use setComponentContext() method for this.
     * @param ctx the context to associate to the current thread.
     */
    public static void resetComponentContext(final Context ctx) 
    {
        _threadContext.set(ctx);
    }

} 
