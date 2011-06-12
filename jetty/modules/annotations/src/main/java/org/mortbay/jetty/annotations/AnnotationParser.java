//========================================================================
//$Id: AnnotationParser.java 3680 2008-09-21 10:37:13Z janb $
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

package org.mortbay.jetty.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.mortbay.jetty.plus.annotation.InjectionCollection;
import org.mortbay.jetty.plus.annotation.LifeCycleCallbackCollection;
import org.mortbay.jetty.plus.annotation.RunAsCollection;
import org.mortbay.jetty.servlet.Holder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;
import org.mortbay.util.IntrospectionUtil;

/**
 * AnnotationParser
 *
 * None of the common annotations are inheritable, thus
 * calling getAnnotations() is exactly equivalent to 
 * getDeclaredAnnotations(). Therefore, in order to find
 * all relevant annotations, the full inheritance tree of
 * a class must be considered.
 * 
 * From the spec:
 *  Class-level annotations only affect the class they 
 *  annotate and their members, that is, its methods and fields. 
 *  They never affect a member declared by a superclass, even 
 *  if it is not hidden or overridden by the class in question.
 * 
 *  In addition to affecting the annotated class, class-level 
 *  annotations may act as a shorthand for member-level annotations. 
 *  If a member carries a specific member-level annotation, any 
 *  annotations of the same type implied by a class-level annotation 
 *  are ignored. In other words, explicit member-level annotations
 *  have priority over member-level annotations implied by a class-level 
 *  annotation. For example, a @WebService annotation on a class implies 
 *  that all the public method in the class that it is applied on are 
 *  annotated with @WebMethod if there is no @WebMethod annotation on 
 *  any of the methods. However if there is a @WebMethod annotation on 
 *  any method then the @WebService does not imply the presence of 
 *  @WebMethod on the other public methods in the class.
 *  
 *  The interfaces implemented by a class never contribute annotations 
 *  to the class itself or any of its members.
 *  
 *  Members inherited from a superclass and which are not hidden or 
 *  overridden maintain the annotations they had in the class that
 *  declared them, including member-level annotations implied by 
 *  class-level ones.
 *  
 *  Member-level annotations on a hidden or overridden member are 
 *  always ignored
 */
public class AnnotationParser
{
    /**
     * Examine the class hierarchy for a class, finding all annotations. Then, merge any 
     * servlet2.5 spec annotations found with those already existing (from parsing web.xml)
     * respecting the overriding rules found in the spec.
     * 
     * @param webApp the webapp
     * @param clazz the class to inspect
     * @param runAs any run-as elements from web.xml
     * @param injections any injections specified in web.xml
     * @param callbacks any postconstruct/predestroy callbacks in web.xml
     */
    public static void parseAnnotations (WebAppContext webApp, Class clazz, RunAsCollection runAs, InjectionCollection injections, LifeCycleCallbackCollection callbacks)
    {
        if (clazz==null)
            return;
        AnnotationCollection annotations = processClass(clazz);
        annotations.setWebAppContext(webApp);
        annotations.processRunAsAnnotations(runAs);
        annotations.processResourcesAnnotations();
        annotations.processResourceAnnotations(injections);
        annotations.processLifeCycleCallbackAnnotations(callbacks);
    }
    
    

    /**
     * Examine the class hierarchy for this class looking for annotations.
     * 
     * @param clazz
     * @return AnnotationCollection
     */
    static AnnotationCollection processClass (Class clazz)
    { 
        AnnotationCollection collection = new AnnotationCollection();
        if (clazz==null)
            return collection;
       
        collection.setTargetClass(clazz);
        
        //add any class level annotations
        collection.addClass(clazz);
       
        //Add all the fields with annotations.
        Field[] fields = clazz.getDeclaredFields();
        //For each field, get all of it's annotations
        for (int i=0; i<fields.length; i++)
        {
            collection.addField(fields[i]);
        }
        
        //Get all the methods with annotations
        Method[] methods = clazz.getDeclaredMethods();
        for (int i=0; i<methods.length;i++)
        {
            collection.addMethod(methods[i]);
        }
        
        //process the inheritance hierarchy for the class
        Class ancestor = clazz.getSuperclass();
        while (ancestor!=null && (!ancestor.equals(Object.class)))
        {
            processHierarchy (clazz, ancestor, collection);
            ancestor = ancestor.getSuperclass();
        } 
        
        return collection;
    }
    
    
    
