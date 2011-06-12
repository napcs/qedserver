// ========================================================================
// Copyright 1999-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.MimeTypes;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.log.Log;
import org.mortbay.util.ByteArrayISO8859Writer;
import org.mortbay.util.IO;
import org.mortbay.util.StringUtil;


/* ------------------------------------------------------------ */
/** Default Handler.
 * 
 * This handle will deal with unhandled requests in the server.
 * For requests for favicon.ico, the Jetty icon is served. 
 * For reqests to '/' a 404 with a list of known contexts is served.
 * For all other requests a normal 404 is served.
 * TODO Implement OPTIONS and TRACE methods for the server.
 * 
 * @author Greg Wilkins (gregw)
 * @org.apache.xbean.XBean
 */
public class DefaultHandler extends AbstractHandler
{
    long _faviconModified=(System.currentTimeMillis()/1000)*1000;
    byte[] _favicon;
    boolean _serveIcon=true;
    
    public DefaultHandler()
    {
        try
        {
            URL fav = this.getClass().getClassLoader().getResource("org/mortbay/jetty/favicon.ico");
            if (fav!=null)
            _favicon=IO.readBytes(fav.openStream());
        }
        catch(Exception e)
        {
            Log.warn(e);
        }
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.Handler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, int)
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
    {      
        Request base_request = request instanceof Request?(Request)request:HttpConnection.getCurrentConnection().getRequest();
        
        if (response.isCommitted() || base_request.isHandled())
            return;
        base_request.setHandled(true);
        
        String method=request.getMethod();

        // little cheat for common request
        if (_serveIcon && _favicon!=null && method.equals(HttpMethods.GET) && request.getRequestURI().equals("/favicon.ico"))
        {
            if (request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)==_faviconModified)
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            else
            {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("image/x-icon");
                response.setContentLength(_favicon.length);
                response.setDateHeader(HttpHeaders.LAST_MODIFIED, _faviconModified);
                response.getOutputStream().write(_favicon);
            }
            return;
        }
        
        
        if (!method.equals(HttpMethods.GET) || !request.getRequestURI().equals("/"))
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;   
        }

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType(MimeTypes.TEXT_HTML);
        
        ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(1500);

        String uri=request.getRequestURI();
        uri=StringUtil.replace(uri,"<","&lt;");
        uri=StringUtil.replace(uri,">","&gt;");
        
        writer.write("<HTML>\n<HEAD>\n<TITLE>Error 404 - Not Found");
        writer.write("</TITLE>\n<BODY>\n<H2>Error 404 - Not Found.</H2>\n");
        writer.write("No context on this server matched or handled this request.<BR>");
        writer.write("Contexts known to this server are: <ul>");


        Server server = getServer();
        Handler[] handlers = server==null?null:server.getChildHandlersByClass(ContextHandler.class);
 
        for (int i=0;handlers!=null && i<handlers.length;i++)
        {
            ContextHandler context = (ContextHandler)handlers[i];
            if (context.isRunning())
            {
                writer.write("<li><a href=\"");
                if (context.getVirtualHosts()!=null && context.getVirtualHosts().length>0)
                    writer.write("http://"+context.getVirtualHosts()[0]+":"+request.getLocalPort());
                writer.write(context.getContextPath());
                if (context.getContextPath().length()>1 && context.getContextPath().endsWith("/"))
                    writer.write("/");
                writer.write("\">");
                writer.write(context.getContextPath());
                if (context.getVirtualHosts()!=null && context.getVirtualHosts().length>0)
                    writer.write("&nbsp;@&nbsp;"+context.getVirtualHosts()[0]+":"+request.getLocalPort());
                writer.write("&nbsp;--->&nbsp;");
                writer.write(context.toString());
                writer.write("</a></li>\n");
            }
            else
            {
                writer.write("<li>");
                writer.write(context.getContextPath());
                if (context.getVirtualHosts()!=null && context.getVirtualHosts().length>0)
                    writer.write("&nbsp;@&nbsp;"+context.getVirtualHosts()[0]+":"+request.getLocalPort());
                writer.write("&nbsp;--->&nbsp;");
                writer.write(context.toString());
                if (context.isFailed())
                    writer.write(" [failed]");
                if (context.isStopped())
                    writer.write(" [stopped]");
                writer.write("</li>\n");
            }
        }
        
        for (int i=0;i<10;i++)
            writer.write("\n<!-- Padding for IE                  -->");
        
        writer.write("\n</BODY>\n</HTML>\n");
        writer.flush();
        response.setContentLength(writer.size());
        OutputStream out=response.getOutputStream();
        writer.writeTo(out);
        out.close();
        
        return;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns true if the handle can server the jetty favicon.ico
     */
    public boolean getServeIcon()
    {
        return _serveIcon;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param serveIcon true if the handle can server the jetty favicon.ico
     */
    public void setServeIcon(boolean serveIcon)
    {
        _serveIcon = serveIcon;
    }


}
