/**
 *
 * Copyright 2005-2006 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mortbay.jetty.xbean;



import java.util.Collection;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.deployer.ContextDeployer;


/**
 * Creates an instance of Jetty inside an <a href="http://xbean.org/">XBean</a>
 * configuration file
 * 
 * @org.apache.xbean.XBean element="jetty" rootElement="true" description="Creates an
 *                  embedded Jetty web server with optional web application
 *                  context"
 * 
 * @version $Revision: 1294 $
 */
public class JettyFactoryBean  extends Server {
	
	public Collection deployers;
	public ContextDeployer hotDeployer; 

    /**
     * @org.apache.xbean.InitMethod
     * @throws Exception
     */
    public void run () throws Exception {
        start();
    }

	public Collection getDeployers() {
		return deployers;
	}

	public void setDeployers(Collection deployers) {
		this.deployers = deployers;
	}

	public ContextDeployer getHotDeployer() {
		return hotDeployer;
	}

	public void setHotDeployer(ContextDeployer hotDeployer) {
		this.hotDeployer = hotDeployer;
		addLifeCycle(hotDeployer);
	}
    
}