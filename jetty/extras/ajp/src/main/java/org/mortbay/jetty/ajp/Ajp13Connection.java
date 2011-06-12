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

package org.mortbay.jetty.ajp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.io.Buffer;
import org.mortbay.io.EndPoint;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpException;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;

/**
 * Connection implementation of the Ajp13 protocol. <p/> XXX Refactor to remove
 * duplication of HttpConnection
 * 
 * @author Markus Kobler markus(at)inquisitive-mind.com
 * @author Greg Wilkins
 */
public class Ajp13Connection extends HttpConnection
{
    public Ajp13Connection(Connector connector, EndPoint endPoint, Server server)
    {
        super(connector, endPoint, server,
                new Ajp13Parser(connector, endPoint),
                new Ajp13Generator(connector, endPoint, connector.getHeaderBufferSize(), connector.getResponseBufferSize()),
                new Ajp13Request()
                );
        
        ((Ajp13Parser)_parser).setEventHandler(new RequestHandler());
        ((Ajp13Parser)_parser).setGenerator((Ajp13Generator)_generator);
        ((Ajp13Request)_request).setConnection(this);
    }

    public boolean isConfidential(Request request)
    {
        return ((Ajp13Request) request).isSslSecure();
    }

    public boolean isIntegral(Request request)
    {
        return ((Ajp13Request) request).isSslSecure();
    }

    public ServletInputStream getInputStream()
    {
        if (_in == null)
            _in = new Ajp13Parser.Input((Ajp13Parser) _parser, _connector.getMaxIdleTime());
        return _in;
    }

    private class RequestHandler implements Ajp13Parser.EventHandler
    {
        boolean _delayedHandling = false;

        public void startForwardRequest() throws IOException
        {
            _delayedHandling = false;
            _uri.clear();
            ((Ajp13Request) _request).setSslSecure(false);
            _request.setTimeStamp(System.currentTimeMillis());
            _request.setUri(_uri);
            
        }

        public void parsedAuthorizationType(Buffer authType) throws IOException
        {
            _request.setAuthType(authType.toString());
        }

        public void parsedRemoteUser(Buffer remoteUser) throws IOException
        {
            ((Ajp13Request)_request).setRemoteUser(remoteUser.toString());
        }

        public void parsedServletPath(Buffer servletPath) throws IOException
        {
            _request.setServletPath(servletPath.toString());
        }

        public void parsedContextPath(Buffer context) throws IOException
        {
            _request.setContextPath(context.toString());
        }

        public void parsedSslCert(Buffer sslCert) throws IOException
        {
            try 
            {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                ByteArrayInputStream bis = new ByteArrayInputStream(sslCert.toString().getBytes());

                Collection certCollection = cf.generateCertificates(bis);
                X509Certificate[] certificates = new X509Certificate[certCollection.size()];

                int i=0;
                Iterator iter=certCollection.iterator();
                while(iter.hasNext())
                    certificates[i++] = (X509Certificate)iter.next();

                _request.setAttribute("javax.servlet.request.X509Certificate", certificates);
            } 
            catch (Exception e) 
            {
                org.mortbay.log.Log.warn(e.toString());
                org.mortbay.log.Log.ignore(e);
                if (sslCert!=null)
                    _request.setAttribute("javax.servlet.request.X509Certificate", sslCert.toString());
            }
        }

        public void parsedSslCipher(Buffer sslCipher) throws IOException
        {
            _request.setAttribute("javax.servlet.request.cipher_suite", sslCipher.toString());
        }

        public void parsedSslSession(Buffer sslSession) throws IOException
        {
            _request.setAttribute("javax.servlet.request.ssl_session", sslSession.toString());
        }
        
        public void parsedSslKeySize(int keySize) throws IOException
        {
           _request.setAttribute("javax.servlet.request.key_size", new Integer(keySize));
        }

        public void parsedMethod(Buffer method) throws IOException
        {
            if (method == null)
                throw new HttpException(HttpServletResponse.SC_BAD_REQUEST);
            _request.setMethod(method.toString());
        }

        public void parsedUri(Buffer uri) throws IOException
        {
            _uri.parse(uri.toString());
        }

        public void parsedProtocol(Buffer protocol) throws IOException
        {
            if (protocol != null && protocol.length()>0)
            {
                _request.setProtocol(protocol.toString());
            }
        }

        public void parsedRemoteAddr(Buffer addr) throws IOException
        {
            if (addr != null && addr.length()>0)
            {
                ((Ajp13Request) _request).setRemoteAddr(addr.toString());
            }
        }

        public void parsedRemoteHost(Buffer name) throws IOException
        {
            if (name != null && name.length()>0)
            {
                ((Ajp13Request) _request).setRemoteHost(name.toString());
            }
        }

        public void parsedServerName(Buffer name) throws IOException
        {
            if (name != null && name.length()>0)
            {
                _request.setServerName(name.toString());
            }
        }

        public void parsedServerPort(int port) throws IOException
        {
            ((Ajp13Request) _request).setServerPort(port);
        }

        public void parsedSslSecure(boolean secure) throws IOException
        {
            ((Ajp13Request) _request).setSslSecure(secure);
        }

        public void parsedQueryString(Buffer value) throws IOException
        {
            String u = _uri + "?" + value;
            _uri.parse(u);
        }

        public void parsedHeader(Buffer name, Buffer value) throws IOException
        {
            _requestFields.add(name, value);
        }

        public void parsedRequestAttribute(String key, Buffer value) throws IOException
        {
            _request.setAttribute(key, value.toString());
        }
        
        public void parsedRequestAttribute(String key, int value) throws IOException
        {
            _request.setAttribute(key, Integer.toString(value));
        }

        public void headerComplete() throws IOException
        {
            if (((Ajp13Parser) _parser).getContentLength() <= 0)
            {
                handleRequest();
            }
            else
            {
                _delayedHandling = true;
            }
        }

        public void messageComplete(long contextLength) throws IOException
        {
        }

        public void content(Buffer ref) throws IOException
        {
            if (_delayedHandling)
            {
                _delayedHandling = false;
                handleRequest();
            }
        }

    }

}
