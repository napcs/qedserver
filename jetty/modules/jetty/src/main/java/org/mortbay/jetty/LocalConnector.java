//========================================================================
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty;

import java.io.IOException;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.ByteArrayEndPoint;

public class LocalConnector extends AbstractConnector
{
    ByteArrayEndPoint _endp;
    ByteArrayBuffer _in;
    ByteArrayBuffer _out;
    
    Server _server;
    boolean _accepting;
    boolean _keepOpen;
    
    public LocalConnector()
    {
        setPort(1);
    }

    /* ------------------------------------------------------------ */
    public Object getConnection()
    {
        return _endp;
    }
    

    /* ------------------------------------------------------------ */
    public void setServer(Server server)
    {
        super.setServer(server);
        this._server=server;
    }

    /* ------------------------------------------------------------ */
    public void clear()
    {
        _in.clear();
        _out.clear();
    }

    /* ------------------------------------------------------------ */
    public void reopen()
    {
        _in.clear();
        _out.clear();
        _endp = new ByteArrayEndPoint();
        _endp.setIn(_in);
        _endp.setOut(_out);
        _endp.setGrowOutput(true);
        _accepting=false;
    }

    /* ------------------------------------------------------------ */
    public void doStart()
        throws Exception
    {   
        _in=new ByteArrayBuffer(8192);
        _out=new ByteArrayBuffer(8192);
        _endp = new ByteArrayEndPoint();
        _endp.setIn(_in);
        _endp.setOut(_out);
        _endp.setGrowOutput(true);
        _accepting=false;
        
        super.doStart();
    }

    /* ------------------------------------------------------------ */
    public String getResponses(String requests)
        throws Exception
    {
        return getResponses(requests,false);
    }
    
    /* ------------------------------------------------------------ */
    public String getResponses(String requests, boolean keepOpen)
    throws Exception
    {
        // System.out.println("\nREQUESTS :\n"+requests);
        // System.out.flush();
        ByteArrayBuffer buf=new ByteArrayBuffer(requests);
        if (_in.space()<buf.length())
        {
            ByteArrayBuffer n = new ByteArrayBuffer(_in.length()+buf.length());
            n.put(_in);
            _in=n;
            _endp.setIn(_in);
        }
        _in.put(buf);
        
        synchronized (this)
        {
            _keepOpen=keepOpen;
            _accepting=true;
            this.notify();
            
            while(_accepting)
                this.wait();
        }
        
        // System.err.println("\nRESPONSES:\n"+out);
        _out=_endp.getOut();
        return _out.toString();
    }
    
    /* ------------------------------------------------------------ */
    public ByteArrayBuffer getResponses(ByteArrayBuffer buf, boolean keepOpen)
    throws Exception
    {
        if (_in.space()<buf.length())
        {
            ByteArrayBuffer n = new ByteArrayBuffer(_in.length()+buf.length());
            n.put(_in);
            _in=n;
            _endp.setIn(_in);
        }
        _in.put(buf);
        
        synchronized (this)
        {
            _keepOpen=keepOpen;
            _accepting=true;
            this.notify();
            
            while(_accepting)
                this.wait();
        }
        
        // System.err.println("\nRESPONSES:\n"+out);
        _out=_endp.getOut();
        return _out;
    }

    /* ------------------------------------------------------------ */
    protected Buffer newBuffer(int size)
    {
        return new ByteArrayBuffer(size);
    }

    /* ------------------------------------------------------------ */
    protected void accept(int acceptorID) throws IOException, InterruptedException
    {
        HttpConnection connection=null;
        
        while (isRunning())
        {
            synchronized (this)
            {
                try
                {
                    while(!_accepting)
                        this.wait();
                }
                catch(InterruptedException e)
                {
                    return;
                }
            }
            
            try
            {
                if (connection==null)
                {
                    connection=new HttpConnection(this,_endp,getServer());
                    connectionOpened(connection);
                }
                while (_in.length()>0)
                    connection.handle();
            }
            finally
            {
                if (!_keepOpen)
                {
                    connectionClosed(connection);
                    connection.destroy();
                    connection=null;
                }
                synchronized (this)
                {
                    _accepting=false;
                    this.notify();
                }
            }
        }
    }
    

    public void open() throws IOException
    {
    }

    public void close() throws IOException
    {
    }

    /* ------------------------------------------------------------------------------- */
    public int getLocalPort()
    {
        return -1;
    }

    
}