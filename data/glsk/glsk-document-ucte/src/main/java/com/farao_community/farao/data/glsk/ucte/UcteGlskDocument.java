/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.ucte;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.glsk.api.AbstractGlskPoint;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import org.threeten.extra.Interval;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

/**
 * Ucte type GLSK document object: contains a list of GlskPoint
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Amira Kahya {@literal <amira.kahya@rte-france.com>}
 */
public final class UcteGlskDocument implements GlskDocument {
    /**
     * list of GlskPoint in the give Glsk document
     */
    private final List<AbstractGlskPoint> listUcteGlskBlocks;
    /**
     * map of Country EIC and time series
     */
    private final Map<String, UcteGlskSeries> ucteGlskSeriesByCountry; //map<countryEICode, UcteGlskSeries>
    /**
     * List of Glsk point by country code
     */
    private final Map<String, List<UcteGlskPoint>> ucteGlskPointsByCountry; //map <CountryID, List<GlskPoint>>
    /**
     * document GSKTimeInterval
     */
    private Interval gSKTimeInterval; // GSKTimeInterval. ex: <GSKTimeInterval v="2016-07-28T22:00Z/2016-07-29T22:00Z"/>

    public static UcteGlskDocument importGlsk(InputStream data) throws IOException, SAXException, ParserConfigurationException {
        return new UcteGlskDocument(data);
    }

    /**
     * @param data input file as input stream
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    private UcteGlskDocument(InputStream data) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        documentBuilderFactory.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        documentBuilderFactory.setNamespaceAware(true);

        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        Document document = documentBuilder.parse(data);
        document.getDocumentElement().normalize();

        if (document.getElementsByTagName("GSKTimeInterval").getLength() > 0) {
            this.gSKTimeInterval = Interval.parse(((Element) document.getElementsByTagName("GSKTimeInterval").item(0)).getAttribute("v"));
        }

        List<UcteGlskSeries> rawlistUcteGlskSeries = new ArrayList<>();
        ucteGlskSeriesByCountry = new HashMap<>();
        NodeList glskSeriesNodeList = document.getElementsByTagName("GSKSeries");
        for (int i = 0; i < glskSeriesNodeList.getLength(); i++) {
            if (glskSeriesNodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element glskSeriesElement = (Element) glskSeriesNodeList.item(i);
                UcteGlskSeries glskSeries = new UcteGlskSeries(glskSeriesElement);
                rawlistUcteGlskSeries.add(glskSeries);
            }
        }

        //construct ucteGlskSeriesByCountry, merging LSK and GSK for same TimeInterval
        for (UcteGlskSeries glskSeries : rawlistUcteGlskSeries) {
            String currentArea = glskSeries.getArea();
            if (!ucteGlskSeriesByCountry.containsKey(currentArea)) {
                ucteGlskSeriesByCountry.put(currentArea, glskSeries);
            } else {
                UcteGlskSeries ucteGlskSeries = calculateUcteGlskSeries(glskSeries, ucteGlskSeriesByCountry.get(currentArea));
                ucteGlskSeriesByCountry.put(currentArea, ucteGlskSeries);
            }
        }

        //construct list gsk points
        listUcteGlskBlocks = new ArrayList<>();
        ucteGlskSeriesByCountry.keySet().forEach(id -> listUcteGlskBlocks.addAll(ucteGlskSeriesByCountry.get(id).getUcteGlskBlocks()));

        //construct map ucteGlskPointsByCountry
        ucteGlskPointsByCountry = new HashMap<>();
        ucteGlskSeriesByCountry.keySet().forEach(id -> {
            String country = ucteGlskSeriesByCountry.get(id).getArea();
            if (!ucteGlskPointsByCountry.containsKey(country)) {
                List<UcteGlskPoint> glskPointList = ucteGlskSeriesByCountry.get(id).getUcteGlskBlocks();
                ucteGlskPointsByCountry.put(country, glskPointList);
            } else {
                List<UcteGlskPoint> glskPointList = ucteGlskSeriesByCountry.get(id).getUcteGlskBlocks();
                glskPointList.addAll(ucteGlskPointsByCountry.get(country));
                ucteGlskPointsByCountry.put(country, glskPointList);
            }
        });
    }

    /**
     * merge LSK and GSK of the same country and Same Point Interval
     * and add glsk Point for missed  intervals
     * @param incomingSeries incoming time series to be merged with old time series
     * @param oldSeries      current time series to be updated
     * @return
     */
    private UcteGlskSeries calculateUcteGlskSeries(UcteGlskSeries incomingSeries, UcteGlskSeries oldSeries) {
        List<UcteGlskPoint> glskPointListTobeAdded = new ArrayList();
        List<Interval> oldPointsIntervalsList = new ArrayList<>();
        List<UcteGlskPoint> incomingPoints = incomingSeries.getUcteGlskBlocks();
        List<UcteGlskPoint> oldPoints = oldSeries.getUcteGlskBlocks();
        for (UcteGlskPoint oldPoint : oldPoints) {
            for (UcteGlskPoint incomingPoint : incomingPoints) {
                if (oldPoint.getPointInterval().equals(incomingPoint.getPointInterval())) {
                    oldPoint.getGlskShiftKeys().addAll(incomingPoint.getGlskShiftKeys());
                } else {
                    glskPointListTobeAdded = Collections.singletonList(incomingPoint);

                }
            }
        }
        oldPoints.forEach(oldPoint -> oldPointsIntervalsList.add(oldPoint.getPointInterval()));
        glskPointListTobeAdded.forEach(glskPointToBeAdded -> {
            if (!oldPointsIntervalsList.contains(glskPointToBeAdded.getPointInterval())) {
                oldPoints.add(glskPointToBeAdded);
            }
        });
        return oldSeries;
    }

    /**
     * @return getter list of glsk point in the document
     */
    public List<AbstractGlskPoint> getListUcteGlskBlocks() {
        return listUcteGlskBlocks;
    }

    /**
     * @return getter list of time series
     */
    public List<UcteGlskSeries> getListGlskSeries() {
        return new ArrayList<>(ucteGlskSeriesByCountry.values());
    }

    /**
     * @return getter map of list glsk point
     */
    public Map<String, List<UcteGlskPoint>> getUcteGlskPointsByCountry() {
        return ucteGlskPointsByCountry;
    }

    /**
     * @return getter list of country
     */
    @Override
    public List<String> getAreas() {
        return new ArrayList<>(ucteGlskPointsByCountry.keySet());
    }

    @Override
    public List<AbstractGlskPoint> getGlskPoints(String area) {
        return new ArrayList<>(getUcteGlskPointsByCountry().get(area));
    }

    public Map<String, UcteGlskPoint> getGlskPointsForInstant(Instant instant) {
        Map<String, UcteGlskPoint> glskPointInstant = new HashMap<>();
        ucteGlskPointsByCountry.forEach((key, glskPoints) -> {
            UcteGlskPoint glskPoint = glskPoints.stream()
                    .filter(p -> p.containsInstant(instant))
                    .findAny().orElseThrow(() -> new FaraoException("Error during get glsk point by instant for " + key + " country"));
            glskPointInstant.put(key, glskPoint);
        });
        return glskPointInstant;
    }

    /**
     * @return document time interval
     */
    public Interval getGSKTimeInterval() {
        return gSKTimeInterval;
    }
}
