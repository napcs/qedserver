// ========================================================================
// Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.terracotta.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Random;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.client.ContentExchange;
import org.mortbay.jetty.client.HttpClient;

/**
 * Target of this test is to check that when a webapp on nodeA puts in the session
 * an object of a class loaded from the war (and hence with a WebAppClassLoader),
 * the same webapp on nodeB is able to load that object from the session.
 * This is only possible if the NamedClassLoader mechanism in Terracotta is setup
 * appropriately and it is working correctly, which the scope of this test.
 *
 * @version $Revision: 1645 $ $Date: 2009-09-15 20:31:07 +1000 (Tue, 15 Sep 2009) $
 */
public class WebAppObjectInSessionTest
{
    private void copy(File source, File target) throws Exception
    {
        FileInputStream input = new FileInputStream(source);
        FileOutputStream output = new FileOutputStream(target);
        int read = -1;
        byte[] bytes = new byte[64];
        while ((read = input.read(bytes)) >= 0) output.write(bytes, 0, read);
        input.close();
        output.close();
    }

    // TODO: Restore this test once the new Terracotta TIM module has been updated to work with our implementation
    // TODO: The Terracotta TIM module must be referenced from the tc-config.xml file.
//    @Test
    public void testWebappObjectInSession() throws Exception
    {
        String contextName = "webappObjectInSessionTest";
        String contextPath = "/" + contextName;
        String servletMapping = "/server";

        File targetDir = new File(System.getProperty("basedir"), "target");
        File warDir = new File(targetDir, contextName);
        warDir.mkdir();
        File webInfDir = new File(warDir, "WEB-INF");
        webInfDir.mkdir();
        // Write web.xml
        File webXml = new File(webInfDir, "web.xml");
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<web-app xmlns=\"http://java.sun.com/xml/ns/j2ee\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd\"\n" +
                "         version=\"2.4\">\n" +
                "\n" +
                "</web-app>";
        FileWriter w = new FileWriter(webXml);
        w.write(xml);
        w.close();
        File classesDir = new File(webInfDir, "classes");
        classesDir.mkdir();
        String packageName = WebAppObjectInSessionServlet.class.getPackage().getName();
        File packageDirs = new File(classesDir, packageName.replace('.', File.separatorChar));
        packageDirs.mkdirs();
        String resourceName = WebAppObjectInSessionServlet.class.getName().replace('.', File.separatorChar) + ".class";
        File sourceFile = new File(getClass().getClassLoader().getResource(resourceName).toURI());
        File targetFile = new File(packageDirs, resourceName.substring(packageName.length()));
        copy(sourceFile, targetFile);
        resourceName = WebAppObjectInSessionServlet.TestSharedNonStatic.class.getName().replace('.', File.separatorChar) + ".class";
        sourceFile = new File(getClass().getClassLoader().getResource(resourceName).toURI());
        targetFile = new File(packageDirs, resourceName.substring(packageName.length()));
        copy(sourceFile, targetFile);
        resourceName = WebAppObjectInSessionServlet.TestSharedStatic.class.getName().replace('.', File.separatorChar) + ".class";
        sourceFile = new File(getClass().getClassLoader().getResource(resourceName).toURI());
        targetFile = new File(packageDirs, resourceName.substring(packageName.length()));
        copy(sourceFile, targetFile);

        Random random = new Random(System.nanoTime());

        int port1 = random.nextInt(50000) + 10000;
        TerracottaJettyServer server1 = new TerracottaJettyServer(port1);
        server1.addWebAppContext(warDir.getCanonicalPath(), contextPath).addServlet(WebAppObjectInSessionServlet.class.getName(), servletMapping);
        server1.start();
        try
        {
            int port2 = random.nextInt(50000) + 10000;
            TerracottaJettyServer server2 = new TerracottaJettyServer(port2);
            server2.addWebAppContext(warDir.getCanonicalPath(), contextPath).addServlet(WebAppObjectInSessionServlet.class.getName(), servletMapping);
            server2.start();
            try
            {
                HttpClient client = new HttpClient();
                client.setConnectorType(HttpClient.CONNECTOR_SOCKET);
                client.start();
                try
                {
                    // Perform one request to server1 to create a session
                    ContentExchange exchange1 = new ContentExchange(true);
                    exchange1.setMethod(HttpMethods.GET);
                    exchange1.setURL("http://localhost:" + port1 + contextPath + servletMapping + "?action=set");
                    client.send(exchange1);
                    exchange1.waitForDone();
                    assert exchange1.getResponseStatus() == HttpServletResponse.SC_OK : exchange1.getResponseStatus();
                    String sessionCookie = exchange1.getResponseFields().getStringField("Set-Cookie");
                    assert sessionCookie != null;
                    // Mangle the cookie, replacing Path with $Path, etc.
                    sessionCookie = sessionCookie.replaceFirst("(\\W)(P|p)ath=", "$1\\$Path=");

                    // Perform a request to server2 using the session cookie from the previous request
                    ContentExchange exchange2 = new ContentExchange(true);
                    exchange2.setMethod(HttpMethods.GET);
                    exchange2.setURL("http://localhost:" + port2 + contextPath + servletMapping + "?action=get");
                    exchange2.getRequestFields().add("Cookie", sessionCookie);
                    client.send(exchange2);
                    exchange2.waitForDone();
                    assert exchange2.getResponseStatus() == HttpServletResponse.SC_OK;
                }
                finally
                {
                    client.stop();
                }
            }
            finally
            {
                server2.stop();
            }
        }
        finally
        {
            server1.stop();
        }
    }
}
