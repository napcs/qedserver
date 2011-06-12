//========================================================================
//$Id: Jsr77ServletHandler.java 1208 2006-11-13 21:38:44Z janb $
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

package org.mortbay.jetty.servlet.jsr77;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;

public class Jsr77ServletHandler extends ServletHandler 
{
    WebAppContext _webAppContext;
    
    
 
    public void setWebAppContext(WebAppContext wac)
    {
        _webAppContext = wac;
    }
    
    public WebAppContext getWebAppContext ()
    {
        return _webAppContext;
    }
    public ServletHolder newServletHolder ()
    {
        Jsr77ServletHolder holder = new Jsr77ServletHolder();
        holder.setWebAppContext(_webAppContext);
        return holder;
    }
	public ServletHolder newServletHolder(Class servlet) 
	{
		Jsr77ServletHolder holder = new Jsr77ServletHolder(servlet);
        holder.setWebAppContext(_webAppContext);
        return holder;
	}
}
