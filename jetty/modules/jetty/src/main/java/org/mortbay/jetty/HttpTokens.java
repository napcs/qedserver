//========================================================================
//$Id: HttpTokens.java,v 1.1 2005/10/05 14:09:21 janb Exp $
//Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty;

/**
 * HTTP constants
 */
public interface HttpTokens
{
    // Terminal symbols.
    static final byte COLON= (byte)':';
    static final byte SPACE= 0x20;
    static final byte CARRIAGE_RETURN= 0x0D;
    static final byte LINE_FEED= 0x0A;
    static final byte[] CRLF = {CARRIAGE_RETURN,LINE_FEED};
    static final byte SEMI_COLON= (byte)';';
    static final byte TAB= 0x09;

    public static final int SELF_DEFINING_CONTENT= -4;
    public static final int UNKNOWN_CONTENT= -3;
    public static final int CHUNKED_CONTENT= -2;
    public static final int EOF_CONTENT= -1;
    public static final int NO_CONTENT= 0;

    
}
