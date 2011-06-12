//========================================================================
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
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

package com.acme;

import org.mortbay.component.AbstractLifeCycle;

public class Base extends AbstractLifeCycle
{
    String name;
    int value;
    String[] messages;
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the messages.
     */
    public String[] getMessages()
    {
        return messages;
    }
    /* ------------------------------------------------------------ */
    /**
     * @param messages The messages to set.
     */
    public void setMessages(String[] messages)
    {
        this.messages = messages;
    }
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the name.
     */
    public String getName()
    {
        return name;
    }
    /* ------------------------------------------------------------ */
    /**
     * @param name The name to set.
     */
    public void setName(String name)
    {
        this.name = name;
    }
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the value.
     */
    public int getValue()
    {
        return value;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param value The value to set.
     */
    public void setValue(int value)
    {
        this.value = value;
    }

    /* ------------------------------------------------------------ */
    public void doSomething(int arg)
    {
        System.err.println("doSomething "+arg);
    }

    /* ------------------------------------------------------------ */
    public String findSomething(int arg)
    {
        return ("found "+arg);
    }
    
    
}
