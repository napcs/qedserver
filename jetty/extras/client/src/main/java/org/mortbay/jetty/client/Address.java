package org.mortbay.jetty.client;

import java.net.InetSocketAddress;

/**
 * @version $Revision: 3753 $ $Date: 2008-12-02 21:53:28 +1100 (Tue, 02 Dec 2008) $
 */
public class Address
{
    private final String host;
    private final int port;

    public static Address from(String hostAndPort)
    {
        String host;
        int port;
        int colon = hostAndPort.indexOf(':');
        if (colon >= 0)
        {
            host = hostAndPort.substring(0, colon);
            port = Integer.parseInt(hostAndPort.substring(colon + 1));
        }
        else
        {
            host = hostAndPort;
            port = 0;
        }
        return new Address(host, port);
    }

    public Address(String host, int port)
    {
        this.host = host.trim();
        this.port = port;
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Address that = (Address)obj;
        if (!host.equals(that.host)) return false;
        return port == that.port;
    }

    public int hashCode()
    {
        int result = host.hashCode();
        result = 31 * result + port;
        return result;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public InetSocketAddress toSocketAddress()
    {
        return new InetSocketAddress(getHost(), getPort());
    }

    @Override
    public String toString()
    {
        return host + ":" + port;
    }
}
