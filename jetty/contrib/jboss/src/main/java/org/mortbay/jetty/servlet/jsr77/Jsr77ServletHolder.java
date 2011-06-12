//========================================================================
//$Id: Jsr77ServletHolder.java 1216 2006-11-14 14:53:40Z janb $
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

import java.io.IOException;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;

import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;

public class Jsr77ServletHolder extends ServletHolder 
{

	private ServletStatsImpl _servletStats = null;
    private WebAppContext _webAppContext = null;

	public Jsr77ServletHolder() 
	{
	}

	public Jsr77ServletHolder(Servlet servlet)
	{
		super(servlet);
	}
	
	public Jsr77ServletHolder(Class servlet) 
	{
		super(servlet);
	}
	
    public WebAppContext getWebAppContext()
    {
        return _webAppContext;
    }
    public void setWebAppContext(WebAppContext wac)
    {
        _webAppContext = wac;
    }
	public void doStart() throws Exception 
	{
		super.doStart();
		_servletStats = new ServletStatsImpl(getName());
	}
	
	public void handle(ServletRequest request, ServletResponse response) 
		throws ServletException, UnavailableException, IOException 
	{
        long startTime =0L;
        long endTime = 0L;
        try
        {
            //start statistic gathering - get the name of Servlet for which this filter will apply, and therefore
            //on whose behalf we are gathering statistics???
            startTime = System.currentTimeMillis();
            super.handle(request, response);
        }
        finally
        {
            //finish statistic gathering
            endTime = System.currentTimeMillis();
            TimeStatisticImpl statistic = (TimeStatisticImpl)_servletStats.getServiceTime();
            statistic.addSample(endTime-startTime, endTime);

        }
	}
	
	public ServletStatsImpl getServletStats()
	{
		return this._servletStats;
	}
}
