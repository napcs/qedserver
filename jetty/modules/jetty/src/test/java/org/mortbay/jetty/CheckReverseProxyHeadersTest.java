//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.mortbay.jetty.handler.AbstractHandler;

/**
 * Test {@link AbstractConnector#checkForwardedHeaders(org.mortbay.io.EndPoint, Request)}.
 */
public class CheckReverseProxyHeadersTest extends TestCase
{
    Server server=new Server();
    LocalConnector connector=new LocalConnector();

    /**
     * Constructor for CheckReverseProxyHeadersTest.
     * @param name test case name.
     */
    public CheckReverseProxyHeadersTest(String name)
    {
        super(name);
    }
    
    public void testCheckReverseProxyHeaders() throws Exception
    {
        // Classic ProxyPass from example.com:80 to localhost:8080
        testRequest("Host: localhost:8080\n" +
                    "X-Forwarded-For: 10.20.30.40\n" +
                    "X-Forwarded-Host: example.com", new RequestValidator()
        {
            public void validate(HttpServletRequest request)
            {
                assertEquals("example.com", request.getServerName());
                assertEquals(80, request.getServerPort());
                assertEquals("10.20.30.40", request.getRemoteAddr());
                assertEquals("10.20.30.40", request.getRemoteHost());
                assertEquals("example.com", request.getHeader("Host"));
            }
        });
        
        // ProxyPass from example.com:81 to localhost:8080
        testRequest("Host: localhost:8080\n" +
                    "X-Forwarded-For: 10.20.30.40\n" +
                    "X-Forwarded-Host: example.com:81\n" +
                    "X-Forwarded-Server: example.com", new RequestValidator()
        {
            public void validate(HttpServletRequest request)
            {
                assertEquals("example.com", request.getServerName());
                assertEquals(81, request.getServerPort());
                assertEquals("10.20.30.40", request.getRemoteAddr());
                assertEquals("10.20.30.40", request.getRemoteHost());
                assertEquals("example.com:81", request.getHeader("Host"));
            }
        });
        
        // Multiple ProxyPass from example.com:80 to rp.example.com:82 to localhost:8080
        testRequest("Host: localhost:8080\n" +
                    "X-Forwarded-For: 10.20.30.40, 10.0.0.1\n" +
                    "X-Forwarded-Host: example.com, rp.example.com:82\n" +
                    "X-Forwarded-Server: example.com, rp.example.com", new RequestValidator()
        {
            public void validate(HttpServletRequest request)
            {
                assertEquals("example.com", request.getServerName());
                assertEquals(80, request.getServerPort());
                assertEquals("10.20.30.40", request.getRemoteAddr());
                assertEquals("10.20.30.40", request.getRemoteHost());
                assertEquals("example.com", request.getHeader("Host"));
            }
        });
    }

    private void testRequest(String headers, RequestValidator requestValidator) throws Exception
    {
        Server server = new Server();
        LocalConnector connector = new LocalConnector();
        
        // Activate reverse proxy headers checking
        connector.setForwarded(true);
        
        server.setConnectors(new Connector[] {connector});
        ValidationHandler validationHandler = new ValidationHandler(requestValidator);
        server.setHandler(validationHandler);
        
        try
        {
            server.start();
            connector.getResponses("GET / HTTP/1.1\n" + headers + "\n\n");
            
            Error error = validationHandler.getError();
            
            if (error != null)
            {
                throw error;
            }
        }
        finally
        {
            server.stop();
        }
    }

    /**
     * Interface for validate a wrapped request.
     */
    private static interface RequestValidator
    {
        /**
         * Validate the current request.
         * @param request the request.
         */
        void validate(HttpServletRequest request);
    }

    /**
     * Handler for validation.
     */
    private static class ValidationHandler extends AbstractHandler
    {
        private RequestValidator _requestValidator;
        private Error _error;
        
        /**
         * Create the validation handler with a request validator.
         * @param requestValidator the request validator.
         */
        public ValidationHandler(RequestValidator requestValidator)
        {
            _requestValidator = requestValidator;
        }
        
        /**
         * Retrieve the validation error.
         * @return the validation error or <code>null</code> if there was no error.
         */
        public Error getError()
        {
            return _error;
        }
        
        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
        {
            try
            {
                _requestValidator.validate(request);
            }
            catch (Error e)
            {
                _error = e;
            }
            catch (Throwable e)
            {
                _error = new Error(e);
            }
        }
    }
}
