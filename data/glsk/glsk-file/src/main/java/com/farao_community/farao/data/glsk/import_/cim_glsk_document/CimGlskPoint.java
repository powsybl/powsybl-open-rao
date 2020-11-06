/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.import_.cim_glsk_document;

import com.farao_community.farao.data.glsk.import_.glsk_document_api.AbstractGlskPoint;
import org.threeten.extra.Interval;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CimGlskPoint extends AbstractGlskPoint {

    /**
     * @param element Dom element
     * @param periodInterval interval of period
     * @param resolution resolution for point
     * @param subjectDomainmRID country's mrid
     * @param curveType curvetype A01 or A03
     */
    public CimGlskPoint(Element element, Interval periodInterval, String resolution, String subjectDomainmRID, String curveType) {
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
            CimGlskShiftKey glskShiftKey = new CimGlskShiftKey((Element) glskShiftKeyNodes.item(i), this.pointInterval, subjectDomainmRID);
            glskShiftKeys.add(glskShiftKey);
        }
    }
}
