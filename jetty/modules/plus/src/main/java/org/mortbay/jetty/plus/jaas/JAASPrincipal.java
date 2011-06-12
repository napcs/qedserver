// ========================================================================
// $Id: JAASPrincipal.java 305 2006-03-07 10:32:14Z janb $
// Copyright 2002-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plus.jaas;

import java.io.Serializable;
import java.security.Principal;



/* ---------------------------------------------------- */
/** JAASPrincipal
 * <p>Impl class of Principal interface.
 *
 * <p><h4>Notes</h4>
 * <p>
 *
 * <p><h4>Usage</h4>
 * <pre>
 */
/*
 * </pre>
 *
 * @see
 * @version 1.0 Tue Apr 15 2003
 * @author Jan Bartel (janb)
 */
public class JAASPrincipal implements Principal, Serializable
{
    private String name = null;
    
    
    public JAASPrincipal(String userName)
    {
        this.name = userName;
    }


    public boolean equals (Object p)
    {
        if (! (p instanceof JAASPrincipal))
            return false;

        return getName().equals(((JAASPrincipal)p).getName());
    }


    public int hashCode ()
    {
        return getName().hashCode();
    }


    public String getName ()
    {
        return this.name;
    }


    public String toString ()
    {
        return getName();
    }
    

    
}

    
