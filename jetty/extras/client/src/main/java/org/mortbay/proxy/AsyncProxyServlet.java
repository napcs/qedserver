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

package org.mortbay.proxy;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.mortbay.io.Buffer;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpSchemes;
import org.mortbay.jetty.HttpURI;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.client.Address;
import org.mortbay.jetty.client.HttpClient;
import org.mortbay.jetty.client.HttpExchange;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.util.IO;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;



/**
 * Asynchronous Proxy Servlet.
 * 
 * Forward requests to another server either as a standard web proxy (as defined by
 * RFC2616) or as a transparent proxy.
 * 
 * This servlet needs the jetty-util and jetty-client classes to be available to
 * the web application.
 *
 */
public class AsyncProxyServlet implements Servlet
{
    HttpClient _client;

    protected HashSet<String> _DontProxyHeaders = new HashSet<String>();
    {
        _DontProxyHeaders.add("proxy-connection");
        _DontProxyHeaders.add("connection");
        _DontProxyHeaders.add("keep-alive");
        _DontProxyHeaders.add("transfer-encoding");
        _DontProxyHeaders.add("te");
        _DontProxyHeaders.add("trailer");
        _DontProxyHeaders.add("proxy-authorization");
        _DontProxyHeaders.add("proxy-authenticate");
        _DontProxyHeaders.add("upgrade");
    }

    private ServletConfig config;
    private ServletContext context;

    /* (non-Javadoc)
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig config) throws ServletException
    {
        this.config=config;
        this.context=config.getServletContext();

        _client=new HttpClient();
        //_client.setConnectorType(HttpClient.CONNECTOR_SOCKET);
        _client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        try
        {
            _client.start();
        }
        catch (Exception e)
        {
            throw new ServletException(e);
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.Servlet#getServletConfig()
     */
    public ServletConfig getServletConfig()
    {
        return config;
    }

    /* (non-Javadoc)
     * @see javax.servlet.Servlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void service(ServletRequest req, ServletResponse res) throws ServletException,
            IOException
    {
        while (res instanceof HttpServletResponseWrapper)
            res=(HttpServletResponse)((HttpServletResponseWrapper)res).getResponse();
           
        final HttpServletRequest request = (HttpServletRequest)req;
        final HttpServletResponse response = (HttpServletResponse)res;
        
        if ("CONNECT".equalsIgnoreCase(request.getMethod()))
        {
            handleConnect(request,response);
        }
        else
        {
            final InputStream in=request.getInputStream();
            final OutputStream out=response.getOutputStream();
            final Continuation continuation = ContinuationSupport.getContinuation(request,request);


            if (!continuation.isPending())
            {
                final byte[] buffer = new byte[4096]; // TODO avoid this!
                String uri=request.getRequestURI();
                if (request.getQueryString()!=null)
                    uri+="?"+request.getQueryString();

                HttpURI url=proxyHttpURI(request.getScheme(),
                        request.getServerName(),
                        request.getServerPort(),
                        uri);
                
                if (url==null)
                {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

                HttpExchange exchange = new HttpExchange()
                {

                    protected void onRequestCommitted() throws IOException
                    {
                    }

                    protected void onRequestComplete() throws IOException
                    {
                    }

                    protected void onResponseComplete() throws IOException
                    {
                        continuation.resume();
                    }

                    protected void onResponseContent(Buffer content) throws IOException
                    {
                        // TODO Avoid this copy
                        while (content.hasContent())
                        {
                            int len=content.get(buffer,0,buffer.length);
                            out.write(buffer,0,len);  // May block here for a little bit!
                        }
                    }

                    protected void onResponseHeaderComplete() throws IOException
                    {
                    }

                    protected void onResponseStatus(Buffer version, int status, Buffer reason) throws IOException
                    {
                        if (reason!=null && reason.length()>0)
                            response.setStatus(status,reason.toString());
                        else
                            response.setStatus(status);

                    }

                    protected void onResponseHeader(Buffer name, Buffer value) throws IOException
                    {
                        String s = name.toString().toLowerCase();
                        if (!_DontProxyHeaders.contains(s))
                            response.addHeader(name.toString(),value.toString());
                    }

                };
                
                exchange.setVersion(request.getProtocol());
                exchange.setMethod(request.getMethod());
                
                exchange.setURL(url.toString());
                
                // check connection header
                String connectionHdr = request.getHeader("Connection");
                if (connectionHdr!=null)
                {
                    connectionHdr=connectionHdr.toLowerCase();
                    if (connectionHdr.indexOf("keep-alive")<0  &&
                            connectionHdr.indexOf("close")<0)
                        connectionHdr=null;
                }

                // copy headers
                boolean xForwardedFor=false;
                boolean hasContent=false;
                long contentLength=-1;
                Enumeration enm = request.getHeaderNames();
                while (enm.hasMoreElements())
                {
                    // TODO could be better than this!
                    String hdr=(String)enm.nextElement();
                    String lhdr=hdr.toLowerCase();

                    if (_DontProxyHeaders.contains(lhdr))
                        continue;
                    if (connectionHdr!=null && connectionHdr.indexOf(lhdr)>=0)
                        continue;

                    if ("content-type".equals(lhdr))
                        hasContent=true;
                    if ("content-length".equals(lhdr))
                        contentLength=request.getContentLength();

                    Enumeration vals = request.getHeaders(hdr);
                    while (vals.hasMoreElements())
                    {
                        String val = (String)vals.nextElement();
                        if (val!=null)
                        {
                            exchange.setRequestHeader(lhdr,val);
                            xForwardedFor|="X-Forwarded-For".equalsIgnoreCase(hdr);
                        }
                    }
                }

                // Proxy headers
                exchange.setRequestHeader("Via","1.1 (jetty)");
                if (!xForwardedFor)
                    exchange.addRequestHeader("X-Forwarded-For",
                            request.getRemoteAddr());

                if (hasContent)
                    exchange.setRequestContentSource(in);

                _client.send(exchange);

                continuation.suspend(30000);
            }
        }
    }


    /* ------------------------------------------------------------ */
    /**
    /** Resolve requested URL to the Proxied HttpURI
     * @param scheme The scheme of the received request.
     * @param serverName The server encoded in the received request(which 
     * may be from an absolute URL in the request line).
     * @param serverPort The server port of the received request (which 
     * may be from an absolute URL in the request line).
     * @param uri The URI of the received request.
     * @return The HttpURI to which the request should be proxied.
     * @throws MalformedURLException
     */
    protected HttpURI proxyHttpURI(String scheme, String serverName, int serverPort, String uri)
        throws MalformedURLException
    {
        return new HttpURI(scheme+"://"+serverName+":"+serverPort+uri);
    }

