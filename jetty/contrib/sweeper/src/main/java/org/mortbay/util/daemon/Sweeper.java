//========================================================================
//Copyright 2009 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.util.daemon;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Timer;
import java.util.TimerTask;

public class Sweeper implements Runnable, FilenameFilter
{
    
    public static final long DEFAULT_INTERVAL = 3600000; // 1 hour
    
    public static boolean LOG_TO_CONSOLE = true;
    
    public Sweeper()
    {
        // managed by a scheduler
    }
    
    public Sweeper(long interval)
    {
        if(interval==0)
            run();
        else
            schedule(new Timer(), interval);
    }
    
    public Sweeper(Timer timer, long interval)
    {
        schedule(timer, interval);
    }
    
    private void schedule(Timer timer, long interval)
    {
        if (LOG_TO_CONSOLE)
            System.err.println("Sweeping "+System.getProperty("java.io.tmpdir")+" every "+interval+"ms");
        timer.scheduleAtFixedRate(new TimerTask()
        {
            public void run()
            {                
                Sweeper.this.run();
            }
            
        }, 1000, interval);
    }

    public void run()
    {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File[] files = tmpDir.listFiles(this);
        for(int i=0; i<files.length; i++)
        {
            File dir = files[i];
            File sentinel = new File(dir, ".active");
            if(!sentinel.exists() && delete(dir) && LOG_TO_CONSOLE)
                System.err.println("removed dir: " + dir);
        }
    }
    
    public static boolean delete(File file)
    {
        if (file.isDirectory())
        {
            File[] files = file.listFiles();
            for (int i=0; i<files.length; i++)
                delete(files[i]);
        }
        return file.delete();
    }

    public boolean accept(File file, String name)
    {        
        return name.startsWith("Jetty_") && file.isDirectory();
    }
    
    public static void main(String[] args)
    {
        new Sweeper(args.length!=0 ? Long.parseLong(args[0]) : 
            Long.getLong("interval", DEFAULT_INTERVAL).longValue());
    }

}
