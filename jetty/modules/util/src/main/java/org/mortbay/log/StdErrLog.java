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

import org.mortbay.util.DateCache;

/*-----------------------------------------------------------------------*/
/** StdErr Logging.
 * This implementation of the Logging facade sends all logs to StdErr with minimal formatting.
 * 
 * If the system property DEBUG is set, then debug logs are printed if stderr is being used.
 * 
 */
public class StdErrLog implements Logger
{    
    private static DateCache _dateCache;
    private static boolean __debug = System.getProperty("DEBUG",null)!=null;
    private String _name;
    
    StringBuffer _buffer = new StringBuffer();
    
    static
    {
        try
        {
            _dateCache=new DateCache("yyyy-MM-dd HH:mm:ss");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
    }
    
    public StdErrLog()
    {
        this(null);
    }
    
    public StdErrLog(String name)
    {    
        this._name=name==null?"":name;
    }
    
    public boolean isDebugEnabled()
    {
        return __debug;
    }
    
    public void setDebugEnabled(boolean enabled)
    {
        __debug=enabled;
    }
    
    public void info(String msg,Object arg0, Object arg1)
    {
        String d=_dateCache.now();
        int ms=_dateCache.lastMs();
        synchronized(_buffer)
        {
            tag(d,ms,":INFO:");
            format(msg,arg0,arg1);
            System.err.println(_buffer.toString());
        }
    }
    
    public void debug(String msg,Throwable th)
    {
        if (__debug)
        {
            String d=_dateCache.now();
            int ms=_dateCache.lastMs();
            synchronized(_buffer)
            {
                tag(d,ms,":DBUG:");
                format(msg);
                format(th);
                System.err.println(_buffer.toString());
            }
        }
    }
    
    public void debug(String msg,Object arg0, Object arg1)
    {
        if (__debug)
        {
            String d=_dateCache.now();
            int ms=_dateCache.lastMs();
            synchronized(_buffer)
            {
                tag(d,ms,":DBUG:");
                format(msg,arg0,arg1);
                System.err.println(_buffer.toString());
            }
        }
    }
    
    public void warn(String msg,Object arg0, Object arg1)
    {
        String d=_dateCache.now();
        int ms=_dateCache.lastMs();
        synchronized(_buffer)
        {
            tag(d,ms,":WARN:");
            format(msg,arg0,arg1);
            System.err.println(_buffer.toString());
        }
    }
    
    public void warn(String msg, Throwable th)
    {
        String d=_dateCache.now();
        int ms=_dateCache.lastMs();
        synchronized(_buffer)
        {
            tag(d,ms,":WARN:");
            format(msg);
            format(th);
            System.err.println(_buffer.toString());
        }
    }
    
    private void tag(String d,int ms,String tag)
    {
        _buffer.setLength(0);
        _buffer.append(d);
        if (ms>99)
            _buffer.append('.');
        else if (ms>9)
            _buffer.append(".0");
        else
            _buffer.append(".00");
        _buffer.append(ms).append(tag).append(_name).append(':');
    }
    
    private void format(String msg, Object arg0, Object arg1)
    {
        int i0=msg==null?-1:msg.indexOf("{}");
        int i1=i0<0?-1:msg.indexOf("{}",i0+2);
        
        if (i0>=0)
        {
            format(msg.substring(0,i0));
            format(String.valueOf(arg0==null?"null":arg0));
            
            if (i1>=0)
            {
                format(msg.substring(i0+2,i1));
                format(String.valueOf(arg1==null?"null":arg1));
                format(msg.substring(i1+2));
            }
            else
            {
                format(msg.substring(i0+2));
                if (arg1!=null)
                {
                    _buffer.append(' ');
                    format(String.valueOf(arg1));
                }
            }
        }
        else
        {
            format(msg);
            if (arg0!=null)
            {
                _buffer.append(' ');
                format(String.valueOf(arg0));
            }
            if (arg1!=null)
            {
                _buffer.append(' ');
                format(String.valueOf(arg1));
            }
        }
    }
    
    private void format(String msg)
    {
        if (msg == null)
            _buffer.append("null");
        else
            for (int i=0;i<msg.length();i++)
            {
                char c=msg.charAt(i);
                if (Character.isISOControl(c))
                {
                    if (c=='\n')
                        _buffer.append('|');
                    else if (c=='\r')
                        _buffer.append('<');
                    else
                        _buffer.append('?');
                }
                else
                    _buffer.append(c);
            }
    }
    
    private void format(Throwable th)
    {
        if (th == null)
            _buffer.append("null");
        else
        {
            _buffer.append('\n');
            format(th.toString());
            StackTraceElement[] elements = th.getStackTrace();
            for (int i=0;elements!=null && i<elements.length;i++)
            {
                _buffer.append("\n\tat ");
                format(elements[i].toString());
            }
        }
    }
    
    public Logger getLogger(String name)
    {
        if ((name==null && this._name==null) ||
            (name!=null && name.equals(this._name)))
            return this;
        return new StdErrLog(name);
    }
    
    public String toString()
    {
        return "STDERR"+_name;
    }
    

}

