package org.mortbay.jetty.handler.rewrite;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.servlet.PathMap;
import org.mortbay.log.Log;
import org.mortbay.util.LazyList;

/**
 * Base container to group rules. Can be extended so that the contained rules
 * will only be applied under certain conditions
 * 
 * @author Athena Yao
 */

public class RuleContainer extends Rule
{
    protected Rule[] _rules;
    protected boolean _handled;
    
    protected String _originalPathAttribute;
    protected boolean _rewriteRequestURI=true;
    protected boolean _rewritePathInfo=true;
    
    protected LegacyRule _legacy;

    /* ------------------------------------------------------------ */
    private LegacyRule getLegacyRule()
    {
        if (_legacy==null)
        {
            _legacy= new LegacyRule();
            addRule(_legacy);
        }
        return _legacy;
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
        _legacy = legacyRule;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the list of rules.
     * @return an array of {@link Rule}.
     */
    public Rule[] getRules()
    {
        return _rules;
    }

    /* ------------------------------------------------------------ */
    /**
     * Assigns the rules to process.
     * @param rules an array of {@link Rule}. 
     */
    public void setRules(Rule[] rules)
    {
        if (_legacy==null)
            _rules = rules;
        else
        {
            _rules=null;
            addRule(_legacy);
            if (rules!=null)
                for (Rule rule:rules)
                    addRule(rule);
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * Add a Rule
     * @param rule The rule to add to the end of the rules array
     */
    public void addRule(Rule rule)
    {
        _rules = (Rule[])LazyList.addToArray(_rules,rule,Rule.class);
    }
   

    /* ------------------------------------------------------------ */
    /**
     * @return the rewriteRequestURI If true, this handler will rewrite the value
     * returned by {@link HttpServletRequest#getRequestURI()}.
     */
    public boolean isRewriteRequestURI()
    {
        return _rewriteRequestURI;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param rewriteRequestURI true if this handler will rewrite the value
     * returned by {@link HttpServletRequest#getRequestURI()}.
     */
    public void setRewriteRequestURI(boolean rewriteRequestURI)
    {
        _rewriteRequestURI=rewriteRequestURI;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return true if this handler will rewrite the value
     * returned by {@link HttpServletRequest#getPathInfo()}.
     */
    public boolean isRewritePathInfo()
    {
        return _rewritePathInfo;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param rewritePathInfo true if this handler will rewrite the value
     * returned by {@link HttpServletRequest#getPathInfo()}.
     */
    public void setRewritePathInfo(boolean rewritePathInfo)
    {
        _rewritePathInfo=rewritePathInfo;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the originalPathAttribte. If non null, this string will be used
     * as the attribute name to store the original request path.
     */
    public String getOriginalPathAttribute()
    {
        return _originalPathAttribute;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param originalPathAttribte If non null, this string will be used
     * as the attribute name to store the original request path.
     */
    public void setOriginalPathAttribute(String originalPathAttribte)
    {
        _originalPathAttribute=originalPathAttribte;
    }


    /* ------------------------------------------------------------ */
    /**
     * @deprecated 
     */
    public PathMap getRewrite()
    {
        return getLegacyRule().getRewrite();
    }

    /* ------------------------------------------------------------ */
    /**
     * @deprecated
     */
    public void setRewrite(PathMap rewrite)
    {
        getLegacyRule().setRewrite(rewrite);
    }

    /* ------------------------------------------------------------ */
    /**
     * @deprecated
     */
    public void addRewriteRule(String pattern, String prefix)
    {
        getLegacyRule().addRewriteRule(pattern,prefix);
    }

    
    /**
     * @see http://jira.codehaus.org/browse/JETTY-1287
     * 
     * @return handled true if one of the rules within the rule container is handling the request 
     * 
     * @deprecated not thread safe, better to rely on baseRequest.isHandled()
     */
    public boolean isHandled()
    {
        return _handled;
    }
    
    /*------------------------------------------------------------ */
    /**
     * @param handled true if one of the rules within the rule container is handling the request
     * 
     * @deprecated best to use the baseRequest.isHandled()
     */
    public void setHandled(boolean handled)
    {
        _handled=handled;
    }
    

    /**
     * Process the contained rules
     * @param target target field to pass on to the contained rules
     * @param request request object to pass on to the contained rules
     * @param response response object to pass on to the contained rules
     */
    @Override
    public String matchAndApply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        return apply(target, request, response);
    }

    /**
     * Process the contained rules (called by matchAndApply) 
     * @param target target field to pass on to the contained rules
     * @param request request object to pass on to the contained rules
     * @param response response object to pass on to the contained rules
     */
    protected String apply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        _handled=false;
        
        boolean original_set=_originalPathAttribute==null;
                
        for (Rule rule : _rules)
        {
            String applied=rule.matchAndApply(target,request, response);
            if (applied!=null)
            {       
                Log.debug("applied {}",rule);
                if (!target.equals(applied))
                { 
                    Log.debug("rewrote {} to {}",target,applied);
                    if (!original_set)
                    {
                        original_set=true;
                        request.setAttribute(_originalPathAttribute, target);
                    }     
                    
                    if (_rewriteRequestURI)
                        ((Request)request).setRequestURI(applied);

                    if (_rewritePathInfo)
                        ((Request)request).setPathInfo(applied);

                    target=applied;
                }
                
                if (rule.isHandling())
                {
                    Log.debug("handling {}",rule);
                    _handled=true;// leaving for historical purposes - http://jira.codehaus.org/browse/JETTY-1287
                    (request instanceof Request?(Request)request:HttpConnection.getCurrentConnection().getRequest()).setHandled(true);
                }

                if (rule.isTerminating())
                {
                    Log.debug("terminating {}",rule);
                    break;
                }
            }
        }

        return target;
    }
}
