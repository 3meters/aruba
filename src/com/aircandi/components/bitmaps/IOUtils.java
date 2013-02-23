package com.aircandi.components.bitmaps;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/** From libcore.io.IoUtils */
@SuppressWarnings("ucd")
class IoUtils {
    static void deleteContents(File dir) throws IOException {
        final File[] files = dir.listFiles();
        if (files == null) {
            throw new IllegalArgumentException("not a directory: " + dir);
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteContents(file);
            }
            if (!file.delete()) {
                throw new IOException("failed to delete file: " + file);
            }
        }
    }

    static void closeQuietly(/*Auto*/Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) { // $codepro.audit.disable emptyCatchClause
            }
        }
    }
}
