// ========================================================================
// Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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
package org.mortbay.servlet;

import java.io.IOException;
import java.net.URL;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.LocalConnector;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.mortbay.log.Log;

public class QoSFilterTest extends TestCase 
{
    private ServletTester _tester;
    private FakeCountDownLatch _doneRequests;
    private final int NUM_CONNECTIONS = 20;
    private final int NUM_LOOPS = 10;
    private final int MAX_QOS = 5;
    
    protected void setUp() throws Exception 
    {
        _tester = new ServletTester();
        _tester.setContextPath("/context");
        _tester.addServlet(TestServlet.class, "/test");
        TestServlet.__maxSleepers=0;
        TestServlet.__sleepers=0;
        
        _doneRequests = new FakeCountDownLatch(NUM_CONNECTIONS*NUM_LOOPS);
        
        _tester.start();
    }
        
    protected void tearDown() throws Exception 
    {
        _tester.stop();
    }

    public void testNoFilter() throws Exception
    {    
        for(int i = 0; i < NUM_CONNECTIONS; ++i )
        {
            new Thread(new Worker(i)).start();
        }
        
        _doneRequests.await(10);
        
        if (TestServlet.__maxSleepers<=MAX_QOS)
            Log.warn("TEST WAS NOT PARALLEL ENOUGH!");
        assertTrue(TestServlet.__maxSleepers<=NUM_CONNECTIONS);
    }

    public void testBlockingQosFilter() throws Exception
    {    
        FilterHolder holder = new FilterHolder(QoSFilter2.class);
        holder.setInitParameter(QoSFilter.MAX_REQUESTS_INIT_PARAM, ""+MAX_QOS);
        _tester.getContext().getServletHandler().addFilterWithMapping(holder,"/*",Handler.REQUEST);
        
        for(int i = 0; i < NUM_CONNECTIONS; ++i )
        {
            new Thread(new Worker(i)).start();
        }
        
        _doneRequests.await(10);
        if (TestServlet.__maxSleepers<MAX_QOS)
            Log.warn("TEST WAS NOT PARALLEL ENOUGH!");
        assertTrue(TestServlet.__maxSleepers<=MAX_QOS);
    }

    public void testQosFilter() throws Exception
    {    
        FilterHolder holder = new FilterHolder(QoSFilter2.class);
        holder.setInitParameter(QoSFilter.MAX_REQUESTS_INIT_PARAM, ""+MAX_QOS);
        _tester.getContext().getServletHandler().addFilterWithMapping(holder,"/*",Handler.REQUEST);
        
        for(int i = 0; i < NUM_CONNECTIONS; ++i )
        {
            new Thread(new Worker2(i)).start();
        }
        
        _doneRequests.await(10);
        if (TestServlet.__maxSleepers<MAX_QOS)
            Log.warn("TEST WAS NOT PARALLEL ENOUGH!");
        assertTrue(TestServlet.__maxSleepers<=MAX_QOS);
    }
    
    class Worker implements Runnable {
        private int _num;
        public Worker(int num)
        {
            _num = num;
        }

        public void run()
        {
            try
            {
                LocalConnector connector = _tester.createLocalConnector();
                for (int i=0;i<NUM_LOOPS;i++)
                {
                    HttpTester request = new HttpTester();
                    HttpTester response = new HttpTester();

                    request.setMethod("GET");
                    request.setHeader("host", "tester");
                    request.setURI("/context/test?priority="+(_num%QoSFilter.__DEFAULT_MAX_PRIORITY)+"&n="+_num+"&l="+i);
                    request.setHeader("num", _num+"");
                    try
                    {
                        String responseString = _tester.getResponses(request.generate(), connector);
                        int index=-1;
                        if((index = responseString.indexOf("HTTP", index+1))!=-1)
                        {
                            responseString = response.parse(responseString);
                            _doneRequests.countDown();
                        }
                    }
                    catch (IOException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    catch (Exception e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    
    class Worker2 implements Runnable {
        private int _num;
        public Worker2(int num)
        {
            _num = num;
        }

        public void run()
        {
            try
            {
                String addr = _tester.createSocketConnector(true);
                for (int i=0;i<NUM_LOOPS;i++)
                {
                    URL url=new URL(addr+"/context/test?priority="+(_num%QoSFilter.__DEFAULT_MAX_PRIORITY)+"&n="+_num+"&l="+i);
                    url.getContent();
                    _doneRequests.countDown();
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    
    public static class TestServlet extends HttpServlet implements Servlet
    {
        private static int __sleepers;
        private static int __maxSleepers;
         
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            try
            {
                synchronized(TestServlet.class)
                {
                    __sleepers++;
                    if(__sleepers > __maxSleepers)
                        __maxSleepers = __sleepers;
                }

                Thread.sleep(200);

                synchronized(TestServlet.class)
                {
                    __sleepers--;
                    if(__sleepers > __maxSleepers)
                        __maxSleepers = __sleepers;
                }

                response.setContentType("text/plain");
                response.getWriter().println("DONE!");     
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                response.sendError(500);
            }           
        }
    }
    
    public static class QoSFilter2 extends QoSFilter
    {
        public int getPriority(ServletRequest request)
        {
            String p = ((HttpServletRequest)request).getParameter("priority");
            if (p!=null)
                return Integer.parseInt(p);
            return 0;
        }
    }
    
    static class FakeCountDownLatch
    {
        int _latch;

        public FakeCountDownLatch(int i)
        {
            _latch=i;
        }

        public void countDown()
        {
            synchronized(this)
            {
                _latch--;
                if (_latch<=0)
                    this.notifyAll();
            }
        }

        public void await(int seconds)
        {
            long wait_until = System.currentTimeMillis()+seconds*1000;
            synchronized(this)
            {
                while(_latch>0 && System.currentTimeMillis()<wait_until)
                {
                    try
                    {
                        this.wait(1000);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        
    }
    
}
