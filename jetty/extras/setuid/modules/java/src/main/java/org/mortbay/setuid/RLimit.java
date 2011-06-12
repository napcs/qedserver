package org.mortbay.setuid;

public class RLimit
{
    int _soft;
    int _hard;

    
    public int getSoft ()
    {
        return _soft;
    }
    
    public void setSoft (int soft)
    {
        _soft = soft;
    }
    
    public int getHard ()
    {
        return _hard;
    }
    
    public void setHard (int hard)
    {
        _hard = hard;
    }
    
    public String toString()
    {
        return "rlimit_nofiles (soft="+_soft+", hard="+_hard+")";
    }
    
}
