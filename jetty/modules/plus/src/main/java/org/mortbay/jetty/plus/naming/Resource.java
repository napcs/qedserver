// ========================================================================
// $Id: Resource.java 3680 2008-09-21 10:37:13Z janb $
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


import javax.naming.NamingException;



/**
 * Resource
 *
 *
 */
public class Resource extends NamingEntry
{
    
    public  Resource (Object scope, String jndiName, Object objToBind)
    throws NamingException
    {
        super(scope, jndiName, objToBind);
    }
    
    /**
     * @param jndiName
     * @param objToBind
     */
    public Resource (String jndiName, Object objToBind)
    throws NamingException
    {
        super(jndiName, objToBind);
    }

}
