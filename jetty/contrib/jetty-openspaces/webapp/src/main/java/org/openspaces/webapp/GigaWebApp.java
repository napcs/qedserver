package org.openspaces.webapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HandlerContainer;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.HandlerWrapper;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.util.IO;
import org.mortbay.util.URIUtil;
import org.mortbay.util.UrlEncoded;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.context.GigaSpaceContext;

/**
 */
public class GigaWebApp
{
    public final String GIGASPACE_ATTR = "org.openspaces.core.GigaSpace";
    @GigaSpaceContext
    private GigaSpace gigaSpace;
    
    private Server _server;
    private File _tmp;
    
    private WebAppContext _webAppContext;
    private HandlerContainer _container;
    
    private String _contextPath="/";
    private String _webapp="webapp/";

    
    public GigaSpace getGigaSpace()
    {
        return gigaSpace;
    }

    public void setGigaSpace(GigaSpace gigaSpace)
    {
        this.gigaSpace = gigaSpace;
    }

    public Server getServer()
    {
        return _server;
    }

    public void setServer(Server server)
    {
        this._server = server;
    }

    public String getWebapp()
    {
        return _webapp;
    }

    public void setWebapp(String webapp)
    {
        _webapp = webapp;
    }

    public WebAppContext getWebAppContext()
    {
        return _webAppContext;
    }

    public void setWebAppContext(WebAppContext appContext)
    {
        _webAppContext = appContext;
    }

    public String getContextPath()
    {
        return _contextPath;
    }

    public void setContextPath(String path)
    {
        _contextPath = path;
    }

    public void deploy() throws Exception 
    {
    	//gigaSpace.
    	
        if (_server==null)
            throw new IllegalStateException("No Server");
        
        System.out.println("--- Deploy " + this+ " to "+_server.getConnectors()[0].getLocalPort());

        org.jini.rio.boot.ServiceClassLoader loader = (org.jini.rio.boot.ServiceClassLoader)Thread.currentThread().getContextClassLoader();
        System.out.println("\nloader="+loader);
        System.out.println("isA="+loader.getClass());
        System.out.println("search="+Arrays.asList(loader.getSearchPath()));
        URL url = loader.getSearchPath()[0];
        System.out.println("URL="+url);
        
        _tmp = File.createTempFile("gigawebapp",".tmp");
        _tmp.delete();
        _tmp.mkdir();
        String docroot = URIUtil.addPaths(url.toString(),_webapp);

        System.out.println("docroot="+docroot);
        System.out.println("tmp="+_tmp);
        copy(docroot,_tmp);
        
        System.out.println("\n");
        
        if (_webAppContext==null)
            _webAppContext=new WebAppContext();
        
        _webAppContext.setContextPath("/");
        
        File tmp = new File(_tmp,"ctx");
        tmp.mkdir();
        _webAppContext.setTempDirectory(tmp);
        
        _webAppContext.setWar(URIUtil.addPaths(_tmp.toURL().toString(),_webapp));
        _webAppContext.setExtractWAR(true);
        _webAppContext.setCopyWebDir(false);
        
        _webAppContext.setAttribute(GIGASPACE_ATTR,getGigaSpace());
        
        // Look for a place to add the webappcontext
        _container=_server;
        
        Handler[] contexts = _container.getChildHandlersByClass(ContextHandlerCollection.class);
        if (contexts!=null && contexts.length>0)
            _container=(HandlerContainer)contexts[0];
        else
        {
            while (_container!=null)
            {
                if (_container instanceof HandlerWrapper)
                {
                    HandlerWrapper wrapper = (HandlerWrapper)_container;
                    Handler handler=wrapper.getHandler();
                    if (handler==null)
                        break;
                    if (handler instanceof HandlerContainer)
                        _container=(HandlerContainer)handler;
                    else
                        throw new IllegalStateException("No container");
                }
                throw new IllegalStateException("No container");
            }
        }

        _container.addHandler(_webAppContext);
        if (_container.isStarted() || _container.isStarting())
            _webAppContext.start();
    }
    
    private void copy(String url, File toDir) throws IOException
    {
        System.err.println("url="+url+" dir="+toDir);
        if (url.endsWith("/"))
        {
            String basename=url.substring(url.lastIndexOf("/",url.length()-2)+1);
            File directory=new File(toDir,basename);
            directory.mkdir();
            
            LineNumberReader reader = new LineNumberReader(new InputStreamReader(new URL(url).openStream(),"utf-8"));
            String line;
            while ((line=reader.readLine())!=null)
            {
                System.err.println(line.replace('\t','|'));
                int tab=line.lastIndexOf('\t',line.lastIndexOf('\t',line.lastIndexOf('\t')-1)-1);
                System.err.println(tab);
                char type=line.charAt(tab+1);
                String file=line.substring(0,tab);

                copy(url+URIUtil.encodePath(file)+(type=='d'?"/":""),directory);                
            }
        }
        else
        {
            String basename=url.substring(url.lastIndexOf("/")+1);
            File file = new File(toDir,basename);
            System.err.println("copy "+url+" --> "+file);
            IO.copy(new URL(url).openStream(),new FileOutputStream(file));
        }
    }

    public void undeploy() throws Exception 
    {
        System.out.println("--- Undeploy " + this);

        if (!_webAppContext.isRunning())
            return;
        
        _webAppContext.stop();
        
        if (_container!=null)
            _server.removeHandler(_webAppContext);
        
        IO.delete(_tmp);
    }
    
}
