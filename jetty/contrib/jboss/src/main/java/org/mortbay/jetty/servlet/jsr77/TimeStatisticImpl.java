//========================================================================
//$Id: TimeStatisticImpl.java 1195 2006-11-12 23:02:51Z janb $
//Copyright 200-2004 Mort Bay Consulting Pty. Ltd.
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



/**
 * @author janb
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TimeStatisticImpl implements
        javax.management.j2ee.statistics.TimeStatistic
{
 
    private static final String HOUR = "HOUR";
    private static final String MICROSECOND = "MICROSECOND";
    private static final String MILLISECONDS = "MILLISECONDS";
    private static final String MINUTE = "MINUTE";
    private static final String NANOSECOND = "NANOSECOND";
    private static final String SECOND = "SECOND";
    private ServletStatsImpl servletStats = null;
    private long count = 0;
    private long maxTime = 0;
    private long minTime = 0; 
    private long totalTime = 0; 
    private long startTime = 0;
    private long lastSampleTime = 0;
    private String name = null;
    private String description = null;
    private String units = null;
    
    
    public TimeStatisticImpl (ServletStatsImpl servletStats,  String name, String description, String unit)
    {
        setServletStats(servletStats);
        setName (name);
        setDescription (description);
        setUnit (unit);
    }

    /** Return number of times statistic has been gathered.
     * @see javax.management.j2ee.statistics.TimeStatistic#getCount()
     */
    public long getCount ()
    {
       return count;
    }

    /** Return max value of statistic.
     * @see javax.management.j2ee.statistics.TimeStatistic#getMaxTime()
     */
    public long getMaxTime ()
    {
       return maxTime;
    }

    /** Return min value of statistic.
     * @see javax.management.j2ee.statistics.TimeStatistic#getMinTime()
     */
    public long getMinTime ()
    {
        return minTime;
    }

    /** Return total time of statistic
     * @see javax.management.j2ee.statistics.TimeStatistic#getTotalTime()
     */
    public long getTotalTime ()
    {
        return totalTime;
    }

    /** Return name of statistic
     * @see javax.management.j2ee.statistics.Statistic#getName()
     */
    public String getName ()
    {
      return name;
    }

    public void setName (String n)
    {
        name = n;
    }
    
    /** Return units of statistic
     * @see javax.management.j2ee.statistics.Statistic#getUnit()
     */
    public String getUnit ()
    {
        return units;
    }
    
    public void setUnit (String u)
    {
        units = u;
    }

    /** Human readable description of statistic
     * @see javax.management.j2ee.statistics.Statistic#getDescription()
     */
    public String getDescription ()
    {
        return description;
    }

    public void setDescription (String s)
    {
        description = s;
    }
    
    
    /** Get the time the statistic began to be gathered.
     * As milliseconds since the last epoch.
     * @see javax.management.j2ee.statistics.Statistic#getStartTime()
     */
    public long getStartTime ()
    {
        return startTime;
    }

    public void setStartTime ()
    {
        startTime = System.currentTimeMillis();
    }
    
    /** Get the time of the last sample.
     * As milliseconds since the epoch.
     * @see javax.management.j2ee.statistics.Statistic#getLastSampleTime()
     */
    public long getLastSampleTime ()
    {
        return lastSampleTime;
    }
    
    public void setServletStats (ServletStatsImpl stats)
    {
        servletStats = stats;
    }
    
    public synchronized void addSample (long sample, long time)
    {
        count ++;
        if (sample > maxTime)
            maxTime = sample;
        if ((sample < minTime) || (minTime == 0))
            minTime = sample;
        lastSampleTime = time;
        totalTime = totalTime + sample;
    }
    
    public String toString ()
    {
        return "Name="+getName()+
               ", Description="+getDescription()+
               ", Units="+getUnit()+
               ", StartTime="+getStartTime()+
               ", Count="+getCount()+
               ", MinTime="+getMinTime()+
               ", MaxTime="+getMaxTime()+
               ", TotalTime="+getTotalTime()+
               ", LastSampleTime="+getLastSampleTime();
    }

}
