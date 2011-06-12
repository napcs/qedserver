//========================================================================
//$Id:  $
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

package org.jboss.jetty;

import javax.management.MBeanServer;
import org.mortbay.management.MBeanContainer;


/**
 * JBossMBeanContainer
 *
 * Subclass of the jetty MBeanContainer to mesh jetty
 * jmx architecture with jboss jmx architecture.
 *
 */
public class JBossMBeanContainer extends MBeanContainer
{
    
    public static final String JBOSS_DOMAIN = "jboss.jetty";

    
	public JBossMBeanContainer(MBeanServer server)
	{
		super(server);
        setDomain(JBOSS_DOMAIN);
	}
	
	public void start ()
    {
     //do nothing - the superclass does initialization of stuff we don't want 
    }


}
