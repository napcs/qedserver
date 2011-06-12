//========================================================================
//$Id: ServletStatsImpl.java 1195 2006-11-12 23:02:51Z janb $
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


import java.io.Serializable;

import javax.management.j2ee.statistics.Statistic;
import javax.management.j2ee.statistics.TimeStatistic;


/**
 * Jsr77ServletStats
 * 
 * Implementation of ServletStats from JSR77 specification.
 * 
 * @author janb
 */
public class ServletStatsImpl implements javax.management.j2ee.statistics.ServletStats, Serializable
{
    private static final String[] statisticNames = new String[]{"ServiceTime"};
    private TimeStatisticImpl statistic = null;
    private TimeStatisticImpl[] statistics = new TimeStatisticImpl[1];
    private String name = null;

    
    
    public ServletStatsImpl (String servletName)
    {
        name = servletName;
        statistic = new TimeStatisticImpl(this, statisticNames[0], "Servlet service method performance statistics", "MILLISECONDS");
        statistic.setStartTime();
        statistics[0] = statistic;
    }
    /**
     * 
     * @see javax.management.j2ee.statistics.ServletStats#getServiceTime()
     */
    public TimeStatistic getServiceTime ()
    {
        return statistic;
    }

    /** Get the TimeStatistic
     * @see javax.management.j2ee.statistics.Stats#getStatistic(java.lang.String)
     */
    public Statistic getStatistic (String statisticName)
    {
        if (statisticNames[0].equalsIgnoreCase(statisticName))
            return statistic;
            
        return null;
    }

    /** Get the names of supported statistics. 
     * For ServletStats, only the TimeStatistic is supported
     * @see javax.management.j2ee.statistics.Stats#getStatisticNames()
     */
    public String[] getStatisticNames ()
    {
        return statisticNames;
    }

    /** Get an object of all the types of statistics supported.
     * For ServletStats, only the TimeStatistic is supported.
     * @see javax.management.j2ee.statistics.Stats#getStatistics()
     */
    public Statistic[] getStatistics ()
    {
        return statistics;
    }

    public String getName ()
    {
        return name;
    }
    
    public String toString ()
    {
        return statistic.toString();
    }
}
