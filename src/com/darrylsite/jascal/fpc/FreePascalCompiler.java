package com.darrylsite.jascal.fpc;

import com.darrylsite.jascal.util.StringKeys;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import com.darrylsite.jascal.util.Utils;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;

/**
 * 
 * @author Darryl Kpizingui
 */
public class FreePascalCompiler
{
    private static String COMPILER_ROOT_PATH = null;
    private static String COMPILER_UNIT_PATH = null;
    private static String COMPILER_PATH = null;
    private static String ROOT_TMP_FOLDER = null;
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String FPC_PATH;
    private static final long MAX_PROCESS_RUNNING_TIME = 120 * 1000;
    private static final String compilerExec = "ppcjvm";
    private static final String INVALID_VARIABLE_NAME_REGEX = "\\w+[\\w\\d_]";
    private static final String[] WINDOWS_COMMAND_INTERPRETER =
    {
        "cmd", "/C"
    };
    private static final Class[] classLoaderParams = new Class[]
    {
        URL.class
    };

    static
    {
        if (isWindows())
        {
            FPC_PATH = StringKeys.FPC_WIN_PATH;
        } else
        {
            FPC_PATH = StringKeys.FPC_LINUX_PATH;
        }
    }

    public static boolean isWindows()
    {
        return (OS.indexOf("win") >= 0);
    }

    public static boolean isUnix()
    {
        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
    }

    public static void CreateCompiler() throws IOException
    {
        //Get the temporaty folder
        if (ROOT_TMP_FOLDER == null)
        {
            File tmpFile = File.createTempFile("fpc", ".tmp");
            File rootTmpFolder = tmpFile.getParentFile();
            tmpFile.delete();
            ROOT_TMP_FOLDER = rootTmpFolder.getAbsolutePath();
        }

        File fpcFolder = new File(ROOT_TMP_FOLDER + File.separator + "fpc");
        COMPILER_ROOT_PATH = fpcFolder.getAbsolutePath();

        COMPILER_PATH = COMPILER_ROOT_PATH + File.separator + "compiler";

        if (!fpcFolder.exists())
        {
            fpcFolder.mkdirs();
        } else
        {
            //The compiler already exist.
            //I do not check for files modification
            //To speed up the compilation
            return;
        }

        // copie the Compiler to the new created folder
        InputStream stream = (new FreePascalCompiler()).getClass().getResourceAsStream(FPC_PATH);

        if (stream == null)
        {
            throw new IOException("Cannot get stream");
        }

        Utils.unZip(stream, COMPILER_PATH);
    }

    public static void writeConfigFile() throws IOException
    {
        String configFileFolderName = isWindows() ? "bin" : "etc";

        File configFile = new File(COMPILER_PATH + File.separator + configFileFolderName + File.separator + "fpc.cfg");

        StringBuilder config = new StringBuilder();
        COMPILER_UNIT_PATH = null;

        // first line
        config.append(COMPILER_PATH);
        config.append(File.separator);
        config.append("units");
        config.append(File.separator);
        config.append("jvm-java");
        config.append(File.separator);
        config.append("rtl");

        COMPILER_UNIT_PATH = config.toString();

        if (configFile.exists())
        {
            //Already set
            return;
        }

        config.insert(0, "-Fu");
        // other lines
        config.append("\n-l");
        config.append("\n-O2");
        config.append("\n-g");

        configFile.createNewFile();
        FileWriter writer = new FileWriter(configFile);
        writer.write(config.toString());
        writer.close();
    }

    public static String compile(String sourceFilePath, Writer writer) throws IOException
    {
        String outPut = startProcess(new File(sourceFilePath));
        writer.write(outPut);
        return getCompiledClass(outPut);
    }

    /**
     * Analyze the output
     *
     * @param outPut
     * @return
     * @throws IOException
     */
    private static String getCompiledClass(String outPut) throws IOException
    {
        BufferedReader reader = new BufferedReader(new StringReader(outPut));
        String line;
        final String classStartingLine = "Generated: ";

        while ((line = reader.readLine()) != null)
        {
            if (!line.startsWith(classStartingLine))
            {
                continue;
            }

            return line.substring(classStartingLine.length());
        }

        return null;
    }

