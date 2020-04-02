/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_;

import com.farao_community.farao.commons.FaraoException;
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
public class UcteGlskDocument {
    /**
     * list of GlskPoint in the give Glsk document
     */
    private List<GlskPoint> listUcteGlskBlocks;
    /**
     * map of Country EIC and time series
     */
    private Map<String, UcteGlskSeries> ucteGlskSeriesByCountry; //map<countryEICode, UcteGlskSeries>
    /**
     * List of Glsk point by country code
     */
    private Map<String, List<GlskPoint>> ucteGlskPointsByCountry; //map <CountryID, List<GlskPoint>>

    public static UcteGlskDocument importGlsk(InputStream data) throws IOException, SAXException, ParserConfigurationException {
        return new UcteGlskDocument(data);
    }

    /**
     * @param data input file as input stream
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public UcteGlskDocument(InputStream data) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        documentBuilderFactory.setNamespaceAware(true);

        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        Document document = documentBuilder.parse(data);
        document.getDocumentElement().normalize();

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
                List<GlskPoint> glskPointList = ucteGlskSeriesByCountry.get(id).getUcteGlskBlocks();
                ucteGlskPointsByCountry.put(country, glskPointList);
            } else {
                List<GlskPoint> glskPointList = ucteGlskSeriesByCountry.get(id).getUcteGlskBlocks();
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
        List<GlskPoint> glskPointListTobeAdded = new ArrayList();
        List<Interval> oldPointsIntervalsList = new ArrayList<>();
        List<GlskPoint> incomingPoints = incomingSeries.getUcteGlskBlocks();
        List<GlskPoint> oldPoints = oldSeries.getUcteGlskBlocks();
        for (GlskPoint oldPoint : oldPoints) {
            for (GlskPoint incomingPoint : incomingPoints) {
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
    public List<GlskPoint> getListUcteGlskBlocks() {
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
    public Map<String, List<GlskPoint>> getUcteGlskPointsByCountry() {
        return ucteGlskPointsByCountry;
    }

    /**
     * @return getter list of country
     */
    public List<String> getCountries() {
        return new ArrayList<>(ucteGlskPointsByCountry.keySet());
    }

    public Map<String, GlskPoint> getGlskPointsForInstant(Instant instant) {
        Map<String, GlskPoint> glskPointInstant = new HashMap<>();
        ucteGlskPointsByCountry.forEach((key, glskPoints) -> {
            GlskPoint glskPoint = glskPoints.stream()
                    .filter(p -> p.containsInstant(instant))
                    .findAny().orElseThrow(() -> new FaraoException("Error during get glsk point by instant for " + key + " country"));
            glskPointInstant.put(key, glskPoint);
        });
        return glskPointInstant;
    }
}
