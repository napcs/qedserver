//========================================================================
//$Id: Slf4jLog.java,v 1.1 2005/11/14 16:55:09 gregwilkins Exp $
//Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.log;


public class Slf4jLog implements Logger
{
    private org.slf4j.Logger logger;


    public Slf4jLog() throws Exception
    {
        this("org.mortbay.log");
    }
    
    public Slf4jLog(String name)
    {
        logger = org.slf4j.LoggerFactory.getLogger( name );
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.log.Log#doDebug(java.lang.String, java.lang.Object, java.lang.Object)
     */
    public void debug(String msg, Object arg0, Object arg1)
    {
        logger.debug(msg, arg0, arg1);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.log.Log#doDebug(java.lang.String, java.lang.Throwable)
     */
    public void debug(String msg, Throwable th)
    {
        logger.debug(msg, th);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.log.Log#doDebugEnabled()
     */
    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.log.Log#doInfo(java.lang.String, java.lang.Object, java.lang.Object)
     */
    public void info(String msg, Object arg0, Object arg1)
    {
        logger.info(msg, arg0, arg1);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.log.Log#doWarn(java.lang.String, java.lang.Object, java.lang.Object)
     */
    public void warn(String msg, Object arg0, Object arg1)
    {
        logger.warn(msg, arg0, arg1);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.log.Log#doWarn(java.lang.String, java.lang.Throwable)
     */
    public void warn(String msg, Throwable th)
    {

        if (th instanceof RuntimeException || th instanceof Error)
            logger.error(msg, th);
        else
            logger.warn(msg,th);

    }

    /* ------------------------------------------------------------ */
    public Logger getLogger(String name)
    {
        return new Slf4jLog(name);

    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return logger.toString();
    }

    /* ------------------------------------------------------------ */
    public void setDebugEnabled(boolean enabled)
    {
        warn("setDebugEnabled not implemented",null,null);
    }
}