    /**
     * Methods which are inherited retain their annotations.
     * Methods which are not inherited and not overridden or hidden must also have their annotations processed.
     * An overridden method can remove or change it's annotations.
     * @param targetClazz
     * @param ancestor
     * @param targetClazzMethods
     */
    private static void processHierarchy (Class targetClazz, Class ancestor, AnnotationCollection collection)
    {
        if (targetClazz==null)
            return;
        if (ancestor==null)
            return;
        
        //If the ancestor has class level annotations, remember it
        collection.addClass(ancestor);
        
        //Get annotations on the declared methods of the ancestor class. 
        //For each declared method that has an annotation, we need to
        //determine if that method is inheritable&&!overridden or hidden
        //in derived classes of the ancestor, in which case it contributes
        //an annotation to the collection
        //OR
        //if the method is not inheritable, but has an annotation, it still
        //contributes an annotation (even private non-inherited methods must
        //have their annotations honoured)
        Method[] methods = ancestor.getDeclaredMethods();
        for (int i=0; i<methods.length;i++)
        {
            if (methods[i].getAnnotations().length > 0)
            {
               if (!isOverriddenOrHidden(targetClazz, methods[i]))
                   collection.addMethod(methods[i]);
            } 
        }
        
        //Get annotations on declared fields. For each field work out if it is
        //overridden or hidden in targetClazz
        Field[] fields = ancestor.getDeclaredFields();
        for (int i=0;i<fields.length;i++)
        {
            if (fields[i].getAnnotations().length > 0)
            {
                //the field has annotations, so check to see if it should be inherited
                //field is inheritable if it is:
                // NOT private
                // of package scope and of the same package
                if (!isHidden(targetClazz, fields[i]))
                    collection.addField(fields[i]);

            }
        }
    }
    

    
    
    /**
     * isOverriddenOrHidden
     * 
     * Find out if method is overridden or hidden in the hierarchy down towards the 
     * most derived targetClass.
     * 
     * case private: 
     *    never inherited so therefore cannot be overridden or hidden return false;
     *    
     * case public:
     * case protected:
     *     inherited if no class from derived up to class declaring the method declares a method of the same signature
     *     
     * case package:
     *      inherited if all classes in same package from derived to declaring class and no method of the same signature
     * 
     * @param derivedClass the most derived class we are processing
     * @param superclassMethod a method to check for being overridden or hidden
     */
    private static boolean isOverriddenOrHidden (Class derivedClass, Method superclassMethod)
    {
        if (Modifier.isPrivate(superclassMethod.getModifiers()))
            return false; //private methods cannot be inherited therefore cannot be overridden
        
        if (Modifier.isPublic(superclassMethod.getModifiers()) || Modifier.isProtected(superclassMethod.getModifiers()))
        {
            //check to see if any class from most derived up to the declaring class for the method contains a method of the same sig
            boolean sameSig = false;
            Class c = derivedClass;
            while (c != superclassMethod.getDeclaringClass()&&!sameSig)
            {
                sameSig = IntrospectionUtil.containsSameMethodSignature(superclassMethod, c, false);
                c = c.getSuperclass();
            }
            return sameSig;
        }
        
        //package protected
        //check to see if any class from most derived up to declaring class contains method of same sig and that all
        //intervening classes are of the same package (otherwise inheritance is blocked)
        boolean sameSig = false;
        Class c = derivedClass;
        while (c != superclassMethod.getDeclaringClass() && !sameSig)
        {
            sameSig = IntrospectionUtil.containsSameMethodSignature(superclassMethod, c, true);
            c = c.getSuperclass();
        }
        return sameSig;
    }

    
    
    /**
     * isHidden determines if a field from a superclass is hidden by field
     * of the same name in any of the derived classes.
     * 
     * We check upwards from the most derived class to the class containing
     * the field.
     * @param derivedClass the most derived class
     * @param superclassField
     * @return
     */
    private static boolean isHidden (Class derivedClass, Field superclassField)
    {
        if (Modifier.isPrivate(superclassField.getModifiers()))
            return false; //private methods are never inherited therefore never hidden
        
        if (Modifier.isPublic(superclassField.getModifiers()) || Modifier.isProtected(superclassField.getModifiers()))
        {
            boolean hidden = false;
            Class c = derivedClass;
            while (!c.equals(superclassField.getDeclaringClass()) && !hidden)
            {
                hidden = IntrospectionUtil.containsSameFieldName(superclassField, c, false);
                c=c.getSuperclass();
            }
            return hidden;
        }
        
        //Package scope
        //Derived classes hide the field if they are in the same package and have same field name
        boolean hidden = false;
        Class c = derivedClass;
        while (!c.equals(superclassField.getDeclaringClass()) && !hidden)
        {
            hidden = IntrospectionUtil.containsSameFieldName(superclassField, c, true);
        }
        return hidden;
    }
}
