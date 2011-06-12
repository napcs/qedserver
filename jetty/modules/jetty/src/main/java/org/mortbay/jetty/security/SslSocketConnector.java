// ========================================================================
// Copyright 2000-2005 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.jetty.security;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.mortbay.io.EndPoint;
import org.mortbay.io.bio.SocketEndPoint;
import org.mortbay.jetty.HttpSchemes;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.log.Log;
import org.mortbay.resource.Resource;

/* ------------------------------------------------------------ */
/**
 * JSSE Socket Listener.
 * 
 * This specialization of HttpListener is an abstract listener that can be used as the basis for a
 * specific JSSE listener.
 * 
 * This is heavily based on the work from Court Demas, which in turn is based on the work from Forge
 * Research.
 * 
 * @author Greg Wilkins (gregw@mortbay.com)
 * @author Court Demas (court@kiwiconsulting.com)
 * @author Forge Research Pty Ltd ACN 003 491 576
 * @author Jan Hlavat�
 */
public class SslSocketConnector extends SocketConnector
{
    /**
     * The name of the SSLSession attribute that will contain any cached information.
     */
    static final String CACHED_INFO_ATTR = CachedInfo.class.getName();

    /** Default value for the keystore location path. */
    public static final String DEFAULT_KEYSTORE = System.getProperty("user.home") + File.separator
            + ".keystore";

    /** String name of key password property. */
    public static final String KEYPASSWORD_PROPERTY = "jetty.ssl.keypassword";

    /** String name of keystore password property. */
    public static final String PASSWORD_PROPERTY = "jetty.ssl.password";

    /**
     * Return the chain of X509 certificates used to negotiate the SSL Session.
     * <p>
     * Note: in order to do this we must convert a javax.security.cert.X509Certificate[], as used by
     * JSSE to a java.security.cert.X509Certificate[],as required by the Servlet specs.
     * 
     * @param sslSession the javax.net.ssl.SSLSession to use as the source of the cert chain.
     * @return the chain of java.security.cert.X509Certificates used to negotiate the SSL
     *         connection. <br>
     *         Will be null if the chain is missing or empty.
     */
    private static X509Certificate[] getCertChain(SSLSession sslSession)
    {
        try
        {
            javax.security.cert.X509Certificate javaxCerts[] = sslSession.getPeerCertificateChain();
            if (javaxCerts == null || javaxCerts.length == 0)
                return null;

            int length = javaxCerts.length;
            X509Certificate[] javaCerts = new X509Certificate[length];

            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            for (int i = 0; i < length; i++)
            {
                byte bytes[] = javaxCerts[i].getEncoded();
                ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
                javaCerts[i] = (X509Certificate) cf.generateCertificate(stream);
            }

            return javaCerts;
        }
        catch (SSLPeerUnverifiedException pue)
        {
            return null;
        }
        catch (Exception e)
        {
            Log.warn(Log.EXCEPTION, e);
            return null;
        }
    }


    /** Default value for the cipher Suites. */
    private String _excludeCipherSuites[] = null;
    
    /** Default value for the keystore location path. */
    private String _keystore=DEFAULT_KEYSTORE ;
    private String _keystoreType = "JKS"; // type of the key store
    
    /** Set to true if we require client certificate authentication. */
    private boolean _needClientAuth = false;
    private transient Password _password;
    private transient Password _keyPassword;
    private transient Password _trustPassword;
    private String _protocol= "TLS";
    private String _provider;
    private String _secureRandomAlgorithm; // cert algorithm
    private String _sslKeyManagerFactoryAlgorithm = (Security.getProperty("ssl.KeyManagerFactory.algorithm")==null?"SunX509":Security.getProperty("ssl.KeyManagerFactory.algorithm")); // cert algorithm
    private String _sslTrustManagerFactoryAlgorithm = (Security.getProperty("ssl.TrustManagerFactory.algorithm")==null?"SunX509":Security.getProperty("ssl.TrustManagerFactory.algorithm")); // cert algorithm
    
    private String _truststore;
    private String _truststoreType = "JKS"; // type of the key store

