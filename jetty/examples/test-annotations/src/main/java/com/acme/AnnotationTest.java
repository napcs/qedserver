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

/**
 * 
 */
package com.acme;

import java.io.IOException;

import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.RunAs;

/**
 * AnnotationTest
 * 
 * Use Annotations from within Jetty.
 * 
 * Also, use servlet spec 2.5 resource injection and lifecycle callbacks from within the web.xml
 * to set up some of the JNDI resources.
 *
 */

@RunAs("special")
public class AnnotationTest extends HttpServlet 
{
    private String postConstructResult = "";
    private String dsResult = "";
    private String envResult = "";
    private String envLookupResult = "";
    private String envResult2 ="";
    private String envLookupResult2 = "";
    private String envResult3 = "";
    private String envLookupResult3 = "";
    private String dsLookupResult = "";
    private String txResult = "";
    private String txLookupResult = "";
    private DataSource myDS;
    
    @Resource(mappedName="UserTransaction")
    private UserTransaction myUserTransaction;


    @Resource(mappedName="maxAmount")
    private Double maxAmount;
    
    @Resource(name="someAmount")
    private Double minAmount;

    @Resource
    private Double avgAmount;

   
    @Resource(mappedName="jdbc/mydatasource")
    public void setMyDatasource(DataSource ds)
    {
        myDS=ds;
    }
  
    
    @PostConstruct
    private void myPostConstructMethod ()
    {       
        postConstructResult = "Called";
       try 
       {
           dsResult = (myDS==null?"FAIL":"myDS="+myDS.toString());
       }
       catch (Exception e)
       {
           dsResult = "FAIL: "+e;
       }


       envResult = (maxAmount==null?"FAIL":"maxAmount="+maxAmount.toString());
       
       try
       {
           InitialContext ic = new InitialContext();
           envLookupResult = "java:comp/env/com.acme.AnnotationTest/maxAmount="+ic.lookup("java:comp/env/com.acme.AnnotationTest/maxAmount");
       }
       catch (Exception e)
       {
           envLookupResult = "FAIL: "+e;
       }

      envResult2 = (minAmount==null?"FAIL":"minAmount="+minAmount.toString());
      try
      {
          InitialContext ic = new InitialContext();
          envLookupResult2 = "java:comp/env/someAmount="+ic.lookup("java:comp/env/someAmount");
      }
      catch (Exception e)
      {
          envLookupResult2 = "FAIL: "+e;
      }
      envResult3 = (minAmount==null?"FAIL":"avgAmount="+avgAmount.toString());
      try
      {
          InitialContext ic = new InitialContext();
          envLookupResult3 = "java:comp/env/com.acme.AnnotationTest/avgAmount="+ic.lookup("java:comp/env/com.acme.AnnotationTest/avgAmount");
      }
      catch (Exception e)
      {
          envLookupResult3 = "FAIL: "+e;
      }
      
      
      
       try
       {
           InitialContext ic = new InitialContext();
           dsLookupResult = "java:comp/env/com.acme.AnnotationTest/myDatasource="+ic.lookup("java:comp/env/com.acme.AnnotationTest/myDatasource");
       }
       catch (Exception e)
       {
           dsLookupResult = "FAIL: "+e;
       }
       
       txResult = (myUserTransaction==null?"FAIL":"myUserTransaction="+myUserTransaction);
       try
       {
           InitialContext ic = new InitialContext();
           txLookupResult = "java:comp/env/com.acme.AnnotationTest/myUserTransaction="+ic.lookup("java:comp/env/com.acme.AnnotationTest/myUserTransaction");
       }
       catch (Exception e)
       {
           txLookupResult = "FAIL: "+e;
       }
    }
    
    @PreDestroy
    private void myPreDestroyMethod()
    {
        System.err.println("PreDestroy called");
    }
    
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
    }

    
    
    /* ------------------------------------------------------------ */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        doGet(request, response);
    }

    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {      
        try
        {
            response.setContentType("text/html");
            ServletOutputStream out = response.getOutputStream();
            out.println("<html>");
            out.println("<h1>Jetty6 Annotation Results</h1>");
            out.println("<body>");
            
            out.println("<h2>@PostConstruct Callback</h2>");
            out.println("<pre>");
            out.println("@PostConstruct");
            out.println("private void myPostConstructMethod ()");
            out.println("{}"); 
            out.println("</pre>");
            out.println("<br/><b>Result: "+postConstructResult+"</b>");
           
            
            out.println("<h2>@Resource Injection for DataSource</h2>");    
            out.println("<pre>");         
            out.println("@Resource(mappedName=\"jdbc/mydatasource\");");
            out.println("public void setMyDatasource(DataSource ds)");
            out.println("{");
            out.println("myDS=ds;");
            out.println("}");
            out.println("</pre>");
            out.println("<br/><b>Result: "+dsResult+"</b>");
            out.println("<br/><b>JNDI Lookup Result: "+dsLookupResult+"</b>");

            
            out.println("<h2>@Resource Injection for env-entry </h2>");
            out.println("<pre>");
            out.println("@Resource(mappedName=\"maxAmount\")");
            out.println("private Double maxAmount;");
            out.println("@Resource(name=\"minAmount\")");
            out.println("private Double minAmount;");
            out.println("</pre>");
            out.println("<br/><b>Result: "+envResult+": "+(maxAmount.compareTo(new Double(55))==0?" PASS":" FAIL")+"</b>");     
            out.println("<br/><b>JNDI Lookup Result: "+envLookupResult+"</b>");
            out.println("<br/><b>Result: "+envResult2+": "+(minAmount.compareTo(new Double("0.99"))==0?" PASS":" FAIL")+"</b>");     
            out.println("<br/><b>JNDI Lookup Result: "+envLookupResult2+"</b>");
            out.println("<br/><b>Result: "+envResult3+": "+(avgAmount.compareTo(new Double("1.25"))==0?" PASS":" FAIL")+"</b>");     
            out.println("<br/><b>JNDI Lookup Result: "+envLookupResult3+"</b>");          
         
            out.println("<h2>@Resource Injection for UserTransaction </h2>");
            out.println("<pre>");
            out.println("@Resource(mappedName=\"UserTransaction\")");
            out.println("private UserTransaction myUserTransaction;");
            out.println("</pre>");
            out.println("<br/><b>Result: "+txResult+"</b>");
            out.println("<br/><b>JNDI Lookup Result: "+txLookupResult+"</b>");
            
            out.println("<h2>@RunAs</h2>");
            boolean result = request.isUserInRole("special");
            out.println("<br/><b>Result: isUserInRole(\"special\")="+result+":"+(result==true?" PASS":" FAIL")+"</b>");    
            result = request.isUserInRole("other");
            out.println("<br/><b>Result: isUserInRole(\"other\")="+result+":"+ (result==false?" PASS":" FAIL")+"</b>");
            
            
            out.println("</body>");            
            out.println("</html>");
            out.flush();
        }
        catch (Exception e)
        {
            throw new ServletException(e);
        }
    }
    

  
   
}
