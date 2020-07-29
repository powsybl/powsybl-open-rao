/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.google.common.base.Suppliers;
import com.powsybl.commons.util.ServiceLoaderCache;
import org.joda.time.DateTime;

import java.io.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CracImporters {

    private static final Supplier<List<CracImporter>> CRAC_IMPORTERS
        = Suppliers.memoize(() -> new ServiceLoaderCache<>(CracImporter.class).getServices())::get;

    private CracImporters() {
    }

    public static Crac importCrac(Path cracPath) {
        try (InputStream is = new FileInputStream(cracPath.toFile())) {
            return importCrac(cracPath.getFileName().toString(), is);
        } catch (FileNotFoundException e) {
            throw new FaraoException("File not found.");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Crac importCrac(Path cracPath, Instant instant) {
        org.joda.time.Instant jodaInstant = new org.joda.time.Instant(instant.toEpochMilli());
        return importCrac(cracPath, jodaInstant.toDateTime());
    }

    public static Crac importCrac(Path cracPath, DateTime timeStampFilter) {
        try (InputStream is = new FileInputStream(cracPath.toFile())) {
            return importCrac(cracPath.getFileName().toString(), is, timeStampFilter);
        } catch (FileNotFoundException e) {
            throw new FaraoException("File not found.");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Crac importCrac(String fileName, InputStream inputStream) {
        byte[] bytes = getBytesFromInputStream(inputStream);
        CracImporter importer = findImporter(fileName, bytes);
        return importer.importCrac(new ByteArrayInputStream(bytes));
    }

    public static Crac importCrac(String fileName, InputStream inputStream, Instant instant) {
        org.joda.time.Instant jodaInstant = new org.joda.time.Instant(instant.toEpochMilli());
        return importCrac(fileName, inputStream, jodaInstant.toDateTime());
    }

    public static Crac importCrac(String fileName, InputStream inputStream, DateTime timeStampFilter) {
        byte[] bytes = getBytesFromInputStream(inputStream);
        CracImporter importer = findImporter(fileName, bytes);
        return importer.importCrac(new ByteArrayInputStream(bytes), timeStampFilter);
    }

    private static CracImporter findImporter(String fileName, byte[] bytes) {
        for (CracImporter importer : CRAC_IMPORTERS.get()) {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            if (importer.exists(fileName, bais)) {
                return importer;
            }
        }
        throw new FaraoException("No importer found for this file");
    }

    private static byte[] getBytesFromInputStream(InputStream inputStream) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            org.apache.commons.io.IOUtils.copy(inputStream, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
