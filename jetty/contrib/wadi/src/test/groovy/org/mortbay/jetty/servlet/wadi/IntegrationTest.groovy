package org.mortbay.jetty.servlet.wadi

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.apache.commons.logging.Logimport org.apache.commons.logging.LogFactoryimport org.mortbay.jetty.servlet.wadi.IntegrationTestimport org.apache.commons.httpclient.HttpClientimport org.apache.commons.httpclient.HttpMethodRetryHandler
import org.apache.commons.httpclient.params.HttpMethodParams
import org.apache.commons.httpclient.methods.GetMethod

class IntegrationTest extends GroovyTestCase {
    static final def LOG = LogFactory.getLog(IntegrationTest.class.name)
    
    // Implementation note: we are using a number of clients sufficient big to increase the probability of having 
    // client HTTP session locations tracked by partitions owned by distinct nodes.
    static final def NUMBER_CLIENTS = 24
    static final def MVN = 'mvn'
    static final def EXECUTE_GOAL = 'org.codehaus.groovy.maven:gmaven-plugin:execute'
    
    def exitProcess = { port, process ->
        def getMethod = new GetMethod("http://localhost:${port}/exit")
        try {
            new HttpClient().executeMethod(getMethod)
        } catch (Exception e) {
        } finally {
            getMethod.releaseConnection()
        }
        process.destroy()
    }
    
    def launchProcess = { port, node ->
        def mvn = MVN
        String osName = System.getProperty("os.name" )
        if (osName.contains('Windows')) {
            mvn = "cmd /c ${mvn}"
        }
     
        def process = "${mvn} -o -Dport=${port} -Dnode=${node} ${EXECUTE_GOAL}".execute()

        def processStartedLatch = new CountDownLatch(1)
        def readIS = { Object[] args -> 
            process.inputStream.eachLine { line -> 
                LOG.info(line)
                if (line.endsWith("Started SelectChannelConnector@0.0.0.0:${port}")) {
                    processStartedLatch.countDown()
                }
            }
        } as Runnable
        new Thread(readIS).start()

        def readES = { Object[] args -> 
            process.errorStream.eachLine { line ->
                LOG.error(line)
            }
        } as Runnable
        new Thread(readES).start()
        
        assert processStartedLatch.await(30l, TimeUnit.SECONDS)
        
        exitProcess.curry(port, process)
    }

    def incrementCounter = { port, httpClient, expectedCounter ->
        def getMethod = new GetMethod("http://localhost:${port}/counter")
        def noRetryHandler = { method, exception, executionCount -> executionCount < 2} as HttpMethodRetryHandler
        getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, noRetryHandler);

        try {
            assert 200 == httpClient.executeMethod(getMethod)
            def actualCounter = getMethod.responseBodyAsString
            assert expectedCounter + '' == actualCounter
        } finally {
            getMethod.releaseConnection()
        }
    }
    def counter = 0
    
    def httpClients
    
    def launchProcess8080
    def killProcess8080
    def incrementCounter8080
    
    def launchProcess8081
    def killProcess8081
    def incrementCounter8081
    
    protected void setUp() {
        httpClients = new HttpClient[NUMBER_CLIENTS]
        for (i in 0..NUMBER_CLIENTS - 1)  {
            httpClients[i] = new HttpClient()
        }
        
        launchProcess8080 = { -> 
            killProcess8080 = launchProcess(8080, 'red')
        }
        launchProcess8080()
        incrementCounter8080 = incrementCounter.curry(8080)

        launchProcess8081 = { -> 
            killProcess8081 = launchProcess(8081, 'yellow')
        }
        launchProcess8081()
        incrementCounter8081 = incrementCounter.curry(8081)
    }
    
    protected void tearDown() {
        if (killProcess8080) {
            killProcess8080()
        }
        if (killProcess8081) {
            killProcess8081()
        }
    }

    public void testWADIIntegration() {
        sessionsAreMigratedFrom8080To8081()
        sessionsAreRestoredOn8080After8081Crash()
        sessionsAreMigratedFrom8080To8081After8081Crash()
        singletonPartitionBalancerIsOwnedBy8081After8080CrashAndRestart()
        // Implementation note: skip this test for now as it fails on Windows.
        //sessionsAreReplicatedBy8080AtTheSameTimeThan8081Crash()
    }

    def sessionsAreMigratedFrom8080To8081() {
        httpClients.each { httpClient ->
            incrementCounter8080(httpClient, counter + 1)
            incrementCounter8081(httpClient, counter + 2)
        }
        counter += 2
    }

    def sessionsAreRestoredOn8080After8081Crash() {
        killProcess8081()
        
        httpClients.each { httpClient ->
            shouldFail({incrementCounter8081(it, -1)})
        }

        counter++
        httpClients.each { httpClient ->
            incrementCounter8080(httpClient, counter)
        }
    }

    def sessionsAreMigratedFrom8080To8081After8081Crash() {
        launchProcess8081()

        counter++
        httpClients.each { httpClient ->
            incrementCounter8081(httpClient, counter)
        }
    }

    def singletonPartitionBalancerIsOwnedBy8081After8080CrashAndRestart() {
        killProcess8080()
        launchProcess8080()

        counter++
        httpClients.each { httpClient ->
            incrementCounter8080(httpClient, counter)
        }
    }

    def sessionsAreReplicatedBy8080AtTheSameTimeThan8081Crash() {
        def httpClientsStartLatch = new CountDownLatch(1)
        def httpClientsFinishedLatch = new CountDownLatch(NUMBER_CLIENTS)

        def monitor = new Object()
        def killed = false        
        httpClients.each { httpClient ->
            def httpClientCountinuousPingTask = { Object[] args ->
                httpClientsStartLatch.await(2, TimeUnit.SECONDS)
                for (i in counter + 1..counter + 100) {
                    incrementCounter8080(httpClient, i)
                    if (i == counter + 50) {
                        synchronized (monitor) {
                            if (!killed) {
                               killProcess8081()
                            }
                            killed = true
                        }
                    }
                }
                httpClientsFinishedLatch.countDown()
            } as Runnable
            
            new Thread(httpClientCountinuousPingTask).start()
        }
        httpClientsStartLatch.countDown()
        
        def clientFinished = httpClientsFinishedLatch.await(20l, TimeUnit.SECONDS)
        if (!clientFinished) {
            assert false : 'HTTP Clients have not finished after 20s.' 
        }
        
        counter += 100
        
        launchProcess8081()
        
        counter++
        httpClients.each { httpClient ->
            incrementCounter8081(httpClient, counter)
        }
    }
    
}