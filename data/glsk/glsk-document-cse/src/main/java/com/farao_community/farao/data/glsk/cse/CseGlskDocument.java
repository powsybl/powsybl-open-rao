/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.cse;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.commons.ZonalDataChronology;
import com.farao_community.farao.commons.ZonalDataImpl;
import com.farao_community.farao.data.glsk.api.AbstractGlskPoint;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.GlskException;
import com.farao_community.farao.data.glsk.api.util.converters.GlskPointScalableConverter;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.apache.commons.lang3.NotImplementedException;
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
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class CseGlskDocument implements GlskDocument {
    private static final String LINEAR_GLSK_NOT_HANDLED = "CSE GLSK document does not handle Linear GLSK conversion";

    /**
     * list of GlskPoint in the given Glsk document
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
            throw new GlskException("Unable to import CSE GLSK file.", e);
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

    @Override
    public ZonalData<LinearGlsk> getZonalGlsks(Network network) {
        throw new NotImplementedException(LINEAR_GLSK_NOT_HANDLED);
    }

    @Override
    public ZonalData<LinearGlsk> getZonalGlsks(Network network, Instant instant) {
        throw new NotImplementedException(LINEAR_GLSK_NOT_HANDLED);
    }

    @Override
    public ZonalDataChronology<LinearGlsk> getZonalGlsksChronology(Network network) {
        throw new NotImplementedException(LINEAR_GLSK_NOT_HANDLED);
    }

    @Override
    public ZonalData<Scalable> getZonalScalable(Network network) {
        Map<String, Scalable> zonalData = new HashMap<>();
        for (Map.Entry<String, List<AbstractGlskPoint>> entry : cseGlskPoints.entrySet()) {
            String area = entry.getKey();
            // There is always only one GlskPoint for a zone
            AbstractGlskPoint zonalGlskPoint = entry.getValue().get(0);
            if (isHybridCseGlskPoint(zonalGlskPoint)) {
                List<Scalable> scalables = zonalGlskPoint.getGlskShiftKeys().stream()
                    .sorted(Comparator.comparingInt(sk -> ((CseGlskShiftKey) sk).getOrder()))
                    .map(sk -> GlskPointScalableConverter.convert(network, List.of(sk)))
                    .collect(Collectors.toList());
                zonalData.put(area, Scalable.upDown(Scalable.stack(scalables.get(0), scalables.get(1)), scalables.get(1)));
            } else {
                zonalData.put(area, GlskPointScalableConverter.convert(network, zonalGlskPoint));
            }
        }
        return new ZonalDataImpl<>(zonalData);
    }

    private boolean isHybridCseGlskPoint(AbstractGlskPoint zonalGlskPoint) {
        // If 2 shift keys have different orders, this is a hybrid glsk for Swiss's ID CSE GSK.
        return zonalGlskPoint.getGlskShiftKeys().size() == 2 &&
            ((CseGlskShiftKey) zonalGlskPoint.getGlskShiftKeys().get(0)).getOrder() !=
                ((CseGlskShiftKey) zonalGlskPoint.getGlskShiftKeys().get(1)).getOrder();
    }

    @Override
    public ZonalData<Scalable> getZonalScalable(Network network, Instant instant) {
        throw new NotImplementedException("CSE GLSK document does only support hourly data");
    }

    @Override
    public ZonalDataChronology<Scalable> getZonalScalableChronology(Network network) {
        throw new NotImplementedException("CSE GLSK document does only support hourly data");
    }
}
