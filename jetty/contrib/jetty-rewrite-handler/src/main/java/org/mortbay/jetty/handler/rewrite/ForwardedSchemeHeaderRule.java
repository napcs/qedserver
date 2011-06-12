package org.mortbay.jetty.handler.rewrite;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.rewrite.PatternRule;


/**
 * Set the scheme for the request 
 *
 * @author Ervin Varga
 * @author Athena Yao
 */
public class ForwardedSchemeHeaderRule extends HeaderRule {
    private String _scheme="https";

    /* ------------------------------------------------------------ */
    public String getScheme() 
    {
        return _scheme;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param scheme the scheme to set on the request. Defaults to "https"
     */
    public void setScheme(String scheme)
    {
        _scheme = scheme;
    }
    
    /* ------------------------------------------------------------ */
    protected String apply(String target, String value, HttpServletRequest request, HttpServletResponse response) 
    {
        ((Request) request).setScheme(_scheme);
        return target;
    }    
}
