/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.badge;

import java.io.Writer;
import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
class WriterOutput {
    private final ByteArrayOutputStream outputStream;
    private final Writer writer;

    public WriterOutput(ByteArrayOutputStream outputStream, Writer writer) {
        this.outputStream = outputStream;
        this.writer = writer;
    }

    public ByteArrayOutputStream getOutputStream() {
        return outputStream;
    }

    public Writer getWriter() {
        return writer;
    }
    
}
