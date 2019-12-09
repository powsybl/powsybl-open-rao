/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CIM type GlskDocument
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class GlskDocument {

    /**
     * IIDM GlskDocument: map < CountryCode, all GlskTimeSeries of the country
     */
    private Map<String, GlskTimeSeries> mapGlskTimeSeries; //map<CountryCode, GlskTimesSeries of the Country>
    //We consider there are one timeSeries per country. Otherwise: Map<Country, List<GlskTimesSeries>>. but do we have a use case?

    /**
     * Interval start instant
     */
    private Instant instantStart;

    /**
     * Interval end instant
     */
    private Instant instantEnd;

    /**
     * @param data input file stream
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public GlskDocument(InputStream data) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        documentBuilderFactory.setNamespaceAware(true);

        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        Document document = documentBuilder.parse(data);
        document.getDocumentElement().normalize();

        //get interval start and end
        NodeList intervalNodeList = document.getElementsByTagName("time_Period.timeInterval");
        String intervalStart = ((Element) intervalNodeList.item(0)).getElementsByTagName("start").item(0).getTextContent();
        String intervalEnd = ((Element) intervalNodeList.item(0)).getElementsByTagName("end").item(0).getTextContent();
        Interval periodInterval = Interval.parse(intervalStart + "/" + intervalEnd);
        instantStart = periodInterval.getStart();
        instantEnd = periodInterval.getEnd();

        //get map GlskTimeSeries
        mapGlskTimeSeries = new HashMap<>();

        NodeList timeSeriesNodeList = document.getElementsByTagName("TimeSeries");
        for (int i = 0; i < timeSeriesNodeList.getLength(); i++) {

            if (timeSeriesNodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element timeSeriesElement = (Element) timeSeriesNodeList.item(i);
                GlskTimeSeries timeSeries = new GlskTimeSeries(timeSeriesElement);

                String countryMrid = timeSeriesElement.getElementsByTagName("subject_Domain.mRID").item(0).getTextContent();
                mapGlskTimeSeries.put(countryMrid, timeSeries);

            }
        }
    }

    /**
     * @return getter of all country's time series map
     */
    public Map<String, GlskTimeSeries> getMapGlskTimeSeries() {
        return mapGlskTimeSeries;
    }

    /**
     * @return getter of all GlskPoint in document
     */
    public List<GlskPoint> getGlskPoints() {
        List<GlskPoint> glskPointList = new ArrayList<>();
        for (String s : getMapGlskTimeSeries().keySet()) {
            List<GlskPoint> list = getMapGlskTimeSeries().get(s).getGlskPointListInGlskTimeSeries();
            glskPointList.addAll(list);
        }
        return glskPointList;
    }

    /**
     * @return getter of all countries in document
     */
    public List<String> getCountries() {
        return new ArrayList<>(getMapGlskTimeSeries().keySet());
    }

    /**
     * @return Instant start of interval
     */
    public Instant getInstantStart() {
        return instantStart;
    }

    /**
     * @return Instant end of interval
     */
    public Instant getInstantEnd() {
        return instantEnd;
    }

}
