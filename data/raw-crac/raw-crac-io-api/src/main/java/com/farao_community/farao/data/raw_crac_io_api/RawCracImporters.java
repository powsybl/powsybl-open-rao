/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.raw_crac_io_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.raw_crac_api.RawCrac;
import com.google.common.base.Suppliers;
import com.powsybl.commons.util.ServiceLoaderCache;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A utility class to work with raw CRAC importers
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class RawCracImporters {

    private static final Supplier<List<RawCracImporter>> RAW_CRAC_IMPORTERS
        = Suppliers.memoize(() -> new ServiceLoaderCache<>(RawCracImporter.class).getServices())::get;

    private RawCracImporters() {
    }

    /**
     * Flexible method to import a RawCrac from a file, trying to guess its format
     * @param rawCracPath {@link Path} of the raw CRAC file
     */
    public static RawCrac importData(Path rawCracPath) {
        try (InputStream is = new FileInputStream(rawCracPath.toFile())) {
            return importData(rawCracPath.getFileName().toString(), is);
        } catch (FileNotFoundException e) {
            throw new FaraoException("File not found.");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Flexible method to import a RawCrac from a file, trying to guess its format
     * @param fileName name of the raw CRAC file
     * @param inputStream input stream of the raw CRAC file
     */
    public static RawCrac importData(String fileName, InputStream inputStream) {
        try {
            byte[] bytes = getBytesFromInputStream(inputStream);

            RawCracImporter importer = findImporter(fileName, new ByteArrayInputStream(bytes));
            if (importer == null) {
                throw new FaraoException("No importer found for this file");
            }
            return importer.importRawCrac(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Find an importer for a specify file, trying to guess its format
     * @param fileName name of the raw CRAC file
     * @param inputStream input stream of the raw CRAC file
     * @return the importer if one exists for the given file or <code>null</code> otherwise.
     */
    public static RawCracImporter findImporter(String fileName, InputStream inputStream) {
        try {
            byte[] bytes = getBytesFromInputStream(inputStream);

            for (RawCracImporter importer : RAW_CRAC_IMPORTERS.get()) {
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

    /**
     * Find an importer for the specified RawCrac format name.
     * @param fileFormat unique identifier of a raw CRAC file format
     * @return the importer if one exists for the given format or <code>null</code> otherwise.
     */
    public static RawCracImporter findImporter(String fileFormat) {
        List<RawCracImporter> importersWithFormat =  RAW_CRAC_IMPORTERS.get().stream()
            .filter(importer -> importer.getFormat().equals(fileFormat))
            .collect(Collectors.toList());

        if (importersWithFormat.size() == 1) {
            return importersWithFormat.get(0);
        } else if (importersWithFormat.size() == 0) {
            return null;
        } else {
            throw new FaraoException(String.format("Several RawCracImporters have been found for format %s", fileFormat));
        }
    }

    private static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        org.apache.commons.io.IOUtils.copy(inputStream, baos);
        return baos.toByteArray();
    }
}
