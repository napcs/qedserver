package org.mortbay.jetty.handler.rewrite;

public class LegacyRuleTest extends AbstractRuleTestCase
{
    private LegacyRule _rule;
    
    String[][] _tests=
    {
            {"/foo/bar","/*","/replace/foo/bar"},
            {"/foo/bar","/foo/*","/replace/bar"},
            {"/foo/bar","/foo/bar","/replace"}
    };
    
    public void setUp() throws Exception
    {
        super.setUp();
        _rule = new LegacyRule();
    }
    
    public void tearDown()
    {
        _rule = null;
    }
    
    public void testMatchAndApply() throws Exception
    {
        for (int i=0;i<_tests.length;i++)
        {
            _rule.addRewriteRule(_tests[i][1], "/replace");
            
            String result = _rule.matchAndApply(_tests[i][0], _request, _response);
        
            assertEquals(_tests[i][1], _tests[i][2], result);
        }
    }
    
    public void testAddRewrite()
    {
        try
        {
            _rule.addRewriteRule("*.txt", "/replace");
            fail();
        } 
        catch (IllegalArgumentException e)
        {
        }
    }
}
