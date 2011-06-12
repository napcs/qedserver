// ========================================================================
// $Id: NamingEntry.java 4027 2008-11-12 00:59:06Z janb $
// Copyright 2006 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plus.naming;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

import org.mortbay.log.Log;
import org.mortbay.naming.NamingUtil;



/**
 * NamingEntry
 *
 * Base class for all jndi related entities. Instances of
 * subclasses of this class are declared in jetty.xml or in a 
 * webapp's WEB-INF/jetty-env.xml file.
 *
 * NOTE: that all NamingEntries will be bound in a single namespace.
 *  The "global" level is just in the top level context. The "local"
 *  level is a context specific to a webapp.
 */
public abstract class NamingEntry
{
    public static final String __contextName = "__"; //all NamingEntries stored in context called "__"
    protected String jndiName;  //the name representing the object associated with the NamingEntry
    protected Object objectToBind; //the object associated with the NamingEntry
    protected String namingEntryNameString; //the name of the NamingEntry relative to the context it is stored in
    protected String objectNameString; //the name of the object relative to the context it is stored in
   
   
  
 
    
    public NamingEntry (Object scope, String jndiName, Object object)
    throws NamingException
    {
        this.jndiName = jndiName;
        this.objectToBind = object;
        save(scope); 
    }
    
    /** 
     * Create a NamingEntry. 
     * A NamingEntry is a name associated with a value which can later
     * be looked up in JNDI by a webapp.
     * 
     * We create the NamingEntry and put it into JNDI where it can
     * be linked to the webapp's env-entry, resource-ref etc entries.
     * 
     * @param jndiName the name of the object which will eventually be in java:comp/env
     * @param object the object to be bound
     * @throws NamingException
     */
    public NamingEntry (String jndiName, Object object)
    throws NamingException
    {
        this (null, jndiName, object);
    }

    
 
    
    /**
     * Add a java:comp/env binding for the object represented by this NamingEntry,
     * but bind it as the name supplied
     * @throws NamingException
     */
    public void bindToENC(String localName)
    throws NamingException
    {
        //TODO - check on the whole overriding/non-overriding thing
        InitialContext ic = new InitialContext();
        Context env = (Context)ic.lookup("java:comp/env");
        Log.debug("Binding java:comp/env/"+localName+" to "+objectNameString);
        NamingUtil.bind(env, localName, new LinkRef(objectNameString));
    }
    
    /**
     * Unbind this NamingEntry from a java:comp/env
     */
    public void unbindENC ()
    {
        try
        {
            InitialContext ic = new InitialContext();
            Context env = (Context)ic.lookup("java:comp/env");
            Log.debug("Unbinding java:comp/env/"+getJndiName());
            env.unbind(getJndiName());
        }
        catch (NamingException e)
        {
            Log.warn(e);
        }
    }
    
    /**
     * Unbind this NamingEntry entirely
     */
    public void release ()
    {
        try
        {
            InitialContext ic = new InitialContext();
            ic.unbind(objectNameString);
            ic.unbind(namingEntryNameString);
            this.jndiName=null;
            this.namingEntryNameString=null;
            this.objectNameString=null;
            this.objectToBind=null;
        }
        catch (NamingException e)
        {
            Log.warn(e);
        }
    }
    
    /**
     * Get the unique name of the object
     * relative to the scope
     * @return
     */
    public String getJndiName ()
    {
        return this.jndiName;
    }
    
    /**
     * Get the object that is to be bound
     * @return
     */
    public Object getObjectToBind()
    throws NamingException
    {   
        return this.objectToBind;
    }
    

    /**
     * Get the name of the object, fully
     * qualified with the scope
     * @return
     */
    public String getJndiNameInScope ()
    {
        return objectNameString;
    }
 
 
    
    /**
     * Save the NamingEntry for later use.
     * 
     * Saving is done by binding the NamingEntry
     * itself, and the value it represents into
     * JNDI. In this way, we can link to the
     * value it represents later, but also
     * still retrieve the NamingEntry itself too.
     * 
     * The object is bound at the jndiName passed in.
     * This NamingEntry is bound at __/jndiName.
     * 
     * eg
     * 
     * jdbc/foo    : DataSource
     * __/jdbc/foo : NamingEntry
     * 
     * @throws NamingException
     */
    protected void save (Object scope)
    throws NamingException
    {
        InitialContext ic = new InitialContext();
        NameParser parser = ic.getNameParser("");
        Name prefix = NamingEntryUtil.getNameForScope(scope);
      
        //bind the NamingEntry into the context
        Name namingEntryName = NamingEntryUtil.makeNamingEntryName(parser, getJndiName());
        namingEntryName.addAll(0, prefix);
        namingEntryNameString = namingEntryName.toString();
        NamingUtil.bind(ic, namingEntryNameString, this);
                
        //bind the object as well
        Name objectName = parser.parse(getJndiName());
        objectName.addAll(0, prefix);
        objectNameString = objectName.toString();
        NamingUtil.bind(ic, objectNameString, objectToBind);
    } 
    
}
