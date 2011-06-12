//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

import junit.framework.TestCase;

public class SecurityResolverTest extends TestCase
{
    public void testNothing()
    {
        
    }
    /* TODO

    public void testCredentialParsing() throws Exception
    {
        SecurityListener resolver = new SecurityListener();
        Buffer value = new ByteArrayBuffer("basic a=b".getBytes());
        
        assertEquals( "basic", resolver.scrapeAuthenticationType( value.toString() ) );
        assertEquals( 1, resolver.scrapeAuthenticationDetails( value.toString() ).size() );

        value = new ByteArrayBuffer("digest a=boo, c=\"doo\" , egg=foo".getBytes());
        
        assertEquals( "digest", resolver.scrapeAuthenticationType( value.toString() ) );
        Map<String,String> testMap = resolver.scrapeAuthenticationDetails( value.toString() );
        assertEquals( 3, testMap.size() );
        assertEquals( "boo", testMap.get("a") );
        assertEquals( "doo", testMap.get("c") );
        assertEquals( "foo", testMap.get("egg") );
    }
    
    */
}
