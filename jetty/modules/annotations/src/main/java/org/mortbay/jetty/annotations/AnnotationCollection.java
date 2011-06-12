//========================================================================
//$Id: AnnotationCollection.java 3680 2008-09-21 10:37:13Z janb $
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


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resources;
import javax.annotation.security.RunAs;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.servlet.Servlet;
import javax.transaction.UserTransaction;

import org.mortbay.jetty.plus.annotation.Injection;
import org.mortbay.jetty.plus.annotation.InjectionCollection;
import org.mortbay.jetty.plus.annotation.LifeCycleCallbackCollection;
import org.mortbay.jetty.plus.annotation.PostConstructCallback;
import org.mortbay.jetty.plus.annotation.PreDestroyCallback;
import org.mortbay.jetty.plus.annotation.RunAsCollection;
import org.mortbay.jetty.plus.naming.EnvEntry;
import org.mortbay.jetty.plus.naming.Transaction;
import org.mortbay.jetty.servlet.Holder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;
import org.mortbay.util.IntrospectionUtil;
import org.mortbay.util.Loader;



/**
 * AnnotationCollection
 * 
 * An AnnotationCollection represents all of the annotated classes, methods and fields in the
 * inheritance hierarchy for a class. NOTE that methods and fields in this collection are NOT
 * just the ones that are inherited by the class, but represent ALL annotations that must be
 * processed for a single instance of a given class.
 * 
 * The class to which this collection pertains is obtained by calling
 * getTargetClass().
 * 
 * Using the list of annotated classes, methods and fields, the collection will generate
 * the appropriate JNDI entries and the appropriate Injection and LifeCycleCallback objects
 * to be later applied to instances of the getTargetClass().
 */
public class AnnotationCollection
{
    private WebAppContext _webApp; //the webapp
    private Class _targetClass; //the most derived class to which this collection pertains
    private List _methods = new ArrayList(); //list of methods relating to the _targetClass which have annotations
    private List _fields = new ArrayList(); //list of fields relating to the _targetClass which have annotations
    private List _classes = new ArrayList();//list of classes in the inheritance hierarchy that have annotations
    private static Class[] __envEntryTypes = 
        new Class[] {String.class, Character.class, Integer.class, Boolean.class, Double.class, Byte.class, Short.class, Long.class, Float.class};
   
    
    public void setWebAppContext(WebAppContext webApp)
    {
        _webApp=webApp;
    }
  
    public WebAppContext getWebAppContext()
    {
        return _webApp;
    }
    
    /**
     * Get the class which is the subject of these annotations
     * @return the clazz
     */
    public Class getTargetClass()
    {
        return _targetClass;
    }
    
    /** 
     * Set the class to which this collection pertains
     * @param clazz the clazz to set
     */
    public void setTargetClass(Class clazz)
    {
        _targetClass=clazz;
    }
    
    
    public void addClass (Class clazz)
    {
        if (clazz.getDeclaredAnnotations().length==0)
            return;
        _classes.add(clazz);
    }
    
    public void addMethod (Method method)
    {
        if (method.getDeclaredAnnotations().length==0)
            return;
       _methods.add(method);
    }
    
    public void addField(Field field)
    {
        if (field.getDeclaredAnnotations().length==0)
            return;
        _fields.add(field);
    }
    
    public List getClasses()
    {
        return _classes;
    }
    public List getMethods ()
    {
        return _methods;
    }
    
    
    public List getFields()
    {
        return _fields;
    }
    
    
    
    public void processRunAsAnnotations (RunAsCollection runAsCollection)
    {
        for (int i=0; i<_classes.size();i++)
        {
            Class clazz = (Class)_classes.get(i);

            //if this implements javax.servlet.Servlet check for run-as
            if (Servlet.class.isAssignableFrom(clazz))
            { 
                RunAs runAs = (RunAs)clazz.getAnnotation(RunAs.class);
                if (runAs != null)
                {
                    String role = runAs.value();
                    if (role != null)
                    {
                        org.mortbay.jetty.plus.annotation.RunAs ra = new org.mortbay.jetty.plus.annotation.RunAs();
                        ra.setTargetClass(clazz);
                        ra.setRoleName(role);
                        runAsCollection.add(ra);
                    }
                }
            }
        } 
    }
    
    
    
