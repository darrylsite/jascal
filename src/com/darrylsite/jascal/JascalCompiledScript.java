package com.darrylsite.jascal;

import java.io.IOException;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.darrylsite.jascal.fpc.FreePascalCompiler;

/**
 * 
 * @author Darryl Kpizingui
 */
public class JascalCompiledScript extends CompiledScript
{
	private String classFilePath;
	private String className;
	private ScriptEngine scriptEngine;
	private final String MAIN_METHOD = "main";
	
	public JascalCompiledScript(String classFilePath) throws IOException
	{
		super();
		this.classFilePath = classFilePath;
		this.className = FreePascalCompiler.loadClass(classFilePath);
	}

	@Override
	public Object eval(ScriptContext context) throws ScriptException
	{
		String[] newArgs = (String[]) context.getBindings(ScriptContext.ENGINE_SCOPE).get(ScriptEngine.ARGV);
		if(newArgs == null)
                {
			newArgs = new String[0];
                }
		
		try
		{
			FreePascalCompiler.runMethod(className, MAIN_METHOD, context, newArgs);
		} 
		catch (Exception e)
		{
			throw new ScriptException(e);
		}
		
		return null;
	}

	@Override
	public ScriptEngine getEngine()
	{
		return scriptEngine;
	}
	
	public void setScriptEngine(ScriptEngine scriptEngine)
	{
		this.scriptEngine = scriptEngine;
	}
}