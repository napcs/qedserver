package org.mortbay.jetty.handler.rewrite;

import org.mortbay.jetty.HttpFields;
import org.mortbay.jetty.HttpHeaderValues;
import org.mortbay.jetty.HttpHeaders;


public class MsieSslRuleTest extends AbstractRuleTestCase
{ 
    private MsieSslRule _rule;
    
    public void setUp() throws Exception
    {
        // enable SSL                
        _isSecure = true;
        
        super.setUp();
        _rule = new MsieSslRule();
    }
    
    public void testWin2kWithIE5() throws Exception
    {
        HttpFields fields = _connection.getRequestFields();
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT 5.0)");
        
        String result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        
        assertEquals(_request.getRequestURI(), result);
        assertEquals(HttpHeaderValues.CLOSE, _response.getHeader(HttpHeaders.CONNECTION));
        
        
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 5.0)");
        result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        assertEquals(_request.getRequestURI(), result);
        assertEquals(HttpHeaderValues.CLOSE, _response.getHeader(HttpHeaders.CONNECTION));;
        
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0)");
        result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        assertEquals(_request.getRequestURI(), result);
        assertEquals(HttpHeaderValues.CLOSE, _response.getHeader(HttpHeaders.CONNECTION));;
    }
    
    public void testWin2kWithIE6() throws Exception
    {        
        HttpFields fields = _connection.getRequestFields();
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
        
        String result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        
        assertEquals(_request.getRequestURI(), result);
        assertEquals(HttpHeaderValues.CLOSE, _response.getHeader(HttpHeaders.CONNECTION));
    }
    
    public void testWin2kWithIE7() throws Exception
    {        
        HttpFields fields = _connection.getRequestFields();
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.0)");
        
        String result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        
        assertEquals(null, result);
        assertEquals(null, _response.getHeader(HttpHeaders.CONNECTION));
    }
    
    public void testWin2kSP1WithIE5() throws Exception
    {
        HttpFields fields = _connection.getRequestFields();
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT 5.01)");
        
        String result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        
        assertEquals(_request.getRequestURI(), result);
        assertEquals(HttpHeaderValues.CLOSE, _response.getHeader(HttpHeaders.CONNECTION));
        
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 5.01)");
        result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        assertEquals(_request.getRequestURI(), result);
        assertEquals(HttpHeaderValues.CLOSE, _response.getHeader(HttpHeaders.CONNECTION));
        
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.01)");
        result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        assertEquals(_request.getRequestURI(), result);
        assertEquals(HttpHeaderValues.CLOSE, _response.getHeader(HttpHeaders.CONNECTION));
    }
    
    public void testWin2kSP1WithIE6() throws Exception
    {        
        HttpFields fields = _connection.getRequestFields();
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.01)");
        
        String result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        
        assertEquals(_request.getRequestURI(), result);
        assertEquals(HttpHeaderValues.CLOSE, _response.getHeader(HttpHeaders.CONNECTION));
    }
    
    public void testWin2kSP1WithIE7() throws Exception
    {        
        HttpFields fields = _connection.getRequestFields();
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.01)");
        
        String result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        
        assertEquals(null, result);
        assertEquals(null, _response.getHeader(HttpHeaders.CONNECTION));
    }
    
    public void testWinXpWithIE5() throws Exception
    {
        HttpFields fields = _connection.getRequestFields();
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT 5.1)");
        
        String result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        
        assertEquals(_request.getRequestURI(), result);
        assertEquals(HttpHeaderValues.CLOSE, _response.getHeader(HttpHeaders.CONNECTION));
        
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 5.1)");
        result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        assertEquals(_request.getRequestURI(), result);
        assertEquals(HttpHeaderValues.CLOSE, _response.getHeader(HttpHeaders.CONNECTION));
        
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.1)");
        result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        assertEquals(_request.getRequestURI(), result);
        assertEquals(HttpHeaderValues.CLOSE, _response.getHeader(HttpHeaders.CONNECTION));
    }
    
    public void testWinXpWithIE6() throws Exception
    {
        HttpFields fields = _connection.getRequestFields();
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
        
        String result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        
        assertEquals(null, result);
        assertEquals(null, _response.getHeader(HttpHeaders.CONNECTION));
    }
    
    public void testWinXpWithIE7() throws Exception
    {
        HttpFields fields = _connection.getRequestFields();
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)");
        
        String result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        
        assertEquals(null, result);
        assertEquals(null, _response.getHeader(HttpHeaders.CONNECTION));
    }
    
    public void testWinVistaWithIE5() throws Exception
    {
        HttpFields fields = _connection.getRequestFields();
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT 6.0)");
        
        String result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        
        assertEquals(_request.getRequestURI(), result);
        assertEquals(HttpHeaderValues.CLOSE, _response.getHeader(HttpHeaders.CONNECTION));
        
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 6.0)");
        result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        assertEquals(_request.getRequestURI(), result);
        assertEquals(HttpHeaderValues.CLOSE, _response.getHeader(HttpHeaders.CONNECTION));
        
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 6.0)");
        result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        assertEquals(_request.getRequestURI(), result);
        assertEquals(HttpHeaderValues.CLOSE, _response.getHeader(HttpHeaders.CONNECTION));
    }
    
    public void testWinVistaWithIE6() throws Exception
    {
        HttpFields fields = _connection.getRequestFields();
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 6.0)");
        
        String result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        
        assertEquals(null, result);
        assertEquals(null, _response.getHeader(HttpHeaders.CONNECTION));
    }
    
    public void testWinVistaWithIE7() throws Exception
    {
        HttpFields fields = _connection.getRequestFields();
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0)");
        
        String result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        
        assertEquals(null, result);
        assertEquals(null, _response.getHeader(HttpHeaders.CONNECTION));
    }
    
    public void testWithoutSsl() throws Exception
    {
        // disable SSL
        _isSecure = false;
        super.stop();
        super.start();
        
        HttpFields fields = _connection.getRequestFields();
        fields.add("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT 5.0)");
        
        String result = _rule.matchAndApply(_request.getRequestURI(), _request, _response);
        
        assertEquals(null, result);
        assertEquals(null, _response.getHeader(HttpHeaders.CONNECTION));
    }
}
