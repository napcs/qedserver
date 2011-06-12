//========================================================================
//$Id: JEEContextLoaderListener.java 1359 2006-12-06 17:29:50Z janb $
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


import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;

/**
 * JEEContextLoaderListener
 * 
 * This creates the JEEContextLoader to hook the pitchfork ebj3
 * environment into webapps.
 *
 */
public class JEEContextLoaderListener extends ContextLoaderListener
{

    /** 
     * @see org.springframework.web.context.ContextLoaderListener#createContextLoader()
     */
    protected ContextLoader createContextLoader()
    {
        return new JEEContextLoader();
    }
}
