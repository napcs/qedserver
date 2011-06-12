import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DebugHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.servlet.jetty.IncludableGzipFilter;

import com.acme.Dump;


public class DumpServer
{
    public static void main(String[] args)
        throws Exception
    {
        Server server = new Server(8080);
        DebugHandler debug = new DebugHandler();
        debug.setOutputStream(System.err);
        server.setHandler(debug);
        
        Context context = new Context(debug,"/",Context.SESSIONS);
        FilterHolder gzip=context.addFilter(IncludableGzipFilter.class,"/*",0);
        gzip.setInitParameter("uncheckedPrintWriter","true");
        context.addServlet(new ServletHolder(new Dump()), "/*");
        
        server.start();
        server.join();
    }

}
