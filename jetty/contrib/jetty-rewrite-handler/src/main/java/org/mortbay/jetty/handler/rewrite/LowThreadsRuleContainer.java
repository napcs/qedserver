package org.mortbay.jetty.handler.rewrite;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.AbstractConnector;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.log.Log;
import org.mortbay.thread.ThreadPool;

/**
 * {@link RuleContainer} for when the {@link ThreadPool} is low on threads
 * 
 * @author joakime
 */
public class LowThreadsRuleContainer
    extends RuleContainer
{
    private ThreadPool _threadPool;

    private Server _server;

    /* ------------------------------------------------------------------------------- */
    public Server getServer()
    {
        return _server;
    }

    /* ------------------------------------------------------------------------------- */
    public void setServer( Server server )
    {
        _server = server;
    }

    /* ------------------------------------------------------------------------------- */
    public ThreadPool getThreadPool()
    {
        return _threadPool;
    }

    /* ------------------------------------------------------------------------------- */
    private ThreadPool getThreadPool( Request request )
    {
        if ( _threadPool == null )
        {
            // Lazy load the thread pool from the connector.
            Connector connector = request.getConnection().getConnector();
            if ( connector instanceof AbstractConnector )
            {
                _threadPool = ( (AbstractConnector) connector ).getThreadPool();
                return _threadPool;
            }

            if ( _server != null )
            {
                // Next, try to load the thread pool from the server.
                _threadPool = _server.getThreadPool();
                return _threadPool;
            }
        }

        return _threadPool;
    }

    /* ------------------------------------------------------------------------------- */
    public void setThreadPool( ThreadPool pool )
    {
        _threadPool = pool;
    }

    /**
     * Process the contained rules if the threadpool is low on threads 
     * @param target target field to pass on to the contained rules
     * @param request request object to pass on to the contained rules
     * @param response response object to pass on to the contained rules
     */
    public String matchAndApply( String target, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        _threadPool = getThreadPool( (Request) request );

        if ( _threadPool == null )
        {
            Log.warn( "ThreadPool not found" );
            return target;
        }

        Log.debug( "Low on threads: ", _threadPool.isLowOnThreads() );
        if ( !_threadPool.isLowOnThreads() )
        {
            return target;
        }

        return apply( target, request, response );
    }
}
