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

/**
 * @author Markus Kobler
 */
public class Ajp13Packet
{

    public final static int MAX_PACKET_SIZE=(8*1024);
    public final static int HDR_SIZE=4;

    // Used in writing response...
    public final static int DATA_HDR_SIZE=7;
    public final static int MAX_DATA_SIZE=MAX_PACKET_SIZE-DATA_HDR_SIZE;

    public final static String
    // Server -> Container
            FORWARD_REQUEST="FORWARD REQUEST",
            SHUTDOWN="SHUTDOWN",
            PING_REQUEST="PING REQUEST", // Obsolete
            CPING_REQUEST="CPING REQUEST",

            // Server <- Container
            SEND_BODY_CHUNK="SEND BODY CHUNK", SEND_HEADERS="SEND HEADERS", END_RESPONSE="END RESPONSE",
            GET_BODY_CHUNK="GET BODY CHUNK",
            CPONG_REPLY="CPONG REPLY";

    public final static int FORWARD_REQUEST_ORDINAL=2, SHUTDOWN_ORDINAL=7,
            PING_REQUEST_ORDINAL=8, // Obsolete
            CPING_REQUEST_ORDINAL=10, SEND_BODY_CHUNK_ORDINAL=3, SEND_HEADERS_ORDINAL=4, END_RESPONSE_ORDINAL=5, GET_BODY_CHUNK_ORDINAL=6,
            CPONG_REPLY_ORDINAL=9;

    public final static BufferCache CACHE=new BufferCache();

    static
    {
        CACHE.add(FORWARD_REQUEST,FORWARD_REQUEST_ORDINAL);
        CACHE.add(SHUTDOWN,SHUTDOWN_ORDINAL);
        CACHE.add(PING_REQUEST,PING_REQUEST_ORDINAL); // Obsolete
        CACHE.add(CPING_REQUEST,CPING_REQUEST_ORDINAL);
        CACHE.add(SEND_BODY_CHUNK,SEND_BODY_CHUNK_ORDINAL);
        CACHE.add(SEND_HEADERS,SEND_HEADERS_ORDINAL);
        CACHE.add(END_RESPONSE,END_RESPONSE_ORDINAL);
        CACHE.add(GET_BODY_CHUNK,GET_BODY_CHUNK_ORDINAL);
        CACHE.add(CPONG_REPLY,CPONG_REPLY_ORDINAL);
    }

}
