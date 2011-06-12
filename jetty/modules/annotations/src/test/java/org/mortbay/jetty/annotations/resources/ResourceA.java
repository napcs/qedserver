//========================================================================
//$Id: ResourceA.java 1540 2007-01-19 12:24:10Z janb $
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

package org.mortbay.jetty.annotations.resources;

import javax.annotation.Resource;

/**
 * ResourceA
 *
 *
 */
public class ResourceA
{
    private Integer e;
    private Integer h;
    private Integer k;

    
    @Resource(name="myf", mappedName="resB") //test giving both a name and mapped name from the environment
    private Integer f;//test a non inherited field that needs injection
    
    @Resource(mappedName="resA") //test the default naming scheme but using a mapped name from the environment
    private Integer g;
    
    @Resource(name="resA") //test using the given name as the name from the environment
    private Integer j;
    
    @Resource(mappedName="resB") //test using the default name on an inherited field
    protected Integer n; //TODO - if it's inherited, is it supposed to use the classname of the class it is inherited by?
    
    
    @Resource(name="mye", mappedName="resA", type=Integer.class)
    public void setE(Integer e)
    {
        this.e=e;
    }
    public Integer getE()
    {
        return this.e;
    }
    
    public Integer getF()
    {
        return this.f;
    }
    
    public Integer getG()
    {
        return this.g;
    }
    
    public Integer getJ()
    {
        return this.j;
    }
    
    @Resource(mappedName="resA")
    public void setH(Integer h)
    {
        this.h=h;
    }
    
    @Resource(name="resA")
    public void setK(Integer k)
    {
        this.k=k;
    }
    public void x()
    {
        System.err.println("ResourceA.x");
    }
}