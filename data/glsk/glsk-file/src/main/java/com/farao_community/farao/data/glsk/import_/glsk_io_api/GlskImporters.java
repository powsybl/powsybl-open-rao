/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.import_.glsk_io_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.glsk.import_.glsk_provider.Glsk;
import com.google.common.base.Suppliers;
import com.powsybl.commons.util.ServiceLoaderCache;
import org.joda.time.DateTime;

import java.io.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class GlskImporters {

    private static final Supplier<List<GlskImporter>> GLSK_IMPORTERS
        = Suppliers.memoize(() -> new ServiceLoaderCache<>(GlskImporter.class).getServices())::get;

    private GlskImporters() {
    }

    public static Glsk importGlsk(Path glskPath) {
        return importGlsk(glskPath, Optional.empty());
    }

    public static Glsk importGlsk(Path glskPath, Instant instant) {
        return importGlsk(glskPath, convertToDatetime(instant));
    }

    public static Glsk importGlsk(Path glskPath, Optional<DateTime> timeStampFilter) {
        try (InputStream is = new FileInputStream(glskPath.toFile())) {
            return importGlsk(glskPath.getFileName().toString(), is, timeStampFilter);
        } catch (FileNotFoundException e) {
            throw new FaraoException("File not found.");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        org.apache.commons.io.IOUtils.copy(inputStream, baos);
        return baos.toByteArray();
    }

    public static Glsk importGlsk(String fileName, InputStream inputStream) {
        return importGlsk(fileName, inputStream, Optional.empty());
    }

    public static Glsk importGlsk(String fileName, InputStream inputStream, Instant instant) {
        return importGlsk(fileName, inputStream, convertToDatetime(instant));
    }

    public static Glsk importGlsk(String fileName, InputStream inputStream, Optional<DateTime> timeStampFilter) {
        try {
            byte[] bytes = getBytesFromInputStream(inputStream);

            GlskImporter importer = findImporter(fileName, new ByteArrayInputStream(bytes));
            if (importer == null) {
                throw new FaraoException("No importer found for this file");
            }
            return importer.importGlsk(new ByteArrayInputStream(bytes), timeStampFilter);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static GlskImporter findImporter(String fileName, InputStream inputStream) {
        try {
            byte[] bytes = getBytesFromInputStream(inputStream);

            for (GlskImporter importer : GLSK_IMPORTERS.get()) {
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                if (importer.exists(fileName, bais)) {
                    return importer;
                }
            }
            return null;

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Optional<DateTime> convertToDatetime(Instant instant) {
        if (instant == null) {
            return Optional.empty();
        } else {
            org.joda.time.Instant jodaInstant = new org.joda.time.Instant(instant.toEpochMilli());
            return Optional.of(jodaInstant.toDateTime());
        }
    }
}
