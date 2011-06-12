//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.spring;

import org.mortbay.jetty.Server;
import org.mortbay.resource.Resource;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.UrlResource;

public class Main
{
	public static void main(String[] args)
		throws Exception	
	{		
		Resource config=Resource.newResource(args.length==1?args[0]:"etc/jetty-spring.xml");
		XmlBeanFactory bf = new XmlBeanFactory(new UrlResource(config.getURL()));
		Server server = (Server) bf.getBean(args.length==2?args[1]:"Server");
		server.join();
	}
}
