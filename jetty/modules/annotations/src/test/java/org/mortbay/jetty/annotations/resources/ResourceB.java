//========================================================================
//$Id: ResourceB.java 1540 2007-01-19 12:24:10Z janb $
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
import javax.annotation.Resources;

/**
 * ResourceB
 *
 *
 */
@Resources({
    @Resource(name="fluff", mappedName="resA"),
    @Resource(name="stuff", mappedName="resB")
})
public class ResourceB extends ResourceA
{
    @Resource(mappedName="resB")
    private Integer f;//test no inheritance of private fields
    
    @Resource
    private Integer p = new Integer(8); //test no injection because no value
    
    //test no annotation
    public void z()
    {
        System.err.println("ResourceB.z");
    }
}
