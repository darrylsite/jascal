package com.darrylsite.jascal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import com.darrylsite.jascal.fpc.FreePascalCompiler;
import com.darrylsite.jascal.util.InitPascalSource;
import com.darrylsite.jascal.util.StringPool;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * 
 * @author Darryl Kpizingui
 */
public final class JascalEngine implements ScriptEngine, Compilable
{
    private static final String __ENGINE_VERSION__ = "V0.0";
    private static final String MY_NAME = "Jascal";
    private static final String MY_SHORT_NAME = "Jascal";
    private static final String STR_JASCAL_LANGUAGE = "Jascal";
    private static final String STR_PREFIX_FILE = "jascal";
    private static final String STR_EXT = ".jpas";
    private static final ScriptEngineFactory myFactory = new JascalEngineFactory();
    private ScriptContext defaultContext;
    private final String FPC_LANGUAGE_VERSION = "Free Pascal Compiler version 2.7.1 [2013/09/06] for jvm";

    public JascalEngine()
    {
        defaultContext = new SimpleScriptContext();

        put(LANGUAGE_VERSION, FPC_LANGUAGE_VERSION);
        put(LANGUAGE, STR_JASCAL_LANGUAGE);
        put(ENGINE, MY_NAME);
        put(ENGINE_VERSION, __ENGINE_VERSION__);
        put(NAME, MY_SHORT_NAME);

        put(ARGV, null);
        put(FILENAME, null);
    }

    @Override
    public Object eval(String script) throws ScriptException
    {
        return eval(script, getContext());
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException
    {
        CompiledScript compiledScript = compile(script);
        return compiledScript.eval(context);
    }

    @Override
    public Object eval(String script, Bindings bindings) throws ScriptException
    {
        Bindings current = getContext().getBindings(ScriptContext.ENGINE_SCOPE);
        getContext().setBindings(bindings, ScriptContext.ENGINE_SCOPE);

        Object result = eval(script);

        getContext().setBindings(current, ScriptContext.ENGINE_SCOPE);
        return result;
    }

    @Override
    public Object eval(Reader reader) throws ScriptException
    {
        return eval(getScriptFromReader(reader));
    }

    @Override
    public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException
    {
        return eval(getScriptFromReader(reader), scriptContext);
    }

    @Override
    public Object eval(Reader reader, Bindings bindings) throws ScriptException
    {
        return eval(getScriptFromReader(reader), bindings);
    }

    @Override
    public void put(String key, Object value)
    {
        getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
    }

    @Override
    public Object get(String key)
    {
        return getBindings(ScriptContext.ENGINE_SCOPE).get(key);
    }

    @Override
    public Bindings getBindings(int scope)
    {
        return getContext().getBindings(scope);
    }

    @Override
    public void setBindings(Bindings bindings, int scope)
    {
        getContext().setBindings(bindings, scope);
    }

    @Override
    public Bindings createBindings()
    {
        return new SimpleBindings();
    }

    @Override
    public ScriptContext getContext()
    {
        return defaultContext;
    }

    @Override
    public void setContext(ScriptContext context)
    {
        defaultContext = context;
    }

    @Override
    public ScriptEngineFactory getFactory()
    {
        return myFactory;
    }

    /**
     * private methods
     */
    private static String getScriptFromReader(Reader reader)
    {
        try
        {
            StringWriter script = new StringWriter();
            int data;
            while ((data = reader.read()) != -1)
            {
                script.write(data);
            }
            script.flush();
            return script.toString();
        } catch (IOException ex)
        {
        }
        return null;
    }

    /**
     * Compile the Script using FreePascal for JVM
     */
    @Override
    public CompiledScript compile(String script) throws ScriptException
    {
        script = transformScript(script);

        File file = null;
        try
        {
            file = File.createTempFile(STR_PREFIX_FILE, STR_EXT);
            FileWriter writer = new FileWriter(file);
            writer.write(script);
            writer.close();

            //Compilation
            FreePascalCompiler.CreateCompiler();
            FreePascalCompiler.writeConfigFile();
            String compiledClass = FreePascalCompiler.compile(file.getAbsolutePath(), getContext().getWriter());

            JascalCompiledScript compiledScript = new JascalCompiledScript(compiledClass);
            compiledScript.setScriptEngine(this);

            return compiledScript;
        } catch (Exception e)
        {
            throw new ScriptException(e);
        } finally
        {
            if (file != null)
            {
                file.delete();
            }
        }
    }

    @Override
    public CompiledScript compile(Reader scriptReader) throws ScriptException
    {
        return compile(getScriptFromReader(scriptReader));
    }

    /**
     * not needed anymore
     * bindings are set by reflections
     * @param script
     * @return 
     */
    private String setFPCBindings(String script)
    {
        Bindings engineBindings = getContext().getBindings(ScriptContext.ENGINE_SCOPE);
        Bindings globalBindings = getContext().getBindings(ScriptContext.GLOBAL_SCOPE);

        StringBuilder varNames = new StringBuilder();
        for (String key : engineBindings.keySet())
        {
            if (!key.matches("\\w+[\\w\\d_]")) //Not a valid variable name
            {
                continue;
            }
            varNames.append(key).append(StringPool.COMA);
        }

        if (globalBindings != null)
        {
            for (String key : globalBindings.keySet())
            {
                if (!key.matches("\\w+[\\w\\d_]")) //Not a valid variable name
                {
                    continue;
                }

                varNames.append(key).append(StringPool.COMA);
            }
        }

        String vars = varNames.toString();
        if (vars.endsWith(StringPool.COMA)) // should be if var is not nul
        {
            vars = vars.substring(0, vars.length()-1) + ":JLObject;";
        }

        if (vars.isEmpty())
        {
            return script;
        } else
        {
            return script.replace("{$binding_vars$}", vars);
        }

    }

    private String transformScript(String script)
    {
        StringReader reader = new StringReader(script);
        BufferedReader bf = new BufferedReader(reader);
        StringBuilder builder = new StringBuilder();

        boolean replaceHeader = false;
        String headerCode = InitPascalSource.getHeader();

        String line;
        try
        {
            while ((line = bf.readLine()) != null)
            {
                if (!replaceHeader)
                {
                    replaceHeader = true;
                    line = line.replaceAll("([pP][Rr][Oo][Gg][Gr][Aa][Mm]\\s+\\w+;)", Matcher.quoteReplacement(headerCode));
                }

                line = line.replaceAll("\\w*[wW][rR][iI][tT][eE]\\s*;", "programStream.write\\(''\\);");
                line = line.replaceAll("\\w*[wW][rR][iI][tT][eE][lL][nN]\\s*;", "programStream.writeln\\(''\\);");
                line = line.replaceAll("\\w*[wW][rR][iI][tT][eE]\\s*\\(", "programStream.write\\(");
                line = line.replaceAll("\\w*[wW][rR][iI][tT][eE][lL][nN]\\s*\\(", "programStream.writeln\\(");

                builder.append(line).append(StringPool.NEW_LINE);
            }

            /**
             * No header found
             */
            if (!replaceHeader)
            {
                builder.insert(0, headerCode);
            }
        } catch (IOException ex)
        {
            Logger.getLogger(JascalEngine.class.getName()).severe(ex.getMessage());
        }

        return builder.toString();
    }
}