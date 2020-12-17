/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.cse;

import com.farao_community.farao.data.glsk.api.AbstractGlskPoint;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.GlskException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class CseGlskDocument implements GlskDocument {
    /**
     * list of GlskPoint in the give Glsk document
     */
    private final Map<String, List<AbstractGlskPoint>> cseGlskPoints = new TreeMap<>();

    public static CseGlskDocument importGlsk(Document document) {
        return new CseGlskDocument(document);
    }

    public static CseGlskDocument importGlsk(InputStream inputStream) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            documentBuilderFactory.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            documentBuilderFactory.setNamespaceAware(true);

            Document document = documentBuilderFactory.newDocumentBuilder().parse(inputStream);
            document.getDocumentElement().normalize();
            return new CseGlskDocument(document);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new GlskException("Unable to import CIM GLSK file.", e);
        }
    }

    private CseGlskDocument(Document document) {
        NodeList timeSeriesNodeList = document.getElementsByTagName("TimeSeries");
        for (int i = 0; i < timeSeriesNodeList.getLength(); i++) {
            if (timeSeriesNodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element timeSeriesElement = (Element) timeSeriesNodeList.item(i);
                AbstractGlskPoint glskPoint = new CseGlskPoint(timeSeriesElement);
                cseGlskPoints.computeIfAbsent(glskPoint.getSubjectDomainmRID(), area -> cseGlskPoints.put(area, new ArrayList<>()));
                cseGlskPoints.get(glskPoint.getSubjectDomainmRID()).add(glskPoint);
            }
        }
    }

    @Override
    public List<String> getZones() {
        return new ArrayList<>(cseGlskPoints.keySet());
    }

    @Override
    public List<AbstractGlskPoint> getGlskPoints(String zone) {
        return cseGlskPoints.getOrDefault(zone, Collections.emptyList());
    }
}
