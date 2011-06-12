// ========================================================================
// Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferCache;

/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @author gregw
 */
public class HttpMethods
{
    public final static String GET= "GET",
        POST= "POST",
        HEAD= "HEAD",
        PUT= "PUT",
        OPTIONS= "OPTIONS",
        DELETE= "DELETE",
        TRACE= "TRACE",
        CONNECT= "CONNECT",
        MOVE= "MOVE";

    public final static int GET_ORDINAL= 1,
        POST_ORDINAL= 2,
        HEAD_ORDINAL= 3,
        PUT_ORDINAL= 4,
        OPTIONS_ORDINAL= 5,
        DELETE_ORDINAL= 6,
        TRACE_ORDINAL= 7,
        CONNECT_ORDINAL= 8,
        MOVE_ORDINAL= 9;

    public final static BufferCache CACHE= new BufferCache();

    public final static Buffer 
        GET_BUFFER= CACHE.add(GET, GET_ORDINAL),
        POST_BUFFER= CACHE.add(POST, POST_ORDINAL),
        HEAD_BUFFER= CACHE.add(HEAD, HEAD_ORDINAL),
        PUT_BUFFER= CACHE.add(PUT, PUT_ORDINAL),
        OPTIONS_BUFFER= CACHE.add(OPTIONS, OPTIONS_ORDINAL),
        DELETE_BUFFER= CACHE.add(DELETE, DELETE_ORDINAL),
        TRACE_BUFFER= CACHE.add(TRACE, TRACE_ORDINAL),
        CONNECT_BUFFER= CACHE.add(CONNECT, CONNECT_ORDINAL),
        MOVE_BUFFER= CACHE.add(MOVE, MOVE_ORDINAL);

}
