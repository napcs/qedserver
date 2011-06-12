// ========================================================================
// Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.log;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.mortbay.util.Loader;



/*-----------------------------------------------------------------------*/
/** Logging.
 * This class provides a static logging interface.  If an instance of the 
 * org.slf4j.Logger class is found on the classpath, the static log methods
 * are directed to a slf4j logger for "org.mortbay.log".   Otherwise the logs
 * are directed to stderr.
 * 
 * If the system property VERBOSE is set, then ignored exceptions are logged in detail.
 * 
 */
public class Log 
{    
    private static final String[] __nestedEx =
        {"getTargetException","getTargetError","getException","getRootCause"};
    /*-------------------------------------------------------------------*/
    private static final Class[] __noArgs=new Class[0];
    public final static String EXCEPTION= "EXCEPTION ";
    public final static String IGNORED= "IGNORED";
    public final static String IGNORED_FMT= "IGNORED: {}";
    public final static String NOT_IMPLEMENTED= "NOT IMPLEMENTED ";
    
    public static String __logClass;
    public static boolean __verbose;
    public static boolean __ignored;
    
    private static Logger __log;
    
    static
    {
        AccessController.doPrivileged(new PrivilegedAction() 
            {
                public Object run() 
                { 
                    __logClass = System.getProperty("org.mortbay.log.class","org.mortbay.log.Slf4jLog"); 
                    __verbose = System.getProperty("VERBOSE",null)!=null; 
                    __ignored = System.getProperty("IGNORED",null)!=null; 
                    return new Boolean(true); 
                }
            });
   
        Class log_class=null;
        try
        {
            log_class=Loader.loadClass(Log.class, __logClass);
            __log=(Logger) log_class.newInstance();
        }
        catch(Throwable e)
        {
            log_class = StdErrLog.class;
            __log = new StdErrLog();
            __logClass = log_class.getName();
            if(__verbose)
                e.printStackTrace();
        }
        
        __log.info("Logging to {} via {}",__log,log_class.getName());
    }
    
    public static void setLog(Logger log)
    {
        Log.__log=log;
    }
    
    public static Logger getLog()
    {
        return __log;
    }
    
    
    public static void debug(Throwable th)
    {
        if (__log==null || !isDebugEnabled())
            return;
        __log.debug(EXCEPTION,th);
        unwind(th);
    }

    public static void debug(String msg)
    {
        if (__log==null)
            return;
        __log.debug(msg,null,null);
    }
    
    public static void debug(String msg,Object arg)
    {
        if (__log==null) 
            return;
        __log.debug(msg,arg,null);
    }
    
    public static void debug(String msg,Object arg0, Object arg1)
    {
        if (__log==null)
            return;
        __log.debug(msg,arg0,arg1);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Ignore an exception unless trace is enabled.
     * This works around the problem that log4j does not support the trace level.
     */
    public static void ignore(Throwable th)
    {
        if (__log==null)
            return;
	if (__ignored)
	{
            __log.warn(IGNORED,th);
            unwind(th);
	}
        else if (__verbose)
        {
            __log.debug(IGNORED,th);
            unwind(th);
        }
    }
    
    public static void info(String msg)
    {
        if (__log==null)
            return;
        __log.info(msg,null,null);
    }
    
    public static void info(String msg,Object arg)
    {
        if (__log==null)
            return;
        __log.info(msg,arg,null);
    }
    
    public static void info(String msg,Object arg0, Object arg1)
    {
        if (__log==null)
            return;
        __log.info(msg,arg0,arg1);
    }
    
    public static boolean isDebugEnabled()
    {
        if (__log==null)
            return false;
        return __log.isDebugEnabled();
    }
    
    public static void warn(String msg)
    {
        if (__log==null)
            return;
        __log.warn(msg,null,null);
    }
    
    public static void warn(String msg,Object arg)
    {
        if (__log==null)
            return;
        __log.warn(msg,arg,null);        
    }
    
    public static void warn(String msg,Object arg0, Object arg1)
    {
        if (__log==null)
            return;
        __log.warn(msg,arg0,arg1);        
    }
    
    public static void warn(String msg, Throwable th)
    {
        if (__log==null)
            return;
        __log.warn(msg,th);
        unwind(th);
    }

    public static void warn(Throwable th)
    {
        if (__log==null)
            return;
        __log.warn(EXCEPTION,th);
        unwind(th);
    }

    /** Obtain a named Logger.
     * Obtain a named Logger or the default Logger if null is passed.
     */
    public static Logger getLogger(String name)
    {
        if (__log==null)
            return __log;
        if (name==null)
          return __log;
        return __log.getLogger(name);
    }

    private static void unwind(Throwable th)
    {
        if (th==null)
            return;
        for (int i=0;i<__nestedEx.length;i++)
        {
            try
            {
                Method get_target = th.getClass().getMethod(__nestedEx[i],__noArgs);
                Throwable th2=(Throwable)get_target.invoke(th,(Object[])null);
                if (th2!=null && th2!=th)
                    warn("Nested in "+th+":",th2);
            }
            catch(Exception ignore){}
        }
    }
    

}

