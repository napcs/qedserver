//========================================================================
//$Id: Jetty6DeployWar.java 2301 2008-01-04 05:19:03Z janb $
//Copyright 2000-2009 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plugin;


/**
 * <p>
 * This goal is used to run Jetty with a pre-assembled war.
 * </p>
 * <p>
 * It accepts exactly the same options as the <a href="run-war-mojo.html">run-war</a> goal. 
 * However, it doesn't assume that the current artifact is a
 * webapp and doesn't try to assemble it into a war before its execution. 
 * So using it makes sense only when used in conjunction with the 
 * <a href="run-war-mojo.html#webApp">webApp</a> configuration parameter pointing to a pre-built WAR.
 * </p>
 * <p>
 * This goal is useful e.g. for launching a web app in Jetty as a target for unit-tested 
 * HTTP client components.
 * </p>
 * 
 * @goal deploy-war
 * @requiresDependencyResolution runtime
 * @execute phase="validate"
 * @description Deploy a pre-assembled war
 * 
 */
public class Jetty6DeployWar extends Jetty6RunWar
{
}
