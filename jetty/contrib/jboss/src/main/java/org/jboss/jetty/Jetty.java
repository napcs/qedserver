//========================================================================
//$Id:  $
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
package org.jboss.jetty;



import java.io.CharArrayWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jboss.logging.Logger;
import org.mortbay.xml.XmlConfiguration;
import org.w3c.dom.Element;



/**
 * Jetty 
 *
 * Extends the jetty Server class. 
 * 
 * TODO remove this class and apply jboss-web.xml configuration in
 * JettyService class instead.
 * 
 * @author <a href="mailto:jules_gosnell@yahoo..com">Julian Gosnell </a>
 * @author <a href="mailto:andreas@jboss.org">Andreas Schaefer </a>.
 * 
 * <p>
 * <b>20011201 andreas: </b>
 * <ul>
 * <li>Fixed fixURL() because it is to "Unix" centric. Right now the method looks for the last
 * part of the JAR URL (file:/...) which should be the JAR file name and add a "/." before them.
 * Now this should work for Windows as well (the problem with windows was that after "file:" came
 * the DRIVE LETTER which created a wrong URL).
 * </ul>
 */
public class Jetty extends org.mortbay.jetty.Server
{

    protected static final Logger _log = Logger.getLogger("org.jboss.jetty");

    JettyService _service;

    /**
     * the XML snippet
     */ 
    String _xmlConfigString = null;

    /**
     * the XML snippet as a DOM element
     */ 
    Element _configElement = null;

    //TODO move these to JettyDeployer?
    protected boolean _stopWebApplicationsGracefully = false;
    
    Jetty(JettyService service)
    {
        super();
        _service = service;
    }


    public Element getConfigurationElement()
    {
        return _configElement;
    }

    /**
     * @param configElement XML fragment from jboss-service.xml
     */
    public void setConfigurationElement(Element configElement)
    {

        // convert to an xml string to pass into Jetty's normal
        // configuration mechanism
        _configElement = configElement;

        try
        {
            DOMSource source = new DOMSource(configElement);

            CharArrayWriter writer = new CharArrayWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
            _xmlConfigString = writer.toString();

            // get rid of the first line, as this will be prepended by
            // the XmlConfiguration
            int index = _xmlConfigString.indexOf("?>");
            if (index >= 0)
            {
                index += 2;

                while ((_xmlConfigString.charAt(index) == '\n')
                        || (_xmlConfigString.charAt(index) == '\r'))
                    index++;
            }

            _xmlConfigString = _xmlConfigString.substring(index);

            if (_log.isDebugEnabled())
                    _log.debug("Passing xml config to jetty:\n" + _xmlConfigString);

            setXMLConfiguration(_xmlConfigString);

        }
        catch (TransformerConfigurationException tce)
        {
            _log.error("Can't transform config Element -> xml:", tce);
        }
        catch (TransformerException te)
        {
            _log.error("Can't transform config Element -> xml:", te);
        }
        catch (Exception e)
        {
            _log.error("Unexpected exception converting configuration Element -> xml", e);
        }
    }

    /*
     * Actually perform the configuration @param xmlString
     */
    private void setXMLConfiguration(String xmlString)
    {

        try
        {
            XmlConfiguration xmlConfigurator = new XmlConfiguration(xmlString);
            xmlConfigurator.configure(this);
        }
        catch (Exception e)
        {
            _log.error("problem configuring Jetty:", e);
        }
    }



    public String[] getCompileClasspath(ClassLoader cl)
    {
        return _service.getCompileClasspath(cl);
    }


    public boolean getStopWebApplicationsGracefully()
    {
        return _stopWebApplicationsGracefully;
    }

    public void setStopWebApplicationsGracefully(boolean graceful)
    {
        _stopWebApplicationsGracefully = graceful;
    }
}
