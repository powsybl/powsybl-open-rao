/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_.actors;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.glsk.import_.GlskDocument;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;


/**
 * Importer for CIM type GlskDocument
 * Create a GlskDocument object
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public final class GlskDocumentImporter {
    private static final String ERROR_MESSAGE = "Error while parsing GLSK document";

    private GlskDocumentImporter() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * @param filepathstring absolute file path in string
     * @return GLSKDocument object
     */
    public static GlskDocument importGlsk(String filepathstring) {
        try {
            InputStream data = new FileInputStream(filepathstring);
            return importGlsk(data);
        } catch (FileNotFoundException e) {
            throw new FaraoException(ERROR_MESSAGE, e);
        }
    }

    /**
     * @param filepath file path in java Path
     * @return GlskDocument object
     */
    public static GlskDocument importGlsk(Path filepath) {
        try {
            InputStream data = new FileInputStream(filepath.toFile());
            return importGlsk(data);
        } catch (FileNotFoundException e) {
            throw new FaraoException(ERROR_MESSAGE, e);
        }
    }

    /**
     * @param data InputStream of GLSKDocument
     * @return GlskDocument
     */
    public static GlskDocument importGlsk(InputStream data) {
        try {
            return new GlskDocument(data);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new FaraoException(ERROR_MESSAGE, e);
        }
    }

}
