/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.import_.glsk_document_io_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.GlskDocument;
import com.google.common.base.Suppliers;
import com.powsybl.commons.util.ServiceLoaderCache;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class GlskDocumentImporters {

    private static final Supplier<List<GlskDocumentImporter>> GLSK_IMPORTERS
        = Suppliers.memoize(() -> new ServiceLoaderCache<>(GlskDocumentImporter.class).getServices())::get;

    private GlskDocumentImporters() {
    }

    public static GlskDocument importGlsk(Path glskPath) throws ParserConfigurationException, SAXException {
        try (InputStream is = new FileInputStream(glskPath.toFile())) {
            return importGlsk(glskPath.getFileName().toString(), is);
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

    public static GlskDocument importGlsk(String fileName, InputStream inputStream) throws ParserConfigurationException, SAXException {
        try {
            byte[] bytes = getBytesFromInputStream(inputStream);

            GlskDocumentImporter importer = findImporter(fileName, new ByteArrayInputStream(bytes));
            if (importer == null) {
                throw new FaraoException("No importer found for this file");
            }
            return importer.importGlsk(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static GlskDocumentImporter findImporter(String fileName, InputStream inputStream) {
        try {
            byte[] bytes = getBytesFromInputStream(inputStream);

            for (GlskDocumentImporter importer : GLSK_IMPORTERS.get()) {
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
}
