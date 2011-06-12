package org.mortbay.jetty.handler;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Response;

/**
 * @version $Revision$ $Date: 2009-06-17 07:36:08 +1000 (Wed, 17 Jun 2009) $
 */
public class AtomicStatisticsHandler extends AbstractStatisticsHandler
{
    private transient final AtomicLong _statsStartedAt = new AtomicLong();
    private transient final AtomicInteger _requests = new AtomicInteger();
    private transient final AtomicLong _minRequestTime = new AtomicLong();
    private transient final AtomicLong _maxRequestTime = new AtomicLong();
    private transient final AtomicLong _totalRequestTime = new AtomicLong();
    private transient final AtomicInteger _requestsActive = new AtomicInteger();
    private transient final AtomicInteger _requestsActiveMax = new AtomicInteger();
    private transient final AtomicInteger _responses1xx = new AtomicInteger();
    private transient final AtomicInteger _responses2xx = new AtomicInteger();
    private transient final AtomicInteger _responses3xx = new AtomicInteger();
    private transient final AtomicInteger _responses4xx = new AtomicInteger();
    private transient final AtomicInteger _responses5xx = new AtomicInteger();

    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
    {
        _requests.incrementAndGet();
        int actives = _requestsActive.incrementAndGet();
        // Update the max value, using a non-blocking algorithm
        int oldMaxActives = _requestsActiveMax.get();
        while (actives > oldMaxActives)
        {
            if (_requestsActiveMax.compareAndSet(oldMaxActives, actives)) break;
            oldMaxActives = _requestsActiveMax.get();
        }

        long requestStartTime = System.currentTimeMillis();
        try
        {
            super.handle(target, request, response, dispatch);
        }
        finally
        {
            long requestTime = System.currentTimeMillis() - requestStartTime;

            // Set to 0 if the value is negative, using a non-blocking algorithm
            actives = _requestsActive.decrementAndGet();
            while (actives < 0)
            {
                if (_requestsActive.compareAndSet(actives, 0)) break;
                actives = _requestsActive.get();
            }

            // Update the times, using a non-blocking algorithm
            long oldMinTime = _minRequestTime.get();
            while (requestTime < oldMinTime)
            {
                if (_minRequestTime.compareAndSet(oldMinTime, requestTime)) break;
                oldMinTime = _minRequestTime.get();
            }
            long oldMaxTime = _maxRequestTime.get();
            while (requestTime > oldMaxTime)
            {
                if (_maxRequestTime.compareAndSet(oldMaxTime, requestTime)) break;
                oldMaxTime = _maxRequestTime.get();
            }
            _totalRequestTime.addAndGet(requestTime);

            Response jettyResponse = (response instanceof Response) ? (Response) response : HttpConnection.getCurrentConnection().getResponse();
            switch (jettyResponse.getStatus() / 100)
            {
                case 1:
                    _responses1xx.incrementAndGet();
                    break;
                case 2:
                    _responses2xx.incrementAndGet();
                    break;
                case 3:
                    _responses3xx.incrementAndGet();
                    break;
                case 4:
                    _responses4xx.incrementAndGet();
                    break;
                case 5:
                    _responses5xx.incrementAndGet();
                    break;
                default:
                    break;
            }
        }
    }

    public void statsReset()
    {
        _statsStartedAt.set(System.currentTimeMillis());
        _requests.set(0);
        _minRequestTime.set(Long.MAX_VALUE);
        _maxRequestTime.set(0L);
        _totalRequestTime.set(0L);
        _requestsActive.set(0);
        _requestsActiveMax.set(0);
        _responses1xx.set(0);
        _responses2xx.set(0);
        _responses3xx.set(0);
        _responses4xx.set(0);
        _responses5xx.set(0);
    }

    public int getRequests()
    {
        return _requests.get();
    }

    public int getRequestsActive()
    {
        return _requestsActive.get();
    }

    public int getRequestsActiveMax()
    {
        return _requestsActiveMax.get();
    }

    public int getResponses1xx()
    {
        return _responses1xx.get();
    }

    public int getResponses2xx()
    {
        return _responses2xx.get();
    }

    public int getResponses3xx()
    {
        return _responses3xx.get();
    }

    public int getResponses4xx()
    {
        return _responses4xx.get();
    }

    public int getResponses5xx()
    {
        return _responses5xx.get();
    }

    public long getStatsOnMs()
    {
        return System.currentTimeMillis() - _statsStartedAt.get();
    }

    public long getRequestTimeMin()
    {
        return _minRequestTime.get();
    }

    public long getRequestTimeMax()
    {
        return _maxRequestTime.get();
    }

    public long getRequestTimeTotal()
    {
        return _totalRequestTime.get();
    }

    public long getRequestTimeAverage()
    {
        int requests = getRequests();
        return requests == 0 ? 0 : getRequestTimeTotal() / requests;
    }
}
