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
import org.mortbay.io.ByteArrayBuffer;

/* ------------------------------------------------------------------------------- */
/**
 * 
 */
public class HttpStatus
{
    public final static String Continue= "Continue",
        Switching_Protocols= "Switching Protocols",
        Processing= "Processing",
        OK= "OK",
        Created= "Created",
        Accepted= "Accepted",
        Non_Authoritative_Information= "Non Authoritative Information",
        No_Content= "No Content",
        Reset_Content= "Reset Content",
        Partial_Content= "Partial Content",
        Multi_Status= "Multi Status",
        Multiple_Choices= "Multiple Choices",
        Moved_Permanently= "Moved Permanently",
        Moved_Temporarily= "Moved Temporarily",
        Found= "Found",
        See_Other= "See Other",
        Not_Modified= "Not Modified",
        Use_Proxy= "Use Proxy",
        Bad_Request= "Bad Request",
        Unauthorized= "Unauthorized",
        Payment_Required= "Payment Required",
        Forbidden= "Forbidden",
        Not_Found= "Not Found",
        Method_Not_Allowed= "Method Not Allowed",
        Not_Acceptable= "Not Acceptable",
        Proxy_Authentication_Required= "Proxy Authentication Required",
        Request_Timeout= "Request Timeout",
        Conflict= "Conflict",
        Gone= "Gone",
        Length_Required= "Length Required",
        Precondition_Failed= "Precondition Failed",
        Request_Entity_Too_Large= "Request Entity Too Large",
        Request_URI_Too_Large= "Request URI Too Large",
        Unsupported_Media_Type= "Unsupported Media Type",
        Requested_Range_Not_Satisfiable= "Requested Range Not Satisfiable",
        Expectation_Failed= "Expectation Failed",
        Unprocessable_Entity= "Unprocessable Entity",
        Locked= "Locked",
        Failed_Dependency= "Failed Dependency",
        Internal_Server_Error= "Internal Server Error",
        Not_Implemented= "Not Implemented",
        Bad_Gateway= "Bad Gateway",
        Service_Unavailable= "Service Unavailable",
        Gateway_Timeout= "Gateway Timeout",
        HTTP_Version_Not_Supported= "HTTP Version Not Supported",
        Insufficient_Storage= "Insufficient Storage",
        Unknown="Unknown";

    public final static int 
        ORDINAL_100_Continue= 100,
        ORDINAL_101_Switching_Protocols= 101,
        ORDINAL_102_Processing= 102,
        ORDINAL_200_OK= 200,
        ORDINAL_201_Created= 201,
        ORDINAL_202_Accepted= 202,
        ORDINAL_203_Non_Authoritative_Information= 203,
        ORDINAL_204_No_Content= 204,
        ORDINAL_205_Reset_Content= 205,
        ORDINAL_206_Partial_Content= 206,
        ORDINAL_207_Multi_Status= 207,
        ORDINAL_300_Multiple_Choices= 300,
        ORDINAL_301_Moved_Permanently= 301,
        ORDINAL_302_Moved_Temporarily= 302,
        ORDINAL_302_Found= 302,
        ORDINAL_303_See_Other= 303,
        ORDINAL_304_Not_Modified= 304,
        ORDINAL_305_Use_Proxy= 305,
        ORDINAL_400_Bad_Request= 400,
        ORDINAL_401_Unauthorized= 401,
        ORDINAL_402_Payment_Required= 402,
        ORDINAL_403_Forbidden= 403,
        ORDINAL_404_Not_Found= 404,
        ORDINAL_405_Method_Not_Allowed= 405,
        ORDINAL_406_Not_Acceptable= 406,
        ORDINAL_407_Proxy_Authentication_Required= 407,
        ORDINAL_408_Request_Timeout= 408,
        ORDINAL_409_Conflict= 409,
        ORDINAL_410_Gone= 410,
        ORDINAL_411_Length_Required= 411,
        ORDINAL_412_Precondition_Failed= 412,
        ORDINAL_413_Request_Entity_Too_Large= 413,
        ORDINAL_414_Request_URI_Too_Large= 414,
        ORDINAL_415_Unsupported_Media_Type= 415,
        ORDINAL_416_Requested_Range_Not_Satisfiable= 416,
        ORDINAL_417_Expectation_Failed= 417,
        ORDINAL_422_Unprocessable_Entity= 422,
        ORDINAL_423_Locked= 423,
        ORDINAL_424_Failed_Dependency= 424,
        ORDINAL_500_Internal_Server_Error= 500,
        ORDINAL_501_Not_Implemented= 501,
        ORDINAL_502_Bad_Gateway= 502,
        ORDINAL_503_Service_Unavailable= 503,
        ORDINAL_504_Gateway_Timeout= 504,
        ORDINAL_505_HTTP_Version_Not_Supported= 505,
        ORDINAL_507_Insufficient_Storage= 507,
        ORDINAL_999_Unknown = 999;

