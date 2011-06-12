//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.util.ajax;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mortbay.util.ajax.JSON.Output;

/* ------------------------------------------------------------ */
/**
 * Convert an Object to JSON using reflection on getters methods.
 * 
 * @author gregw
 *
 */
public class JSONObjectConvertor implements JSON.Convertor
{
    private boolean _fromJSON;
    private Set _excluded=null;

    public JSONObjectConvertor()
    {
        _fromJSON=false;
    }
    
    public JSONObjectConvertor(boolean fromJSON)
    {
        _fromJSON=fromJSON;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param fromJSON
     * @param excluded An array of field names to exclude from the conversion
     */
    public JSONObjectConvertor(boolean fromJSON,String[] excluded)
    {
        _fromJSON=fromJSON;
        if (excluded!=null)
            _excluded=new HashSet(Arrays.asList(excluded));
    }

    public Object fromJSON(Map map)
    {
        if (_fromJSON)
            throw new UnsupportedOperationException();
        return map;
    }

    public void toJSON(Object obj, Output out)
    {
        try
        {
            Class c=obj.getClass();

            if (_fromJSON)
                out.addClass(obj.getClass());

            Method[] methods = obj.getClass().getMethods();

            for (int i=0;i<methods.length;i++)
            {
                Method m=methods[i];
                if (!Modifier.isStatic(m.getModifiers()) &&  
                        m.getParameterTypes().length==0 && 
                        m.getReturnType()!=null &&
                        m.getDeclaringClass()!=Object.class)
                {
                    String name=m.getName();
                    if (name.startsWith("is"))
                        name=name.substring(2,3).toLowerCase()+name.substring(3);
                    else if (name.startsWith("get"))
                        name=name.substring(3,4).toLowerCase()+name.substring(4);
                    else
                        continue;

                    if (includeField(name,obj,m))
                        out.add(name, m.invoke(obj,(Object[])null));
                }
            }
        } 
        catch (Throwable e)
        {
            throw new RuntimeException("Illegal argument", e);
        }
    }
    
    protected boolean includeField(String name, Object o, Method m)
    {
        return _excluded==null || !_excluded.contains(name);
    }

}