    /**
     * Process @Resource annotations at the class, method and field level.
     * @return
     */
    public InjectionCollection processResourceAnnotations(InjectionCollection injections)
    {      
        processClassResourceAnnotations();
        processMethodResourceAnnotations(injections);
        processFieldResourceAnnotations(injections);
        
        return injections;
    }
  
  
    /**
     * Process @PostConstruct and @PreDestroy annotations.
     * @return
     */
    public LifeCycleCallbackCollection processLifeCycleCallbackAnnotations(LifeCycleCallbackCollection callbacks)
    {
        processPostConstructAnnotations(callbacks);
        processPreDestroyAnnotations(callbacks);
        return callbacks;
    }
    
    
    
    
    /**
     * Process @Resources annotation on classes
     */
    public void processResourcesAnnotations ()
    {        
        for (int i=0; i<_classes.size();i++)
        {
            Class clazz = (Class)_classes.get(i);
            Resources resources = (Resources)clazz.getAnnotation(Resources.class);
            if (resources != null)
            {
                Resource[] resArray = resources.value();
                if (resArray==null||resArray.length==0)
                    continue;

                for (int j=0;j<resArray.length;j++)
                {

                    String name = resArray[j].name();
                    String mappedName = resArray[j].mappedName();
                    Resource.AuthenticationType auth = resArray[j].authenticationType();
                    Class type = resArray[j].type();
                    boolean shareable = resArray[j].shareable();

                    if (name==null || name.trim().equals(""))
                        throw new IllegalStateException ("Class level Resource annotations must contain a name (Common Annotations Spec Section 2.3)");

                    try
                    {
                        //TODO don't ignore the shareable, auth etc etc

                           if (!org.mortbay.jetty.plus.naming.NamingEntryUtil.bindToENC(_webApp, name, mappedName))
                               if (!org.mortbay.jetty.plus.naming.NamingEntryUtil.bindToENC(_webApp.getServer(), name, mappedName))
                                   throw new IllegalStateException("No resource bound at "+(mappedName==null?name:mappedName));
                    }
                    catch (NamingException e)
                    {
                        throw new IllegalStateException(e);
                    }
                }
            }
        } 
    }
    
    
    /**
     *  Class level Resource annotations declare a name in the
     *  environment that will be looked up at runtime. They do
     *  not specify an injection.
     */
    private void processClassResourceAnnotations ()
    {
        for (int i=0; i<_classes.size();i++)
        {
            Class clazz = (Class)_classes.get(i);
            Resource resource = (Resource)clazz.getAnnotation(Resource.class);
            if (resource != null)
            {
               String name = resource.name();
               String mappedName = resource.mappedName();
               Resource.AuthenticationType auth = resource.authenticationType();
               Class type = resource.type();
               boolean shareable = resource.shareable();
               
               if (name==null || name.trim().equals(""))
                   throw new IllegalStateException ("Class level Resource annotations must contain a name (Common Annotations Spec Section 2.3)");
               
               try
               {
                   //TODO don't ignore the shareable, auth etc etc
                   if (!org.mortbay.jetty.plus.naming.NamingEntryUtil.bindToENC(_webApp, name,mappedName))
                       if (!org.mortbay.jetty.plus.naming.NamingEntryUtil.bindToENC(_webApp.getServer(), name,mappedName))
                           throw new IllegalStateException("No resource at "+(mappedName==null?name:mappedName));
               }
               catch (NamingException e)
               {
                   throw new IllegalStateException(e);
               }
            }
        }
    }
    
