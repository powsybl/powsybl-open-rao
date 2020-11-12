/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.api.io;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.google.common.base.Suppliers;
import com.powsybl.commons.util.ServiceLoaderCache;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import static org.apache.commons.io.IOUtils.copy;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class GlskDocumentImporters {

    private static final Supplier<List<GlskDocumentImporter>> GLSK_IMPORTERS
        = Suppliers.memoize(() -> new ServiceLoaderCache<>(GlskDocumentImporter.class).getServices());

    private GlskDocumentImporters() {
    }

    public static GlskDocument importGlsk(String filePath) throws FileNotFoundException {
        return importGlsk(Path.of(filePath));
    }

    public static GlskDocument importGlsk(Path glskPath) throws FileNotFoundException {
        InputStream is = new FileInputStream(glskPath.toFile());
        return importGlsk(is);
    }

    private static byte[] getBytesFromInputStream(InputStream inputStream) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            copy(inputStream, baos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return baos.toByteArray();
    }

    public static GlskDocument importGlsk(InputStream inputStream) {
        byte[] bytes = getBytesFromInputStream(inputStream);

        GlskDocumentImporter importer = findImporter(new ByteArrayInputStream(bytes));
        if (importer == null) {
            throw new FaraoException("No importer found for this file");
        }
        return importer.importGlsk(new ByteArrayInputStream(bytes));
    }

    public static GlskDocumentImporter findImporter(InputStream inputStream) {
        byte[] bytes = getBytesFromInputStream(inputStream);

        for (GlskDocumentImporter importer : GLSK_IMPORTERS.get()) {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            if (importer.exists(bais)) {
                return importer;
            }
        }
        return null;
    }
}