    /* ------------------------------------------------------------ */
    public void handleConnect(HttpServletRequest request,
                              HttpServletResponse response)
        throws IOException
    {
        String uri = request.getRequestURI();

        String port = "";
        String host = "";

        int c = uri.indexOf(':');
        if (c>=0)
        {
            port = uri.substring(c+1);
            host = uri.substring(0,c);
            if (host.indexOf('/')>0)
                host = host.substring(host.indexOf('/')+1);
        }

        InetSocketAddress inetAddress = new InetSocketAddress (host, Integer.parseInt(port));

        //if (isForbidden(HttpMessage.__SSL_SCHEME,addrPort.getHost(),addrPort.getPort(),false))
        //{
        //    sendForbid(request,response,uri);
        //}
        //else
        {
            InputStream in=request.getInputStream();
            OutputStream out=response.getOutputStream();

            Socket socket = new Socket(inetAddress.getAddress(),inetAddress.getPort());

            response.setStatus(200);
            response.setHeader("Connection","close");
            response.flushBuffer();



            IO.copyThread(socket.getInputStream(),out);
            IO.copy(in,socket.getOutputStream());
        }
    }




    /* (non-Javadoc)
     * @see javax.servlet.Servlet#getServletInfo()
     */
    public String getServletInfo()
    {
        return "Proxy Servlet";
    }

    /* (non-Javadoc)
     * @see javax.servlet.Servlet#destroy()
     */
    public void destroy()
    {

    }
    
    /**
     * Transparent Proxy.
     * 
     * This convenience extension to AsyncProxyServlet configures the servlet
     * as a transparent proxy.   The servlet is configured with init parameter:<ul>
     * <li> ProxyTo - a URI like http://host:80/context to which the request is proxied.
     * <li> Prefix  - a URI prefix that is striped from the start of the forwarded URI.
     * </ul>
     * For example, if a request was received at /foo/bar and the ProxyTo was  http://host:80/context
     * and the Prefix was /foo, then the request would be proxied to http://host:80/context/bar
     *
     */
    public static class Transparent extends AsyncProxyServlet
    {
        String _prefix;
        String _proxyTo;
        
        public Transparent()
        {    
        }
        
        public Transparent(String prefix,String server, int port)
        {
            _prefix=prefix;
            _proxyTo="http://"+server+":"+port;
        }

        public void init(ServletConfig config) throws ServletException
        {
            if (config.getInitParameter("ProxyTo")!=null)
                _proxyTo=config.getInitParameter("ProxyTo");
            if (config.getInitParameter("Prefix")!=null)
                _prefix=config.getInitParameter("Prefix");
            if (_proxyTo==null)
                throw new UnavailableException("No ProxyTo");
            super.init(config);
            config.getServletContext().log("Transparent AsyncProxyServlet @ "+(_prefix==null?"-":_prefix)+ " to "+_proxyTo);
            
        }
        
        protected HttpURI proxyHttpURI(final String scheme, final String serverName, int serverPort, final String uri) throws MalformedURLException
        {
            if (_prefix!=null && !uri.startsWith(_prefix))
                return null;

            if (_prefix!=null)
                return new HttpURI(_proxyTo+uri.substring(_prefix.length()));
            return new HttpURI(_proxyTo+uri);
        }
    }
}
