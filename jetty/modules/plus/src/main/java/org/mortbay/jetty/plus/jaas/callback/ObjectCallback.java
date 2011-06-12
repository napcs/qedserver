// ========================================================================
// $Id: ObjectCallback.java 305 2006-03-07 10:32:14Z janb $
// Copyright 2003-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plus.jaas.callback;

import javax.security.auth.callback.Callback;


/* ---------------------------------------------------- */
/** ObjectCallback
 *
 * <p>Can be used as a LoginModule Callback to
 * obtain a user's credential as an Object, rather than
 * a char[], to which some credentials may not be able
 * to be converted
 *
 * <p><h4>Notes</h4>
 * <p>
 *
 * <p><h4>Usage</h4>
 * <pre>
 */
/*
 * </pre>
 *
 * @see
 * @version 1.0 Tue Apr 15 2003
 * @author Jan Bartel (janb)
 */
public class ObjectCallback implements Callback
{

    protected Object _object;
    
    public void setObject(Object o)
    {
        _object = o;
    }

    public Object getObject ()
    {
        return _object;
    }


    public void clearObject ()
    {
        _object = null;
    }
    
    
}
