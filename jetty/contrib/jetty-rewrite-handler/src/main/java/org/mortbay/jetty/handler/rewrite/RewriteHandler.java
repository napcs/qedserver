//========================================================================
//$Id: RewriteHandler.java 1918 2010-10-06 18:48:08Z jesse $
//Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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
package org.mortbay.jetty.handler.rewrite;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.HandlerWrapper;
import org.mortbay.jetty.servlet.PathMap;
import org.mortbay.log.Log;
import org.mortbay.util.LazyList;

/* ------------------------------------------------------------ */
/**
 *<p> The RewriteHandler is responsible for managing a list of rules to be applied to requests. 
 * Its capabilities are not only limited to url rewrites such as RewritePatternRule or RewriteRegexRule; 
 * there is also handling for cookies, headers, redirection, setting status or error codes 
 * whenever the rule finds a match. 
 * 
 * <p> The rules can be matched by the ff. options: pattern matching of PathMap 
 * (class PatternRule), regular expressions (class RegexRule) or certain conditions set 
 * (e.g. MsieSslRule - the requests must be in SSL mode).
 * 
 * Here are the list of rules:
 * <ul>
 * <li> CookiePatternRule - adds a new cookie in response. </li>
 * <li> HeaderPatternRule - adds/modifies the HTTP headers in response. </li>
 * <li> RedirectPatternRule - sets the redirect location. </li>
 * <li> ResponsePatternRule - sets the status/error codes. </li>
 * <li> RewritePatternRule - rewrites the requested URI. </li>
 * <li> RewriteRegexRule - rewrites the requested URI using regular expression for pattern matching. </li>
 * <li> MsieSslRule - disables the keep alive on SSL for IE5 and IE6. </li>
 * <li> LegacyRule - the old version of rewrite. </li>
 * <li> ForwardedSchemeHeaderRule - set the scheme according to the headers present. </li>
 * </ul>
 *
 * <p> The rules can be grouped into rule containers (class RuleContainerRule), and will only 
 * be applied if the request matches the conditions for their container
 * (e.g., by virtual host name)
 *
 * Here are a list of rule containers:
 * <ul>
 * <li> VirtualHostRuleContainer - checks whether the request matches one of a set of virtual host names.</li>
 * <li> LowThreadsRuleContainer - checks whether the threadpool is low on threads</li>
 * </ul>
 * 
 */
public class RewriteHandler extends HandlerWrapper
{
    
    private RuleContainer _rules;
    
    /* ------------------------------------------------------------ */
    public RewriteHandler()
    {
        _rules = new RuleContainer();
    }

    /* ------------------------------------------------------------ */
    /**
     * To enable configuration from jetty.xml on rewriteRequestURI, rewritePathInfo and
     * originalPathAttribute
     * 
     * @param legacyRule old style rewrite rule
     */
    public void setLegacyRule(LegacyRule legacyRule)
    {
        _rules.setLegacyRule(legacyRule);
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the list of rules.
     * @return an array of {@link Rule}.
     */
    public Rule[] getRules()
    {
        return _rules.getRules();
    }

    /* ------------------------------------------------------------ */
    /**
     * Assigns the rules to process.
     * @param rules an array of {@link Rule}. 
     */
    public void setRules(Rule[] rules)
    {
        _rules.setRules(rules);
    }

    /*------------------------------------------------------------ */
    /**
     * Assigns the rules to process.
     * @param rules a {@link RuleContainer} containing other rules to process
     */
    public void setRules(RuleContainer rules)
    {
        _rules = rules;
    }

    /* ------------------------------------------------------------ */
    /**
     * Add a Rule
     * @param rule The rule to add to the end of the rules array
     */
    public void addRule(Rule rule)
    {
        _rules.addRule(rule);
    }
   

    /* ------------------------------------------------------------ */
    /**
     * @return the rewriteRequestURI If true, this handler will rewrite the value
     * returned by {@link HttpServletRequest#getRequestURI()}.
     */
    public boolean isRewriteRequestURI()
    {
        return _rules.isRewriteRequestURI();
    }

    /* ------------------------------------------------------------ */
    /**
     * @param rewriteRequestURI true if this handler will rewrite the value
     * returned by {@link HttpServletRequest#getRequestURI()}.
     */
    public void setRewriteRequestURI(boolean rewriteRequestURI)
    {
        _rules.setRewriteRequestURI(rewriteRequestURI);
    }

    /* ------------------------------------------------------------ */
    /**
     * @return true if this handler will rewrite the value
     * returned by {@link HttpServletRequest#getPathInfo()}.
     */
    public boolean isRewritePathInfo()
    {
        return _rules.isRewritePathInfo();
    }

    /* ------------------------------------------------------------ */
    /**
     * @param rewritePathInfo true if this handler will rewrite the value
     * returned by {@link HttpServletRequest#getPathInfo()}.
     */
    public void setRewritePathInfo(boolean rewritePathInfo)
    {
        _rules.setRewritePathInfo(rewritePathInfo);
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the originalPathAttribute. If non null, this string will be used
     * as the attribute name to store the original request path.
     */
    public String getOriginalPathAttribute()
    {
        return _rules.getOriginalPathAttribute();
    }

    /* ------------------------------------------------------------ */
    /**
     * @param originalPathAttribute If non null, this string will be used
     * as the attribute name to store the original request path.
     */
    public void setOriginalPathAttribute(String originalPathAttribute)
    {
        _rules.setOriginalPathAttribute(originalPathAttribute);
    }


    /* ------------------------------------------------------------ */
    /**
     * @deprecated 
     */
    public PathMap getRewrite()
    {
        return _rules.getRewrite();
    }

    /* ------------------------------------------------------------ */
    /**
     * @deprecated
     */
    public void setRewrite(PathMap rewrite)
    {
        _rules.setRewrite(rewrite);
    }

    /* ------------------------------------------------------------ */
    /**
     * @deprecated
     */
    public void addRewriteRule(String pattern, String prefix)
    {
        _rules.addRewriteRule(pattern,prefix);
    }
    
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.jetty.handler.HandlerWrapper#handle(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, int)
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
    {
        if (isStarted())
        { 
            String returned = _rules.matchAndApply(target, request, response);
            target = (returned == null) ? target : returned;
            
            Request baseRequest = (request instanceof Request?(Request)request:HttpConnection.getCurrentConnection().getRequest());
            
            if (!baseRequest.isHandled())
            {
                super.handle(target, request, response, dispatch);
            }
        }
    }
    
}
