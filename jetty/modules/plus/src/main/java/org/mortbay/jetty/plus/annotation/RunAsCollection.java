//========================================================================
//$Id: RunAsCollection.java 1604 2007-02-15 12:49:23Z janb $
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plus.annotation;

import java.util.HashMap;

import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.log.Log;


/**
 * RunAsCollection
 *
 *
 */
public class RunAsCollection
{
    private HashMap _runAsMap = new HashMap();//map of classname to run-as
    
    
    public void add (RunAs runAs)
    {
        if ((runAs==null) || (runAs.getTargetClass()==null)) 
            return;
        
        if (Log.isDebugEnabled())
            Log.debug("Adding run-as for class="+runAs.getTargetClass());
        _runAsMap.put(runAs.getTargetClass().getName(), runAs);
    }

    
    public void setRunAs (Object o)
    {
        if (o==null)
            return;
        
        if (!(o instanceof ServletHolder))
            return;

        ServletHolder holder = (ServletHolder)o;

        RunAs runAs = (RunAs)_runAsMap.get(holder.getClassName());
        if (runAs == null)
            return;

        runAs.setRunAs(holder); 
    }

}
