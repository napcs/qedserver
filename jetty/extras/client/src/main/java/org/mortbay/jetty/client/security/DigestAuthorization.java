//========================================================================
//Copyright 2006-2008 Mort Bay Consulting Pty. Ltd.
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
import java.security.MessageDigest;
import java.util.Map;

import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.client.HttpExchange;
import org.mortbay.util.StringUtil;
import org.mortbay.util.TypeUtil;

public class DigestAuthorization implements Authorization
{
    private static final String NC = "00000001";
    Realm securityRealm;
    Map details;
    
    public DigestAuthorization(Realm realm, Map details)
    {
        this.securityRealm=realm;
        this.details=details;
    }
    

    public void setCredentials( HttpExchange exchange ) 
    throws IOException
    {        
        StringBuilder buffer = new StringBuilder().append("Digest");
        
        buffer.append(" ").append("username").append('=').append('"').append(securityRealm.getPrincipal()).append('"');
        
        buffer.append(", ").append("realm").append('=').append('"').append(String.valueOf(details.get("realm"))).append('"');
        
        buffer.append(", ").append("nonce").append('=').append('"').append(String.valueOf(details.get("nonce"))).append('"');
        
        buffer.append(", ").append("uri").append('=').append('"').append(exchange.getURI()).append('"');
        
        buffer.append(", ").append("algorithm").append('=').append(String.valueOf(details.get("algorithm")));
        
        String cnonce = newCnonce(exchange, securityRealm, details);
        
        buffer.append(", ").append("response").append('=').append('"').append(newResponse(cnonce, 
                exchange, securityRealm, details)).append('"');
        
        buffer.append(", ").append("qop").append('=').append(String.valueOf(details.get("qop")));
        

        buffer.append(", ").append("nc").append('=').append(NC);
        
        buffer.append(", ").append("cnonce").append('=').append('"').append(cnonce).append('"');
        
        exchange.setRequestHeader( HttpHeaders.AUTHORIZATION, 
                new String(buffer.toString().getBytes(StringUtil.__ISO_8859_1)));
    }
    
    protected String newResponse(String cnonce, HttpExchange exchange, Realm securityRealm, Map details)
    {        
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            // calc A1 digest
            md.update(securityRealm.getPrincipal().getBytes(StringUtil.__ISO_8859_1));
            md.update((byte)':');
            md.update(String.valueOf(details.get("realm")).getBytes(StringUtil.__ISO_8859_1));
            md.update((byte)':');
            md.update(securityRealm.getCredentials().getBytes(StringUtil.__ISO_8859_1));
            byte[] ha1 = md.digest();
            // calc A2 digest
            md.reset();
            md.update(exchange.getMethod().getBytes(StringUtil.__ISO_8859_1));
            md.update((byte)':');
            md.update(exchange.getURI().getBytes(StringUtil.__ISO_8859_1));
            byte[] ha2=md.digest();
            
            md.update(TypeUtil.toString(ha1,16).getBytes(StringUtil.__ISO_8859_1));
            md.update((byte)':');
            md.update(String.valueOf(details.get("nonce")).getBytes(StringUtil.__ISO_8859_1));
            md.update((byte)':');
            md.update(NC.getBytes(StringUtil.__ISO_8859_1));
            md.update((byte)':');
            md.update(cnonce.getBytes(StringUtil.__ISO_8859_1));
            md.update((byte)':');
            md.update(String.valueOf(details.get("qop")).getBytes(StringUtil.__ISO_8859_1));
            md.update((byte)':');
            md.update(TypeUtil.toString(ha2,16).getBytes(StringUtil.__ISO_8859_1));
            byte[] digest=md.digest();
            
            // check digest
            return encode(digest);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }        
    }
    
    protected String newCnonce(HttpExchange exchange, Realm securityRealm, Map details)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] b= md.digest(String.valueOf(System.currentTimeMillis()).getBytes(StringUtil.__ISO_8859_1));            
            return encode(b);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    private static String encode(byte[] data)
    {
        StringBuilder buffer = new StringBuilder();
        for (int i=0; i<data.length; i++) 
        {
            buffer.append(Integer.toHexString((data[i] & 0xf0) >>> 4));
            buffer.append(Integer.toHexString(data[i] & 0x0f));
        }
        return buffer.toString();
    }

}