    public static final BufferCache CACHE = new BufferCache();

    public static final Buffer
        Continue_BUFFER=CACHE.add(Continue,ORDINAL_100_Continue),
        Switching_Protocols_BUFFER=CACHE.add(Switching_Protocols,ORDINAL_101_Switching_Protocols),
        Processing_BUFFER=CACHE.add(Processing,ORDINAL_102_Processing),
        OK_BUFFER=CACHE.add(OK,ORDINAL_200_OK),
        Created_BUFFER=CACHE.add(Created,ORDINAL_201_Created),
        Accepted_BUFFER=CACHE.add(Accepted,ORDINAL_202_Accepted),
        Non_Authoritative_Information_BUFFER=CACHE.add(Non_Authoritative_Information,ORDINAL_203_Non_Authoritative_Information),
        No_Content_BUFFER=CACHE.add(No_Content,ORDINAL_204_No_Content),
        Reset_Content_BUFFER=CACHE.add(Reset_Content,ORDINAL_205_Reset_Content),
        Partial_Content_BUFFER=CACHE.add(Partial_Content,ORDINAL_206_Partial_Content),
        Multi_Status_BUFFER=CACHE.add(Multi_Status,ORDINAL_207_Multi_Status),
        Multiple_Choices_BUFFER=CACHE.add(Multiple_Choices,ORDINAL_300_Multiple_Choices),
        Moved_Permanently_BUFFER=CACHE.add(Moved_Permanently,ORDINAL_301_Moved_Permanently),
        Moved_Temporarily_BUFFER=CACHE.add(Moved_Temporarily,ORDINAL_302_Moved_Temporarily),
        Found_BUFFER=CACHE.add(Found,ORDINAL_302_Found),
        See_Other_BUFFER=CACHE.add(See_Other,ORDINAL_303_See_Other),
        Not_Modified_BUFFER=CACHE.add(Not_Modified,ORDINAL_304_Not_Modified),
        Use_Proxy_BUFFER=CACHE.add(Use_Proxy,ORDINAL_305_Use_Proxy),
        Bad_Request_BUFFER=CACHE.add(Bad_Request,ORDINAL_400_Bad_Request),
        Unauthorized_BUFFER=CACHE.add(Unauthorized,ORDINAL_401_Unauthorized),
        Payment_Required_BUFFER=CACHE.add(Payment_Required,ORDINAL_402_Payment_Required),
        Forbidden_BUFFER=CACHE.add(Forbidden,ORDINAL_403_Forbidden),
        Not_Found_BUFFER=CACHE.add(Not_Found,ORDINAL_404_Not_Found),
        Method_Not_Allowed_BUFFER=CACHE.add(Method_Not_Allowed,ORDINAL_405_Method_Not_Allowed),
        Not_Acceptable_BUFFER=CACHE.add(Not_Acceptable,ORDINAL_406_Not_Acceptable),
        Proxy_Authentication_Required_BUFFER=CACHE.add(Proxy_Authentication_Required,ORDINAL_407_Proxy_Authentication_Required),
        Request_Timeout_BUFFER=CACHE.add(Request_Timeout,ORDINAL_408_Request_Timeout),
        Conflict_BUFFER=CACHE.add(Conflict,ORDINAL_409_Conflict),
        Gone_BUFFER=CACHE.add(Gone,ORDINAL_410_Gone),
        Length_Required_BUFFER=CACHE.add(Length_Required,ORDINAL_411_Length_Required),
        Precondition_Failed_BUFFER=CACHE.add(Precondition_Failed,ORDINAL_412_Precondition_Failed),
        Request_Entity_Too_Large_BUFFER=CACHE.add(Request_Entity_Too_Large,ORDINAL_413_Request_Entity_Too_Large),
        Request_URI_Too_Large_BUFFER=CACHE.add(Request_URI_Too_Large,ORDINAL_414_Request_URI_Too_Large),
        Unsupported_Media_Type_BUFFER=CACHE.add(Unsupported_Media_Type,ORDINAL_415_Unsupported_Media_Type),
        Requested_Range_Not_Satisfiable_BUFFER=CACHE.add(Requested_Range_Not_Satisfiable,ORDINAL_416_Requested_Range_Not_Satisfiable),
        Expectation_Failed_BUFFER=CACHE.add(Expectation_Failed,ORDINAL_417_Expectation_Failed),
        Unprocessable_Entity_BUFFER=CACHE.add(Unprocessable_Entity,ORDINAL_422_Unprocessable_Entity),
        Locked_BUFFER=CACHE.add(Locked,ORDINAL_423_Locked),
        Failed_Dependency_BUFFER=CACHE.add(Failed_Dependency,ORDINAL_424_Failed_Dependency),
        Internal_Server_Error_BUFFER=CACHE.add(Internal_Server_Error,ORDINAL_500_Internal_Server_Error),
        Not_Implemented_BUFFER=CACHE.add(Not_Implemented,ORDINAL_501_Not_Implemented),
        Bad_Gateway_BUFFER=CACHE.add(Bad_Gateway,ORDINAL_502_Bad_Gateway),
        Service_Unavailable_BUFFER=CACHE.add(Service_Unavailable,ORDINAL_503_Service_Unavailable),
        Gateway_Timeout_BUFFER=CACHE.add(Gateway_Timeout,ORDINAL_504_Gateway_Timeout),
        HTTP_Version_Not_Supported_BUFFER=CACHE.add(HTTP_Version_Not_Supported,ORDINAL_505_HTTP_Version_Not_Supported),
        Insufficient_Storage_BUFFER=CACHE.add(Insufficient_Storage,ORDINAL_507_Insufficient_Storage),
        Unknown_BUFFER=CACHE.add(Unknown,ORDINAL_999_Unknown);
    
    
    // Build cache of response lines for status
    private static Buffer[] __responseLine = new Buffer[600];
    static
    {
        int versionLength=HttpVersions.HTTP_1_1_BUFFER.length();
        
        for (int i=0;i<__responseLine.length;i++)
        {
            Buffer reason = CACHE.get(i);
            if (reason==null)
                continue;
            
            byte[] bytes=new byte[versionLength+5+reason.length()+2];
            HttpVersions.HTTP_1_1_BUFFER.peek(0,bytes, 0, versionLength);
            bytes[versionLength+0]=' ';
            bytes[versionLength+1]=(byte)('0'+i/100);
            bytes[versionLength+2]=(byte)('0'+(i%100)/10);
            bytes[versionLength+3]=(byte)('0'+(i%10));
            bytes[versionLength+4]=' ';
            reason.peek(0, bytes, versionLength+5, reason.length());
            bytes[versionLength+5+reason.length()]=HttpTokens.CARRIAGE_RETURN;
            bytes[versionLength+6+reason.length()]=HttpTokens.LINE_FEED;
            __responseLine[i]=new ByteArrayBuffer(bytes,0,bytes.length,Buffer.IMMUTABLE);
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * @param status
     * @return HTTP response line for the status code including CRLF
     */
    public static Buffer getResponseLine(int status)
    {
        if (status>=__responseLine.length)
            return null;
        return __responseLine[status];
    }
}
