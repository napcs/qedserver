// ========================================================================
// Copyright (c) 2004-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================

package org.mortbay.util.ajax;

import java.util.Map;

import org.mortbay.util.Loader;
import org.mortbay.util.ajax.JSON.Convertor;
import org.mortbay.util.ajax.JSON.Output;

/**
 * {@link JSONPojoConvertor} factory convertor.
 * <p>
 * This {@link Convertor} will create and register {@link JSONPojoConvertor} instances for unknown classes.
 * 
 */
public class JSONPojoConvertorFactory implements JSON.Convertor
{
    private final JSON _json;
    private final boolean _fromJSON;
    
    /**
     * @param json The JSON to use for conversions and registrations
     */
    public JSONPojoConvertorFactory(JSON json)
    {
        _json=json;
        _fromJSON=true;
        if (json==null)
        {
            throw new IllegalArgumentException();
        }
    }
    
    /**
     * @param json The JSON to use for conversions and registrations
     * @param fromJSON Passed to {@link JSONPojoConvertor} constructor.
     */
    public JSONPojoConvertorFactory(JSON json,boolean fromJSON)
    {
        _json=json;
        _fromJSON=fromJSON;
        if (json==null)
        {
            throw new IllegalArgumentException();
        }
    }
    
    public void toJSON(Object obj, Output out)
    {
        String clsName=obj.getClass().getName();
        Convertor convertor=_json.getConvertorFor(clsName);
        if (convertor==null)
        {
            try
            {
                Class cls=Loader.loadClass(JSON.class,clsName);
                convertor=new JSONPojoConvertor(cls,_fromJSON);
                _json.addConvertorFor(clsName, convertor);
             }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
        }
        if (convertor!=null&&obj.getClass()!=Object.class)
        {
            convertor.toJSON(obj, out);
        }
        else
        {
            out.add(obj.toString());
        }
    }

    public Object fromJSON(Map object)
    {
        Map map=object;
        String clsName=(String)map.get("class");
        if (clsName!=null)
        {
            Convertor convertor=_json.getConvertorFor(clsName);
            if (convertor==null)
            {
                try
                {
                    Class cls=Loader.loadClass(JSON.class,clsName);
                    convertor=new JSONPojoConvertor(cls);
                    _json.addConvertorFor(clsName, convertor);
                }
                catch (ClassNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
            if (convertor!=null&&!clsName.equals(Object.class.getName()))
            {
                return convertor.fromJSON(object);
            }
        }
        return map;
    }
}
