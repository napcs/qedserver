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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.util.StringUtil;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

/* ------------------------------------------------------------ */
/** Dump request handler.
 * Dumps GET and POST requests.
 * Useful for testing and debugging.
 * 
 * @version $Id: DumpHandler.java,v 1.14 2005/08/13 00:01:26 gregwilkins Exp $
 * @author Greg Wilkins (gregw)
 */
public class DumpHandler extends AbstractHandler
{
    String label="Dump HttpHandler";
    
    public DumpHandler()
    {
    }
    
    public DumpHandler(String label)
    {
        this.label=label;
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.Handler#handle(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, int)
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
    {
        Request base_request = (request instanceof Request) ? (Request)request:HttpConnection.getCurrentConnection().getRequest();
        
        if (!isStarted())
            return;

        if (request.getParameter("read")!=null)
        {
            Reader in = request.getReader();
            for (int i=Integer.parseInt(request.getParameter("read"));i-->0;)
                in.read();
        }
        
        if (request.getParameter("ISE")!=null)
        {
            throw new IllegalStateException();
        }
        
        if (request.getParameter("error")!=null)
        {
            response.sendError(Integer.parseInt(request.getParameter("error")));
            return;
        }
        
        if (request.getParameter("continue")!=null)
        {
            Continuation continuation = ContinuationSupport.getContinuation(request, null);
            continuation.suspend(Long.parseLong(request.getParameter("continue")));
        }
        
        base_request.setHandled(true);
        response.setHeader(HttpHeaders.CONTENT_TYPE,MimeTypes.TEXT_HTML);
        
        OutputStream out = response.getOutputStream();
        ByteArrayOutputStream buf = new ByteArrayOutputStream(2048);
        Writer writer = new OutputStreamWriter(buf,StringUtil.__ISO_8859_1);
        writer.write("<html><h1>"+label+"</h1>");
        writer.write("<pre>\npathInfo="+request.getPathInfo()+"\n</pre>\n");
        writer.write("<pre>\ncontentType="+request.getContentType()+"\n</pre>\n");
        writer.write("<pre>\nencoding="+request.getCharacterEncoding()+"\n</pre>\n");
        writer.write("<h3>Header:</h3><pre>");
        writer.write(request.toString());
        writer.write("</pre>\n<h3>Parameters:</h3>\n<pre>");
        Enumeration names=request.getParameterNames();
        while(names.hasMoreElements())
        {
            String name=names.nextElement().toString();
            String[] values=request.getParameterValues(name);
            if (values==null || values.length==0)
            {
                writer.write(name);
                writer.write("=\n");
            }
            else if (values.length==1)
            {
                writer.write(name);
                writer.write("=");
                writer.write(values[0]);
                writer.write("\n");
            }
            else
            {
                for (int i=0; i<values.length; i++)
                {
                    writer.write(name);
                    writer.write("["+i+"]=");
                    writer.write(values[i]);
                    writer.write("\n");
                }
            }
        }
        
        String cookie_name=request.getParameter("CookieName");
        if (cookie_name!=null && cookie_name.trim().length()>0)
        {
            String cookie_action=request.getParameter("Button");
            try{
                Cookie cookie=
                    new Cookie(cookie_name.trim(),
                                    request.getParameter("CookieVal"));
                if ("Clear Cookie".equals(cookie_action))
                    cookie.setMaxAge(0);
                response.addCookie(cookie);
            }
            catch(IllegalArgumentException e)
            {
                writer.write("</pre>\n<h3>BAD Set-Cookie:</h3>\n<pre>");
                writer.write(e.toString());
            }
        }
        
        writer.write("</pre>\n<h3>Cookies:</h3>\n<pre>");
        Cookie[] cookies=request.getCookies();
        if (cookies!=null && cookies.length>0)
        {
            for(int c=0;c<cookies.length;c++)
            {
                Cookie cookie=cookies[c];
                writer.write(cookie.getName());
                writer.write("=");
                writer.write(cookie.getValue());
                writer.write("\n");
            }
        }
        
        writer.write("</pre>\n<h3>Attributes:</h3>\n<pre>");
        Enumeration attributes=request.getAttributeNames();
        if (attributes!=null && attributes.hasMoreElements())
        {
            while(attributes.hasMoreElements())
            {
                String attr=attributes.nextElement().toString();
                writer.write(attr);
                writer.write("=");
                writer.write(request.getAttribute(attr).toString());
                writer.write("\n");
            }
        }
        
        writer.write("</pre>\n<h3>Content:</h3>\n<pre>");
        char[] content= new char[4096];
        int len;
        try{
            Reader in=request.getReader();
            while((len=in.read(content))>=0)
                writer.write(new String(content,0,len));
        }
        catch(IOException e)
        {
            writer.write(e.toString());
        }
        
        writer.write("</pre>");
        writer.write("</html>");
        
        // commit now
        writer.flush();
        response.setContentLength(buf.size()+1000);
        buf.writeTo(out);
        
        buf.reset();
        writer.flush();
        for (int pad=998-buf.size();pad-->0;)
            writer.write(" ");
        writer.write("\015\012");
        writer.flush();
        buf.writeTo(out);
        
        response.setHeader("IgnoreMe","ignored");
    }
}