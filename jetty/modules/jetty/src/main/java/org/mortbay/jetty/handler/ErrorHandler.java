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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpGenerator;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.MimeTypes;
import org.mortbay.util.ByteArrayISO8859Writer;
import org.mortbay.util.StringUtil;


/* ------------------------------------------------------------ */
/** Handler for Error pages
 * A handler that is registered at the org.mortbay.http.ErrorHandler
 * context attributed and called by the HttpResponse.sendError method to write a
 * error page.
 * 
 * @author Greg Wilkins (gregw)
 */
public class ErrorHandler extends AbstractHandler
{
    boolean _showStacks=true;
    String _cacheControl="must-revalidate,no-cache,no-store";
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.Handler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, int)
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException
    {
        HttpConnection connection = HttpConnection.getCurrentConnection();
        connection.getRequest().setHandled(true);
        String method = request.getMethod();
        if(!method.equals(HttpMethods.GET) && !method.equals(HttpMethods.POST) && !method.equals(HttpMethods.HEAD))
            return;
        response.setContentType(MimeTypes.TEXT_HTML_8859_1);   
        if (_cacheControl!=null)
            response.setHeader(HttpHeaders.CACHE_CONTROL, _cacheControl);     
        ByteArrayISO8859Writer writer= new ByteArrayISO8859Writer(4096);
        handleErrorPage(request, writer, connection.getResponse().getStatus(), connection.getResponse().getReason());
        writer.flush();
        response.setContentLength(writer.size());
        writer.writeTo(response.getOutputStream());
        writer.destroy();
    }

    /* ------------------------------------------------------------ */
    protected void handleErrorPage(HttpServletRequest request, Writer writer, int code, String message)
        throws IOException
    {
        writeErrorPage(request, writer, code, message, _showStacks);
    }
    
    /* ------------------------------------------------------------ */
    protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks)
        throws IOException
    {
        if (message == null)
            message=HttpGenerator.getReason(code);

        writer.write("<html>\n<head>\n");
        writeErrorPageHead(request,writer,code,message);
        writer.write("</head>\n<body>");
        writeErrorPageBody(request,writer,code,message,showStacks);
        writer.write("\n</body>\n</html>\n");
    }

    /* ------------------------------------------------------------ */
    protected void writeErrorPageHead(HttpServletRequest request, Writer writer, int code, String message)
        throws IOException
    {
        writer.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>\n");
        writer.write("<title>Error ");
        writer.write(Integer.toString(code));
        writer.write(' ');
        write(writer,message);
        writer.write("</title>\n");    
    }

    /* ------------------------------------------------------------ */
    protected void writeErrorPageBody(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks)
        throws IOException
    {
        String uri= request.getRequestURI();
        
        writeErrorPageMessage(request,writer,code,message,uri);
        if (showStacks)
            writeErrorPageStacks(request,writer);
        writer.write("<hr /><i><small>Powered by Jetty://</small></i>");
        for (int i= 0; i < 20; i++)
            writer.write("<br/>                                                \n");
    }

    /* ------------------------------------------------------------ */
    protected void writeErrorPageMessage(HttpServletRequest request, Writer writer, int code, String message,String uri)
    throws IOException
    {
        writer.write("<h2>HTTP ERROR ");
        writer.write(Integer.toString(code));
        writer.write("</h2>\n<p>Problem accessing ");
        write(writer,uri);
        writer.write(". Reason:\n<pre>    ");
        write(writer,message);
        writer.write("</pre></p>");
    }

    /* ------------------------------------------------------------ */
    protected void writeErrorPageStacks(HttpServletRequest request, Writer writer)
        throws IOException
    {
        Throwable th = (Throwable)request.getAttribute("javax.servlet.error.exception");
        while(th!=null)
        {
            writer.write("<h3>Caused by:</h3><pre>");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            th.printStackTrace(pw);
            pw.flush();
            write(writer,sw.getBuffer().toString());
            writer.write("</pre>\n");

            th =th.getCause();
        }
    }
        


    /* ------------------------------------------------------------ */
    /** Get the cacheControl.
     * @return the cacheControl header to set on error responses.
     */
    public String getCacheControl()
    {
        return _cacheControl;
    }

    /* ------------------------------------------------------------ */
    /** Set the cacheControl.
     * @param cacheControl the cacheControl header to set on error responses.
     */
    public void setCacheControl(String cacheControl)
    {
        _cacheControl = cacheControl;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return True if stack traces are shown in the error pages
     */
    public boolean isShowStacks()
    {
        return _showStacks;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param showStacks True if stack traces are shown in the error pages
     */
    public void setShowStacks(boolean showStacks)
    {
        _showStacks = showStacks;
    }

    /* ------------------------------------------------------------ */
    protected void write(Writer writer,String string)
        throws IOException
    {
        if (string==null)
            return;
        
        for (int i=0;i<string.length();i++)
        {
            char c=string.charAt(i);
            
            switch(c)
            {
                case '&' :
                    writer.write("&amp;");
                    break;
                case '<' :
                    writer.write("&lt;");
                    break;
                case '>' :
                    writer.write("&gt;");
                    break;
                    
                default:
                    if (Character.isISOControl(c) && !Character.isWhitespace(c))
                        writer.write('?');
                    else 
                        writer.write(c);
            }          
        }
    }
}
