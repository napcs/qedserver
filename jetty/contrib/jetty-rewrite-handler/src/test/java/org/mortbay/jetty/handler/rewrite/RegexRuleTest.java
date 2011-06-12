package org.mortbay.jetty.handler.rewrite;

import java.io.IOException;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.rewrite.RegexRule;

import junit.framework.TestCase;

public class RegexRuleTest extends TestCase
{
    private RegexRule _rule;
    
    public void setUp()
    {
        _rule = new TestRegexRule();
        
    }
    
    public void tearDown()
    {
        _rule = null;
    }
    
    public void testTrueMatch() throws IOException
    {
        String[][] matchCases = {
                // regex: *.jsp
                {"/.*.jsp", "/hello.jsp"},
                {"/.*.jsp", "/abc/hello.jsp"},
                
                // regex: /abc or /def
                {"/abc|/def", "/abc"},
                {"/abc|/def", "/def"},
                
                // regex: *.do or *.jsp
                {".*\\.do|.*\\.jsp", "/hello.do"},
                {".*\\.do|.*\\.jsp", "/hello.jsp"},
                {".*\\.do|.*\\.jsp", "/abc/hello.do"},
                {".*\\.do|.*\\.jsp", "/abc/hello.jsp"},
                
                {"/abc/.*.htm|/def/.*.htm", "/abc/hello.htm"},
                {"/abc/.*.htm|/def/.*.htm", "/abc/def/hello.htm"},
                
                // regex: /abc/*.jsp
                {"/abc/.*.jsp", "/abc/hello.jsp"},
                {"/abc/.*.jsp", "/abc/def/hello.jsp"}
        };
        
        for (int i = 0; i < matchCases.length; i++)
        {
            String[] matchCase = matchCases[i];
            assertMatch(true, matchCase);
        }
    }
    
    public void testFalseMatch() throws IOException
    {
        String[][] matchCases = {
                {"/abc/.*.jsp", "/hello.jsp"}
        };
        
        for (int i = 0; i < matchCases.length; i++)
        {
            String[] matchCase = matchCases[i];
            assertMatch(false, matchCase);
        }
    }
    
    private void assertMatch(boolean flag, String[] matchCase) throws IOException
    {
        _rule.setRegex(matchCase[0]);
        final String uri=matchCase[1];
        String result = _rule.matchAndApply(uri,
        new Request()
        {
            public String getRequestURI()
            {
                return uri;
            }
        }, null
        );
        
        assertEquals("regex: " + matchCase[0] + " uri: " + matchCase[1], flag, result!=null);
    }
    
    private class TestRegexRule extends RegexRule
    {
        public String apply(String target,HttpServletRequest request,HttpServletResponse response, Matcher matcher) throws IOException
        {
            return target;
        }
    }
}
