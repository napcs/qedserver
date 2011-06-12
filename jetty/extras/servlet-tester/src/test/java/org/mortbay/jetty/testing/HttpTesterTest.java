//========================================================================
//Copyright 2007 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.testing;

import junit.framework.TestCase;

public class HttpTesterTest extends TestCase
{
    
    public void testCharset() throws Exception
    {
        HttpTester tester = new HttpTester();
        tester.parse(
                "POST /uri© HTTP/1.1\r\n"+
                "Host: fakehost\r\n"+
                "Content-Length: 11\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "\r\n" +
                "123456789©");
        System.err.println(tester.getMethod());
        System.err.println(tester.getURI());
        System.err.println(tester.getVersion());
        System.err.println(tester.getHeader("Host"));
        System.err.println(tester.getContentType());
        System.err.println(tester.getCharacterEncoding());
        System.err.println(tester.getContent());
        assertEquals(tester.getContent(), "123456789©");
        System.err.println(tester.generate());
    }

}
