//========================================================================
//$Id: ClassB.java 1540 2007-01-19 12:24:10Z janb $
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



/**
 * ClassB
 *
 *
 */
@Sample(50)
public class ClassB extends ClassA
{

    //test override of public scope method
    @Sample(51)
    public void a()
    {
       System.err.println("ClassB.public");
    }
    
    //test override of package scope method
    @Sample(52)
    void c()
    {
        System.err.println("ClassB.package");
    }
    
    public void l()
    {
        System.err.println("Overridden method l has no annotation");
    }
    
    //test no annotation
    public void z()
    {
        System.err.println("ClassB.z");
    }

}
