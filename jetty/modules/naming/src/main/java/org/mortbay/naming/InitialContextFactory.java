// ========================================================================
// $Id: InitialContextFactory.java 1327 2006-11-27 18:40:14Z janb $
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
import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

import org.mortbay.log.Log;
import org.mortbay.naming.local.localContextRoot;


/*------------------------------------------------*/    
/**
 * InitialContextFactory.java
 *
 * Factory for the default InitialContext.
 * Created: Tue Jul  1 19:08:08 2003
 *
 * @author <a href="mailto:janb@mortbay.com">Jan Bartel</a>
 * @version 1.0
 */
public class InitialContextFactory implements javax.naming.spi.InitialContextFactory
{
    public static class DefaultParser implements NameParser
    { 
        static Properties syntax = new Properties();   
        static 
        {
            syntax.put("jndi.syntax.direction", "left_to_right");
            syntax.put("jndi.syntax.separator", "/");
            syntax.put("jndi.syntax.ignorecase", "false");
        }
        public Name parse (String name)
            throws NamingException
        {
            return new CompoundName (name, syntax);
        }
    };
    


    /*------------------------------------------------*/    
    /**
     * Get Context that has access to default Namespace.
     * This method won't be called if a name URL beginning
     * with java: is passed to an InitialContext.
     *
     * @see org.mortbay.naming.java.javaURLContextFactory
     * @param env a <code>Hashtable</code> value
     * @return a <code>Context</code> value
     */
    public Context getInitialContext(Hashtable env) 
    {
        Log.debug("InitialContextFactory.getInitialContext()");

        Context ctx = new localContextRoot(env);
        if(Log.isDebugEnabled())Log.debug("Created initial context delegate for local namespace:"+ctx);

        return ctx;
    }
} 
