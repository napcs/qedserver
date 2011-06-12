//========================================================================
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.ajp;

import org.mortbay.io.BufferCache;
import org.mortbay.io.Buffer;

/**
 * XXX Should this implement the Buffer interface?
 * 
 * @author Markus Kobler
 */
public class Ajp13RequestHeaders extends BufferCache
{

    public final static int MAGIC=0x1234;

    public final static String ACCEPT="accept", ACCEPT_CHARSET="accept-charset", ACCEPT_ENCODING="accept-encoding", ACCEPT_LANGUAGE="accept-language",
            AUTHORIZATION="authorization", CONNECTION="connection", CONTENT_TYPE="content-type", CONTENT_LENGTH="content-length", COOKIE="cookie",
            COOKIE2="cookie2", HOST="host", PRAGMA="pragma", REFERER="referer", USER_AGENT="user-agent";

    public final static int ACCEPT_ORDINAL=1, ACCEPT_CHARSET_ORDINAL=2, ACCEPT_ENCODING_ORDINAL=3, ACCEPT_LANGUAGE_ORDINAL=4, AUTHORIZATION_ORDINAL=5,
            CONNECTION_ORDINAL=6, CONTENT_TYPE_ORDINAL=7, CONTENT_LENGTH_ORDINAL=8, COOKIE_ORDINAL=9, COOKIE2_ORDINAL=10, HOST_ORDINAL=11, PRAGMA_ORDINAL=12,
            REFERER_ORDINAL=13, USER_AGENT_ORDINAL=14;

    public final static BufferCache CACHE=new BufferCache();

    public final static Buffer ACCEPT_BUFFER=CACHE.add(ACCEPT,ACCEPT_ORDINAL), ACCEPT_CHARSET_BUFFER=CACHE.add(ACCEPT_CHARSET,ACCEPT_CHARSET_ORDINAL),
            ACCEPT_ENCODING_BUFFER=CACHE.add(ACCEPT_ENCODING,ACCEPT_ENCODING_ORDINAL), ACCEPT_LANGUAGE_BUFFER=CACHE
                    .add(ACCEPT_LANGUAGE,ACCEPT_LANGUAGE_ORDINAL), AUTHORIZATION_BUFFER=CACHE.add(AUTHORIZATION,AUTHORIZATION_ORDINAL), CONNECTION_BUFFER=CACHE
                    .add(CONNECTION,CONNECTION_ORDINAL), CONTENT_TYPE_BUFFER=CACHE.add(CONTENT_TYPE,CONTENT_TYPE_ORDINAL), CONTENT_LENGTH_BUFFER=CACHE.add(
                    CONTENT_LENGTH,CONTENT_LENGTH_ORDINAL), COOKIE_BUFFER=CACHE.add(COOKIE,COOKIE_ORDINAL), COOKIE2_BUFFER=CACHE.add(COOKIE2,COOKIE2_ORDINAL),
            HOST_BUFFER=CACHE.add(HOST,HOST_ORDINAL), PRAGMA_BUFFER=CACHE.add(PRAGMA,PRAGMA_ORDINAL), REFERER_BUFFER=CACHE.add(REFERER,REFERER_ORDINAL),
            USER_AGENT_BUFFER=CACHE.add(USER_AGENT,USER_AGENT_ORDINAL);

    public final static byte 
            CONTEXT_ATTR=1, // Legacy
            SERVLET_PATH_ATTR=2, // Legacy
            REMOTE_USER_ATTR=3, 
            AUTH_TYPE_ATTR=4, 
            QUERY_STRING_ATTR=5, 
            JVM_ROUTE_ATTR=6, 
            SSL_CERT_ATTR=7, 
            SSL_CIPHER_ATTR=8,
            SSL_SESSION_ATTR=9,
            REQUEST_ATTR=10, 
            SSL_KEYSIZE_ATTR=11, 
            SECRET_ATTR=12, 
            STORED_METHOD_ATTR=13;

}
