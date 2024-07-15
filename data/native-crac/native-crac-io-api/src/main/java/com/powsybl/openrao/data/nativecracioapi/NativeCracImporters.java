/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.nativecracioapi;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.nativecracapi.NativeCrac;
import com.google.common.base.Suppliers;
import com.powsybl.commons.util.ServiceLoaderCache;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * A utility class to work with native CRAC importers
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class NativeCracImporters {

    private static final Supplier<List<NativeCracImporter>> NATIVE_CRAC_IMPORTERS
        = Suppliers.memoize(() -> new ServiceLoaderCache<>(NativeCracImporter.class).getServices())::get;

    private NativeCracImporters() {
    }

    /**
     * Flexible method to import a NativeCrac from a file, trying to guess its format
     *
     * @param nativeCracPath {@link Path} of the native CRAC file
     */
    public static NativeCrac importData(Path nativeCracPath) {
        return importData(nativeCracPath, ReportNode.NO_OP);
    }

    /**
     * Flexible method to import a NativeCrac from a file, trying to guess its format
     *
     * @param nativeCracPath {@link Path} of the native CRAC file
     * @param reportNode
     */
    public static NativeCrac importData(Path nativeCracPath, ReportNode reportNode) {
        try (InputStream is = new FileInputStream(nativeCracPath.toFile())) {
            return importData(nativeCracPath.getFileName().toString(), is, reportNode);
        } catch (FileNotFoundException e) {
            throw new OpenRaoException("File not found.");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Flexible method to import a NativeCrac from a file, trying to guess its format
     *
     * @param fileName    name of the native CRAC file
     * @param inputStream input stream of the native CRAC file
     */
    public static NativeCrac importData(String fileName, InputStream inputStream) {
        return importData(fileName, inputStream, ReportNode.NO_OP);
    }

    /**
     * Flexible method to import a NativeCrac from a file, trying to guess its format
     *
     * @param fileName    name of the native CRAC file
     * @param inputStream input stream of the native CRAC file
     * @param reportNode
     */
    public static NativeCrac importData(String fileName, InputStream inputStream, ReportNode reportNode) {
        try {
            byte[] bytes = getBytesFromInputStream(inputStream);

            NativeCracImporter importer = findImporter(fileName, new ByteArrayInputStream(bytes), reportNode);
            if (importer == null) {
                throw new OpenRaoException("No importer found for this file");
            }
            return importer.importNativeCrac(new ByteArrayInputStream(bytes), reportNode);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Find an importer for a specified file, trying to guess its format
     * @param fileName name of the native CRAC file
     * @param inputStream input stream of the native CRAC file
     * @return the importer if one exists for the given file or <code>null</code> otherwise.
     */
    public static NativeCracImporter findImporter(String fileName, InputStream inputStream) {
        return findImporter(fileName, inputStream, ReportNode.NO_OP);
    }

    /**
     * Find an importer for a specified file, trying to guess its format
     * @param fileName name of the native CRAC file
     * @param inputStream input stream of the native CRAC file
     * @param reportNode
     * @return the importer if one exists for the given file or <code>null</code> otherwise.
     */
    public static NativeCracImporter findImporter(String fileName, InputStream inputStream, ReportNode reportNode) {
        try {
            byte[] bytes = getBytesFromInputStream(inputStream);

            for (NativeCracImporter importer : NATIVE_CRAC_IMPORTERS.get()) {
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                if (importer.exists(fileName, bais, reportNode)) {
                    return importer;
                }
            }
            return null;

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Find an importer for the specified NativeCrac format name.
     * @param fileFormat unique identifier of a native CRAC file format
     * @return the importer if one exists for the given format or <code>null</code> otherwise.
     */
    public static NativeCracImporter findImporter(String fileFormat) {
        List<NativeCracImporter> importersWithFormat = NATIVE_CRAC_IMPORTERS.get().stream()
            .filter(importer -> importer.getFormat().equals(fileFormat))
            .toList();

        if (importersWithFormat.size() == 1) {
            return importersWithFormat.get(0);
        } else if (importersWithFormat.isEmpty()) {
            return null;
        } else {
            throw new OpenRaoException(String.format("Several NativeCracImporters have been found for format %s", fileFormat));
        }
    }

    private static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        org.apache.commons.io.IOUtils.copy(inputStream, baos);
        return baos.toByteArray();
    }
}
