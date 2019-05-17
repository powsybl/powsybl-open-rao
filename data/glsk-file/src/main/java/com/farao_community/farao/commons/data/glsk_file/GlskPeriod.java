/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.glsk_file;

import org.threeten.extra.Interval;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CIM type glsk internal object: a period contains a list of GlskPoint
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class GlskPeriod {

    /**
     * Interval of Period
     */
    private Interval periodInterval;
    /**
     * Resolution of point : ex. PT60M = 60 minutes
     */
    private String resolution;
    /**
     * CurveType: A01 and A03
     */
    private String curveType;
    /**
     * List of GlskPoint in the Period
     */
    private List<GlskPoint> glskPoints;

    /**
     * @param element Dom element
     * @param subjectDomainmRID Country mRID
     * @param curveType curveType
     */
    public GlskPeriod(Element element, String subjectDomainmRID, String curveType) {
        Objects.requireNonNull(element);
        String intervalStart = element.getElementsByTagName("start").item(0).getTextContent();
        String intervalEnd = element.getElementsByTagName("end").item(0).getTextContent();
        this.periodInterval = Interval.parse(intervalStart + "/" + intervalEnd);
        this.resolution = element.getElementsByTagName("resolution").item(0).getTextContent();
        this.curveType = curveType;
        this.glskPoints = new ArrayList<>();
        NodeList glskPointsNodes = element.getElementsByTagName("Point");
        for (int i = 0; i < glskPointsNodes.getLength(); i++) {
            GlskPoint glskPoint = new GlskPoint((Element) glskPointsNodes.item(i), this.periodInterval, this.resolution, subjectDomainmRID, curveType);
            glskPoints.add(glskPoint);
        }
        resetGlskPointsIntervalAccordingToCurveType();

    }

    /**
     * use to rearange the points interval's value if the curve type is A03
     */
    private void resetGlskPointsIntervalAccordingToCurveType() {
        /*  A01: all time interval in a period is described
            A03: a point is described only for the changing point => The interval is equivalent to [this.point.interval.begin, next.point.interval.begin)
         */
        if (curveType.equals("A03")) {
            //curveType.equals("A03")
            Instant nextPointStart = getPeriodInterval().getEnd();
            for (int i = glskPoints.size() - 1; i >= 0; --i) {
                GlskPoint point = glskPoints.get(i);
                Interval newInterval = Interval.of(point.getPointInterval().getStart(), nextPointStart);
                point.setPointInterval(newInterval);
                nextPointStart = point.getPointInterval().getStart();
            }
        }
    }


    /**
     * @return getter Interval of period
     */
    public Interval getPeriodInterval() {
        return periodInterval;
    }

    /**
     * @return getter all glsk points in period
     */
    public List<GlskPoint> getGlskPoints() {
        return glskPoints;
    }

    /**
     * @return getter resolution
     */
    public String getResolution() {
        return resolution;
    }

    /**
     * @param resolution point resolution setter
     */
    public void setResolution(String resolution) {
        this.resolution = resolution;
    }
}
