
package com.powsybl.openrao.data.crac.api.io.utils;

import com.google.common.io.CountingInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Utility for safely creating temporary files
 *
 * @author Georg Haider {@literal <georg.haider at artelys.com>}
 *
 */

public class TmpFile implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmpFile.class);

    private static final FileAttribute<Set<PosixFilePermission>> FILE_ATTRS = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));

    private final Path tempFile;
    private final BufferSize buffer;

    protected TmpFile(File tempFile, BufferSize buffer) {
        this.tempFile = tempFile.toPath();
        this.buffer = buffer;
    }

    public Path getTempFile() {
        return tempFile;
    }

    public static TmpFile create(String suffix, BufferSize buffer) throws IOException {
        File tempFile;
        boolean tempFileOk;
        if (SystemUtils.IS_OS_UNIX) {
            tempFile = Files.createTempFile("openRao", suffix, FILE_ATTRS).toFile();
            tempFileOk = true;
        } else {
            tempFile = Files.createTempFile("openRao", suffix).toFile();
            tempFileOk = tempFile.setReadable(true, true) &&
                tempFile.setWritable(true, true) &&
                tempFile.setExecutable(true, true);
        }
        tempFile.deleteOnExit();
        if (!tempFileOk) {
            throw new IOException("Error creating permissions on file: " + tempFile.getAbsolutePath());
        }
        return new TmpFile(tempFile, buffer);
    }

    public static TmpFile create(String suffix, File inputData, BufferSize buffer) throws IOException {
        try (var is = buffer.apply(Files.newInputStream(inputData.toPath()))) {
            return create(suffix, is, buffer);
        }
    }

    /**
     * Note: InputStream will be closed
     */
    public static TmpFile create(String suffix, InputStream inputData, BufferSize buffer) throws IOException {
        var tmp = create(suffix, buffer);
        tmp.loadInputStream(inputData);
        return tmp;
    }

    /**
     * Note we dont apply buffer for reading input here
     */
    protected void loadInputStream(InputStream inputStream) throws IOException {
        try (var cis = new CountingInputStream(inputStream);
            var out = this.buffer.apply(Files.newOutputStream(tempFile))) {
            long start = System.currentTimeMillis();
            cis.transferTo(out);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Loaded. Read={}. Time={}", IOUtils.humanReadableBytes(cis.getCount()),
                    System.currentTimeMillis() - start);
            }
        }
    }

    @Override
    public void close() {
        try {
            Files.delete(this.tempFile);
        } catch (Exception e) {
            // ignore
        }
    }

    public <T> T withReadStream(ThrowingFunctions.Runner<InputStream, T> runn) {
        return SafeFileReader.create(tempFile, buffer).withReadStream(runn);
    }

    public void withWriteStream(ThrowingFunctions.VoidRunner<OutputStream> runn) {
        SafeFileReader.create(tempFile, buffer).withWriteStream(runn);
    }

    public String getFileSize() {
        var size = IOUtils.getSafeFileSize(this.tempFile);
        return  size + " bytes (" + IOUtils.humanReadableBytes(size) + ")";
    }

}
