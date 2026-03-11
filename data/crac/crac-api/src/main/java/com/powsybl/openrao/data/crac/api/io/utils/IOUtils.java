
package com.powsybl.openrao.data.crac.api.io.utils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Utility for generic Input/Output operations
 *
 * @author Georg Haider {@literal <georg.haider at artelys.com>}
 *
 */

public final class IOUtils {

    private IOUtils() {
        //empty
    }

    public static void safeClose(Closeable... streams) {
        Arrays.stream(streams).forEach(IOUtils::safeClose);
    }

    public static void safeClose(Closeable is) {
        try {
            if (null != is) {
                is.close();
            }
        } catch (IOException e) {
            // empty
        }
    }

    public static void safeDelete(File... files) {
        Arrays.stream(files).forEach(IOUtils::safeDelete);
    }

    public static void safeDelete(File f) {
        try {
            if (null != f) {
                Files.delete(f.toPath());
            }
        } catch (IOException e) {
            // empty
        }
    }

    public static boolean hasExtension(String filename, String extension) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0) {
            return false;
        }
        return filename.substring(dotIndex + 1).equalsIgnoreCase(extension);
    }

    public static String humanReadableBytes(long bytes) {
        if (bytes < 1) {
            return "0";
        }
        if (bytes < 1024) {
            return bytes + "bytes";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public static long getSafeFileSize(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return -1;
        }
    }

}
