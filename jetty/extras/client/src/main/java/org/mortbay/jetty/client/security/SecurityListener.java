//========================================================================
//Copyright 2008 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.client.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;

import org.mortbay.io.Buffer;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.client.HttpDestination;
import org.mortbay.jetty.client.HttpEventListenerWrapper;
import org.mortbay.jetty.client.HttpExchange;
import org.mortbay.log.Log;
import org.mortbay.util.StringUtil;


/**
 * SecurityListener
 * 
 * Allow for insertion of security dialog when performing an
 * HttpExchange.
 */
public class SecurityListener extends HttpEventListenerWrapper
{
    private HttpDestination _destination;
    private HttpExchange _exchange;
    private boolean _requestComplete;
    private boolean _responseComplete;  
    private boolean _needIntercept;
    
    private int _attempts = 0; // TODO remember to settle on winning solution

    public SecurityListener(HttpDestination destination, HttpExchange ex)
    {
        // Start of sending events through to the wrapped listener
        // Next decision point is the onResponseStatus
        super(ex.getEventListener(),true);
        _destination=destination;
        _exchange=ex;
    }
    
    
    /**
     * scrapes an authentication type from the authString
     * 
     * @param authString
     * @return
     */
    protected String scrapeAuthenticationType( String authString )
    {
        String authType;

        if ( authString.indexOf( " " ) == -1 )
        {
            authType = authString.toString().trim();
        }
        else
        {
            String authResponse = authString.toString();
            authType = authResponse.substring( 0, authResponse.indexOf( " " ) ).trim();
        }
        return authType;
    }
    
    /**
     * scrapes a set of authentication details from the authString
     * 
     * @param authString
     * @return
     */
    protected Map<String, String> scrapeAuthenticationDetails( String authString )
    {
        Map<String, String> authenticationDetails = new HashMap<String, String>();
        authString = authString.substring( authString.indexOf( " " ) + 1, authString.length() );
        StringTokenizer strtok = new StringTokenizer( authString, ",");
        
        while ( strtok.hasMoreTokens() )
        {
            String[] pair = strtok.nextToken().split( "=" );
            if ( pair.length == 2 )
            {
                String itemName = pair[0].trim();
                String itemValue = pair[1].trim();
                
                itemValue = StringUtil.unquote( itemValue );
                
                authenticationDetails.put( itemName, itemValue );
            }
            else
            {
                throw new IllegalArgumentException( "unable to process authentication details" );
            }      
        }
        return authenticationDetails;
    }

  
    public void onResponseStatus( Buffer version, int status, Buffer reason )
        throws IOException
    {
        if (Log.isDebugEnabled())
            Log.debug("SecurityListener:Response Status: " + status );

        if ( status == HttpServletResponse.SC_UNAUTHORIZED && _attempts<_destination.getHttpClient().maxRetries()) 
        {
            // Let's absorb events until we have done some retries
            setDelegatingResponses(false);
            _needIntercept = true;
        }
        else 
        {
            setDelegatingResponses(true);
            setDelegatingRequests(true);
            _needIntercept = false;
        }
        super.onResponseStatus(version,status,reason);
    }


    public void onResponseHeader( Buffer name, Buffer value )
        throws IOException
    {
        if (Log.isDebugEnabled())
            Log.debug( "SecurityListener:Header: " + name.toString() + " / " + value.toString() );
        
        
        if (!isDelegatingResponses())
        {
            int header = HttpHeaders.CACHE.getOrdinal(name);
            switch (header)
            {
                case HttpHeaders.WWW_AUTHENTICATE_ORDINAL:

                    // TODO don't hard code this bit.
                    String authString = value.toString();
                    String type = scrapeAuthenticationType( authString );                  

                    // TODO maybe avoid this map creation
                    Map<String,String> details = scrapeAuthenticationDetails( authString );
                    String pathSpec="/"; // TODO work out the real path spec
                    RealmResolver realmResolver = _destination.getHttpClient().getRealmResolver();
                    
                    if ( realmResolver == null )
                    {
                        break;
                    }
                    
                    Realm realm = realmResolver.getRealm( details.get("realm"), _destination, pathSpec ); // TODO work our realm correctly 
                    
                    if ( realm == null )
                    {
                        Log.warn( "Unknown Security Realm: " + details.get("realm") );
                    }
                    else if ("digest".equalsIgnoreCase(type))
                    {
                        _destination.addAuthorization("/",new DigestAuthorization(realm,details));
                        
                    }
                    else if ("basic".equalsIgnoreCase(type))
                    {
                        _destination.addAuthorization(pathSpec,new BasicAuthorization(realm));
                    }
                    
                    break;
            }
        }
        super.onResponseHeader(name,value);
    }
    

    public void onRequestComplete() throws IOException
    {
        _requestComplete = true;

        if (_needIntercept)
        {
            if (_requestComplete && _responseComplete)
            {
               if (Log.isDebugEnabled())
                   Log.debug("onRequestComplete, Both complete: Resending from onResponseComplete "+_exchange); 
                _responseComplete = false;
                _requestComplete = false;
                setDelegatingRequests(true);
                setDelegatingResponses(true);
                _destination.resend(_exchange);  
            } 
            else
            {
                if (Log.isDebugEnabled())
                    Log.debug("onRequestComplete, Response not yet complete onRequestComplete, calling super for "+_exchange);
                super.onRequestComplete(); 
            }
        }
        else
        {
            if (Log.isDebugEnabled())
                Log.debug("onRequestComplete, delegating to super with Request complete="+_requestComplete+", response complete="+_responseComplete+" "+_exchange);
            super.onRequestComplete();
        }
    }


    public void onResponseComplete() throws IOException
    {   
        _responseComplete = true;
        if (_needIntercept)
        {  
            if (_requestComplete && _responseComplete)
            {              
                if (Log.isDebugEnabled())
                    Log.debug("onResponseComplete, Both complete: Resending from onResponseComplete"+_exchange);
                _responseComplete = false;
                _requestComplete = false;
                setDelegatingResponses(true);
                setDelegatingRequests(true);
                _destination.resend(_exchange); 

            }
            else
            {
               if (Log.isDebugEnabled())
                   Log.debug("onResponseComplete, Request not yet complete from onResponseComplete,  calling super "+_exchange);
                super.onResponseComplete(); 
            }
        }
        else
        {
            if (Log.isDebugEnabled())
                Log.debug("OnResponseComplete, delegating to super with Request complete="+_requestComplete+", response complete="+_responseComplete+" "+_exchange);
            super.onResponseComplete();  
        }
    }

    public void onRetry()
    {
        _attempts++;
        setDelegatingRequests(true);
        setDelegatingResponses(true);
        _requestComplete=false;
        _responseComplete=false;
        _needIntercept=false;
        super.onRetry();
    }  
    
    
}
