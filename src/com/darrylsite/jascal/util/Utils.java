package com.darrylsite.jascal.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 
 * @author Darryl Kpizingui
 */
public class Utils
{
    public static void unZip(String zipFile, String outputFolder)
    {
        try
        {
            unZip(new FileInputStream(zipFile), outputFolder);
        } 
        catch (FileNotFoundException ex)
        {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void unZip(InputStream zipStream, String outputFolder)
    {
        byte[] buffer = new byte[1024];

        try
        {
            File folder = new File(outputFolder);
            if (!folder.exists())
            {
                folder.mkdir();
            }

            ZipInputStream zis = new ZipInputStream(zipStream);
            ZipEntry zipEntry;

            while ((zipEntry = zis.getNextEntry()) != null)
            {
                String fileName = zipEntry.getName();

                File newFile = new File(outputFolder + File.separator + fileName);
                if (!zipEntry.isDirectory())
                {
                    new File(newFile.getParent()).mkdirs();
                } else
                {
                    new File(newFile.getAbsolutePath()).mkdirs();
                    continue;
                }

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0)
                {
                    fos.write(buffer, 0, len);
                }

                fos.close();
            }

            zis.closeEntry();
            zis.close();
        }
        catch (IOException ex)
        {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
