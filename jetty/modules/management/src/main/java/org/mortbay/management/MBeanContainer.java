//========================================================================
//Copyright 2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.management;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.loading.PrivateMLet;

import org.mortbay.component.Container;
import org.mortbay.component.Container.Relationship;
import org.mortbay.log.Log;
import org.mortbay.log.Logger;
import org.mortbay.util.MultiMap;
import org.mortbay.util.TypeUtil;

public class MBeanContainer implements Container.Listener
{
    private final MBeanServer _server;
    private volatile int _managementPort;
    private final WeakHashMap _beans = new WeakHashMap();
    private final HashMap _unique = new HashMap();
    private String _domain = null;
    private MultiMap _relations = new MultiMap();
    

    public synchronized ObjectName findMBean(Object object)
    {
        ObjectName bean = (ObjectName)_beans.get(object);
        return bean==null?null:bean; 
    }

    public synchronized Object findBean(ObjectName oname)
    {
        for (Iterator iter = _beans.entrySet().iterator(); iter.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iter.next();
            ObjectName bean = (ObjectName)entry.getValue();
            if (bean.equals(oname))
                return entry.getKey();
        }
        return null;
    }

    public MBeanContainer(MBeanServer server)
    {
        this._server = server;
        Logger log = Log.getLog();
        if (log!=null)
            addBean(log);
    }
    
    public MBeanServer getMBeanServer()
    {
        return _server;
    }
    
    
    public void setDomain (String domain)
    {
        _domain =domain;
    }
    
    public String getDomain()
    {
        return _domain;
    }
    
    public void setManagementPort(int port)
    {
        this._managementPort = port;
    }

    public void start()
    {
        if (_managementPort > 0)
        {
            try
            {
                Log.warn("HttpAdaptor for mx4j is not secure");

                PrivateMLet mlet = new PrivateMLet(new URL[0], Thread.currentThread().getContextClassLoader(), false);
                ObjectName mletName = ObjectName.getInstance("mx4j", "name", "HttpAdaptorLoader");
                _server.registerMBean(mlet, mletName);

                ObjectName adaptorName = ObjectName.getInstance("mx4j", "name", "HttpAdaptor");
                _server.createMBean("mx4j.tools.adaptor.http.HttpAdaptor", adaptorName, mletName);
                _server.setAttribute(adaptorName, new Attribute("Port", new Integer(_managementPort)));
                _server.setAttribute(adaptorName, new Attribute("Host", "localhost"));

                ObjectName processorName = ObjectName.getInstance("mx4j", "name", "XSLTProcessor");
                _server.createMBean("mx4j.tools.adaptor.http.XSLTProcessor", processorName, mletName);
                _server.setAttribute(adaptorName, new Attribute("ProcessorName", processorName));

                _server.invoke(adaptorName, "start", null, null);

                Runtime.getRuntime().addShutdownHook(new ShutdownHook(mletName, adaptorName, processorName));
            }
            catch (Exception e)
            {
                Log.warn(e);
            }
        }
    }

    public synchronized void add(Relationship relationship)
    {
        ObjectName parent=(ObjectName)_beans.get(relationship.getParent());
        if (parent==null)
        {
            addBean(relationship.getParent());
            parent=(ObjectName)_beans.get(relationship.getParent());
        }
        
        ObjectName child=(ObjectName)_beans.get(relationship.getChild());
        if (child==null)
        {
            addBean(relationship.getChild());
            child=(ObjectName)_beans.get(relationship.getChild());
        }
        
        if (parent!=null && child!=null)
            _relations.add(parent,relationship);
        
        
    }

    public synchronized void remove(Relationship relationship)
    {
        ObjectName parent=(ObjectName)_beans.get(relationship.getParent());
        ObjectName child=(ObjectName)_beans.get(relationship.getChild());
        if (parent!=null && child!=null)
            _relations.removeValue(parent,relationship);
    }