    /** Set to true if we would like client certificate authentication. */
    private boolean _wantClientAuth = false;
    private int _handshakeTimeout = 0; //0 means use maxIdleTime

    private boolean _allowRenegotiate =false;

    /* ------------------------------------------------------------ */
    /**
     * Constructor.
     */
    public SslSocketConnector()
    {
        super();
    }


    /* ------------------------------------------------------------ */
    /**
     * @return True if SSL re-negotiation is allowed (default false)
     */
    public boolean isAllowRenegotiate()
    {
        return _allowRenegotiate;
    }

    /* ------------------------------------------------------------ */
    /**
     * Set if SSL re-negotiation is allowed. CVE-2009-3555 discovered
     * a vulnerability in SSL/TLS with re-negotiation.  If your JVM
     * does not have CVE-2009-3555 fixed, then re-negotiation should 
     * not be allowed.
     * @param allowRenegotiate true if re-negotiation is allowed (default false)
     */
    public void setAllowRenegotiate(boolean allowRenegotiate)
    {
        _allowRenegotiate = allowRenegotiate;
    }

    /* ------------------------------------------------------------ */
    public void accept(int acceptorID)
        throws IOException, InterruptedException
    {   
        try
        {
            Socket socket = _serverSocket.accept();
            configure(socket);

            Connection connection=new SslConnection(socket);
            connection.dispatch();
        }
        catch(SSLException e)
        {
            Log.warn(e);
            try
            {
                stop();
            }
            catch(Exception e2)
            {
                Log.warn(e2);
                throw new IllegalStateException(e2.getMessage());
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    protected void configure(Socket socket)
        throws IOException
    {   
        super.configure(socket);
    }

    /* ------------------------------------------------------------ */
    protected SSLServerSocketFactory createFactory() 
        throws Exception
    {
        if (_truststore==null)
        {
            _truststore=_keystore;
            _truststoreType=_keystoreType;
        }

        KeyManager[] keyManagers = null;
        InputStream keystoreInputStream = null;
        KeyStore keyStore = null;
        try
        {
			if (_keystore != null) 
			{
				keystoreInputStream = Resource.newResource(_keystore).getInputStream();
			}

			keyStore = KeyStore.getInstance(_keystoreType);
			keyStore.load(keystoreInputStream, _password == null ? null : _password.toString().toCharArray());
		} 
        finally 
		{
			if (keystoreInputStream != null) 
			{
				keystoreInputStream.close();
			}
		}
        
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(_sslKeyManagerFactoryAlgorithm);        
        keyManagerFactory.init(keyStore,_keyPassword==null?null:_keyPassword.toString().toCharArray());
        keyManagers = keyManagerFactory.getKeyManagers();

        TrustManager[] trustManagers = null;
        InputStream truststoreInputStream = null;
        KeyStore trustStore = null;
        
		try
		{
			if (_truststore != null)
			{
				truststoreInputStream = Resource.newResource(_truststore).getInputStream();
			}
			trustStore = KeyStore.getInstance(_truststoreType);
			trustStore.load(truststoreInputStream, _trustPassword == null ? null : _trustPassword.toString().toCharArray());
		} 
		finally 
		{
			if (truststoreInputStream != null) 
			{
				truststoreInputStream.close();
			}
		}
        
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(_sslTrustManagerFactoryAlgorithm);
        trustManagerFactory.init(trustStore);
        trustManagers = trustManagerFactory.getTrustManagers();
        

        SecureRandom secureRandom = _secureRandomAlgorithm==null?null:SecureRandom.getInstance(_secureRandomAlgorithm);

        SSLContext context = _provider==null?SSLContext.getInstance(_protocol):SSLContext.getInstance(_protocol, _provider);

        context.init(keyManagers, trustManagers, secureRandom);

        return context.getServerSocketFactory();
    }

    /* ------------------------------------------------------------ */
    /**
     * Allow the Listener a chance to customise the request. before the server does its stuff. <br>
     * This allows the required attributes to be set for SSL requests. <br>
     * The requirements of the Servlet specs are:
     * <ul>
     * <li> an attribute named "javax.servlet.request.cipher_suite" of type String.</li>
     * <li> an attribute named "javax.servlet.request.key_size" of type Integer.</li>
     * <li> an attribute named "javax.servlet.request.X509Certificate" of type
     * java.security.cert.X509Certificate[]. This is an array of objects of type X509Certificate,
     * the order of this array is defined as being in ascending order of trust. The first
     * certificate in the chain is the one set by the client, the next is the one used to
     * authenticate the first, and so on. </li>
     * </ul>
     * 
     * @param endpoint The Socket the request arrived on. 
     *        This should be a {@link SocketEndPoint} wrapping a {@link SSLSocket}.
     * @param request HttpRequest to be customised.
     */
    public void customize(EndPoint endpoint, Request request)
        throws IOException
    {
        super.customize(endpoint, request);
        request.setScheme(HttpSchemes.HTTPS);
        
        SocketEndPoint socket_end_point = (SocketEndPoint)endpoint;
        SSLSocket sslSocket = (SSLSocket)socket_end_point.getTransport();
        
        try
        {
            SSLSession sslSession = sslSocket.getSession();
            String cipherSuite = sslSession.getCipherSuite();
            Integer keySize;
            X509Certificate[] certs;

            CachedInfo cachedInfo = (CachedInfo) sslSession.getValue(CACHED_INFO_ATTR);
            if (cachedInfo != null)
            {
                keySize = cachedInfo.getKeySize();
                certs = cachedInfo.getCerts();
            }
            else
            {
                keySize = new Integer(ServletSSL.deduceKeyLength(cipherSuite));
                certs = getCertChain(sslSession);
                cachedInfo = new CachedInfo(keySize, certs);
                sslSession.putValue(CACHED_INFO_ATTR, cachedInfo);
            }

            if (certs != null)
                request.setAttribute("javax.servlet.request.X509Certificate", certs);
            else if (_needClientAuth) // Sanity check
                throw new IllegalStateException("no client auth");

            request.setAttribute("javax.servlet.request.cipher_suite", cipherSuite);
            request.setAttribute("javax.servlet.request.key_size", keySize);
        }
        catch (Exception e)
        {
            Log.warn(Log.EXCEPTION, e);
        }
    }

    /* ------------------------------------------------------------ */    
    public String[] getExcludeCipherSuites() {
        return _excludeCipherSuites;
    }

    /* ------------------------------------------------------------ */
    public String getKeystore()
    {
        return _keystore;
    }

    /* ------------------------------------------------------------ */
    public String getKeystoreType() 
    {
        return (_keystoreType);
    }

    /* ------------------------------------------------------------ */
    public boolean getNeedClientAuth()
    {
        return _needClientAuth;
    }

    /* ------------------------------------------------------------ */
    public String getProtocol() 
    {
        return _protocol;
    }

    /* ------------------------------------------------------------ */
    public String getProvider() {
	return _provider;
    }

    /* ------------------------------------------------------------ */
    public String getSecureRandomAlgorithm() 
    {
        return (this._secureRandomAlgorithm);
    }

    /* ------------------------------------------------------------ */
    public String getSslKeyManagerFactoryAlgorithm() 
    {
        return (this._sslKeyManagerFactoryAlgorithm);
    }

    /* ------------------------------------------------------------ */
    public String getSslTrustManagerFactoryAlgorithm() 
    {
        return (this._sslTrustManagerFactoryAlgorithm);
    }

    /* ------------------------------------------------------------ */
    public String getTruststore()
    {
        return _truststore;
    }

    /* ------------------------------------------------------------ */
    public String getTruststoreType()
    {
        return _truststoreType;
    }

    /* ------------------------------------------------------------ */
    public boolean getWantClientAuth()
    {
        return _wantClientAuth;
    }

    /* ------------------------------------------------------------ */
    /**
     * By default, we're confidential, given we speak SSL. But, if we've been told about an
     * confidential port, and said port is not our port, then we're not. This allows separation of
     * listeners providing INTEGRAL versus CONFIDENTIAL constraints, such as one SSL listener
     * configured to require client certs providing CONFIDENTIAL, whereas another SSL listener not
     * requiring client certs providing mere INTEGRAL constraints.
     */
    public boolean isConfidential(Request request)
    {
        final int confidentialPort = getConfidentialPort();
        return confidentialPort == 0 || confidentialPort == request.getServerPort();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * By default, we're integral, given we speak SSL. But, if we've been told about an integral
     * port, and said port is not our port, then we're not. This allows separation of listeners
     * providing INTEGRAL versus CONFIDENTIAL constraints, such as one SSL listener configured to
     * require client certs providing CONFIDENTIAL, whereas another SSL listener not requiring
     * client certs providing mere INTEGRAL constraints.
     */
    public boolean isIntegral(Request request)
    {
        final int integralPort = getIntegralPort();
        return integralPort == 0 || integralPort == request.getServerPort();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param addr The {@link SocketAddress address} that this server should listen on 
     * @param backlog See {@link ServerSocket#bind(java.net.SocketAddress, int)}
     * @return A new {@link ServerSocket socket object} bound to the supplied address with all other
     * settings as per the current configuration of this connector. 
     * @see #setWantClientAuth
     * @see #setNeedClientAuth
     * @see #setCipherSuites
     * @exception IOException
     */

    /* ------------------------------------------------------------ */
    protected ServerSocket newServerSocket(String host, int port,int backlog) throws IOException
    {
        SSLServerSocketFactory factory = null;
        SSLServerSocket socket = null;

        try
        {
            factory = createFactory();

            socket = (SSLServerSocket) (host==null?
                            factory.createServerSocket(port,backlog):
                            factory.createServerSocket(port,backlog,InetAddress.getByName(host)));

            if (_wantClientAuth)
                socket.setWantClientAuth(_wantClientAuth);
            if (_needClientAuth)
                socket.setNeedClientAuth(_needClientAuth);

            if (_excludeCipherSuites != null && _excludeCipherSuites.length >0) 
            {
                List excludedCSList = Arrays.asList(_excludeCipherSuites);
                String[] enabledCipherSuites = socket.getEnabledCipherSuites();
            	List enabledCSList = new ArrayList(Arrays.asList(enabledCipherSuites));
            	Iterator exIter = excludedCSList.iterator();

                while (exIter.hasNext())
            	{
            	    String cipherName = (String)exIter.next();
                    if (enabledCSList.contains(cipherName))
                    {
                        enabledCSList.remove(cipherName);
                    }
            	}
                enabledCipherSuites = (String[])enabledCSList.toArray(new String[enabledCSList.size()]);

                socket.setEnabledCipherSuites(enabledCipherSuites);
            }
            
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Log.warn(e.toString());
            Log.debug(e);
            throw new IOException("!JsseListener: " + e);
        }
        return socket;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @author Tony Jiang
     */
    public void setExcludeCipherSuites(String[] cipherSuites) {
        this._excludeCipherSuites = cipherSuites;
    }

    /* ------------------------------------------------------------ */
    public void setKeyPassword(String password)
    {
        _keyPassword = Password.getPassword(KEYPASSWORD_PROPERTY,password,null);
    }

    /* ------------------------------------------------------------ */
    /**
     * @param keystore The resource path to the keystore, or null for built in keystores.
     */
    public void setKeystore(String keystore)
    {
        _keystore = keystore;
    }

    /* ------------------------------------------------------------ */
    public void setKeystoreType(String keystoreType) 
    {
        _keystoreType = keystoreType;
    }

    /* ------------------------------------------------------------ */
    /**
     * Set the value of the needClientAuth property
     * 
     * @param needClientAuth true iff we require client certificate authentication.
     */
    public void setNeedClientAuth(boolean needClientAuth)
    {
        _needClientAuth = needClientAuth;
    }
    
    /* ------------------------------------------------------------ */
    public void setPassword(String password)
    {
        _password = Password.getPassword(PASSWORD_PROPERTY,password,null);
    }
    
    /* ------------------------------------------------------------ */
    public void setTrustPassword(String password)
    {
        _trustPassword = Password.getPassword(PASSWORD_PROPERTY,password,null);
    }

    /* ------------------------------------------------------------ */
    public void setProtocol(String protocol) 
    {
        _protocol = protocol;
    }

    /* ------------------------------------------------------------ */
    public void setProvider(String _provider) {
	this._provider = _provider;
    }

    /* ------------------------------------------------------------ */
    public void setSecureRandomAlgorithm(String algorithm) 
    {
        this._secureRandomAlgorithm = algorithm;
    }

    /* ------------------------------------------------------------ */
    public void setSslKeyManagerFactoryAlgorithm(String algorithm) 
    {
        this._sslKeyManagerFactoryAlgorithm = algorithm;
    }
    
    /* ------------------------------------------------------------ */
    public void setSslTrustManagerFactoryAlgorithm(String algorithm) 
    {
        this._sslTrustManagerFactoryAlgorithm = algorithm;
    }


    public void setTruststore(String truststore)
    {
        _truststore = truststore;
    }
    

    public void setTruststoreType(String truststoreType)
    {
        _truststoreType = truststoreType;
    }

    /* ------------------------------------------------------------ */
    /**
     * Set the value of the _wantClientAuth property. This property is used when
     * {@link #newServerSocket(SocketAddress, int) opening server sockets}.
     * 
     * @param wantClientAuth true iff we want client certificate authentication.
     * @see SSLServerSocket#setWantClientAuth
     */
    public void setWantClientAuth(boolean wantClientAuth)
    {
        _wantClientAuth = wantClientAuth;
    }

    /**
     * Set the time in milliseconds for so_timeout during ssl handshaking
     * @param msec a non-zero value will be used to set so_timeout during
     * ssl handshakes. A zero value means the maxIdleTime is used instead.
     */
    public void setHandshakeTimeout (int msec)
    {
        _handshakeTimeout = msec;
    }
    
    
    public int getHandshakeTimeout ()
    {
        return _handshakeTimeout;
    }
    /**
     * Simple bundle of information that is cached in the SSLSession. Stores the effective keySize
     * and the client certificate chain.
     */
    private class CachedInfo
    {
        private X509Certificate[] _certs;
        private Integer _keySize;

        CachedInfo(Integer keySize, X509Certificate[] certs)
        {
            this._keySize = keySize;
            this._certs = certs;
        }

        X509Certificate[] getCerts()
        {
            return _certs;
        }

        Integer getKeySize()
        {
            return _keySize;
        }
    }
    
    
    public class SslConnection extends Connection
    {
        public SslConnection(Socket socket) throws IOException
        {
            super(socket);
        }
        
        public void shutdownOutput() throws IOException
        {
			close();
		}

		public void run()
        {
            try
            {
                int handshakeTimeout = getHandshakeTimeout();
                int oldTimeout = _socket.getSoTimeout();
                if (handshakeTimeout > 0)            
                    _socket.setSoTimeout(handshakeTimeout);

                final SSLSocket ssl=(SSLSocket)_socket;
                ssl.addHandshakeCompletedListener(new HandshakeCompletedListener()
                {
                    boolean handshook=false;
                    public void handshakeCompleted(HandshakeCompletedEvent event)
                    {
                        if (handshook)
                        {
                            if (!_allowRenegotiate)
                            {
                                Log.warn("SSL renegotiate denied: "+ssl);
                                try{ssl.close();}catch(IOException e){Log.warn(e);}
                            }
                        }
                        else
                            handshook=true;
                    }
                });
                ssl.startHandshake();

                if (handshakeTimeout>0)
                    _socket.setSoTimeout(oldTimeout);

                super.run();
            }
            catch (SSLException e)
            {
                Log.warn(e); 
                try{close();}
                catch(IOException e2){Log.ignore(e2);}
            }
            catch (IOException e)
            {
                Log.debug(e);
                try{close();}
                catch(IOException e2){Log.ignore(e2);}
            } 
        }
    }

}
