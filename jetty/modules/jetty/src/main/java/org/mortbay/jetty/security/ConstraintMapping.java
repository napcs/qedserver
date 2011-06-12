//========================================================================
//$Id: ConstraintMapping.java,v 1.1 2005/10/05 14:09:14 janb Exp $
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

package org.mortbay.jetty.security;


public class ConstraintMapping
{
    String method;
    String pathSpec;
    Constraint constraint;
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the constraint.
     */
    public Constraint getConstraint()
    {
        return constraint;
    }
    /* ------------------------------------------------------------ */
    /**
     * @param constraint The constraint to set.
     */
    public void setConstraint(Constraint constraint)
    {
        this.constraint = constraint;
    }
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the method.
     */
    public String getMethod()
    {
        return method;
    }
    /* ------------------------------------------------------------ */
    /**
     * @param method The method to set.
     */
    public void setMethod(String method)
    {
        this.method = method;
    }
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the pathSpec.
     */
    public String getPathSpec()
    {
        return pathSpec;
    }
    /* ------------------------------------------------------------ */
    /**
     * @param pathSpec The pathSpec to set.
     */
    public void setPathSpec(String pathSpec)
    {
        this.pathSpec = pathSpec;
    }
}