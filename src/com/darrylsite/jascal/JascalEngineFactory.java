package com.darrylsite.jascal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * 
 * @author Darryl Kpizingui
 */
public class JascalEngineFactory implements ScriptEngineFactory
{
    private static final String FILEEXT = ".jpas";
    private static final String[] MIMETYPES =
    {
        "text/plain",
        "text/Jascal",
        "application/Jascal"
    };
    private static final String[] NAMES =
    {
        "Jascal"
    };
    private ScriptEngine myScriptEngine;
    private List<String> extensions;
    private List<String> mimeTypes;
    private List<String> names;

    public JascalEngineFactory()
    {
        myScriptEngine = new JascalEngine();
        extensions = Collections.nCopies(1, FILEEXT);
        mimeTypes = Arrays.asList(MIMETYPES);
        names = Arrays.asList(NAMES);
    }

    @Override
    public String getEngineName()
    {
        return getScriptEngine().get(ScriptEngine.ENGINE).toString();
    }

    @Override
    public String getEngineVersion()
    {
        return getScriptEngine().get(ScriptEngine.ENGINE_VERSION).toString();
    }

    @Override
    public List<String> getExtensions()
    {
        return extensions;
    }

    @Override
    public List<String> getMimeTypes()
    {
        return mimeTypes;
    }

    @Override
    public List<String> getNames()
    {
        return names;
    }

    @Override
    public String getLanguageName()
    {
        return getScriptEngine().get(ScriptEngine.LANGUAGE).toString();
    }

    @Override
    public String getLanguageVersion()
    {
        return getScriptEngine().get(ScriptEngine.LANGUAGE_VERSION).toString();
    }

    @Override
    public Object getParameter(String key)
    {
        return getScriptEngine().get(key).toString();
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(obj).append(".").append(m).append("(");
        int len = args.length;
        for (int i = 0; i < len; i++)
        {
            if (i > 0)
            {
                sb.append(',');
            }
            sb.append(args[i]);
        }
        sb.append(")");

        return sb.toString();
    }

    @Override
    public String getOutputStatement(String toDisplay)
    {
        return "write(" + toDisplay + ")";
    }

    @Override
    public String getProgram(String... statements)
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("BEGIN");

        int len = statements.length;

        for (int i = 0; i < len; i++)
        {
            if (i > 0)
            {
                sb.append('\n');
            }

            sb.append(statements[i]);
        }
        
        sb.append("END");

        return sb.toString();
    }

    @Override
    public ScriptEngine getScriptEngine()
    {
        return myScriptEngine;
    }
}
