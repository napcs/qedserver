//========================================================================
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

package org.mortbay.jetty.servlet.management;

import org.mortbay.jetty.servlet.Holder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.management.ObjectMBean;

public class HolderMBean extends ObjectMBean
{
    public HolderMBean(Object managedObject)
    {
        super(managedObject);
    }

    /* ------------------------------------------------------------ */
    public String getObjectNameBasis()
    {
        if (_managed!=null && _managed instanceof Holder)
        {
            Holder holder = (Holder)_managed;
            String name = holder.getName();
            if (name!=null)
                return name;
        }
        return super.getObjectNameBasis();
    }
}
