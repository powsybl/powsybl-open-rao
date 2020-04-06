/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_;

import com.farao_community.farao.commons.FaraoException;
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
 */
public class UcteGlskDocument {
    /**
     * list of GlskPoint in the give Glsk document
     */
    private List<GlskPoint> listUcteGlskBlocks;
    /**
     * map of time series id and time series
     */
    private Map<String, UcteGlskSeries> ucteGlskSeriesByID; //map<timeSerieID, UcteGlskSeries>
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
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        documentBuilderFactory.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        documentBuilderFactory.setNamespaceAware(true);

        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        Document document = documentBuilder.parse(data);
        document.getDocumentElement().normalize();

        List<UcteGlskSeries> rawlistUcteGlskSeries = new ArrayList<>();
        ucteGlskSeriesByID = new HashMap<>();

        NodeList glskSeriesNodeList = document.getElementsByTagName("GSKSeries");
        for (int i = 0; i < glskSeriesNodeList.getLength(); i++) {
            if (glskSeriesNodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element glskSeriesElement = (Element) glskSeriesNodeList.item(i);
                UcteGlskSeries glskSeries = new UcteGlskSeries(glskSeriesElement);

                rawlistUcteGlskSeries.add(glskSeries);

            }
        }

        //construct ucteGlskSeriesByID, merging LSK and GSK for same TimeInterval
        for (UcteGlskSeries glskSeries : rawlistUcteGlskSeries) {

            String currentID = glskSeries.getTimeSeriesID();
            if (!ucteGlskSeriesByID.containsKey(currentID)) {
                ucteGlskSeriesByID.put(currentID, glskSeries);
            } else {

                UcteGlskSeries mergedSeries = mergeUcteGlskSeries(glskSeries, ucteGlskSeriesByID.get(currentID));
                ucteGlskSeriesByID.put(currentID, mergedSeries);
            }
        }

        //construct list gsk points
        listUcteGlskBlocks = new ArrayList<>();
        ucteGlskSeriesByID.keySet().forEach(id -> listUcteGlskBlocks.addAll(ucteGlskSeriesByID.get(id).getUcteGlskBlocks()));

        //construct map ucteGlskPointsByCountry
        ucteGlskPointsByCountry = new HashMap<>();
        ucteGlskSeriesByID.keySet().forEach(id -> {
            String country = ucteGlskSeriesByID.get(id).getArea();
            if (!ucteGlskPointsByCountry.containsKey(country)) {
                List<GlskPoint> glskPointList = ucteGlskSeriesByID.get(id).getUcteGlskBlocks();
                ucteGlskPointsByCountry.put(country, glskPointList);
            } else {
                List<GlskPoint> glskPointList = ucteGlskSeriesByID.get(id).getUcteGlskBlocks();
                glskPointList.addAll(ucteGlskPointsByCountry.get(country));
                ucteGlskPointsByCountry.put(country, glskPointList);
            }
        });
    }

    /**
     * merge LSK and GSK of the same time series id
     * @param incomingSeries incoming time series to be merged with old time series
     * @param oldSeries current time series to be updated
     * @return
     */
    private UcteGlskSeries mergeUcteGlskSeries(UcteGlskSeries incomingSeries, UcteGlskSeries oldSeries) {
        if (!incomingSeries.getArea().equals(oldSeries.getArea())) {
            return oldSeries;
        } else {
            List<GlskPoint> incomingPoints = incomingSeries.getUcteGlskBlocks();
            List<GlskPoint> oldPoints = oldSeries.getUcteGlskBlocks();
            for (GlskPoint oldPoint : oldPoints) {
                for (GlskPoint incomingPoint : incomingPoints) {
                    if (oldPoint.getPointInterval().equals(incomingPoint.getPointInterval())) {
                        oldPoint.getGlskShiftKeys().addAll(incomingPoint.getGlskShiftKeys());
                        break;
                    }
                }
            }
            return oldSeries;
        }
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
        return new ArrayList<>(ucteGlskSeriesByID.values());
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
