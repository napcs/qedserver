//========================================================================
//$Id: StringEndPoint.java,v 1.1 2005/10/05 14:09:39 janb Exp $
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

package org.mortbay.io.bio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.mortbay.util.StringUtil;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class StringEndPoint extends StreamEndPoint
{
    String _encoding=StringUtil.__UTF8;
    ByteArrayInputStream _bin = new ByteArrayInputStream(new byte[0]);
    ByteArrayOutputStream _bout = new ByteArrayOutputStream();
    
    public StringEndPoint()
    {
        super(null,null);
        _in=_bin;
        _out=_bout;
    }
    
    public StringEndPoint(String encoding)
    	throws IOException
    {
        this();
        if (encoding!=null)
            _encoding=encoding;
    }
    
    public void setInput(String s) 
    {
        try
        {
            byte[] bytes = s.getBytes(_encoding);
            _bin=new ByteArrayInputStream(bytes);
            _in=_bin;
            _bout = new ByteArrayOutputStream();
            _out=_bout;
        }
        catch(Exception e)
        {
            throw new IllegalStateException(e.toString());
        }
    }
    
    public String getOutput() 
    {
        try
        {
            String s = new String(_bout.toByteArray(),_encoding);
            _bout.reset();
      	  return s;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw new IllegalStateException(_encoding+": "+e.toString());
        }
    }

    /**
     * @return <code>true</code> if there are bytes remaining to be read from the encoded input
     */
    public boolean hasMore()
    {
        return _bin.available()>0;
    }   
}
