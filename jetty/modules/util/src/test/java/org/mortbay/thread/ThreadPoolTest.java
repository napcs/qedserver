//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.thread;

import java.util.Random;

import junit.framework.TestCase;

public class ThreadPoolTest extends TestCase
{
    int _jobs;
    long _result;
    volatile long _sleep=100;
    
    Runnable _job = new Runnable()
    {
        public void run()
        {
            try 
            {
                Thread.sleep(_sleep);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            
            long t = System.currentTimeMillis()%10000;
            long r=t;
            for (int i=0;i<t;i++)
                r+=i;
                
            synchronized(ThreadPoolTest.class)
            {
                _jobs++;
                _result+=r;
            }
        }
        
    };
    
    
    public void testQueuedThreadPool() throws Exception
    {
        _sleep=100;
        QueuedThreadPool tp= new QueuedThreadPool();
        tp.setMinThreads(5);
        tp.setMaxThreads(10);
        tp.setMaxIdleTimeMs(1000);
        tp.setSpawnOrShrinkAt(2);
        tp.setThreadsPriority(Thread.NORM_PRIORITY-1);
        
        tp.start();
        Thread.sleep(500);
     
        assertEquals(5,tp.getThreads());
        assertEquals(5,tp.getIdleThreads());
        tp.dispatch(_job);
        tp.dispatch(_job);
        assertEquals(5,tp.getThreads());
        assertEquals(3,tp.getIdleThreads());
        Thread.sleep(500);
        assertEquals(5,tp.getThreads());
        assertEquals(5,tp.getIdleThreads());
        
        for (int i=0;i<100;i++)
            tp.dispatch(_job);

        
        assertTrue(tp.getQueueSize()>10);
        assertTrue(tp.getIdleThreads()<=1);

        Thread.sleep(2000);

        assertEquals(0,tp.getQueueSize());
        assertTrue(tp.getIdleThreads()>5);
        
        int threads=tp.getThreads();
        assertTrue(threads>5);
        Thread.sleep(1500);
        assertTrue(tp.getThreads()<threads);
    }

    
    public void testQueuedThreadPoolShrink() throws Exception
    {
        QueuedThreadPool tp= new QueuedThreadPool();
        tp.setMinThreads(2);
        tp.setMaxThreads(10);
        tp.setMaxIdleTimeMs(400);
        tp.setSpawnOrShrinkAt(2);
        tp.setThreadsPriority(Thread.NORM_PRIORITY-1);
        
        tp.start();
        Thread.sleep(100);
        assertEquals(2,tp.getThreads());
        assertEquals(2,tp.getIdleThreads());
        _sleep=200;
        tp.dispatch(_job);
        tp.dispatch(_job);
        for (int i=0;i<20;i++)
            tp.dispatch(_job);
        Thread.sleep(100);
        assertEquals(10,tp.getThreads());
        assertEquals(0,tp.getIdleThreads());
        
        _sleep=1;
        for (int i=0;i<500;i++)
        {
            tp.dispatch(_job);
            Thread.sleep(10);
            if (i%100==0)
            {
                System.err.println(i+" threads="+tp.getThreads()+" idle="+tp.getIdleThreads());
            }
        }
        System.err.println("500 threads="+tp.getThreads()+" idle="+tp.getIdleThreads());
        assertEquals(2,tp.getThreads());
        assertEquals(2,tp.getIdleThreads());
        
    }
    
    public void testStress() throws Exception
    {
        _sleep=100;
        boolean stress=Boolean.getBoolean("STRESS");
        
        QueuedThreadPool tp= new QueuedThreadPool();
        tp.setMinThreads(stress?240:24);
        tp.setMaxThreads(stress?250:25);
        tp.setMaxIdleTimeMs(100);
        tp.start();

        tp.setMinThreads(stress?90:9);
        final int[] count={0};
        
        final Random random = new Random(System.currentTimeMillis());
        int loops = stress?16000:1600;

        try
        {
            for (int i=0;i<loops;)
            {
                int burst=random.nextInt(100);
                for (int b=0;b<burst && i<loops; b++)
                {
                    if (i%20==0)
                        System.err.print('.');
                    if (i%1600==1599)
                        System.err.println();
                    if (i==1000)
                        tp.setMinThreads(10);
                    
                    if (i==10000)
                        tp.setMaxThreads(20);
                    
                    i++;
                    tp.dispatch(new Runnable()
                    {
                        public void run()
                        {
                            int s=random.nextInt(40)+10;
                            try
                            {
                                Thread.sleep(s);
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                            finally
                            {
                                synchronized (ThreadPoolTest.this)
                                {
                                    count[0]++;
                                }
                            }
                        }
                    });
                }

                Thread.sleep(random.nextInt(100));
            }
            
            int waits=0;
            while(true)
            {
                synchronized (ThreadPoolTest.this)
                {
                    if (loops==count[0] || waits++>10)
                        break;
                }
                Thread.sleep(500);
            }
            
            synchronized (ThreadPoolTest.this)
            {
                assertEquals(loops,count[0]);
            }
            
            tp.stop();
            Thread.sleep(100);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public void testMaxStopTime() throws Exception
    {
        QueuedThreadPool tp= new QueuedThreadPool();
        tp.setMaxStopTimeMs(500);
        tp.start();
        tp.dispatch(new Runnable(){
            public void run () {
                while (true) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ie) {}
                }
            }
        });

        long beforeStop = System.currentTimeMillis();
        tp.stop();
        long afterStop = System.currentTimeMillis();
        assertTrue(tp.isStopped());
        assertTrue(afterStop - beforeStop < 1000);
    }

}