    private static String startProcess(File sourceFile) throws IOException
    {
        String[] interpreter;
        String[] comm;

        if (isWindows())
        {
            interpreter = WINDOWS_COMMAND_INTERPRETER;
            comm = new String[4];
            comm[0] = interpreter[0];
            comm[1] = interpreter[1];
            comm[2] = compilerExec;
            comm[3] = sourceFile.getAbsolutePath();
        } else
        {
            comm = new String[3];
            comm[0] = "./" + compilerExec;
            comm[1] = "-v";
            comm[2] = sourceFile.getAbsolutePath();

            File execFile = new File(COMPILER_PATH + File.separator + "bin" + File.separator + compilerExec);
            execFile.setExecutable(true);

            String[] setPerm = new String[]
            {
                "sh", "chmod", "777", execFile.getAbsolutePath()
            };
            Runtime.getRuntime().exec(setPerm, null, execFile.getParentFile().getAbsoluteFile());
        }

        StringBuilder outPutBuilder = new StringBuilder();
        long start = System.currentTimeMillis();

        try
        {
            Process ls_proc = Runtime.getRuntime().exec(comm, null, new File(COMPILER_PATH + File.separator + "bin"));
            BufferedInputStream ls_in = new BufferedInputStream(ls_proc.getInputStream());
            BufferedInputStream ls_err = new BufferedInputStream(ls_proc.getErrorStream());
            boolean end = false;

            while (!end)
            {
                int c = 0;
                while ((ls_err.available() > 0) && (++c <= 1000))
                {
                    char car = (char) (ls_err.read());
                    outPutBuilder.append(car);
                }

                c = 0;

                while ((ls_in.available() > 0) && (++c <= 1000))
                {
                    char car = (char) (ls_in.read());
                    outPutBuilder.append(car);
                }

                try
                {
                    ls_proc.exitValue();
                    while (ls_err.available() > 0)
                    {
                        outPutBuilder.append((char) (ls_err.read()));
                    }

                    while (ls_in.available() > 0)
                    {
                        char car = (char) (ls_in.read());
                        outPutBuilder.append(car);
                    }

                    end = true;
                } catch (IllegalThreadStateException e)
                {
                    //Logger.getLogger(FreePascalCompiler.class.getName()).log(Level.SEVERE, e.getMessage(), e);
                }

                if ((System.currentTimeMillis() - start) > MAX_PROCESS_RUNNING_TIME)
                {
                    ls_proc.destroy();
                    end = true;
                }
            }
        } catch (IOException e)
        {
            Logger.getLogger(FreePascalCompiler.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }

        return outPutBuilder.toString();
    }

    /**
     * Load the compiled class in the ClassLoader
     *
     * @param compiledClass
     * @param methodName
     * @param params
     * @return the Class name
     * @throws IOException
     */
    public static String loadClass(String compiledClass) throws IOException
    {
        URLClassLoader sysloader = (URLClassLoader) FreePascalCompiler.class.getClassLoader();
        Class<URLClassLoader> sysclass = URLClassLoader.class;

        try
        {
            File classFile = new File(compiledClass);
            final String classLoaderMethodName = "addURL";
            Method method = sysclass.getDeclaredMethod(classLoaderMethodName, classLoaderParams);
            method.setAccessible(true);

            method.invoke(sysloader, new Object[]
            {
                new File(COMPILER_ROOT_PATH).getParentFile().toURI().toURL()
            });

            method.invoke(sysloader, new Object[]
            {
                new File(COMPILER_UNIT_PATH).toURI().toURL()
            });

            String classFolder = new File(FreePascalCompiler.COMPILER_ROOT_PATH).getParentFile().getAbsolutePath();

            return classFile.getAbsolutePath().substring(classFolder.length() + 1, classFile.getAbsolutePath().lastIndexOf(".class")).replace(File.separator, ".");

        } catch (Throwable t)
        {
            throw new IOException("Error, could not add URL to system classloader :: ");
        }
    }

    public static Object runMethod(String className, String methodName, ScriptContext context, String... args) throws ClassNotFoundException, SecurityException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        ClassLoader classLoader = FreePascalCompiler.class.getClassLoader();
        Class classToLoad = classLoader.loadClass(className);

        //init
        Method methodInit = classToLoad.getDeclaredMethod("initProgram", args.getClass());
        methodInit.setAccessible(true);
        methodInit.invoke(null, (Object) args);
        //Set the writer by refection
        //May not work if a security manager is enabled
        Method methodSetWriter = classToLoad.getDeclaredMethod("setOutPut", Writer.class);
        methodSetWriter.setAccessible(true);
        methodSetWriter.invoke(null, context.getWriter());
        //define bindings
        setBindings(context, classToLoad);

        //Let's run the script now
        Method method = classToLoad.getDeclaredMethod(methodName, args.getClass());
        Object scriptOutput = method.invoke(null, (Object) args);

        getBindings(context, classToLoad);

        return scriptOutput;
    }

    private static void setBindings(ScriptContext context, Class classToLoad)
    {
        Bindings allBindings = new SimpleBindings();
        Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        Bindings globalBindings = context.getBindings(ScriptContext.GLOBAL_SCOPE);

        if (globalBindings != null)
        {
            for (String key : globalBindings.keySet())
            {
                allBindings.put(key, globalBindings.get(key));
            }
        }

        if (bindings != null) //override the global is necessary
        {
            for (String key : bindings.keySet())
            {
                allBindings.put(key, bindings.get(key));
            }
        }

        for (String key : allBindings.keySet())
        {
            if (!key.matches(INVALID_VARIABLE_NAME_REGEX)) //Not a valid variable name
            {
                continue;
            }

            try
            {
                Field field = classToLoad.getDeclaredField(key);
                field.setAccessible(true);
                field.set(null, allBindings.get(key));

            } catch (NoSuchFieldException ex)
            {
                Logger.getLogger(FreePascalCompiler.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex)
            {
                Logger.getLogger(FreePascalCompiler.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex)
            {
                Logger.getLogger(FreePascalCompiler.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex)
            {
                Logger.getLogger(FreePascalCompiler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    /**
     * Set binding values from the script
     * @param context
     * @param classToLoad 
     */
    private static void getBindings(ScriptContext context, Class classToLoad)
    {
        Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);

        for (String key : bindings.keySet())
        {
            if (!key.matches(INVALID_VARIABLE_NAME_REGEX))
            {
                continue;
            }

            try
            {
                Field field = classToLoad.getDeclaredField(key);
                field.setAccessible(true);
                bindings.put(key, field.get(key));

            } catch (NoSuchFieldException ex)
            {
                Logger.getLogger(FreePascalCompiler.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex)
            {
                Logger.getLogger(FreePascalCompiler.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex)
            {
                Logger.getLogger(FreePascalCompiler.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex)
            {
                Logger.getLogger(FreePascalCompiler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
