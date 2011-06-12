//========================================================================
//$Id: Buffers.java,v 1.1 2005/10/05 14:09:25 janb Exp $
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
/** BufferSource.
 * Represents a pool or other source of buffers and abstracts the creation
 * of specific types of buffers (eg NIO).   The concept of big and little buffers
 * is supported, but these terms have no absolute meaning and must be determined by context.
 * 
 * @author gregw
 *
 */
public interface Buffers
{
    public Buffer getBuffer(int size);
    public void returnBuffer(Buffer buffer);
}