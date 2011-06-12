//========================================================================
//$Id: SimpleBuffers.java,v 1.1 2005/10/05 14:09:25 janb Exp $
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

package org.mortbay.io;

/* ------------------------------------------------------------ */
/** SimpleBuffers.
 * Simple implementation of Buffers holder.
 * @author gregw
 *
 */
public class SimpleBuffers implements Buffers
{   
    Buffer[] _buffers;
    
    /* ------------------------------------------------------------ */
    /**
     * 
     */
    public SimpleBuffers(Buffer[] buffers)
    {
        _buffers=buffers;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.Buffers#getBuffer(boolean)
     */
    public Buffer getBuffer(int size)
    {
        if (_buffers!=null)
        {
            for (int i=0;i<_buffers.length;i++)
            {
                if (_buffers[i]!=null && _buffers[i].capacity()==size)
                {
                    Buffer b=_buffers[i];
                    _buffers[i]=null;
                    return b;
                }
            }
        }
        return new ByteArrayBuffer(size);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.Buffers#returnBuffer(org.mortbay.io.Buffer)
     */
    public void returnBuffer(Buffer buffer)
    {
        buffer.clear();
        if (_buffers!=null)
        {
            for (int i=0;i<_buffers.length;i++)
            {
                if (_buffers[i]==null)
                    _buffers[i]=buffer;
            }
        }
    }
    

}
