/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.api.io;

import com.farao_community.farao.data.glsk.api.GlskDocument;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Interface for GLSK object import
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public interface GlskDocumentImporter {

    default GlskDocument importGlsk(Path filepath) throws IOException, ParserConfigurationException, SAXException {
        InputStream data = new FileInputStream(filepath.toFile());
        return importGlsk(data);
    }

    default GlskDocument importGlsk(String filePath) throws IOException, ParserConfigurationException, SAXException {
        InputStream data = new FileInputStream(filePath);
        return importGlsk(data);
    }

    GlskDocument importGlsk(InputStream inputStream) throws IOException, SAXException, ParserConfigurationException;

    boolean exists(String fileName, InputStream inputStream);
}
