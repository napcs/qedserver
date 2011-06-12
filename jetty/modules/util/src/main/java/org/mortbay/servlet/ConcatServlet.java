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

package org.mortbay.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/* ------------------------------------------------------------ */
/** Concatenation Servlet
 * This servlet may be used to concatenate multiple resources into
 * a single response.  It is intended to be used to load multiple
 * javascript or css files, but may be used for any content of the 
 * same mime type that can be meaningfully concatenated.
 * <p>
 * The servlet uses {@link RequestDispatcher#include(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
 * to combine the requested content, so dynamically generated content
 * may be combined (Eg engine.js for DWR).
 * <p>
 * The servlet uses parameter names of the query string as resource names
 * relative to the context root.  So these script tags:
 * <pre>
 *  &lt;script type="text/javascript" src="../js/behaviour.js"&gt;&lt;/script&gt;
 *  &lt;script type="text/javascript" src="../js/ajax.js&/chat/chat.js"&gt;&lt;/script&gt;
 *  &lt;script type="text/javascript" src="../chat/chat.js"&gt;&lt;/script&gt;
 * </pre> can be replaced with the single tag (with the ConcatServlet mapped to /concat):
 * <pre>
 *  &lt;script type="text/javascript" src="../concat?/js/behaviour.js&/js/ajax.js&/chat/chat.js"&gt;&lt;/script&gt;
 * </pre>
 * The {@link ServletContext#getMimeType(String)} method is used to determine the 
 * mime type of each resource.  If the types of all resources do not match, then a 415 
 * UNSUPPORTED_MEDIA_TYPE error is returned.
 * <p>
 * If the init parameter "development" is set to "true" then the servlet will run in
 * development mode and the content will be concatenated on every request. Otherwise
 * the init time of the servlet is used as the lastModifiedTime of the combined content
 * and If-Modified-Since requests are handled with 206 NOT Modified responses if 
 * appropriate. This means that when not in development mode, the servlet must be 
 * restarted before changed content will be served.
 * 
 * @author gregw
 *
 */
public class ConcatServlet extends HttpServlet
{
    boolean _development;
    long _lastModified;
    ServletContext _context;

    /* ------------------------------------------------------------ */
    public void init() throws ServletException
    {
        _lastModified=System.currentTimeMillis();
        _context=getServletContext();   
        _development="true".equals(getInitParameter("development"));
    }

    /* ------------------------------------------------------------ */
    /* 
     * @return The start time of the servlet unless in development mode, in which case -1 is returned.
     */
    protected long getLastModified(HttpServletRequest req)
    {
        return _development?-1:_lastModified;
    }
    
    /* ------------------------------------------------------------ */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String q=req.getQueryString();
        if (q==null)
        {
            resp.sendError(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        
        String[] parts = q.split("\\&");
        String type=null;
        for (int i=0;i<parts.length;i++)
        {
            String t = _context.getMimeType(parts[i]);
            if (t!=null)
            {
                if (type==null)
                    type=t;
                else if (!type.equals(t))
                {
                    resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                    return;
                }
            }   
        }

        if (type!=null)
            resp.setContentType(type);

        for (int i=0;i<parts.length;i++)
        {
            RequestDispatcher dispatcher=_context.getRequestDispatcher(parts[i]);
            if (dispatcher!=null)
                dispatcher.include(req,resp);
        }
    }
}
