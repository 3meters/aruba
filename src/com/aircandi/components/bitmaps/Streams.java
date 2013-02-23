package com.aircandi.components.bitmaps;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

/** From libcore.io.Streams */
class Streams {
    static String readFully(Reader reader) throws IOException {
        try {
            final StringWriter writer = new StringWriter();
            final char[] buffer = new char[1024];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, count);
            }
            return writer.toString();
        } finally {
            reader.close();
        }
    }
}