    /**
     * Process a Resource annotation on the Methods.
     * 
     * This will generate a JNDI entry, and an Injection to be
     * processed when an instance of the class is created.
     * @param injections
     */
    private void processMethodResourceAnnotations(InjectionCollection webXmlInjections)
    {
        //Get the method level Resource annotations        
        for (int i=0;i<_methods.size();i++)
        {
            Method m = (Method)_methods.get(i);
            Resource resource = (Resource)m.getAnnotation(Resource.class);
            if (resource != null)
            {
                //JavaEE Spec 5.2.3: Method cannot be static
                if (Modifier.isStatic(m.getModifiers()))
                    throw new IllegalStateException(m+" cannot be static");
                
                
                // Check it is a valid javabean 
                if (!IntrospectionUtil.isJavaBeanCompliantSetter(m))
                    throw new IllegalStateException(m+" is not a java bean compliant setter method");
     
                //allow default name to be overridden
                String name = (resource.name()!=null && !resource.name().trim().equals("")? resource.name(): defaultResourceNameFromMethod(m));
                //get the mappedName if there is one
                String mappedName = (resource.mappedName()!=null && !resource.mappedName().trim().equals("")?resource.mappedName():null);       
                Class type = m.getParameterTypes()[0];
                //get other parts that can be specified in @Resource
                Resource.AuthenticationType auth = resource.authenticationType();
                boolean shareable = resource.shareable();

                //if @Resource specifies a type, check it is compatible with setter param
                if ((resource.type() != null) 
                        && 
                        !resource.type().equals(Object.class)
                        &&
                        (!IntrospectionUtil.isTypeCompatible(type, resource.type(), false)))
                    throw new IllegalStateException("@Resource incompatible type="+resource.type()+ " with method param="+type+ " for "+m);
               
                //check if an injection has already been setup for this target by web.xml
                Injection webXmlInjection = webXmlInjections.getInjection(getTargetClass(), m);
                
                if (webXmlInjection == null)
                {
                    try
                    {
                        //try binding name to environment
                        //try the webapp's environment first
                        boolean bound = org.mortbay.jetty.plus.naming.NamingEntryUtil.bindToENC(_webApp, name, mappedName);
                        
                        //try the server's environment
                        if (!bound)
                            bound = org.mortbay.jetty.plus.naming.NamingEntryUtil.bindToENC(_webApp.getServer(), name, mappedName);
                        
                        //try the jvm's environment
                        if (!bound)
                            bound = org.mortbay.jetty.plus.naming.NamingEntryUtil.bindToENC(null, name, mappedName);
                        
                        //TODO if it is an env-entry from web.xml it can be injected, in which case there will be no
                        //NamingEntry, just a value bound in java:comp/env
                        if (!bound)
                        {
                            try
                            {
                                InitialContext ic = new InitialContext();
                                String nameInEnvironment = (mappedName!=null?mappedName:name);
                                ic.lookup("java:comp/env/"+nameInEnvironment);                               
                                bound = true;
                            }
                            catch (NameNotFoundException e)
                            {
                                bound = false;
                            }
                        }
                        
                        if (bound)
                        {
                            Log.debug("Bound "+(mappedName==null?name:mappedName) + " as "+ name);
                            //   Make the Injection for it
                            Injection injection = new Injection();
                            injection.setTargetClass(getTargetClass());
                            injection.setJndiName(name);
                            injection.setMappingName(mappedName);
                            injection.setTarget(m);
                            webXmlInjections.add(injection);
                        }
                        else if (!isEnvEntryType(type))
                        {

                            //if this is an env-entry type resource and there is no value bound for it, it isn't
                            //an error, it just means that perhaps the code will use a default value instead
                            // JavaEE Spec. sec 5.4.1.3   
                            throw new IllegalStateException("No resource at "+(mappedName==null?name:mappedName));
                        }

                    }
                    catch (NamingException e)
                    {  
                      
                        throw new IllegalStateException(e);
                    }
                }
                else
                {
                    //if an injection is already set up for this name, then the types must be compatible
                    //JavaEE spec sec 5.2.4
                    try
                    {
                         Object value = webXmlInjection.lookupInjectedValue();
                         if (!IntrospectionUtil.isTypeCompatible(type, value.getClass(), false))
                             throw new IllegalStateException("Type of field="+type+" is not compatible with Resource type="+value.getClass());
                    }
                    catch (NamingException e)
                    {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
    }
    
    
    /**
     * Process @Resource annotation for a Field. These will both set up a
     * JNDI entry and generate an Injection. Or they can be the equivalent
     * of env-entries with default values
     * 
     * @param injections
     */
    private void processFieldResourceAnnotations (InjectionCollection webXmlInjections)
    {
        for (int i=0;i<_fields.size();i++)
        {
            Field f = (Field)_fields.get(i);
            Resource resource = (Resource)f.getAnnotation(Resource.class);
            if (resource != null)
            {
                //JavaEE Spec 5.2.3: Field cannot be static
                if (Modifier.isStatic(f.getModifiers()))
                    throw new IllegalStateException(f+" cannot be static");
                
                //JavaEE Spec 5.2.3: Field cannot be final
                if (Modifier.isFinal(f.getModifiers()))
                    throw new IllegalStateException(f+" cannot be final");
                
                //work out default name
                String name = f.getDeclaringClass().getCanonicalName()+"/"+f.getName();
                //allow @Resource name= to override the field name
                name = (resource.name()!=null && !resource.name().trim().equals("")? resource.name(): name);
                
                //get the type of the Field
                Class type = f.getType();
                //if @Resource specifies a type, check it is compatible with field type
                if ((resource.type() != null)
                        && 
                        !resource.type().equals(Object.class)
                        &&
                        (!IntrospectionUtil.isTypeCompatible(type, resource.type(), false)))
                    throw new IllegalStateException("@Resource incompatible type="+resource.type()+ " with field type ="+f.getType());
                
                //get the mappedName if there is one
                String mappedName = (resource.mappedName()!=null && !resource.mappedName().trim().equals("")?resource.mappedName():null);
                //get other parts that can be specified in @Resource
                Resource.AuthenticationType auth = resource.authenticationType();
                boolean shareable = resource.shareable();
            
                //check if an injection has already been setup for this target by web.xml
                Injection webXmlInjection = webXmlInjections.getInjection(getTargetClass(), f);
                if (webXmlInjection == null)
                {
                    try
                    {
                        boolean bound = org.mortbay.jetty.plus.naming.NamingEntryUtil.bindToENC(_webApp, name, mappedName);
                        if (!bound)
                            bound = org.mortbay.jetty.plus.naming.NamingEntryUtil.bindToENC(_webApp.getServer(), name, mappedName);
                        if (!bound)
                            bound =  org.mortbay.jetty.plus.naming.NamingEntryUtil.bindToENC(null, name, mappedName); 
                        if (!bound)
                        {
                            //see if there is an env-entry value been bound from web.xml
                            try
                            {
                                InitialContext ic = new InitialContext();
                                String nameInEnvironment = (mappedName!=null?mappedName:name);
                                ic.lookup("java:comp/env/"+nameInEnvironment);                               
                                bound = true;
                            }
                            catch (NameNotFoundException e)
                            {
                                bound = false;
                            }
                        }
                        //Check there is a JNDI entry for this annotation 
                        if (bound)
                        { 
                            Log.debug("Bound "+(mappedName==null?name:mappedName) + " as "+ name);
                            //   Make the Injection for it if the binding succeeded
                            Injection injection = new Injection();
                            injection.setTargetClass(getTargetClass());
                            injection.setJndiName(name);
                            injection.setMappingName(mappedName);
                            injection.setTarget(f);
                            webXmlInjections.add(injection); 
                        }
                        else if (!isEnvEntryType(type))
                        {
                            //if this is an env-entry type resource and there is no value bound for it, it isn't
                            //an error, it just means that perhaps the code will use a default value instead
                            // JavaEE Spec. sec 5.4.1.3

                            throw new IllegalStateException("No resource at "+(mappedName==null?name:mappedName));
                        }
                    }
                    catch (NamingException e)
                    {
                        throw new IllegalStateException(e);
                    }
                }
                else
                {
                    //if an injection is already set up for this name, then the types must be compatible
                    //JavaEE spec sec 5.2.4
                    try
                    {
                         Object value = webXmlInjection.lookupInjectedValue();
                         if (!IntrospectionUtil.isTypeCompatible(type, value.getClass(), false))
                             throw new IllegalStateException("Type of field="+type+" is not compatible with Resource type="+value.getClass());
                    }
                    catch (NamingException e)
                    {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }  
    }
    
    
    /**
     * Find @PostConstruct annotations.
     * 
     * The spec says (Common Annotations Sec 2.5) that only ONE method
     * may be adorned with the PostConstruct annotation, however this does
     * not clarify how this works with inheritance.
     * 
     * TODO work out what to do with inherited PostConstruct annotations
     * 
     * @param callbacks
     */
    private void processPostConstructAnnotations (LifeCycleCallbackCollection callbacks)
    {
        //      TODO: check that the same class does not have more than one
        for (int i=0; i<_methods.size(); i++)
        {
            Method m = (Method)_methods.get(i);
            if (m.isAnnotationPresent(PostConstruct.class))
            {
                if (m.getParameterTypes().length != 0)
                    throw new IllegalStateException(m+" has parameters");
                if (m.getReturnType() != Void.TYPE)
                    throw new IllegalStateException(m+" is not void");
                if (m.getExceptionTypes().length != 0)
                    throw new IllegalStateException(m+" throws checked exceptions");
                if (Modifier.isStatic(m.getModifiers()))
                    throw new IllegalStateException(m+" is static");
                
                
                PostConstructCallback callback = new PostConstructCallback();
                callback.setTargetClass(getTargetClass());
                callback.setTarget(m);
                callbacks.add(callback);
            }
        }
    }
    
    /**
     * Find @PreDestroy annotations.
     * 
     * The spec says (Common Annotations Sec 2.5) that only ONE method
     * may be adorned with the PreDestroy annotation, however this does
     * not clarify how this works with inheritance.
     * 
     * TODO work out what to do with inherited PreDestroy annotations
     * @param callbacks
     */
    private void processPreDestroyAnnotations (LifeCycleCallbackCollection callbacks)
    {
        //TODO: check that the same class does not have more than one
        
        for (int i=0; i<_methods.size(); i++)
        {
            Method m = (Method)_methods.get(i);
            if (m.isAnnotationPresent(PreDestroy.class))
            {
                if (m.getParameterTypes().length != 0)
                    throw new IllegalStateException(m+" has parameters");
                if (m.getReturnType() != Void.TYPE)
                    throw new IllegalStateException(m+" is not void");
                if (m.getExceptionTypes().length != 0)
                    throw new IllegalStateException(m+" throws checked exceptions");
                if (Modifier.isStatic(m.getModifiers()))
                    throw new IllegalStateException(m+" is static");
                
                PreDestroyCallback callback = new PreDestroyCallback(); 
                callback.setTargetClass(getTargetClass());
                callback.setTarget(m);
                callbacks.add(callback);
            }
        }
    }
    
 
    private static boolean isEnvEntryType (Class type)
    {
        boolean result = false;
        for (int i=0;i<__envEntryTypes.length && !result;i++)
        {
            result = (type.equals(__envEntryTypes[i]));
        }
        return result;
    }
    
    private static Class getNamingEntryType (Class type)
    {
        if (type==null)
            return null;
        
        if (isEnvEntryType(type))
            return EnvEntry.class;
        
        if (type.getName().equals("javax.transaction.UserTransaction"))
                return Transaction.class;
        else
            return org.mortbay.jetty.plus.naming.Resource.class;
    }
    
    private String defaultResourceNameFromMethod (Method m)
    {
        String name = m.getName().substring(3);
        name = name.substring(0,1).toLowerCase()+name.substring(1);
        return m.getDeclaringClass().getCanonicalName()+"/"+name;
    }

}
