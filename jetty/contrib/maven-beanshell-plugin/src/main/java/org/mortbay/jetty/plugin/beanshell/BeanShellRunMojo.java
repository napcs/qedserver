package org.mortbay.jetty.plugin.beanshell;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import bsh.Interpreter;
import bsh.EvalError;


import java.util.TreeMap;

/**
 * Used to run scripts in maven
 * Scripts: Beanshell and etc..
 *
 * @author Leopoldo Lee Agdeppa III
 * @goal run
 * @requiresDependencyResolution runtime
 * @execute phase="test-compile"
 * @description Runs jetty6 directly from a maven project
 */
public class BeanShellRunMojo extends AbstractMojo
{

    /**
     * BeanShell Script
     *
     * @parameter
     * @required
     */
    private String script;

    /**
     * Script parameters params.get("param_name");
     *
     * @parameter
     */
    private TreeMap params;


    public void execute() throws MojoExecutionException, MojoFailureException
    {
        Interpreter i = new Interpreter();
        try
        {
            i.set("params", params);
            i.eval(script);
        }
        catch (EvalError evalError)
        {
            evalError.printStackTrace();
            throw new MojoExecutionException(evalError.getMessage());

        }


    }


    public String getScript()
    {
        return script;
    }

    public void setScript(String script)
    {
        this.script = script;
    }

    public TreeMap getParams()
    {
        return params;
    }

    public void setParams(TreeMap params)
    {
        this.params = params;
    }
}
