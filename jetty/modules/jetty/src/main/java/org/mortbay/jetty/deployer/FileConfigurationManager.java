//========================================================================
//$Id: FileConfigurationManager.java 1096 2006-10-12 20:59:46Z janb $
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

package org.mortbay.jetty.deployer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Properties;

import org.mortbay.resource.Resource;

/**
 * FileConfigurationManager
 *
 * Supplies properties defined in a file.
 */
public class FileConfigurationManager implements ConfigurationManager
{
    private Resource _file;
    private Properties _properties = new Properties();

    public FileConfigurationManager()
    {        
    }
    
    
    public void setFile (String filename) 
    throws MalformedURLException, IOException
    {
        _file = Resource.newResource(filename);
    }
    
    
    /** 
     * @see org.mortbay.jetty.deployer.ConfigurationManager#getProperties()
     */
    public Map getProperties()
    {
        try
        {
            loadProperties();
            return _properties;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    
    private void loadProperties () 
    throws FileNotFoundException, IOException
    {
        if (_properties.isEmpty())
            _properties.load(_file.getInputStream());
    }
}