    public synchronized void removeBean(Object obj)
    {
        ObjectName bean=(ObjectName)_beans.get(obj);

        if (bean!=null)
        {
            List r=_relations.getValues(bean);
            if (r!=null && r.size()>0)
            {
                Log.debug("Unregister {}", r);
                Iterator iter = new ArrayList(r).iterator();
                while (iter.hasNext())
                {
                    Relationship rel = (Relationship)iter.next();
                    rel.getContainer().update(rel.getParent(),rel.getChild(),null,rel.getRelationship(),true);
                }
            }
            
            try
            {
                _server.unregisterMBean(bean);
                Log.debug("Unregistered {}", bean);
            }
            catch (javax.management.InstanceNotFoundException e)
            {
                Log.ignore(e);
            }
            catch (Exception e)
            {
                Log.warn(e);
            }
        }
    }
    
    public synchronized void addBean(Object obj)
    {
        try
        {
            if (obj == null || _beans.containsKey(obj))
                return;
            
            Object mbean = ObjectMBean.mbeanFor(obj);
            if (mbean == null)
                return;

            ObjectName oname = null;
            if (mbean instanceof ObjectMBean)
            {
                ((ObjectMBean) mbean).setMBeanContainer(this);
                oname = ((ObjectMBean)mbean).getObjectName();
            }
            
            //no override mbean object name, so make a generic one
            if (oname == null)
            {
                String type=obj.getClass().getName().toLowerCase();
                int dot = type.lastIndexOf('.');
                if (dot >= 0)
                    type = type.substring(dot + 1);
                
                String name=null;
                if (mbean instanceof ObjectMBean)
                {
                    name = ((ObjectMBean)mbean).getObjectNameBasis();
                    if (name!=null)
                    {
                        name=name.replace('\\','/');
                        if (name.endsWith("/"))
                            name=name.substring(0,name.length()-1);

                        int slash=name.lastIndexOf('/',name.length()-1);
                        if (slash>0)
                            name=name.substring(slash+1);
                        dot=name.lastIndexOf('.');
                        if (dot>0)
                            name=name.substring(0,dot);

                        name=name.replace(':','_').replace('*','_').replace('?','_').replace('=','_').replace(',','_').replace(' ','_');
                    }
                }
                
                String basis=(name!=null&&name.length()>1)?("type="+type+",name="+name):("type="+type);
                
                Integer count = (Integer) _unique.get(basis);
                count = TypeUtil.newInteger(count == null ? 0 : (1 + count.intValue()));
                _unique.put(basis, count);

                //if no explicit domain, create one
                String domain = _domain;
                if (domain==null)
                    domain = obj.getClass().getPackage().getName();

                oname = ObjectName.getInstance(domain+":"+basis+",id="+count);
            }
            
            ObjectInstance oinstance = _server.registerMBean(mbean, oname);
            Log.debug("Registered {}" , oinstance.getObjectName());
            _beans.put(obj, oinstance.getObjectName());

        }
        catch (Exception e)
        {
            Log.warn("bean: "+obj,e);
        }
    }

    private class ShutdownHook extends Thread
    {
        private final ObjectName mletName;
        private final ObjectName adaptorName;
        private final ObjectName processorName;

        public ShutdownHook(ObjectName mletName, ObjectName adaptorName, ObjectName processorName)
        {
            this.mletName = mletName;
            this.adaptorName = adaptorName;
            this.processorName = processorName;
        }

        public void run()
        {
            halt();
            unregister(processorName);
            unregister(adaptorName);
            unregister(mletName);
        }

        private void halt()
        {
            try
            {
                _server.invoke(adaptorName, "stop", null, null);
            }
            catch (Exception e)
            {
                Log.warn(e);
            }
        }

        private void unregister(ObjectName objectName)
        {
            try
            {
                _server.unregisterMBean(objectName);
                Log.debug("Unregistered " + objectName);
            }
            catch (Exception e)
            {
                Log.warn(e);
            }
        }
    }
    
}
