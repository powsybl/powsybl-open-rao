/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.ucte;

import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.io.GlskDocumentImporter;
import com.google.auto.service.AutoService;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(GlskDocumentImporter.class)
public class UcteGlskDocumentImporter implements GlskDocumentImporter {
    @Override
    public GlskDocument importGlsk(InputStream inputStream) throws IOException, SAXException, ParserConfigurationException {
        return UcteGlskDocument.importGlsk(inputStream);
    }

    @Override
    public boolean exists(String fileName, InputStream inputStream) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        documentBuilderFactory.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        documentBuilderFactory.setNamespaceAware(true);

        Document document;
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilder.parse(inputStream);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return false;
        }
        document.getDocumentElement().normalize();

        return "GSKDocument".equals(document.getDocumentElement().getTagName());
    }
}
