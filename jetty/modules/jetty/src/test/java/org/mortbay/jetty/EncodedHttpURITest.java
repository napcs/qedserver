package org.mortbay.jetty;

import java.net.URLDecoder;
import java.net.URLEncoder;

import junit.framework.TestCase;

public class EncodedHttpURITest extends TestCase
{

    public void testNonURIAscii ()
    throws Exception
    {
        String url = "http://www.foo.com/ma\u00F1ana";
        byte[] asISO = url.getBytes("ISO-8859-1");
        String str = new String(asISO, "ISO-8859-1");
        
        //use a non UTF-8 charset as the encoding and url-escape as per
        //http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars
        String s = URLEncoder.encode(url, "ISO-8859-1");     
        EncodedHttpURI uri = new EncodedHttpURI("ISO-8859-1");
        
        //parse it, using the same encoding
        uri.parse(s);
        
        //decode the url encoding
        String d = URLDecoder.decode(uri.getCompletePath(), "ISO-8859-1");
        assertEquals(url, d);
    }
}
