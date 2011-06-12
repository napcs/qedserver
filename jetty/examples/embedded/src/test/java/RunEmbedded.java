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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashMap;

import junit.framework.TestCase;

public class RunEmbedded
{
    private static HashMap argMap;
    
    private static String[] stringArrayType = new String[0];
    
    private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    static 
    {
        argMap = new HashMap();
        argMap.put ("FileServer", new String[]{"port", "resourceBase"});
    }
    
    
    private String askWhichOneToRun()
    throws Exception
    {
        System.err.println("Embedded examples to choose from: ");
        File classesDir = new File ("./target/classes/org/mortbay/jetty/example");
        String[] classNames = classesDir.list();
        for (int i=0; i<classNames.length; i++)
        {
            String className = classNames[i].substring(0, classNames[i].length()-6);
            if (className.indexOf('$') < 0)
            {
                if (!className.equals("LikeJettyXml"))
                System.err.println("\t"+className);
            }
        }
        System.err.println("The example LikeJettyXml can only be run from $(jetty.home), so try it using your IDE instead.\n\n");
        System.err.print("Enter the name (case sensitive) > ");
        return reader.readLine();
    }
    
    public void testEmbedded ()
    throws Exception
    {
        String whichOneToRun = askWhichOneToRun();
        
        String className = "org.mortbay.jetty.example."+ whichOneToRun;
        
        Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
        Object o = clazz.newInstance();
        
        Method main = clazz.getMethod("main", new Class[]{stringArrayType.getClass()});
        
        String[] prompts = (String[])argMap.get(whichOneToRun);
        String[] args = new String[0];
        if (prompts != null)
        {
            args = new String[prompts.length];
            
            for (int i=0; i<prompts.length; i++)
            {
                System.err.print("Enter arg "+i+": "+prompts[i]+" > ");
                args[i]=reader.readLine();
            }
        }
        Object[] methodArgs = new Object[1];
        methodArgs[0]=args;
        main.invoke(null, methodArgs);
    }
    
    public static final void main (String[] args)
    {
        try
        {
        RunEmbedded runner = new RunEmbedded();
        runner.testEmbedded();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(2);
        }
    }
}
