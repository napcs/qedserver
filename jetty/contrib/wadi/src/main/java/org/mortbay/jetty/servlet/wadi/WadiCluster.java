package org.mortbay.jetty.servlet.wadi;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.catalina.tribes.Member;
import org.codehaus.wadi.core.reflect.base.DeclaredMemberFilter;
import org.codehaus.wadi.core.reflect.jdk.JDKClassIndexerRegistry;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.DispatcherRegistry;
import org.codehaus.wadi.group.StaticDispatcherRegistry;
import org.codehaus.wadi.servicespace.admin.AdminServiceSpace;
import org.codehaus.wadi.tribes.TribesDispatcher;
import org.codehaus.wadi.web.impl.URIEndPoint;
import org.mortbay.component.AbstractLifeCycle;

public class WadiCluster extends AbstractLifeCycle
{

    private final String _clusterName;
    private final String _nodeName;
    private final URI _endPointURI;
    private final Collection<Member> _staticMembers;
    
    private Dispatcher _dispatcher;

    /**
     * Constructs a cluster having the specified details.
     * 
     * @param clusterName name of the cluster.
     * @param nodeName name of the node this cluster instance is running on
     * @param endPointURI base URL to use from other nodes to reach this node. This value is actually not used and you 
     * pretty much do not need to define a meaningful value; however, it must be a valid URI.
     * 
     * @throws Exception
     */
    public WadiCluster(String clusterName, String nodeName, String endPointURI) throws Exception {
        if (null == clusterName) {
            throw new IllegalArgumentException("clusterName is required");
        } else if (null == nodeName) {
            throw new IllegalArgumentException("nodeName is required");
        }
        _clusterName = clusterName;
        _nodeName = nodeName;
        _endPointURI = new URI(endPointURI);
        
        _staticMembers = new ArrayList<Member>();
    }

    public void addStaticMember(Member member)
    {
        _staticMembers.add(member);
    }

    public Dispatcher getDispatcher()
    {
        return _dispatcher;
    }
    
    public String getNodeName()
    {
        return _nodeName;
    }

    @Override
    protected void doStart() throws Exception {
        _dispatcher = newDispatcher();
        DispatcherRegistry dispatcherRegistry = new StaticDispatcherRegistry();
        dispatcherRegistry.register(_dispatcher);
        _dispatcher.start();
        
        AdminServiceSpace adminServiceSpace = new AdminServiceSpace(_dispatcher,
            new JDKClassIndexerRegistry(new DeclaredMemberFilter()));
        adminServiceSpace.start();
    }

    @Override
    protected void doStop() throws Exception {
        _dispatcher.stop();
    }
    
    protected Dispatcher newDispatcher()
    {
        return new TribesDispatcher(_clusterName, _nodeName, new URIEndPoint(_endPointURI), _staticMembers);
    }

}
