package org.barbon.mangaget;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.text.DecimalFormat;

import java.util.List;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CBZFile {
    public static void createFile(String fullPath, List<String> pages) {
        ZipOutputStream stream = null;

        try {
            // create parent directory if needed
            File basePath = new File(fullPath).getParentFile();

            if (!basePath.exists())
                basePath.mkdirs();

            // create CBZ file
            FileOutputStream file = new FileOutputStream(fullPath);

            stream = new ZipOutputStream(file);

            doCreateFile(stream, pages);

            stream.close();
            stream = null;

            // cleanup pages
            for (String page : pages)
                new File(page).delete();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                if (stream != null)
                    stream.close();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void doCreateFile(ZipOutputStream stream,
                                     List<String> pages) throws IOException {
        DecimalFormat format = new DecimalFormat("*0####-");
        int index = 0;

        for (String page : pages) {
            index += 1;

            File path = new File(page);
            String name = format.format(index) + path.getName();
            ZipEntry entry = new ZipEntry(name);

            stream.putNextEntry(entry);

            final int BUFFER_SIZE = 1024;
            FileInputStream in = new FileInputStream(path);
            byte[] buffer = new byte[BUFFER_SIZE];

            for (;;) {
                int size = in.read(buffer);

                if (size == -1)
                    break;

                stream.write(buffer);
            }

            stream.closeEntry();
        }
    }
}