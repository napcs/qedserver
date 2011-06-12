package org.mortbay.jetty.handler.rewrite;

import org.mortbay.jetty.HttpFields;

public class ForwardedSchemeHeaderRuleTest extends AbstractRuleTestCase
{
    private ForwardedSchemeHeaderRule _rule;
    private HttpFields _requestHeaderFields;

    public void setUp() throws Exception
    {
        super.setUp();
        _rule = new ForwardedSchemeHeaderRule();
        _requestHeaderFields = _connection.getRequestFields();
        _request.setScheme(null);
    }
    
    public void testDefaultScheme() throws Exception
    {
        setRequestHeader("X-Forwarded-Scheme", "https");
        _rule.setHeader("X-Forwarded-Scheme");
        _rule.setHeaderValue("https");
        
        _rule.matchAndApply("/", _request, _response);
        assertEquals("https", _request.getScheme());
    }

    public void testScheme() throws Exception
    {
        setRequestHeader("X-Forwarded-Scheme", "https");
        _rule.setHeader("X-Forwarded-Scheme");
        _rule.setHeaderValue("https");
        _rule.setScheme("https");

        _rule.matchAndApply("/", _request, _response);
        assertEquals("https", _request.getScheme());
        
        
        _rule.setScheme("http");
        _rule.matchAndApply("/", _request, _response);
        assertEquals("http", _request.getScheme());
    }
    
    public void testHeaderValue() throws Exception
    {
        setRequestHeader("Front-End-Https", "on");
        _rule.setHeader("Front-End-Https");
        _rule.setHeaderValue("on");
        _rule.setScheme("https");
        
        _rule.matchAndApply("/",_request,_response);
        assertEquals("https",_request.getScheme());
        _request.setScheme(null);
        
        
        // header value doesn't match rule's value
        setRequestHeader("Front-End-Https", "off");
        
        _rule.matchAndApply("/",_request,_response);
        assertEquals(null,_request.getScheme());
        _request.setScheme(null);

        
        // header value can be any value
        setRequestHeader("Front-End-Https", "any");
        _rule.setHeaderValue(null);
        
        _rule.matchAndApply("/",_request,_response);
        assertEquals("https",_request.getScheme());
    }
    
    private void setRequestHeader(String header, String headerValue)
    {
        _requestHeaderFields.put(header, headerValue);
    }
}
