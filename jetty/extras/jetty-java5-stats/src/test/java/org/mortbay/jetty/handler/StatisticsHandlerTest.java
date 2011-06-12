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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.util.IO;

/**
 * @version $Revision: 5303 $ $Date: 2009-07-03 12:07:03 +1000 (Fri, 03 Jul 2009) $
 */
public class StatisticsHandlerTest extends TestCase
{
    private final int workerCount = 100;
    private final int requestCount = 50;

    public void testSynchronizedStatisticsHandler() throws Exception
    {
        StatisticsHandler statisticsHandler = new StatisticsHandler();
        runTestWithStatisticsHandler(statisticsHandler);
    }

    public void testAtomicStatisticsHandler() throws Exception
    {
        AtomicStatisticsHandler statisticsHandler = new AtomicStatisticsHandler();
        runTestWithStatisticsHandler(statisticsHandler);
    }

    private void runTestWithStatisticsHandler(AbstractStatisticsHandler statisticsHandler) throws Exception
    {
        Server server = new Server();
        Connector connector = new SelectChannelConnector();
        server.setConnectors(new Connector[]{connector});
        server.setHandler(statisticsHandler);
        TestHandler test = new TestHandler();
        statisticsHandler.setHandler(test);
        server.start();
        int serverPort = connector.getLocalPort();
        try
        {
            AtomicInteger count = new AtomicInteger();
            test.count=count;
            
            CountDownLatch latch = new CountDownLatch(workerCount);
            
            Worker[] workers = new Worker[workerCount];
            for (int i = 0; i < workers.length; ++i) 
                workers[i] = new Worker(i, serverPort, requestCount, latch);
            
            long start = System.nanoTime();
            for (Worker worker : workers) 
                worker.start();
            
            boolean latched = latch.await(100 * requestCount, TimeUnit.MILLISECONDS);
            long end = System.nanoTime();
            Thread.sleep(1000);

            assertTrue(latched);
            assertEquals(workers.length * requestCount,count.get());
            assertEquals(workers.length * requestCount, statisticsHandler.getRequests());
            assertEquals(workers.length * requestCount, statisticsHandler.getResponses2xx());
            System.out.println(statisticsHandler.getClass().getSimpleName() + " - " + workerCount + " threads: " + TimeUnit.NANOSECONDS.toMillis(end - start) + " ms");
        }
        finally
        {
            server.stop();
        }
    }

    private static class Worker extends Thread
    {
        private final int id;
        private final int port;
        private final int requestCount;
        private final CountDownLatch latch;

        private Worker(int id, int port, int requestCount, CountDownLatch latch)
        {
            this.id = id;
            this.port = port;
            this.requestCount = requestCount;
            this.latch = latch;
        }

        public void run()
        {
            for (int i = 0; i < requestCount; i++)
            {
                try
                {
                    Socket socket = new Socket(InetAddress.getByName(null), port);
                    OutputStream output = socket.getOutputStream();
                    
                    for (int j=20;j-->0 && i+1<requestCount;)
                    {
                        String request = "GET /?id=" + id + " HTTP/1.1\r\n" + "Host: localhost\r\n\r\n";
                        output.write(request.getBytes("UTF-8"));
                        output.flush();
                        i++;
                    }

                    String request = "GET /?id=" + id + " HTTP/1.1\r\n" + "Host: localhost\r\nConnection: close\r\n\r\n";
                    output.write(request.getBytes("UTF-8"));
                    output.flush();
                    
                    InputStream input = socket.getInputStream();
                    String responses = IO.toString(input);
                    socket.close();
                }
                catch (Exception x)
                {
                    x.printStackTrace();
                    break;
                }
            }
            latch.countDown();
        }
    }

    private class TestHandler extends AbstractHandler
    {
        AtomicInteger count;
        public void handle(String target, HttpServletRequest servletRequest, HttpServletResponse servletResponse, int dispatch) throws IOException, ServletException
        {
            count.incrementAndGet();
            Request request = (Request)servletRequest;
            request.setHandled(true);
            servletResponse.setStatus(HttpServletResponse.SC_OK);
        }
    }
}
