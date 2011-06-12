package org.openspaces.webapp;

import org.mortbay.jetty.Server;
import org.mortbay.log.Log;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.cluster.ClusterInfoAware;
import org.openspaces.core.context.GigaSpaceContext;
import org.springframework.beans.factory.InitializingBean;

public class GigaServer extends Server implements InitializingBean, ClusterInfoAware {

    public final String GIGASPACE_ATTR = "org.openspaces.core.GigaSpace";
    @GigaSpaceContext
    private GigaSpace gigaSpace;
    private ClusterInfo _clusterInfo;
    
    public void setGigaSpace(GigaSpace gigaSpace) 
    {
    	this.gigaSpace = gigaSpace;
    }
    
    
	
	public void afterPropertiesSet() throws Exception 
	{
		// not the right place to write out a full on configuration object, server not initialized, but this does appear
		// to write out an object.  Need to work out the reading and writing bit, maybe try a consolidated configuration object with a
		// a map of configurations keys by a unique key based on clusterInfo and then read/write that.  would want to ensure its
		// unique though, so maybe instance Id 1 would be the only one allowed to create it, others would block?
		
		// of course this is dependent on the space setting, currently configured with in jvm space only, is this the default
		// way we want to do it?
		gigaSpace.write( new GigaServerConfiguration( _clusterInfo, this ) );
		
		GigaServerConfiguration gsc = gigaSpace.read( new GigaServerConfiguration() );
		
		if ( gsc == null )
		{
			Log.info( "GSC is null :(" );	
		}
		else
		{
			Log.info( "GSC is not null and  server port is " + gsc.getServerPort() );  // currently 0, need another place for doing this
		}
	}

	
	public void setClusterInfo( ClusterInfo clusterInfo ) 
	{
		if ( clusterInfo == null )
		{
			Log.info( "\n\n\nGIGASERVER: cluster info not set\n\n\n" );
			return;
		}
		
		Log.info( "\n\n\nGIGASERVER: My instanceId is: " + clusterInfo.getInstanceId() + "\n\n\n" );
		
		_clusterInfo = clusterInfo;		
	}
}
