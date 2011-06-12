// ========================================================================
// Copyright 2006-2007 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import junit.framework.TestCase;

import org.mortbay.io.Buffer;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.client.security.HashRealmResolver;
import org.mortbay.jetty.client.security.Realm;
import org.mortbay.jetty.client.security.SimpleRealmResolver;
import org.mortbay.jetty.client.webdav.WebdavListener;

/**
 * Functional testing for HttpExchange.
 *
 * @author Matthew Purland
 * @author Greg Wilkins
 */
public class WebdavListenerTest extends TestCase//extends HttpExchangeTest
{
    protected String _scheme = "http://";
    protected Server _server;
    protected int _port;
    protected HttpClient _httpClient;
    protected Connector _connector;

    private String _username = "janb";
    private String _password = "xxxxx";

    private String _singleFileURL;
    private String _dirFileURL;
    private String _dirURL;
    

    
   
    protected void setUp() throws Exception
    {
        _singleFileURL = "https://dav.codehaus.org/user/" + _username + "/foo.txt";
        _dirURL = "https://dav.codehaus.org/user/" + _username + "/ttt/";
        _dirFileURL = _dirURL+"foo.txt";
        _scheme="https://";
 
        _httpClient=new HttpClient();
        _httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        //_httpClient.setMaxConnectionsPerAddress(4);

        _httpClient.setRealmResolver( new SimpleRealmResolver (
                new Realm(){
                    public String getId()
                    {
                        return _username + "'s webspace";  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public String getPrincipal()
                    {
                        return _username;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public String getCredentials()
                    {
                        return _password;  //To change body of implemented methods use File | Settings | File Templates.
                    }
                }
        ));

        _httpClient.registerListener( "org.mortbay.jetty.client.webdav.WebdavListener");
        _httpClient.start();
    }
    
    
    public void tearDown () throws Exception
    {
        _httpClient.stop();
    }
   

    public void testPUTandDELETEwithSSL() throws Exception
    { 
        File file = new File("src/test/resources/foo.txt");
        assertTrue(file.exists());
        
        
        /*
         * UNCOMMENT TO TEST WITH REAL DAV SERVER
         * Remember to set _username and _password to a real user's account.
         * 
         */
        /*
        //PUT a FILE
        ContentExchange singleFileExchange = new ContentExchange();
        singleFileExchange.setURL(_singleFileURL);
        singleFileExchange.setMethod( HttpMethods.PUT );
        singleFileExchange.setFileForUpload(file);
        singleFileExchange.setRequestHeader( "Content-Type", "application/octet-stream");
        singleFileExchange.setRequestHeader("Content-Length", String.valueOf( file.length() ));
        _httpClient.send(singleFileExchange);
        singleFileExchange.waitForDone();
        
        String result = singleFileExchange.getResponseContent();
        assertEquals(201, singleFileExchange.getResponseStatus());    
      
       
        //PUT a FILE in a directory hierarchy
        ContentExchange dirFileExchange = new ContentExchange();
        dirFileExchange.setURL(_dirFileURL);
        dirFileExchange.setMethod( HttpMethods.PUT );
        dirFileExchange.setFileForUpload(file);
        dirFileExchange.setRequestHeader( "Content-Type", "application/octet-stream");
        dirFileExchange.setRequestHeader("Content-Length", String.valueOf( file.length() ));
        _httpClient.send(dirFileExchange);
        dirFileExchange.waitForDone();
        result = dirFileExchange.getResponseContent();        
        assertEquals(201, singleFileExchange.getResponseStatus());
       
      
       
     
        //DELETE the single file
        HttpExchange del = new HttpExchange();
        del.setURL(_singleFileURL);
        del.setMethod(HttpMethods.DELETE);
        _httpClient.send(del);
        del.waitForCompletion();
          
        //DELETE the whole dir
        del.setURL(_dirURL);
        del.setMethod(HttpMethods.DELETE);  
        del.setRequestHeader("Depth", "infinity");
        _httpClient.send(del);
        del.waitForCompletion();
        */ 
    }
  
}