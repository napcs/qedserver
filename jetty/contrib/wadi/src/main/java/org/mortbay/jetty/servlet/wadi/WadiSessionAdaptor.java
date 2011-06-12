package org.mortbay.jetty.servlet.wadi;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.codehaus.wadi.core.session.Session;

public class WadiSessionAdaptor implements WadiSession
{
    private final Session session;

    public WadiSessionAdaptor(Session session)
    {
        this.session = session;
    }

    public String getSessionId()
    {
        return session.getName();
    }

    public void release()
    {
        try
        {
            session.destroy();
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Cannot release session " + session);
        }
    }

    public Object addState(String key, Object value)
    {
        return session.addState(key, value);
    }

    public Object getState(String key)
    {
        return session.getState(key);
    }

    public Object removeState(String key)
    {
        return session.removeState(key);
    }

    public Map getState()
    {
        return session.getState();
    }

    public void onEndAccess()
    {
        session.onEndProcessing();
    }
}
