//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

//========================================================================
//$Id: $
// JBoss Jetty Integration
//------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//========================================================================
package org.jboss.jetty.http;

import java.io.IOException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.net.ssl.SSLServerSocketFactory;

import org.jboss.security.SecurityDomain;
import org.jboss.security.ssl.DomainServerSocketFactory;
import org.mortbay.jetty.security.SslSocketConnector;

/** 
 * SecurityDomain
 * 
 * A subclass of JsseListener that uses the KeyStore associated with the
 * SecurityDomain given by the SecurityDomain attribute.
 *
 * @see org.jboss.security.SecurityDoamin
 *
 * @author  Scott.Stark@jboss.org
 *
 */
public class SecurityDomainListener extends SslSocketConnector
{
   private String securityDomainName;
   private SecurityDomain securityDomain;

   public SecurityDomainListener()
       throws IOException
   {
       super();
   }

   public String getSecurityDomain()
   {
      return securityDomainName;
   }
   public void setSecurityDomain(String securityDomainName)
      throws NamingException
   {
      this.securityDomainName = securityDomainName;
      InitialContext iniCtx = new InitialContext();
      this.securityDomain = (SecurityDomain) iniCtx.lookup(securityDomainName);
   }

   protected SSLServerSocketFactory createFactory() throws Exception
   {
      DomainServerSocketFactory dssf = new DomainServerSocketFactory(securityDomain);
      return dssf;
   }
   
}
