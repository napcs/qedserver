// ========================================================================
// Copyright 1999-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.util;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;


/* ------------------------------------------------------------ */
/** Wraps multiple exceptions.
 *
 * Allows multiple exceptions to be thrown as a single exception.
 *
 * @author Greg Wilkins (gregw)
 */
public class MultiException extends Exception
{
    private Object nested;

    /* ------------------------------------------------------------ */
    public MultiException()
    {
        super("Multiple exceptions");
    }

    /* ------------------------------------------------------------ */
    public void add(Throwable e)
    {
        if (e instanceof MultiException)
        {
            MultiException me = (MultiException)e;
            for (int i=0;i<LazyList.size(me.nested);i++)
                nested=LazyList.add(nested,LazyList.get(me.nested,i));
        }
        else
            nested=LazyList.add(nested,e);
    }

    /* ------------------------------------------------------------ */
    public int size()
    {
        return LazyList.size(nested);
    }
    
    /* ------------------------------------------------------------ */
    public List getThrowables()
    {
        return LazyList.getList(nested);
    }
    
    /* ------------------------------------------------------------ */
    public Throwable getThrowable(int i)
    {
        return (Throwable) LazyList.get(nested,i);
    }

    /* ------------------------------------------------------------ */
    /** Throw a multiexception.
     * If this multi exception is empty then no action is taken. If it
     * contains a single exception that is thrown, otherwise the this
     * multi exception is thrown. 
     * @exception Exception 
     */
    public void ifExceptionThrow()
        throws Exception
    {
        switch (LazyList.size(nested))
        {
          case 0:
              break;
          case 1:
              Throwable th=(Throwable)LazyList.get(nested,0);
              if (th instanceof Error)
                  throw (Error)th;
              if (th instanceof Exception)
                  throw (Exception)th;
          default:
              throw this;
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Throw a Runtime exception.
     * If this multi exception is empty then no action is taken. If it
     * contains a single error or runtime exception that is thrown, otherwise the this
     * multi exception is thrown, wrapped in a runtime exception. 
     * @exception Error If this exception contains exactly 1 {@link Error} 
     * @exception RuntimeException If this exception contains 1 {@link Throwable} but it is not an error,
     *                             or it contains more than 1 {@link Throwable} of any type.
     */
    public void ifExceptionThrowRuntime()
        throws Error
    {
        switch (LazyList.size(nested))
        {
          case 0:
              break;
          case 1:
              Throwable th=(Throwable)LazyList.get(nested,0);
              if (th instanceof Error)
                  throw (Error)th;
              else if (th instanceof RuntimeException)
                  throw (RuntimeException)th;
              else
                  throw new RuntimeException(th);
          default:
              throw new RuntimeException(this);
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Throw a multiexception.
     * If this multi exception is empty then no action is taken. If it
     * contains a any exceptions then this
     * multi exception is thrown. 
     */
    public void ifExceptionThrowMulti()
        throws MultiException
    {
        if (LazyList.size(nested)>0)
            throw this;
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        if (LazyList.size(nested)>0)
            return "org.mortbay.util.MultiException"+
                LazyList.getList(nested);
        return "org.mortbay.util.MultiException[]";
    }

    /* ------------------------------------------------------------ */
    public void printStackTrace()
    {
        super.printStackTrace();
        for (int i=0;i<LazyList.size(nested);i++)
            ((Throwable)LazyList.get(nested,i)).printStackTrace();
    }
   

    /* ------------------------------------------------------------------------------- */
    /**
     * @see java.lang.Throwable#printStackTrace(java.io.PrintStream)
     */
    public void printStackTrace(PrintStream out)
    {
        super.printStackTrace(out);
        for (int i=0;i<LazyList.size(nested);i++)
            ((Throwable)LazyList.get(nested,i)).printStackTrace(out);
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * @see java.lang.Throwable#printStackTrace(java.io.PrintWriter)
     */
    public void printStackTrace(PrintWriter out)
    {
        super.printStackTrace(out);
        for (int i=0;i<LazyList.size(nested);i++)
            ((Throwable)LazyList.get(nested,i)).printStackTrace(out);
    }

}
