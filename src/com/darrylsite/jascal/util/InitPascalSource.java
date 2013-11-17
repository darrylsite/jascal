package com.darrylsite.jascal.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Darryl Kpizingui
 */
public class InitPascalSource
{
    private static final String HEADER_PROGRAM = "{$program_name$}";
    private static final String HEADER_PRG = "prg";
    private static String HEADER_CODE;

    static
    {
        try
        {
            HEADER_CODE = getScriptFile();
        } catch (IOException ex)
        {
            Logger.getLogger(InitPascalSource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String getHeader()
    {
        return HEADER_CODE.replace(HEADER_PROGRAM, HEADER_PRG + UUID.randomUUID().toString().replace(StringPool.DASH, StringPool.BLANK));
    }

    public static String getScriptFile() throws IOException
    {
        InputStream in = InitPascalSource.class.getResourceAsStream("/com/darrylsite/jascal/res/header.jpas");
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        StringBuilder scriptSource = new StringBuilder();
        String line;
        final String newLine = "\n";
        while ((line = input.readLine()) != null)
        {
            scriptSource.append(line).append(newLine);
        }

        return scriptSource.toString();
    }
}
