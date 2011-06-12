//========================================================================
//$Id: Jsr77ServletHolderMBean.java 1259 2006-11-19 16:00:48Z janb $
//Copyright 2000-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.servlet.jsr77.management;

import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.j2ee.statistics.ServletStats;

import org.jboss.jetty.JBossWebAppContext;
import org.mortbay.jetty.servlet.jsr77.Jsr77ServletHolder;
import org.mortbay.management.ObjectMBean;
import org.mortbay.log.Log;


/**
 * 
 * Jsr77ServletHolderMBean
 *
 * @author janb
 * @version $Revision: 1259 $ $Date: 2006-11-19 10:00:48 -0600 (Sun, 19 Nov 2006) $
 *
 */
public class Jsr77ServletHolderMBean extends ObjectMBean
{
    private Jsr77ServletHolder _servletHolder = null;
    private ServletStats _stats;
    
    
    public Jsr77ServletHolderMBean (Object managedObject)
    	throws MBeanException
    {
    	super(managedObject);
    	_servletHolder = (Jsr77ServletHolder)managedObject;
    }
    
    /**StatisticsProvider
     * As per the jsr77 spec, we are providing statistics for a
     * servlet
     * @return true
     */
    public boolean getStatisticsProvider ()
    {
        return true;
    }
    
    /**ServletStats
     * @return the JSR77 servlet stats for the servlet we represent
     */
    public ServletStats getStats ()
    {
        getJsr77Stats();
        return _stats;
    }
    
    /**MaxTime
     * Necessary for JBoss's JSR77 impl.
     * @return the max service time statistic
     */
    public Long getMaxTime ()
    {
        getJsr77Stats();
        
        if (null==_stats)
            return new Long(0L);
        
        return new Long(_stats.getServiceTime().getMaxTime());              
    }
    
    /**MinTime
     * Necessary for JBoss's JSR77 impl.
     * @return the min service time statistic
     */
    public Long getMinTime ()
    {
        getJsr77Stats();
        if (null==_stats)
            return new Long(0L);
        
        return new Long(_stats.getServiceTime().getMinTime());          
    }
   
    
    /**Satisfying JBoss's JSR77 impl
     * @return
     */
    public Long getProcessingTime ()
    {
        return new Long(getTotalTime());
    }
    
    /**Satisfying JBoss's JSR77 impl
     * @return
     */
    public Integer getRequestCount ()
    {
        return new Integer((int)getCount());
    }
    
    /**Count
     * Convenience method. Also helpful for JBoss's JSR77 impl.
     * @return the number of times the servlet service() method has been called.
     */
    private long getCount ()
    {
        getJsr77Stats();
        if (null==_stats)
            return 0L;
        
        return _stats.getServiceTime().getCount();          
    }
    
    /**TotalTime
     * Convenience method. Also helpful for JBoss's JSR77 impl.
     * @return the total time spent in the servlet's service() method.
     */
    private long getTotalTime()
    {
       getJsr77Stats();
        if (null==_stats)
            return 0L;
        
        return _stats.getServiceTime().getTotalTime();    
    }
    
    /** Jsr77Stats
     * Lookup the statistic object for the servlet we represent.
     * Statistics are captured by a filter placed in front of each servlet.
     */
    private void getJsr77Stats ()
    {
        if (null==_stats)
        {
            if (null==_servletHolder)
                return;
            
            _stats = _servletHolder.getServletStats();
        }
    }
    
   public ObjectName getObjectName()
   {
       
       if (getMBeanContainer() == null)
           return null; //not possible to make a name
      
       String name = _servletHolder.getName();
       ObjectName oname = null;
       try
       {
           oname = new ObjectName(getMBeanContainer().getDomain()+":J2EEServer=none,J2EEApplication=none,J2EEWebModule="+((JBossWebAppContext)_servletHolder.getWebAppContext()).getUniqueName()+",j2eeType=Servlet,name="+name);    
       }
       catch (Exception e)
       {
           Log.warn(e);
       }
       return oname;
       
   }
}
