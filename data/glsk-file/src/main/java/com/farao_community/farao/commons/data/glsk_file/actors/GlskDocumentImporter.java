/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.glsk_file.actors;

import com.farao_community.farao.commons.data.glsk_file.GlskDocument;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;


/**
 * Importer for CIM type GlskDocument
 * Create a GlskDocument object
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class GlskDocumentImporter {

    /**
     * @param filename GLSK document file from java resources
     * @return GLSKDocument object
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public GlskDocument importGlskDocumentWithFilename(String filename) throws IOException, ParserConfigurationException, SAXException {
        InputStream data = getClass().getResourceAsStream(filename);
        return new GlskDocument(data);
    }

    /**
     * @param filepathstring absolute file path in string
     * @return GLSKDocument object
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public GlskDocument importGlskDocumentWithFilePathString(String filepathstring) throws IOException, ParserConfigurationException, SAXException {
        InputStream data = new FileInputStream(filepathstring);
        return importGlskDocumentFromInputStream(data);
    }

    /**
     * @param filepath file path in java Path
     * @return GlskDocument object
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public GlskDocument importGlskDocumentWithFilePath(Path filepath) throws IOException, ParserConfigurationException, SAXException {
        InputStream data = new FileInputStream(filepath.toFile());
        return importGlskDocumentFromInputStream(data);
    }

    /**
     * @param data InputStream of GLSKDocument
     * @return GlskDocument
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public GlskDocument importGlskDocumentFromInputStream(InputStream data) throws IOException, ParserConfigurationException, SAXException {
        return new GlskDocument(data);
    }

}
