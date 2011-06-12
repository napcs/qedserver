//========================================================================
//$Id: InjectionCollection.java 1540 2007-01-19 12:24:10Z janb $
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plus.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mortbay.log.Log;

/**
 * InjectionCollection
 *
 *
 */
public class InjectionCollection
{
    private HashMap fieldInjectionsMap = new HashMap();//map of classname to field injections
    private HashMap methodInjectionsMap = new HashMap();//map of classname to method injections
    
    
    public void add (Injection injection)
    {
        if ((injection==null) || (injection.getTarget()==null) || (injection.getTargetClass()==null)) 
            return;
        
        if (Log.isDebugEnabled())
            Log.debug("Adding injection for class="+injection.getTargetClass()+ " on a "+injection.getTarget());
        Map injectionsMap = null;
        if (injection.getTarget() instanceof Field)
            injectionsMap = fieldInjectionsMap;
        if (injection.getTarget() instanceof Method)
            injectionsMap = methodInjectionsMap;
        
        List injections = (List)injectionsMap.get(injection.getTargetClass());
        if (injections==null)
        {
            injections = new ArrayList();
            injectionsMap.put(injection.getTargetClass(), injections);
        }
        
        injections.add(injection);
    }

    public List getFieldInjections (Class clazz)
    {
        if (clazz==null)
            return null;
        List list = (List)fieldInjectionsMap.get(clazz);
        return (list==null?Collections.EMPTY_LIST:list);
    }
    
    public List getMethodInjections (Class clazz)
    {
        if (clazz==null)
            return null;
        List list = (List)methodInjectionsMap.get(clazz);
        return (list==null?Collections.EMPTY_LIST:list);
    }
 
    public List getInjections (Class clazz)
    {
        if (clazz==null)
            return null;
        
        List results = new ArrayList();
        results.addAll(getFieldInjections(clazz));
        results.addAll(getMethodInjections(clazz));
        return results;
    }
    
    public Injection getInjection (Class clazz, Member member)
    {
        if (clazz==null)
            return null;
        if (member==null)
            return null;
        Map map = null;
        if (member instanceof Field)
            map = fieldInjectionsMap;
        else if (member instanceof Method)
            map = methodInjectionsMap;
        
        if (map==null)
            return null;
        
        List injections = (List)map.get(clazz);
        Injection injection = null;
        for (int i=0;injections!=null && i<injections.size() && injection==null;i++)
        {
            Injection candidate = (Injection)injections.get(i);
            if (candidate.getTarget().equals(member))
                injection = candidate;
        }
        return injection;
    }
    
    
    public void inject (Object injectable)
    throws Exception
    {
        if (injectable==null)
            return;

        //Do field injections
        List list = getFieldInjections(injectable.getClass());
        for (int i=0; list!=null && i<list.size();i++)        
            ((Injection)list.get(i)).inject(injectable);
        
        //Do method injections
        list = getMethodInjections(injectable.getClass());
        for (int i=0; list!=null && i<list.size();i++)
            ((Injection)list.get(i)).inject(injectable);
    }
}
