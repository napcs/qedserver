// ========================================================================
// Copyright 2006 Mort Bay Consulting Pty. Ltd.
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
package org.mortbay.jetty.handler.rewrite;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.handler.AbstractHandler;


public class RewriteHandlerTest extends AbstractRuleTestCase
{   
    RewriteHandler _handler;
    RewritePatternRule _rule1;
    RewritePatternRule _rule2;
    RewritePatternRule _rule3;
    
    
    public void setUp() throws Exception
    {
        _handler=new RewriteHandler();
        _server.setHandler(_handler);
        _handler.setHandler(new AbstractHandler(){

            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
            {
                response.setStatus(201);
                request.setAttribute("target",target);
                request.setAttribute("URI",request.getRequestURI());
                request.setAttribute("info",request.getPathInfo());
            }
            
        });
        
        _rule1 = new RewritePatternRule();
        _rule1.setPattern("/aaa/*");
        _rule1.setReplacement("/bbb");
        _rule2 = new RewritePatternRule();
        _rule2.setPattern("/bbb/*");
        _rule2.setReplacement("/ccc");
        _rule3 = new RewritePatternRule();
        _rule3.setPattern("/ccc/*");
        _rule3.setReplacement("/ddd");
        
        _handler.setRules(new Rule[]{_rule1,_rule2,_rule3});
        
        super.setUp();
    }    
    
    
    public void test() throws Exception
    {
        _response.setStatus(200);
        _request.setHandled(false);
        _handler.setOriginalPathAttribute("/before");
        _handler.setRewriteRequestURI(false);
        _handler.setRewritePathInfo(false);
        _request.setRequestURI("/foo/bar");
        _request.setPathInfo("/foo/bar");
        _handler.handle("/foo/bar",_request,_response,0);
        assertEquals(201,_response.getStatus());
        assertEquals("/foo/bar",_request.getAttribute("target"));
        assertEquals("/foo/bar",_request.getAttribute("URI"));
        assertEquals("/foo/bar",_request.getAttribute("info"));
        assertEquals(null,_request.getAttribute("before"));
        

        _response.setStatus(200);
        _request.setHandled(false);
        _handler.setOriginalPathAttribute(null);
        _request.setRequestURI("/aaa/bar");
        _request.setPathInfo("/aaa/bar");
        _handler.handle("/aaa/bar",_request,_response,0);
        assertEquals(201,_response.getStatus());
        assertEquals("/ddd/bar",_request.getAttribute("target"));
        assertEquals("/aaa/bar",_request.getAttribute("URI"));
        assertEquals("/aaa/bar",_request.getAttribute("info"));
        assertEquals(null,_request.getAttribute("before"));
        

        _response.setStatus(200);
        _request.setHandled(false);
        _handler.setOriginalPathAttribute("before");
        _handler.setRewriteRequestURI(true);
        _handler.setRewritePathInfo(true);
        _request.setRequestURI("/aaa/bar");
        _request.setPathInfo("/aaa/bar");
        _handler.handle("/aaa/bar",_request,_response,0);
        assertEquals(201,_response.getStatus());
        assertEquals("/ddd/bar",_request.getAttribute("target"));
        assertEquals("/ddd/bar",_request.getAttribute("URI"));
        assertEquals("/ddd/bar",_request.getAttribute("info"));
        assertEquals("/aaa/bar",_request.getAttribute("before"));
        

        _response.setStatus(200);
        _request.setHandled(false);
        _rule2.setTerminating(true);
        _request.setRequestURI("/aaa/bar");
        _request.setPathInfo("/aaa/bar");
        _handler.handle("/aaa/bar",_request,_response,0);
        assertEquals(201,_response.getStatus());
        assertEquals("/ccc/bar",_request.getAttribute("target"));
        assertEquals("/ccc/bar",_request.getAttribute("URI"));
        assertEquals("/ccc/bar",_request.getAttribute("info"));
        assertEquals("/aaa/bar",_request.getAttribute("before"));

        _response.setStatus(200);
        _request.setHandled(false);
        _rule2.setHandling(true);
        _request.setAttribute("before",null);
        _request.setAttribute("target",null);
        _request.setAttribute("URI",null);
        _request.setAttribute("info",null);
        _request.setRequestURI("/aaa/bar");
        _request.setPathInfo("/aaa/bar");
        _handler.handle("/aaa/bar",_request,_response,0);
        assertEquals(200,_response.getStatus());
        assertEquals(null,_request.getAttribute("target"));
        assertEquals(null,_request.getAttribute("URI"));
        assertEquals(null,_request.getAttribute("info"));
        assertEquals("/aaa/bar",_request.getAttribute("before"));
        assertTrue(_request.isHandled());
        
        
        
    }
}
