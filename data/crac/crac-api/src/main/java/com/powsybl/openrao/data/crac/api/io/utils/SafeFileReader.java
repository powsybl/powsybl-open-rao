
package com.powsybl.openrao.data.crac.api.io.utils;

import com.google.common.io.CountingInputStream;
import com.google.common.io.CountingOutputStream;
import com.powsybl.openrao.commons.OpenRaoException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for safely reading files:
 * - Efficient for large files, does not load the entire file into memory.
 * - Streams and underlying file are automatically closed.
 * - Provides fresh InputStreams starting at the beginning for each operation.
 * - Supports multiple read operations on the same file.
 *
 * @author Georg Haider {@literal <georg.haider at artelys.com>}
 *
 */

public class SafeFileReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SafeFileReader.class);

    private final Path file;
    private final BufferSize bufferSize;

    protected SafeFileReader(Path f, BufferSize bufferSize) {
        this.file = f;
        this.bufferSize = bufferSize;
        LOGGER.debug("Created. File={}. Size={}. BufferParam={}",  file, IOUtils.humanReadableBytes(IOUtils.getSafeFileSize(f)), bufferSize);
    }

    public static SafeFileReader create(File f, BufferSize bufferSize) throws OpenRaoException {
        return create(f.toPath(), bufferSize);
    }

    public static SafeFileReader create(Path f, BufferSize bufferSize) throws OpenRaoException {
        return new SafeFileReader(f, bufferSize);
    }

    public static class RunException extends RuntimeException {
        public RunException(Exception e) {
            super(e);
        }
    }

    public <T> T withReadStream(ThrowingFunctions.Runner<InputStream, T> runn)
        throws RunException {

        InputStream is = null;
        CountingInputStream cis = null;
        try {
            long start = System.currentTimeMillis();

            try {
                is = getReadStream();
                cis = new CountingInputStream(is);
            } catch (IOException e) {
                throw new RuntimeException("Error opening read stream: " + e.getMessage(), e);
            }

            T ret;
            try {
                ret = runn.run(cis);
            } catch (Exception e) {
                // callers should never let this happen
                throw new RunException(e);
            }
            LOGGER.debug("Read done. Read={}. Time={}", IOUtils.humanReadableBytes(cis.getCount()), System.currentTimeMillis() - start);
            return ret;

        } finally {
            IOUtils.safeClose(cis, is);
        }
    }

    private InputStream getReadStream() throws IOException {
        return bufferSize.apply(Files.newInputStream(file));
    }

    public void withWriteStream(ThrowingFunctions.VoidRunner<OutputStream> runn)
        throws RunException {

        OutputStream os = null;
        CountingOutputStream cos = null;
        try {
            long start = System.currentTimeMillis();

            try {
                os = getWriteStream();
                cos = new CountingOutputStream(os);
            } catch (IOException e) {
                throw new RuntimeException("Error opening write stream: " + e.getMessage(), e);
            }

            try {
                runn.run(cos);
            } catch (Exception e) {
                // callers should never let this happen
                throw new RunException(e);
            }
            LOGGER.debug("Write done. Written={}. Time={}", IOUtils.humanReadableBytes(cos.getCount()), System.currentTimeMillis() - start);

        } finally {
            IOUtils.safeClose(cos, os);
        }
    }

    private OutputStream getWriteStream() throws IOException {
        return bufferSize.apply(Files.newOutputStream(file));
    }

    public String getFileName() {
        return file.getFileName().toString();
    }

    public boolean hasFileExtension(String extension) {
        return IOUtils.hasExtension(getFileName(), extension);
    }

}

