// ========================================================================
// Copyright 1996-2005 Mort Bay Consulting Pty. Ltd.
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

package com.acme;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mortbay.util.StringUtil;


/* ------------------------------------------------------------ */
/** Test Servlet Cookies.
 *
 * @author Greg Wilkins (gregw)
 */
public class CookieDump extends HttpServlet
{
    int redirectCount=0;

    /* ------------------------------------------------------------ */
    public void init(ServletConfig config)
         throws ServletException
    {
        super.init(config);        
    }

    /* ------------------------------------------------------------ */
    protected void handleForm(HttpServletRequest request,
                          HttpServletResponse response) 
    {
        String action = request.getParameter("Action");
        String name =  request.getParameter("Name");
        String value =  request.getParameter("Value");
        String age =  request.getParameter("Age");

        if (name!=null && name.length()>0)
        {
            Cookie cookie = new Cookie(name,value);
            if (age!=null && age.length()>0)
                cookie.setMaxAge(Integer.parseInt(age));
            response.addCookie(cookie);
        }
    }
    
    /* ------------------------------------------------------------ */
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) 
        throws ServletException, IOException
    {
        handleForm(request,response);
        String nextUrl = getURI(request)+"?R="+redirectCount++;
        String encodedUrl=response.encodeRedirectURL(nextUrl);
        response.sendRedirect(encodedUrl);
    }
        
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) 
        throws ServletException, IOException
    {
        handleForm(request,response);
        
        response.setContentType("text/html");

        
        PrintWriter out = response.getWriter();
        out.println("<h1>Cookie Dump Servlet:</h1>");       
        
        Cookie[] cookies = request.getCookies();
        
        for (int i=0;cookies!=null && i<cookies.length;i++)
        {
            out.println("<b>"+deScript(cookies[i].getName())+"</b>="+deScript(cookies[i].getValue())+"<br/>");
        }
        
        out.println("<form action=\""+response.encodeURL(getURI(request))+"\" method=\"post\">"); 

        out.println("<b>Name:</b><input type=\"text\" name=\"Name\" value=\"name\"/><br/>");
        out.println("<b>Value:</b><input type=\"text\" name=\"Value\" value=\"value\"/><br/>");
        out.println("<b>Max-Age:</b><input type=\"text\" name=\"Age\" value=\"60\"/><br/>");
        out.println("<input type=\"submit\" name=\"Action\" value=\"Set\"/>");

    }

    /* ------------------------------------------------------------ */
    public String getServletInfo() {
        return "Session Dump Servlet";
    }

    /* ------------------------------------------------------------ */
    private String getURI(HttpServletRequest request)
    {
        String uri=(String)request.getAttribute("javax.servlet.forward.request_uri");
        if (uri==null)
            uri=request.getRequestURI();
        return uri;
    }

    /* ------------------------------------------------------------ */
    protected String deScript(String string)
    {
        if (string==null)
            return null;
        string=StringUtil.replace(string, "&", "&amp;");
        string=StringUtil.replace(string, "<", "&lt;");
        string=StringUtil.replace(string, ">", "&gt;");
        return string;
    }
}
