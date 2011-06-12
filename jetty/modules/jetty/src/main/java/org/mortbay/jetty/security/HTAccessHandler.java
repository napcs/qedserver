// ========================================================================
// Authors : Van den Broeke Iris, Deville Daniel, Dubois Roger, Greg Wilkins
// Copyright (c) 2001 Deville Daniel. All rights reserved.
// Permission to use, copy, modify and distribute this software
// for non-commercial or commercial purposes and without fee is
// hereby granted provided that this copyright notice appears in
// all copies.
// ========================================================================

package org.mortbay.jetty.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.log.Log;
import org.mortbay.log.Logger;
import org.mortbay.resource.Resource;
import org.mortbay.util.StringUtil;
import org.mortbay.util.URIUtil;

/* ------------------------------------------------------------ */
/**
 * Handler to authenticate access using the Apache's .htaccess files.
 * 
 * @author Van den Broeke Iris
 * @author Deville Daniel
 * @author Dubois Roger
 * @author Greg Wilkins
 * @author Konstantin Metlov
 * 
 */
public class HTAccessHandler extends SecurityHandler
{
    private Handler protegee;
    private static Logger log=Log.getLogger(HTAccessHandler.class.getName());

    String _default=null;
    String _accessFile=".htaccess";

    transient HashMap _htCache=new HashMap();
    
    /**
     * DummyPrincipal
     *
     * For use when there is no user realm configured.
     */
    class DummyPrincipal implements Principal
    {
        private String _userName;

        public DummyPrincipal(String name) 
        {
            _userName=name;
        }

        public String getName()
        {
            return _userName;
        }

        public String toString()
        {
            return getName();
        }        
    }


