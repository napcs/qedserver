package org.mortbay.jetty.servlet.wadi;

public class SessionAlreadyExistsException extends Exception
{

    public SessionAlreadyExistsException()
    {
        super();
    }

    public SessionAlreadyExistsException(String arg0, Throwable arg1)
    {
        super(arg0, arg1);
    }

    public SessionAlreadyExistsException(String arg0)
    {
        super(arg0);
    }

    public SessionAlreadyExistsException(Throwable arg0)
    {
        super(arg0);
    }
    
}
