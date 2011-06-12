// ========================================================================
// Copyright 2008 Mort Bay Consulting Pty. Ltd.
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


package org.mortbay.jetty.servlet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SessionTestClient
{
    
    private String _baseUrl;
    
    // e.g http://localhost:8010
    public SessionTestClient(String baseUrl)
    {
        _baseUrl = baseUrl;
    }
    
    public boolean send(String context, String cookie) throws Exception
    {
        HttpURLConnection conn = sendRequest("GET", new URL(_baseUrl + context + "/session/"), 
                cookie);
        return isSessionAvailable(conn);
    }
    
    public String newSession(String context) throws Exception
    {
        HttpURLConnection conn = sendRequest("POST", new URL(_baseUrl + context + 
                "/session/?Action=New%20Session"), null);
        conn.disconnect();
        return getJSESSIONID(conn.getHeaderField("Set-Cookie"));
    }
    
    public boolean setAttribute(String context, String cookie, String name, String value) throws Exception
    {
        // should be POST, GET for now
        HttpURLConnection conn = sendRequest("GET", new URL(_baseUrl + context + 
                "/session/?Action=Set&Name=" + name + "&Value=" + value), cookie);
        return isAttributeSet(conn, name, value);
    }
    
    public boolean hasAttribute(String context, String cookie, String name, String value) throws Exception
    {
        HttpURLConnection conn = sendRequest("GET", new URL(_baseUrl + context + "/session/"), 
                cookie);
        return isAttributeSet(conn, name, value);
    }
    
    public boolean invalidate(String context, String cookie) throws Exception
    {
        // should be POST, GET for now
        HttpURLConnection conn = sendRequest("GET", new URL(_baseUrl + context + 
                "/session/?Action=Invalidate"), cookie);
        return !isSessionAvailable(conn);
    } 
    
    protected static boolean isSessionAvailable(HttpURLConnection conn) throws Exception
    {
        return !isTokenPresent(conn, "<H3>No Session</H3>");
    }
    
    protected static boolean isAttributeSet(HttpURLConnection conn, String name, String value) throws Exception
    {
        return isTokenPresent(conn, "<b>" + name + ":</b> " + value + "<br/>");
    }
    
    protected static boolean isTokenPresent(HttpURLConnection conn, String token) throws Exception
    {        
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));      
        String line = null;
        boolean present = false;
        while((line=br.readLine())!=null)
        {
            
            if(line.indexOf(token)!=-1)
            {                
                present = true;
                break;
            }                    
        }
        conn.disconnect();
        return present;
    }
    
    public HttpURLConnection sendRequest(String method, URL url, String cookie) throws Exception
    {
        return sendRequest(method, url, cookie, false);
    }

    public HttpURLConnection sendRequest(String method, URL url, String cookie, 
            boolean followRedirects) throws Exception
    {
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod(method);
        conn.setDoInput(true);
        conn.setInstanceFollowRedirects(followRedirects);
        if(cookie!=null)
            conn.setRequestProperty("Cookie", cookie);
        conn.connect();        
        return conn;
    }
    
    protected static String getJSESSIONID(String cookie)
    {
        System.err.println("COOKIE: " + cookie);
        int idx = cookie.indexOf("JSESSIONID");
        return cookie.substring(idx, cookie.indexOf(';', idx));
    }
    
    
}
