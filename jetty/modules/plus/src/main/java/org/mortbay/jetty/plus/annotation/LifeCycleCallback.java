//========================================================================
//$Id: LifeCycleCallback.java 1540 2007-01-19 12:24:10Z janb $
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.mortbay.util.IntrospectionUtil;
import org.mortbay.util.Loader;



/**
 * LifeCycleCallback
 *
 *
 */
public abstract class LifeCycleCallback
{
    public static final Object[] __EMPTY_ARGS = new Object[] {};
    private Method _target;
    private Class _targetClass;
    
    
    public LifeCycleCallback()
    {
    }


    /**
     * @return the _targetClass
     */
    public Class getTargetClass()
    {
        return _targetClass;
    }


    /**
     * @param name the class to set
     */
    public void setTargetClass(Class clazz)
    {
        _targetClass = clazz;
    }
    
    
    /**
     * @return the target
     */
    public Method getTarget()
    {
        return _target;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(Method target)
    {
        this._target = target;
    }
    
    
    

    public void setTarget (Class clazz, String methodName)
    {
        try
        {
            Method method = IntrospectionUtil.findMethod(clazz, methodName, null, true, true);
            validate(clazz, method);
            _target = method;
            _targetClass = clazz;
        }
        catch (NoSuchMethodException e)
        {
            throw new IllegalArgumentException ("Method "+methodName+" not found on class "+clazz.getName());
        }
    }


    
    
    public void callback (Object instance)
    throws Exception
    {
        if (getTarget() != null)
        {
            boolean accessibility = getTarget().isAccessible();
            getTarget().setAccessible(true);
            getTarget().invoke(instance, __EMPTY_ARGS);
            getTarget().setAccessible(accessibility);
        }
    }

    

    /**
     * Find a method of the given name either directly in the given
     * class, or inherited.
     * 
     * @param pack the package of the class under inspection
     * @param clazz the class under inspection
     * @param methodName the method to find 
     * @param checkInheritance false on first entry, true if a superclass is being introspected
     * @return
     */
    public Method findMethod (Package pack, Class clazz, String methodName, boolean checkInheritance)
    {
        if (clazz == null)
            return null;

        try
        {
            Method method = clazz.getDeclaredMethod(methodName, null);
            if (checkInheritance)
            {
                int modifiers = method.getModifiers();
                if (Modifier.isProtected(modifiers) || Modifier.isPublic(modifiers) || (!Modifier.isPrivate(modifiers)&&(pack.equals(clazz.getPackage()))))
                    return method;
                else
                    return findMethod(clazz.getPackage(), clazz.getSuperclass(), methodName, true);
            }
            return method;
        }
        catch (NoSuchMethodException e)
        {
            return findMethod(clazz.getPackage(), clazz.getSuperclass(), methodName, true);
        }
    }

    public boolean equals (Object o)
    {
        if (o==null)
            return false;
        if (!(o instanceof LifeCycleCallback))
            return false;
        LifeCycleCallback callback = (LifeCycleCallback)o;
        
        if (callback.getTargetClass()==null)
        {
            if (getTargetClass() != null)
                return false;
        }
        else if(!callback.getTargetClass().equals(getTargetClass()))
           return false;
        if (callback.getTarget()==null)
        {
            if (getTarget() != null)
                return false;
        }
        else if (!callback.getTarget().equals(getTarget()))
            return false;
        
        return true;
    }
    
    public abstract void validate (Class clazz, Method m);
}
