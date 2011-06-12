//========================================================================
//$Id: JettyLog.java 4185 2008-12-12 17:49:55Z janb $
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
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

package com.sun.org.apache.commons.logging.impl;

import com.sun.org.apache.commons.logging.Log;

/**
 * Log
 * 
 * Bridges the com.sun.org.apache.commons.logging.Log to Jetty's log.
 *
 **/
public class JettyLog implements Log
{
    private String _name;
    private org.mortbay.log.Logger _logger;
    
    /**
     * 
     */
    public JettyLog(String name)
    {
        _name = name;
        _logger = org.mortbay.log.Log.getLogger(name);
    }
    public  void fatal (Object message)
    {
        _logger.warn(message.toString(), null, null);
    }
    
    public  void fatal (Object message, Throwable t)
    {
        _logger.warn(message.toString(), t);
    }
    
    public  void debug(Object message)
    {
        _logger.debug(message.toString(), null);
    }
    
    public  void debug (Object message, Throwable t)
    {
        _logger.debug(message.toString(), t);
    }
    
    public  void trace (Object message)
    {
        _logger.debug(message.toString(), null);
    }
    
  
    public  void info(Object message)
    {
       _logger.info(message.toString(), null, null);
    }

    public  void error(Object message)
    {
       _logger.warn(message.toString(), null);
    }
    
    public  void error(Object message, Throwable cause)
    {
        _logger.warn(message.toString(), cause);
    }

    public  void warn(Object message)
    {
        _logger.warn(message.toString(), null);
    }
    
    public  boolean isDebugEnabled ()
    {
        return _logger.isDebugEnabled();
    }
    
    public  boolean isWarnEnabled ()
    {
        return _logger.isDebugEnabled();
    }
    
    public  boolean isInfoEnabled ()
    {
        return true;
    }
    
    
    public  boolean isErrorEnabled ()
    {
        return true;
    }
    
  
    public  boolean isTraceEnabled ()
    {
        return _logger.isDebugEnabled();
    }
    
}
