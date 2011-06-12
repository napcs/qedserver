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

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Response;

public class StatisticsHandler extends AbstractStatisticsHandler
{
    private transient long _statsStartedAt;
    private transient int _requests;
    private transient long _minRequestTime;
    private transient long _maxRequestTime;
    private transient long _totalRequestTime;
    private transient int _requestsActive;
    private transient int _requestsActiveMax;
    private transient int _responses1xx; // Informal
    private transient int _responses2xx; // Success
    private transient int _responses3xx; // Redirection
    private transient int _responses4xx; // Client Error
    private transient int _responses5xx; // Server Error

    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
    {
        synchronized (this)
        {
            ++_requests;
            ++_requestsActive;
            if (_requestsActive > _requestsActiveMax)
                _requestsActiveMax = _requestsActive;
        }

        long requestStartTime = System.currentTimeMillis();
        try
        {
            super.handle(target, request, response, dispatch);
        }
        finally
        {
            long requestTime = System.currentTimeMillis() - requestStartTime;

            synchronized (this)
            {
                _requestsActive--;
                if (_requestsActive < 0) _requestsActive = 0;

                _totalRequestTime += requestTime;
                if (requestTime < _minRequestTime || _minRequestTime == 0)
                    _minRequestTime = requestTime;
                if (requestTime > _maxRequestTime)
                    _maxRequestTime = requestTime;

                Response jettyResponse = (response instanceof Response) ? (Response) response : HttpConnection.getCurrentConnection().getResponse();
                switch (jettyResponse.getStatus() / 100)
                {
                    case 1:
                        _responses1xx++;
                        break;
                    case 2:
                        _responses2xx++;
                        break;
                    case 3:
                        _responses3xx++;
                        break;
                    case 4:
                        _responses4xx++;
                        break;
                    case 5:
                        _responses5xx++;
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public void statsReset()
    {
        synchronized (this)
        {
            _statsStartedAt = System.currentTimeMillis();
            _requests = 0;
            _minRequestTime = 0;
            _maxRequestTime = 0;
            _totalRequestTime = 0;
            _requestsActiveMax = _requestsActive;
            _requestsActive = 0;
            _responses1xx = 0;
            _responses2xx = 0;
            _responses3xx = 0;
            _responses4xx = 0;
            _responses5xx = 0;
        }
    }

    public int getRequests()
    {
        synchronized (this)
        {
            return _requests;
        }
    }

    public int getRequestsActive()
    {
        synchronized (this)
        {
            return _requestsActive;
        }
    }

    public int getRequestsActiveMax()
    {
        synchronized (this)
        {
            return _requestsActiveMax;
        }
    }

    public int getResponses1xx()
    {
        synchronized (this)
        {
            return _responses1xx;
        }
    }

    public int getResponses2xx()
    {
        synchronized (this)
        {
            return _responses2xx;
        }
    }

    public int getResponses3xx()
    {
        synchronized (this)
        {
            return _responses3xx;
        }
    }

    public int getResponses4xx()
    {
        synchronized (this)
        {
            return _responses4xx;
        }
    }

    public int getResponses5xx()
    {
        synchronized (this)
        {
            return _responses5xx;
        }
    }

    public long getStatsOnMs()
    {
        synchronized (this)
        {
            return System.currentTimeMillis() - _statsStartedAt;
        }
    }

    public long getRequestTimeMin()
    {
        synchronized (this)
        {
            return _minRequestTime;
        }
    }

    public long getRequestTimeMax()
    {
        synchronized (this)
        {
            return _maxRequestTime;
        }
    }

    public long getRequestTimeTotal()
    {
        synchronized (this)
        {
            return _totalRequestTime;
        }
    }

    public long getRequestTimeAverage()
    {
        synchronized (this)
        {
            return _requests == 0 ? 0 : (_totalRequestTime / _requests);
        }
    }
}
