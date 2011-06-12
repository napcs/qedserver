// ========================================================================
// Copyright 2009-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.jetty.handler;

/**
 * @version $Revision: 5236 $ $Date: 2009-06-17 07:36:08 +1000 (Wed, 17 Jun 2009) $
 */
public abstract class AbstractStatisticsHandler extends HandlerWrapper
{
    protected void doStart() throws Exception
    {
        super.doStart();
        statsReset();
    }

    /**
     * Resets the current request statistics.
     */
    public abstract void statsReset();

    /**
     * @return the number of requests handled by this handler
     * since {@link #statsReset()} was last called.
     */
    public abstract int getRequests();

    /**
     * @return the number of requests currently active.
     * since {@link #statsReset()} was last called.
     */
    public abstract int getRequestsActive();

    /**
     * @return the maximum number of active requests
     * since {@link #statsReset()} was last called.
     */
    public abstract int getRequestsActiveMax();

    /**
     * @return the number of responses with a 1xx status returned by this context
     * since {@link #statsReset()} was last called.
     */
    public abstract int getResponses1xx();

    /**
     * @return the number of responses with a 2xx status returned by this context
     * since {@link #statsReset()} was last called.
     */
    public abstract int getResponses2xx();

    /**
     * @return the number of responses with a 3xx status returned by this context
     * since {@link #statsReset()} was last called.
     */
    public abstract int getResponses3xx();

    /**
     * @return the number of responses with a 4xx status returned by this context
     * since {@link #statsReset()} was last called.
     */
    public abstract int getResponses4xx();

    /**
     * @return the number of responses with a 5xx status returned by this context
     * since {@link #statsReset()} was last called.
     */
    public abstract int getResponses5xx();

    /**
     * @return the milliseconds since the statistics were started with {@link #statsReset()}.
     */
    public abstract long getStatsOnMs();

    /**
     * @return the minimum time (in milliseconds) of request handling
     * since {@link #statsReset()} was last called.
     */
    public abstract long getRequestTimeMin();

    /**
     * @return the maximum time (in milliseconds) of request handling
     * since {@link #statsReset()} was last called.
     */
    public abstract long getRequestTimeMax();

    /**
     * @return the total time (in milliseconds) of requests handling
     * since {@link #statsReset()} was last called.
     */
    public abstract long getRequestTimeTotal();

    /**
     * @return the average time (in milliseconds) of request handling
     * since {@link #statsReset()} was last called.
     * @see #getRequestTimeTotal()
     * @see #getRequests()
     */
    public abstract long getRequestTimeAverage();
}
