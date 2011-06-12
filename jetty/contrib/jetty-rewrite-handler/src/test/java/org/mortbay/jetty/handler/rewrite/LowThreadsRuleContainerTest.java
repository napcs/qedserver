package org.mortbay.jetty.handler.rewrite;

import org.mortbay.thread.QueuedThreadPool;
import org.mortbay.thread.ThreadPool;

/**
 * Test harness for {@link LowThreadsRuleContainer}
 *
 * @author joakime
 */
public class LowThreadsRuleContainerTest
    extends AbstractRuleTestCase
{
    RewriteHandler _handler;

    RewritePatternRule _rule;

    RewritePatternRule _warnRule;

    QueuedThreadPool _queuedThreadPool;

    LowThreadsRuleContainer _busyContainerRule;

    int maxThreads = 5;

    int lowThreads = 0;

    public void setUp()
        throws Exception
    {
        _queuedThreadPool = new QueuedThreadPool();
        _queuedThreadPool.setMinThreads( maxThreads );
        _queuedThreadPool.setLowThreads( lowThreads );
        _server.setThreadPool( _queuedThreadPool );

        _handler = new RewriteHandler();
        _handler.setRewriteRequestURI( true );

        _rule = new RewritePatternRule();
        _rule.setPattern( "/cheese/*" );
        _rule.setReplacement( "/rule" );

        _warnRule = new RewritePatternRule();
        _warnRule.setPattern( "/cheese/*" );
        _warnRule.setReplacement( "/too-busy/" );

        _busyContainerRule = new LowThreadsRuleContainer();
        _busyContainerRule.setThreadPool( _queuedThreadPool );
        _busyContainerRule.setRules( new Rule[] { _warnRule } );

        _server.setHandler( _handler );

        super.setUp();

        _request.setRequestURI( "/cheese/bar" );
    }

    public void tearDown()
    {
        _rule = null;
    }

    /**
     * Simple happy path test case.
     * The alternative rewrite rules based on low thread availability should
     * not be triggered. 
     * 
     * @throws Exception
     */
    public void testSufficientThreads()
        throws Exception
    {
        LowThreadPool pool = new LowThreadPool();
        pool.setLowOnThreads(false);
        _busyContainerRule.setThreadPool( pool );
        _handler.setRules( new Rule[] { _busyContainerRule, _rule } );
        _server.handle( "/cheese/bar", _request, _response, 0 );
        assertEquals( "/rule/bar", _request.getRequestURI() );
    }

    /**
     * Simple test case to ensure that low on threads causes appropriate 
     * rules to execute. 
     * The main rewrite rule will not execute, as the low rewrite rule is
     * handled in this situation.
     * 
     * @throws Exception
     */
    public void testLowThreads()
        throws Exception
    {
        LowThreadPool pool = new LowThreadPool();
        _busyContainerRule.setThreadPool( pool );
        _server.setThreadPool( pool );
        _handler.setRules( new Rule[] { _busyContainerRule, _rule } );

        _server.handle( "/cheese/bar", _request, _response, 0 );
        assertEquals( "/too-busy/bar", _request.getRequestURI() );
    }

    /**
     * Test the ability of the {@link LowThreadsRuleContainer} to
     * find the ThreadPool from the server. 
     * 
     * @throws Exception
     */
    public void testLowThreadDiscoverViaServer()
        throws Exception
    {
        // Let the rule container find the thread pool itself.
        _busyContainerRule.setThreadPool( null );

        // The thread pool we want to use.
        LowThreadPool pool = new LowThreadPool();
        _server.stop();
        _server.setThreadPool( pool );
        _server.start();

        // Set the rule order
        _handler.setRules( new Rule[] { _busyContainerRule, _rule } );

        _server.handle( "/cheese/bar", _request, _response, 0 );

        assertSame( "Found expected thread pool", pool, _busyContainerRule.getThreadPool() );
        assertEquals( "/too-busy/bar", _request.getRequestURI() );
    }

    /**
     * A Test ThreadPool used to test low thread pool conditions.
     * 
     * @author joakime
     */
    class LowThreadPool
        extends QueuedThreadPool
        implements ThreadPool
    {
        private boolean lowOnThreads = true;

        public void setLowOnThreads( boolean low )
        {
            this.lowOnThreads = low;
        }

        @Override
        public boolean isLowOnThreads()
        {
            return lowOnThreads;
        }
    }

}
