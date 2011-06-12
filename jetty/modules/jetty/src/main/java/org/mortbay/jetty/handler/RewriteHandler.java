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

package org.mortbay.jetty.handler;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.servlet.PathMap;
import org.mortbay.util.URIUtil;

/* ------------------------------------------------------------ */
/** Path Rewrite Handler
 *<p>This path uses the pattern matching of {@link PathMap} to rewrite URI's 
 * of received requests. A typical jetty.xml configuration would be: <pre>
 *     &lt;Set name="handler"&gt;
 *       &lt;New id="Handlers" class="org.mortbay.jetty.handler.RewriteHandler"&gt;
 *         &lt;Set name="rewriteRequestURI"&gt;false&lt;/Set&gt;
 *         &lt;Set name="rewritePathInfo"&gt;false&lt;/Set&gt;
 *         &lt;Set name="originalPathAttribute"&gt;requestedPath&lt;/Set&gt;
 *         &lt;Call name="addRewriteRule"&gt;&lt;Arg&gt;/other/*&lt;/Arg&gt;&lt;Arg&gt;/test&lt;/Arg&gt;&lt;/Call&gt;
 *         &lt;Call name="addRewriteRule"&gt;&lt;Arg&gt;/test/*&lt;/Arg&gt;&lt;Arg&gt;&lt;/Arg&gt;&lt;/Call&gt;
 *         &lt;Call name="addRewriteRule"&gt;&lt;Arg&gt;/*&lt;/Arg&gt;&lt;Arg&gt;/test&lt;/Arg&gt;&lt;/Call&gt;
 *         &lt;Set name="handler"&gt;
 *           &lt;New id="Handlers" class="org.mortbay.jetty.handler.HandlerCollection"&gt;
 *             &lt;Set name="handlers"&gt;
 *              &lt;Array type="org.mortbay.jetty.Handler"&gt;
 *                &lt;Item&gt;
 *                  &lt;New id="Contexts" class="org.mortbay.jetty.handler.ContextHandlerCollection"/&gt;
 *                &lt;/Item&gt;
 *                &lt;Item&gt;
 *                  &lt;New id="DefaultHandler" class="org.mortbay.jetty.handler.DefaultHandler"/&gt;
 *                &lt;/Item&gt;
 *                &lt;Item&gt;
 *                  &lt;New id="RequestLog" class="org.mortbay.jetty.handler.RequestLogHandler"/&gt;
 *                &lt;/Item&gt;
 *              &lt;/Array&gt;
 *             &lt;/Set&gt;
 *           &lt;/New&gt;
 *         &lt;/Set&gt;
 *       &lt;/New&gt;
 *     &lt;/Set&gt;
 * </pre>
 * 
 */
public class RewriteHandler extends HandlerWrapper
{
    private boolean _rewriteRequestURI=true;
    private boolean _rewritePathInfo=true;
    private String _originalPathAttribute;
    private PathMap _rewrite = new PathMap(true);

    /* ------------------------------------------------------------ */
    /**
     * @return the rewriteRequestURI If true, this handler will rewrite the value
     * returned by {@link HttpServletRequest#getRequestURI()}.
     * 
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
     * @return A {@link PathMap} of the rewriting rules.
     */
    public PathMap getRewrite()
    {
        return _rewrite;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param rewrite A {@link PathMap} of the rewriting rules. Only 
     * prefix paths should be included.
     */
    public void setRewrite(PathMap rewrite)
    {
        _rewrite=rewrite;
    }
    
    
    /* ------------------------------------------------------------ */
    /** Add a path rewriting rule
     * @param pattern The path pattern to match. The pattern must start with / and may use
     * a trailing /* as a wildcard.
     * @param prefix The path prefix which will replace the matching part of the path.
     */
    public void addRewriteRule(String pattern, String prefix)
    {
        if (pattern==null || pattern.length()==0 || !pattern.startsWith("/"))
            throw new IllegalArgumentException();
        if (_rewrite==null)
            _rewrite=new PathMap(true);
        _rewrite.put(pattern,prefix);
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.jetty.handler.HandlerWrapper#handle(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, int)
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
    {
        if (isStarted() && _rewrite!=null)
        {
            Map.Entry rewrite =_rewrite.getMatch(target);
            
            if (rewrite!=null && rewrite.getValue()!=null)
            {
                if (_originalPathAttribute!=null)
                    request.setAttribute(_originalPathAttribute,target);

                target=URIUtil.addPaths(rewrite.getValue().toString(),
                        PathMap.pathInfo(rewrite.getKey().toString(),target));

                if (_rewriteRequestURI)
                    ((Request)request).setRequestURI(target);

                if (_rewritePathInfo)
                    ((Request)request).setPathInfo(target);
            }
        }
        super.handle(target,request,response,dispatch);
        
    }
}
