/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_.glsk_document_api;

import com.farao_community.farao.data.glsk.import_.cim_glsk_document.CimGlskShiftKey;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.util.List;

/**
 * GlskPoint: contain a Generator Shift Key and/or a Load Shift Key
 * for a certain Interval and a certain Country
 *  @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public abstract class AbstractGlskPoint {
    /**
     * position of the point; default value is 1; start from 1;
     */
    protected Integer position;
    /**
     * time interval of point
     */
    protected Interval pointInterval;
    /**
     * list of shift keys of point
     */
    protected List<CimGlskShiftKey> glskShiftKeys;
    /**
     * country's mrid
     */
    protected String subjectDomainmRID;
    /**
     * curveType A01 or A03
     */
    protected String curveType;

    /**
     * @return String info of glsk point
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n==== GSK Point ====\n");
        builder.append("Position = ").append(position).append("\n");
        builder.append("PointInterval = ").append(pointInterval.toString()).append("\n");
        builder.append("subjectDomainemRID = ").append(subjectDomainmRID).append("\n");
        builder.append("CurveType = ").append(curveType).append("\n");
        for (CimGlskShiftKey key : glskShiftKeys) {
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
    public List<CimGlskShiftKey> getGlskShiftKeys() {
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
