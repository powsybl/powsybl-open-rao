/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.cim;

import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.io.GlskDocumentImporter;
import com.google.auto.service.AutoService;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(GlskDocumentImporter.class)
public class CimGlskDocumentImporter implements GlskDocumentImporter {

    @Override
    public GlskDocument importGlsk(InputStream inputStream) throws IOException, SAXException, ParserConfigurationException {
        return CimGlskDocument.importGlsk(inputStream);
    }

    @Override
    public boolean exists(String fileName, InputStream inputStream) {
        return false;
    }
}
