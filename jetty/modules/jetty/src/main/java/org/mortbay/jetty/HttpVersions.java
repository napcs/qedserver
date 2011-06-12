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
public class HttpVersions
{
	public final static String
		HTTP_0_9 = "",
		HTTP_1_0 = "HTTP/1.0",
		HTTP_1_1 = "HTTP/1.1";
		
	public final static int
		HTTP_0_9_ORDINAL=9,
		HTTP_1_0_ORDINAL=10,
		HTTP_1_1_ORDINAL=11;
	
	public final static BufferCache CACHE = new BufferCache();
	
    public final static Buffer 
        HTTP_0_9_BUFFER=CACHE.add(HTTP_0_9,HTTP_0_9_ORDINAL),
        HTTP_1_0_BUFFER=CACHE.add(HTTP_1_0,HTTP_1_0_ORDINAL),
        HTTP_1_1_BUFFER=CACHE.add(HTTP_1_1,HTTP_1_1_ORDINAL);
}
