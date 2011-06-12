//========================================================================
//$Id: AttributesMap.java,v 1.3 2005/11/14 17:45:52 gregwilkins Exp $
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

package org.mortbay.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/* ------------------------------------------------------------ */
/** AttributesMap.
 * @author gregw
 *
 */
public class AttributesMap implements Attributes
{
    Map _map;

    /* ------------------------------------------------------------ */
    public AttributesMap()
    {
        _map=new HashMap();
    }
    
    /* ------------------------------------------------------------ */
    public AttributesMap(Map map)
    {
        _map=map;
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.util.Attributes#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String name)
    {
        _map.remove(name);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.util.Attributes#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String name, Object attribute)
    {
        if (attribute==null)
            _map.remove(name);
        else
            _map.put(name, attribute);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.util.Attributes#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name)
    {
        return _map.get(name);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.util.Attributes#getAttributeNames()
     */
    public Enumeration getAttributeNames()
    {
        return Collections.enumeration(_map.keySet());
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.util.Attributes#getAttributeNames()
     */
    public static Enumeration getAttributeNamesCopy(Attributes attrs)
    {
        if (attrs instanceof AttributesMap)
            return Collections.enumeration(((AttributesMap)attrs)._map.keySet());
        ArrayList names = new ArrayList();
        Enumeration e = attrs.getAttributeNames();
        while (e.hasMoreElements())
            names.add(e.nextElement());
        return Collections.enumeration(names);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.util.Attributes#clear()
     */
    public void clearAttributes()
    {
        _map.clear();
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return _map.toString();
    }
    

}
