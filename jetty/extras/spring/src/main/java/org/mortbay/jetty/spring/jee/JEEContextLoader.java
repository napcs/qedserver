//========================================================================
//$Id: JEEContextLoader.java 1360 2006-12-07 09:57:11Z janb $
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

package org.mortbay.jetty.spring.jee;


import javax.servlet.ServletContext;

import org.mortbay.log.Log;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.ContextLoader;

/**
 * JEEContextLoader
 * 
 * Loads the spring-ejb.xml files describing ejbs from any META-INF dir in any
 * jar or classes dir on the runtime classpath.
 * 
 * Also loads the jee bootstrapper to perform resource injection etc on the
 * ejbs. The bootstrapper config file is part of the jetty spring jar.
 * 
 * This spring context will be the parent context of the usual spring web app
 * context (ie applicationContext.xml).
 */
public class JEEContextLoader extends ContextLoader
{

    protected ApplicationContext loadParentContext(ServletContext servletContext)
    throws BeansException 
    {
        Log.info("Bootstrapping JEE application context");
        ApplicationContext parentContext = new ClassPathXmlApplicationContext( 
                new String[] { "classpath*:/META-INF/spring-ejb-jar.xml"});

        return parentContext;
    }
}
