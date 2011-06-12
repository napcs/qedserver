// ========================================================================
// Copyright 2002-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.setuid;

import java.io.File;


/**
 * Class is for changing user and groupId, it can also be use to retrieve user information by using getpwuid(uid) or getpwnam(username) of both linux and unix systems
 * @author Greg Wilkins
 * @author Leopoldo Lee Agdeppa III
 */

public class SetUID
{
    public static final int OK = 0;
    public static final int ERROR = -1;

    public static native int setumask(int mask);
    public static native int setuid(int uid);
    public static native int setgid(int gid);
    
    public static native Passwd getpwnam(String name) throws SecurityException;
    public static native Passwd getpwuid(int uid) throws SecurityException;
    
    public static native Group getgrnam(String name) throws SecurityException;
    public static native Group getgrgid(int gid) throws SecurityException;    
    
    public static native RLimit getrlimitnofiles();
    public static native int setrlimitnofiles(RLimit rlimit);
    
    private static void loadLibrary()
    {
        // load libjettysetuid.so ${jetty.libsetuid.path} 
        try 
        {
            if(System.getProperty("jetty.libsetuid.path") != null)
            {
                File lib = new File(System.getProperty("jetty.libsetuid.path"));
                if(lib.exists())
                {
                    System.load(lib.getCanonicalPath());
                }
                return;
            }
            
        } 
        catch (Throwable e) 
        {
           //Ignorable if there is another way to find the lib 
	   if (Boolean.valueOf(System.getProperty("DEBUG")).booleanValue())
	       e.printStackTrace();
        }
        
        try 
        {
            System.loadLibrary("setuid");
            return;
        } 
        catch (Throwable e) 
        {
           //Ignorable if ther eis another way to find the lib
	   if (Boolean.valueOf(System.getProperty("DEBUG")).booleanValue())
	       e.printStackTrace();
        }
        
        // try to load from usual path @ jetty.home/lib/ext
        try 
        {
            if(System.getProperty("jetty.home") != null)
            {
                File lib = new File(System.getProperty("jetty.home") + "/lib/ext/libsetuid.so");
                if(lib.exists())
                {
                    System.load(lib.getCanonicalPath());
                }
                return;
            }
            
        } 
        catch (Throwable e) 
        {
	   if (Boolean.valueOf(System.getProperty("DEBUG")).booleanValue())
	       e.printStackTrace();
        }
        
        // try to load from jetty.lib where rpm puts this file
        try 
        {
            if(System.getProperty("jetty.lib") != null)
            {
                File lib = new File(System.getProperty("jetty.lib") + "/libsetuid.so");
                if(lib.exists())
                {
                    System.load(lib.getCanonicalPath());
                }
                return;
            }
            
        } 
        catch (Throwable e) 
        {
	   if (Boolean.valueOf(System.getProperty("DEBUG")).booleanValue())
	       e.printStackTrace();
        }

        System.err.println("Error: libsetuid.so could not be found");
    }
    
    
    static 
    {
    	loadLibrary();
    }
    
}
