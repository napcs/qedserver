package org.openspaces.webapp;

import org.mortbay.jetty.Server;
import org.openspaces.core.cluster.ClusterInfo;

public class GigaServerConfiguration 
{
	private int _instanceId;
	private int _backupId;
	private String _schema;
	private int _serverPort;

	public GigaServerConfiguration( )
	{
		
	}
	
	public GigaServerConfiguration( ClusterInfo info, Server server )
	{
		_instanceId = info.getInstanceId();
		if ( info.getBackupId() != null )
		{
			_backupId = info.getBackupId();
		}
		_schema =info.getSchema();
		
		if ( server.getConnectors() != null )
		{
			_serverPort = server.getConnectors()[0].getPort();
		}
	}
	
	public int getServerPort() {
		return _serverPort;
	}


	public int getInstanceId() {
		return _instanceId;
	}


	public int getBackupId() {
		return _backupId;
	}


	public String getSchema() {
		return _schema;
	}		
}
