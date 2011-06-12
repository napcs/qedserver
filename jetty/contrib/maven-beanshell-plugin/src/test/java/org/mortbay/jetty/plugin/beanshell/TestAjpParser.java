package org.mortbay.jetty.plugin.beanshell;

import junit.framework.TestCase;

import java.util.TreeMap;


public class TestAjpParser extends TestCase
{

    public void testBeanShellRun() throws Exception
    {
        TreeMap treeMap = new TreeMap();
        treeMap.put("param1", "The Quick Brown Fox Jumps Over To The Lazy Dog");
        treeMap.put("param2", "thequickbrownfoxjumpsovertothelazydog");


        StringBuffer script = new StringBuffer();


        script.append("\nString param1=(String) params.get(\"param1\");");
        script.append("\nString param2=(String) params.get(\"param2\");");
        script.append("\nSystem.out.println(\"\\nparam1 = \" + param1);");
        script.append("\nSystem.out.println(\"\\nparam2 = \" + param2);");
        script.append("\nSystem.out.println(\"\\n\\n\");");

        script.append("\nif(!\"The Quick Brown Fox Jumps Over To The Lazy Dog\".equals(param1)) " +
                "\nthrow new Exception(\"param1 is not correct\");");
        script.append("\nif(!\"thequickbrownfoxjumpsovertothelazydog\".equals(param2)) " +
                "\nthrow new Exception(\"param2 is not correct\");");


       

        BeanShellRunMojo mojo = new BeanShellRunMojo();
        mojo.setScript(script.toString());
        mojo.setParams(treeMap);
        
        try
        {
            mojo.execute();
            System.out.println("BeanShellTest: Ok");
            assertTrue(true);
        }
        catch(Exception e)
        {
            System.out.println("BeanShellTest: Error on Bean Shell Execution");
            assertTrue(false);

        }



    }
    


}
