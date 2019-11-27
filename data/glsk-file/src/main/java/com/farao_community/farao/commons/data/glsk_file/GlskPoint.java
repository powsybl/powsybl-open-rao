/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.glsk_file;

import com.farao_community.farao.commons.FaraoException;
import org.threeten.extra.Interval;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * GlskPoint: contain a Generator Shift Key and/or a Load Shift Key
 * for a certain Interval and a certain Country
 *  @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class GlskPoint {
    /**
     * position of the point; default value is 1; start from 1;
     */
    private Integer position;
    /**
     * time interval of point
     */
    private Interval pointInterval;
    /**
     * list of shift keys of point
     */
    private List<GlskShiftKey> glskShiftKeys;
    /**
     * country's mrid
     */
    private String subjectDomainmRID;
    /**
     * curveType A01 or A03
     */
    private String curveType;

    /**
     * @param element Dom element
     * @param periodInterval interval of period
     * @param resolution resolution for point
     * @param subjectDomainmRID country's mrid
     * @param curveType curvetype A01 or A03
     */
    public GlskPoint(Element element, Interval periodInterval, String resolution, String subjectDomainmRID, String curveType) {
        Objects.requireNonNull(element);
        this.position = Integer.valueOf(element.getElementsByTagName("position").item(0).getTextContent());

        /*
        A01: all time interval in a period is described
        A03: a point is described only for the changing point => The interval is equivalent to [this.point.interval.begin, next.point.interval.begin)
        */
        Duration resolutionDuration = Duration.parse(resolution);
        Instant start = periodInterval.getStart().plus(resolutionDuration.multipliedBy(position - 1L));
        // if curveType "A03", we will change "end" for each Point after constructing all GlskPoint in the GlskPeriod
        // in GlskPeriod.resetPeriodIntervalAccordingToCurveType()
        Instant end = periodInterval.getEnd();
        if (curveType.equals("A01")) {
            //curveType = A01
            end = periodInterval.getStart().plus(resolutionDuration.multipliedBy(position));
        }

        this.pointInterval = Interval.of(start, end);

        this.subjectDomainmRID = subjectDomainmRID;
        this.curveType = curveType;

        this.glskShiftKeys = new ArrayList<>();
        NodeList glskShiftKeyNodes = element.getElementsByTagName("SKBlock_TimeSeries");
        for (int i = 0; i < glskShiftKeyNodes.getLength(); i++) {
            GlskShiftKey glskShiftKey = new GlskShiftKey((Element) glskShiftKeyNodes.item(i), this.pointInterval, subjectDomainmRID);
            glskShiftKeys.add(glskShiftKey);
        }
    }

    /**
     * @param ucteformat Constructor for UCTE Glsk document
     * @param element Dom element
     * @param ucteBlockType Type of block: CountryGSK, ManualGSK, AutoGSK
     * @param area country
     * @param ucteBusinessType generator or load
     * @param shareFactor shareFactor for generator or load
     */
    public GlskPoint(boolean ucteformat, Element element, String ucteBlockType, String area, String ucteBusinessType, Double shareFactor) {
        Objects.requireNonNull(element);
        if (ucteformat) {
            this.position = 1;
            this.pointInterval = Interval.parse(((Element) element.getElementsByTagName("TimeInterval").item(0)).getAttribute("v"));
            this.subjectDomainmRID = area;
            this.curveType = "A03";

            glskShiftKeys = new ArrayList<>();
            switch (ucteBlockType) {
                case "CountryGSK_Block": {
                    //build a country GSK B42 empty regitered resources list
                    GlskShiftKey countryGlskShiftKey = new GlskShiftKey("B42", ucteBusinessType, this.subjectDomainmRID, this.pointInterval, shareFactor);
                    glskShiftKeys.add(countryGlskShiftKey);
                    break;
                }
                case "ManualGSK_Block": {
                    //build a B43 participation factor
                    GlskShiftKey manuelGlskShiftKey = new GlskShiftKey("B43", ucteBusinessType, this.subjectDomainmRID, this.pointInterval, shareFactor);
                    //set registeredResourcesList for manuelGlskShiftKey
                    List<GlskRegisteredResource> registerdResourceArrayList = new ArrayList<>();
                    NodeList ucteGlskNodesList = element.getElementsByTagName("ManualNodes");

                    for (int i = 0; i < ucteGlskNodesList.getLength(); ++i) {
                        GlskRegisteredResource ucteGlskNode = new GlskRegisteredResource(ucteformat, (Element) ucteGlskNodesList.item(i));
                        registerdResourceArrayList.add(ucteGlskNode);
                    }
                    manuelGlskShiftKey.setRegisteredResourceArrayList(registerdResourceArrayList);
                    glskShiftKeys.add(manuelGlskShiftKey);
                    break;
                }
                case "AutoGSK_Block": {
                    /* build a B42 explicit */
                    GlskShiftKey autoGlskShiftKey = new GlskShiftKey("B42", ucteBusinessType, this.subjectDomainmRID, this.pointInterval, shareFactor);
                    List<GlskRegisteredResource> registerdResourceArrayList = new ArrayList<>();
                    NodeList ucteGlskNodesList = element.getElementsByTagName("AutoNodes");

                    for (int i = 0; i < ucteGlskNodesList.getLength(); ++i) {
                        GlskRegisteredResource ucteGlskNode = new GlskRegisteredResource(ucteformat, (Element) ucteGlskNodesList.item(i));
                        registerdResourceArrayList.add(ucteGlskNode);
                    }
                    autoGlskShiftKey.setRegisteredResourceArrayList(registerdResourceArrayList);
                    glskShiftKeys.add(autoGlskShiftKey);
                    break;
                }
                default:
                    throw new FaraoException("Unknown UCTE Block type");
            }
        }
    }

    /**
     * @return String info of glsk point
     */
    public String glskPointToString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n==== GSK Point ====\n");
        builder.append("Position = ").append(position).append("\n");
        builder.append("PointInterval = ").append(pointInterval.toString()).append("\n");
        builder.append("subjectDomainemRID = ").append(subjectDomainmRID).append("\n");
        builder.append("CurveType = ").append(curveType).append("\n");
        for (GlskShiftKey key : glskShiftKeys) {
            builder.append(key.glskShiftKeyToString());
        }
        builder.append("\n");
        return builder.toString();
    }

    /**
     * @return get point's position
     */
    public Integer getPosition() {
        return position;
    }

    /**
     * @param position position setter
     */
    public void setPosition(Integer position) {
        this.position = position;
    }

    /**
     * @return get all shift keys in points
     */
    public List<GlskShiftKey> getGlskShiftKeys() {
        return glskShiftKeys;
    }

    /**
     * @return get interval of point
     */
    public Interval getPointInterval() {
        return pointInterval;
    }

    public boolean containsInstant(Instant instant) {
        return pointInterval.contains(instant);
    }

    /**
     * @param pointInterval set interval of point
     */
    public void setPointInterval(Interval pointInterval) {
        this.pointInterval =  pointInterval;
    }

    /**
     * @return get country mrid
     */
    public String getSubjectDomainmRID() {
        return subjectDomainmRID;
    }

    /**
     * @return get curvetype
     */
    public String getCurveType() {
        return curveType;
    }
}
