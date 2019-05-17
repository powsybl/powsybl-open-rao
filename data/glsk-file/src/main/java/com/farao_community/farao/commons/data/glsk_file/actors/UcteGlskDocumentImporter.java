/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.glsk_file.actors;

import com.farao_community.farao.commons.data.glsk_file.UcteGlskDocument;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;


/**
 * Import a UCTE type GLSK Document
 * return a UcteGlskDocument object
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class UcteGlskDocumentImporter {

    /**
     * @param filename Ucte GLSK document filename
     * @return UcteGlskDocument object
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public UcteGlskDocument importUcteGlskDocumentWithFilename(String filename) throws IOException, ParserConfigurationException, SAXException {
        InputStream data = getClass().getResourceAsStream(filename);
        return this.importUcteGlskDocumentFromInputStream(data);
    }

    /**
     * @param filepathstring file path in string
     * @return ucte glsk document
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public UcteGlskDocument importUcteGlskDocumentWithFilePathString(String filepathstring) throws IOException, ParserConfigurationException, SAXException {
        InputStream data = new FileInputStream(filepathstring);
        return this.importUcteGlskDocumentFromInputStream(data);
    }


    /**
     * @param filepath file path in java Path
     * @return ucte glsk document
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public UcteGlskDocument importUcteGlskDocumentWithFilePath(Path filepath) throws IOException, ParserConfigurationException, SAXException {
        InputStream data = new FileInputStream(filepath.toFile());
        return this.importUcteGlskDocumentFromInputStream(data);
    }


    /**
     * @param data InputStream
     * @return ucte glsk document
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public UcteGlskDocument importUcteGlskDocumentFromInputStream(InputStream data) throws IOException, ParserConfigurationException, SAXException {
        return new UcteGlskDocument(data);
    }

}
