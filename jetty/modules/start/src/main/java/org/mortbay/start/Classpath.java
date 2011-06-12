// ========================================================================
// Copyright 2002-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.start;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Vector;


/**
 * Class to handle CLASSPATH construction
 * @author Jan Hlavaty
 */
public class Classpath {

    Vector _elements = new Vector();    

    public Classpath()
    {}    

    public Classpath(String initial)
    {
        addClasspath(initial);
    }
        
    public boolean addComponent(String component)
    {
        if ((component != null)&&(component.length()>0)) {
            try {
                File f = new File(component);
                if (f.exists())
                {
                    File key = f.getCanonicalFile();
                    if (!_elements.contains(key))
                    {
                        _elements.add(key);
                        return true;
                    }
                }
            } catch (IOException e) {}
        }
        return false;
    }
    
    public boolean addComponent(File component)
    {
        if (component != null) {
            try {
                if (component.exists()) {
                    File key = component.getCanonicalFile();
                    if (!_elements.contains(key)) {
                        _elements.add(key);
                        return true;
                    }
                }
            } catch (IOException e) {}
        }
        return false;
    }

    public boolean addClasspath(String s)
    {
        boolean added=false;
        if (s != null)
        {
            StringTokenizer t = new StringTokenizer(s, File.pathSeparator);
            while (t.hasMoreTokens())
            {
                added|=addComponent(t.nextToken());
            }
        }
        return added;
    }    
    
    public String toString()
    {
        StringBuffer cp = new StringBuffer(1024);
        int cnt = _elements.size();
        if (cnt >= 1) {
            cp.append( ((File)(_elements.elementAt(0))).getPath() );
        }
        for (int i=1; i < cnt; i++) {
            cp.append(File.pathSeparatorChar);
            cp.append( ((File)(_elements.elementAt(i))).getPath() );
        }
        return cp.toString();
    }
    
    public ClassLoader getClassLoader() {
        int cnt = _elements.size();
        URL[] urls = new URL[cnt];
        for (int i=0; i < cnt; i++) {
            try {
            	String u=((File)(_elements.elementAt(i))).toURL().toString();
            	u=encodeFileURL(u);
                urls[i] = new URL(u);
               
            } catch (MalformedURLException e) {}
        }
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        if (parent == null) {
            parent = Classpath.class.getClassLoader();
        }
        if (parent == null) {
            parent = ClassLoader.getSystemClassLoader();
        }
        return new Loader(urls, parent);
    }

    private class Loader extends URLClassLoader
    {
        String name;
        
        Loader(URL[] urls, ClassLoader parent)
        {
            super(urls, parent);
            name = "StartLoader"+Arrays.asList(urls);
        }

        public String toString()
        {
            return name;
        }
    }
    
    
    public static String encodeFileURL(String path)
    {
		byte[] bytes;
    	try 
    	{ 
    		bytes=path.getBytes("utf-8");
		} 
    	catch (UnsupportedEncodingException e) 
    	{
    		bytes=path.getBytes();
		}
    	
    	StringBuffer buf = new StringBuffer(bytes.length*2);
        buf.append("file:");
        
        synchronized(buf)
        {
            for (int i=5;i<bytes.length;i++)
            {
                byte b=bytes[i]; 
                switch(b)
                {
                  case '%':
                      buf.append("%25");
                      continue;
                  case ' ':
                      buf.append("%20");
                      continue;
                  case '/':
                  case '.':
                  case '-':
                  case '_':
                      buf.append((char)b);
                      continue;
                  default:
                      // let's be over conservative here!
                      if (Character.isJavaIdentifierPart((char)b))
                      {
                          if(b>='a' && b<='z' || b>='A' && b<='Z' || b>='0' && b<='9')
                          {
                              buf.append((char)b);
                              continue;
                          }
                      }
                      buf.append('%');
                      buf.append(Integer.toHexString((0xf0&(int)b)>>4));
                      buf.append(Integer.toHexString((0x0f&(int)b)));
                      continue;
                }
            }
        }

        return buf.toString();
    }
}
