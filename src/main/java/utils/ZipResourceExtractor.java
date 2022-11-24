package utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipResourceExtractor {
    /**
     * Extract the contents of a zip resource file to a destination directory.
     * Will overwrite existing files.
     *
     * @param myClass     The class used to find the zipResource
     * @param zipResource Path to zip resource file
     * @param destDir     Path to existing destination directory
     */
    public static void extractZipResource(Class<?> myClass, String zipResource, Path destDir) throws IOException {
        if (myClass == null || zipResource == null || !zipResource.toLowerCase().endsWith(".zip") || !Files.isDirectory(destDir)) {
            throw new IllegalArgumentException("myClass=" + myClass + " zipResource=" + zipResource + " destDir=" + destDir);
        }

        try (InputStream raw = myClass.getResourceAsStream(zipResource);
             ZipInputStream zipInputStream = new ZipInputStream(raw)) {

            ZipEntry entry = zipInputStream.getNextEntry();
            File dest;
            while (entry != null) {
                dest = destDir.resolve(entry.getName()).toFile();

                if (entry.isDirectory()) {
                    // dest is a directory, re-create it if it is not already present
                    if (!dest.exists() && !dest.mkdirs()) {
                        throw new IOException("Unable to copy directory " + entry.getName() + " to " + dest.getAbsoluteFile());
                    }
                } else {
                    // dest is a file, write it
                    try (FileOutputStream fileOutputStream = new FileOutputStream(dest)) {
                        zipInputStream.transferTo(fileOutputStream);
                    }
                }
                entry = zipInputStream.getNextEntry();
            }
        }
    }
}