    /* ------------------------------------------------------------ */
    /**
     * {@inheritDoc}
     * 
     * @see org.mortbay.jetty.Handler#handle(java.lang.String,
     *      javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, int)
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
    {
        Request base_request=(request instanceof Request)?(Request)request:HttpConnection.getCurrentConnection().getRequest();
        Response base_response=(response instanceof Response)?(Response)response:HttpConnection.getCurrentConnection().getResponse();

        String pathInContext=target;

        String user=null;
        String password=null;
        boolean IPValid=true;

        if (log.isDebugEnabled())
            log.debug("HTAccessHandler pathInContext="+pathInContext,null,null);

        String credentials=request.getHeader(HttpHeaders.AUTHORIZATION);

        if (credentials!=null)
        {
            credentials=credentials.substring(credentials.indexOf(' ')+1);
            credentials=B64Code.decode(credentials,StringUtil.__ISO_8859_1);
            int i=credentials.indexOf(':');
            user=credentials.substring(0,i);
            password=credentials.substring(i+1);

            if (log.isDebugEnabled())
                log.debug("User="+user+", password="+"******************************".substring(0,password.length()),null,null);
        }

        HTAccess ht=null;

        try
        {
            Resource resource=null;
            String directory=pathInContext.endsWith("/")?pathInContext:URIUtil.parentPath(pathInContext);

            // Look for htAccess resource
            while (directory!=null)
            {
                String htPath=directory+_accessFile;
                resource=((ContextHandler)getProtegee()).getResource(htPath);
                if (log.isDebugEnabled())
                    log.debug("directory="+directory+" resource="+resource,null,null);

                if (resource!=null&&resource.exists()&&!resource.isDirectory())
                    break;
                resource=null;
                directory=URIUtil.parentPath(directory);
            }

            boolean haveHtAccess=true;

            // Try default directory
            if (resource==null&&_default!=null)
            {
                resource=Resource.newResource(_default);
                if (!resource.exists()||resource.isDirectory())
                    haveHtAccess=false;
            }
            if (resource==null)
                haveHtAccess=false;

            // prevent access to htaccess files
            if (pathInContext.endsWith(_accessFile)
                // extra security
                ||pathInContext.endsWith(_accessFile+"~")||pathInContext.endsWith(_accessFile+".bak"))
            {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                base_request.setHandled(true);
                return;
            }

            if (haveHtAccess)
            {
                if (log.isDebugEnabled())
                    log.debug("HTACCESS="+resource,null,null);

                ht=(HTAccess)_htCache.get(resource);
                if (ht==null||ht.getLastModified()!=resource.lastModified())
                {
                    ht=new HTAccess(resource);
                    _htCache.put(resource,ht);
                    if (log.isDebugEnabled())
                        log.debug("HTCache loaded "+ht,null,null);
                }

                // See if there is a config problem
                if (ht.isForbidden())
                {
                    log.warn("Mis-configured htaccess: "+ht,null,null);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    base_request.setHandled(true);
                    return;
                }

                // first see if we need to handle based on method type
                Map methods=ht.getMethods();
                if (methods.size()>0&&!methods.containsKey(request.getMethod()))
                {
                    callWrappedHandler(target,request,response,dispatch);
                    return; // Nothing to check
                }

                // Check the accesss
                int satisfy=ht.getSatisfy();

                // second check IP address
                IPValid=ht.checkAccess("",request.getRemoteAddr());
                if (log.isDebugEnabled())
                    log.debug("IPValid = "+IPValid,null,null);

                // If IP is correct and satify is ANY then access is allowed
                if (IPValid==true&&satisfy==HTAccess.ANY)
                {
                    callWrappedHandler(target,request,response,dispatch);
                    return;
                }
                
                // If IP is NOT correct and satify is ALL then access is
                // forbidden
                if (IPValid==false&&satisfy==HTAccess.ALL)
                {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    base_request.setHandled(true);
                    return;
                }

                // set required page
                if (!ht.checkAuth(user,password,getUserRealm(),base_request))
                {
                    log.debug("Auth Failed",null,null);
                    response.setHeader(HttpHeaders.WWW_AUTHENTICATE,"basic realm="+ht.getName());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    base_response.complete();
                    base_request.setHandled(true);
                    return;
                }

                // set user
                if (user!=null)
                {
                    base_request.setAuthType(Constraint.__BASIC_AUTH);
                    base_request.setUserPrincipal(getPrincipal(user, getUserRealm()));
                }
            }
            
            callWrappedHandler(target,request,response,dispatch);
        }
        catch (Exception ex)
        {
            log.warn("Exception",ex);
            if (ht!=null)
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                base_request.setHandled(true);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /**
     */
    private void callWrappedHandler(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
    {
    	Handler handler=getHandler();
        if (handler!=null)
            handler.handle(target,request,response,dispatch);
    }

    /* ------------------------------------------------------------ */
    /** Get a Principal matching the user.
     * If there is no user realm, and therefore we are using a
     * htpassword file instead, then just return a dummy Principal.
     * @param user
     * @param realm
     * @return
     */
    public Principal getPrincipal (String user, UserRealm realm)
    {
        if (realm==null)
            return new DummyPrincipal(user);
        
        return realm.getPrincipal(user);
    }
    /* ------------------------------------------------------------ */
    /**
     * set functions for the following .xml administration statements.
     * 
     * <Call name="addHandler"> <Arg> <New
     * class="org.mortbay.http.handler.HTAccessHandler"> <Set
     * name="Default">./etc/htaccess</Set> <Set name="AccessFile">.htaccess</Set>
     * </New> </Arg> </Call>
     * 
     */
    public void setDefault(String dir)
    {
        _default=dir;
    }

    /* ------------------------------------------------------------ */
    public void setAccessFile(String anArg)
    {
        if (anArg==null)
            _accessFile=".htaccess";
        else
            _accessFile=anArg;
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private static class HTAccess
    {
        // private boolean _debug = false;
        static final int ANY=0;
        static final int ALL=1;
        static final String USER="user";
        static final String GROUP="group";
        static final String VALID_USER="valid-user";

        /* ------------------------------------------------------------ */
        String _userFile;
        Resource _userResource;
        HashMap _users=null;
        long _userModified;

        /* ------------------------------------------------------------ */
        String _groupFile;
        Resource _groupResource;
        HashMap _groups=null;
        long _groupModified;

        int _satisfy=0;
        String _type;
        String _name;
        HashMap _methods=new HashMap();
        HashSet _requireEntities=new HashSet();
        String _requireName;
        int _order;
        ArrayList _allowList=new ArrayList();
        ArrayList _denyList=new ArrayList();
        long _lastModified;
        boolean _forbidden=false;

        /* ------------------------------------------------------------ */
        public HTAccess(Resource resource)
        {
            BufferedReader htin=null;
            try
            {
                htin=new BufferedReader(new InputStreamReader(resource.getInputStream()));
                parse(htin);
                _lastModified=resource.lastModified();

                if (_userFile!=null)
                {
                    _userResource=Resource.newResource(_userFile);
                    if (!_userResource.exists())
                    {
                        _forbidden=true;
                        log.warn("Could not find ht user file: "+_userFile,null,null);
                    }
                    else if (log.isDebugEnabled())
                        log.debug("user file: "+_userResource,null,null);
                }

                if (_groupFile!=null)
                {
                    _groupResource=Resource.newResource(_groupFile);
                    if (!_groupResource.exists())
                    {
                        _forbidden=true;
                        log.warn("Could not find ht group file: "+_groupResource,null,null);
                    }
                    else if (log.isDebugEnabled())
                        log.debug("group file: "+_groupResource,null,null);
                }
            }
            catch (IOException e)
            {
                _forbidden=true;
                log.warn("LogSupport.EXCEPTION",e);
            }
        }

        /* ------------------------------------------------------------ */
        public boolean isForbidden()
        {
            return _forbidden;
        }

        /* ------------------------------------------------------------ */
        public HashMap getMethods()
        {
            return _methods;
        }

        /* ------------------------------------------------------------ */
        public long getLastModified()
        {
            return _lastModified;
        }

        /* ------------------------------------------------------------ */
        public Resource getUserResource()
        {
            return _userResource;
        }

        /* ------------------------------------------------------------ */
        public Resource getGroupResource()
        {
            return _groupResource;
        }

        /* ------------------------------------------------------------ */
        public int getSatisfy()
        {
            return (_satisfy);
        }

        /* ------------------------------------------------------------ */
        public String getName()
        {
            return _name;
        }

        /* ------------------------------------------------------------ */
        public String getType()
        {
            return _type;
        }

        /* ------------------------------------------------------------ */
        public boolean checkAccess(String host, String ip)
        {
            String elm;
            boolean alp=false;
            boolean dep=false;

            // if no allows and no deny defined, then return true
            if (_allowList.size()==0&&_denyList.size()==0)
                return (true);

            // looping for allows
            for (int i=0; i<_allowList.size(); i++)
            {
                elm=(String)_allowList.get(i);
                if (elm.equals("all"))
                {
                    alp=true;
                    break;
                }
                else
                {
                    char c=elm.charAt(0);
                    if (c>='0'&&c<='9')
                    {
                        // ip
                        if (ip.startsWith(elm))
                        {
                            alp=true;
                            break;
                        }
                    }
                    else
                    {
                        // hostname
                        if (host.endsWith(elm))
                        {
                            alp=true;
                            break;
                        }
                    }
                }
            }

            // looping for denies
            for (int i=0; i<_denyList.size(); i++)
            {
                elm=(String)_denyList.get(i);
                if (elm.equals("all"))
                {
                    dep=true;
                    break;
                }
                else
                {
                    char c=elm.charAt(0);
                    if (c>='0'&&c<='9')
                    { // ip
                        if (ip.startsWith(elm))
                        {
                            dep=true;
                            break;
                        }
                    }
                    else
                    { // hostname
                        if (host.endsWith(elm))
                        {
                            dep=true;
                            break;
                        }
                    }
                }
            }

            if (_order<0) // deny,allow
                return !dep||alp;
            // mutual failure == allow,deny
            return alp&&!dep;
        }

        /* ------------------------------------------------------------ */
        public boolean checkAuth(String user, String pass, UserRealm realm, Request request)
        {
            if (_requireName==null)
                return true;

            // Authenticate with realm

            Principal principal=realm==null?null:realm.authenticate(user,pass,request);
            if (principal==null)
            {
                // Have to authenticate the user with the password file
                String code=getUserCode(user);
                String salt=code!=null?code.substring(0,2):user;
                String cred=(user!=null&&pass!=null)?UnixCrypt.crypt(pass,salt):null;
                if (code==null||(code.equals("")&&!pass.equals(""))||!code.equals(cred))
                    return false;
            }

            if (_requireName.equalsIgnoreCase(USER))
            {
                if (_requireEntities.contains(user))
                    return true;
            }
            else if (_requireName.equalsIgnoreCase(GROUP))
            {
                ArrayList gps=getUserGroups(user);
                if (gps!=null)
                    for (int g=gps.size(); g-->0;)
                        if (_requireEntities.contains(gps.get(g)))
                            return true;
            }
            else if (_requireName.equalsIgnoreCase(VALID_USER))
            {
                return true;
            }

            return false;
        }

        /* ------------------------------------------------------------ */
        public boolean isAccessLimited()
        {
            if (_allowList.size()>0||_denyList.size()>0)
                return true;
            else
                return false;
        }

        /* ------------------------------------------------------------ */
        public boolean isAuthLimited()
        {
            if (_requireName!=null)
                return true;
            else
                return false;
        }

        /* ------------------------------------------------------------ */
        private String getUserCode(String user)
        {
            if (_userResource==null)
                return null;

            if (_users==null||_userModified!=_userResource.lastModified())
            {
                if (log.isDebugEnabled())
                    log.debug("LOAD "+_userResource,null,null);
                _users=new HashMap();
                BufferedReader ufin=null;
                try
                {
                    ufin=new BufferedReader(new InputStreamReader(_userResource.getInputStream()));
                    _userModified=_userResource.lastModified();
                    String line;
                    while ((line=ufin.readLine())!=null)
                    {
                        line=line.trim();
                        if (line.startsWith("#"))
                            continue;
                        int spos=line.indexOf(':');
                        if (spos<0)
                            continue;
                        String u=line.substring(0,spos).trim();
                        String p=line.substring(spos+1).trim();
                        _users.put(u,p);
                    }
                }
                catch (IOException e)
                {
                    log.warn("LogSupport.EXCEPTION",e);
                }
                finally
                {
                    try
                    {
                        if (ufin!=null)
                            ufin.close();
                    }
                    catch (IOException e2)
                    {
                        log.warn("LogSupport.EXCEPTION",e2);
                    }
                }
            }

            return (String)_users.get(user);
        }

        /* ------------------------------------------------------------ */
        private ArrayList getUserGroups(String group)
        {
            if (_groupResource==null)
                return null;

            if (_groups==null||_groupModified!=_groupResource.lastModified())
            {
                if (log.isDebugEnabled())
                    log.debug("LOAD "+_groupResource,null,null);

                _groups=new HashMap();
                BufferedReader ufin=null;
                try
                {
                    ufin=new BufferedReader(new InputStreamReader(_groupResource.getInputStream()));
                    _groupModified=_groupResource.lastModified();
                    String line;
                    while ((line=ufin.readLine())!=null)
                    {
                        line=line.trim();
                        if (line.startsWith("#")||line.length()==0)
                            continue;

                        StringTokenizer tok=new StringTokenizer(line,": \t");

                        if (!tok.hasMoreTokens())
                            continue;
                        String g=tok.nextToken();
                        if (!tok.hasMoreTokens())
                            continue;
                        while (tok.hasMoreTokens())
                        {
                            String u=tok.nextToken();
                            ArrayList gl=(ArrayList)_groups.get(u);
                            if (gl==null)
                            {
                                gl=new ArrayList();
                                _groups.put(u,gl);
                            }
                            gl.add(g);
                        }
                    }
                }
                catch (IOException e)
                {
                    log.warn("LogSupport.EXCEPTION",e);
                }
                finally
                {
                    try
                    {
                        if (ufin!=null)
                            ufin.close();
                    }
                    catch (IOException e2)
                    {
                        log.warn("LogSupport.EXCEPTION",e2);
                    }
                }
            }

            return (ArrayList)_groups.get(group);
        }

        /* ------------------------------------------------------------ */
        public String toString()
        {
            StringBuffer buf=new StringBuffer();

            buf.append("AuthUserFile=");
            buf.append(_userFile);
            buf.append(", AuthGroupFile=");
            buf.append(_groupFile);
            buf.append(", AuthName=");
            buf.append(_name);
            buf.append(", AuthType=");
            buf.append(_type);
            buf.append(", Methods=");
            buf.append(_methods);
            buf.append(", satisfy=");
            buf.append(_satisfy);
            if (_order<0)
                buf.append(", order=deny,allow");
            else if (_order>0)
                buf.append(", order=allow,deny");
            else
                buf.append(", order=mutual-failure");

            buf.append(", Allow from=");
            buf.append(_allowList);
            buf.append(", deny from=");
            buf.append(_denyList);
            buf.append(", requireName=");
            buf.append(_requireName);
            buf.append(" ");
            buf.append(_requireEntities);

            return buf.toString();
        }

        /* ------------------------------------------------------------ */
        private void parse(BufferedReader htin) throws IOException
        {
            String line;
            while ((line=htin.readLine())!=null)
            {
                line=line.trim();
                if (line.startsWith("#"))
                    continue;
                else if (line.startsWith("AuthUserFile"))
                {
                    _userFile=line.substring(13).trim();
                }
                else if (line.startsWith("AuthGroupFile"))
                {
                    _groupFile=line.substring(14).trim();
                }
                else if (line.startsWith("AuthName"))
                {
                    _name=line.substring(8).trim();
                }
                else if (line.startsWith("AuthType"))
                {
                    _type=line.substring(8).trim();
                }
                // else if (line.startsWith("<Limit")) {
                else if (line.startsWith("<Limit"))
                {
                    int limit=line.length();
                    int endp=line.indexOf('>');
                    StringTokenizer tkns;

                    if (endp<0)
                        endp=limit;
                    tkns=new StringTokenizer(line.substring(6,endp));
                    while (tkns.hasMoreTokens())
                    {
                        _methods.put(tkns.nextToken(),Boolean.TRUE);
                    }

                    while ((line=htin.readLine())!=null)
                    {
                        line=line.trim();
                        if (line.startsWith("#"))
                            continue;
                        else if (line.startsWith("satisfy"))
                        {
                            int pos1=7;
                            limit=line.length();
                            while ((pos1<limit)&&(line.charAt(pos1)<=' '))
                                pos1++;
                            int pos2=pos1;
                            while ((pos2<limit)&&(line.charAt(pos2)>' '))
                                pos2++;
                            String l_string=line.substring(pos1,pos2);
                            if (l_string.equals("all"))
                                _satisfy=1;
                            else if (l_string.equals("any"))
                                _satisfy=0;
                        }
                        else if (line.startsWith("require"))
                        {
                            int pos1=7;
                            limit=line.length();
                            while ((pos1<limit)&&(line.charAt(pos1)<=' '))
                                pos1++;
                            int pos2=pos1;
                            while ((pos2<limit)&&(line.charAt(pos2)>' '))
                                pos2++;
                            _requireName=line.substring(pos1,pos2).toLowerCase();
                            if (USER.equals(_requireName))
                                _requireName=USER;
                            else if (GROUP.equals(_requireName))
                                _requireName=GROUP;
                            else if (VALID_USER.equals(_requireName))
                                _requireName=VALID_USER;

                            pos1=pos2+1;
                            if (pos1<limit)
                            {
                                while ((pos1<limit)&&(line.charAt(pos1)<=' '))
                                    pos1++;

                                tkns=new StringTokenizer(line.substring(pos1));
                                while (tkns.hasMoreTokens())
                                {
                                    _requireEntities.add(tkns.nextToken());
                                }
                            }

                        }
                        else if (line.startsWith("order"))
                        {
                            if (log.isDebugEnabled())
                                log.debug("orderline="+line+"order="+_order,null,null);
                            if (line.indexOf("allow,deny")>0)
                            {
                                log.debug("==>allow+deny",null,null);
                                _order=1;
                            }
                            else if (line.indexOf("deny,allow")>0)
                            {
                                log.debug("==>deny,allow",null,null);
                                _order=-1;
                            }
                            else if (line.indexOf("mutual-failure")>0)
                            {
                                log.debug("==>mutual",null,null);
                                _order=0;
                            }
                            else
                            {
                            }
                        }
                        else if (line.startsWith("allow from"))
                        {
                            int pos1=10;
                            limit=line.length();
                            while ((pos1<limit)&&(line.charAt(pos1)<=' '))
                                pos1++;
                            if (log.isDebugEnabled())
                                log.debug("allow process:"+line.substring(pos1),null,null);
                            tkns=new StringTokenizer(line.substring(pos1));
                            while (tkns.hasMoreTokens())
                            {
                                _allowList.add(tkns.nextToken());
                            }
                        }
                        else if (line.startsWith("deny from"))
                        {
                            int pos1=9;
                            limit=line.length();
                            while ((pos1<limit)&&(line.charAt(pos1)<=' '))
                                pos1++;
                            if (log.isDebugEnabled())
                                log.debug("deny process:"+line.substring(pos1),null,null);

                            tkns=new StringTokenizer(line.substring(pos1));
                            while (tkns.hasMoreTokens())
                            {
                                _denyList.add(tkns.nextToken());
                            }
                        }
                        else if (line.startsWith("</Limit>"))
                            break;
                    }
                }
            }
        }
    }

    /**
     * Getter for property protegee.
     * 
     * @return Returns the protegee.
     */
    protected Handler getProtegee()
    {
        return this.protegee;
    }

    /**
     * Setter for property protegee.
     * 
     * @param protegee
     *            The protegee to set.
     */
    public void setProtegee(Handler protegee)
    {
        this.protegee=protegee;
    }

}
