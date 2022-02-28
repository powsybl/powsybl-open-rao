/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.api;

import org.threeten.extra.Interval;

import java.util.List;

/**
 * Shift Key
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public abstract class AbstractGlskShiftKey {
    private static final double DEFAULT_QUANTITY = 1.0;
    private static final double DEFAULT_MAXIMUM_SHIFT = Double.MAX_VALUE;

    /**
     * business type of shift key. B42, B43, B45
     */
    protected String businessType;
    /**
     * load A05 or generator A04
     */
    protected String psrType;
    /**
     * explicit shift key factor
     */
    protected Double quantity = DEFAULT_QUANTITY;
    /**
     * list of registered resources
     */
    protected List<AbstractGlskRegisteredResource> registeredResourceArrayList;

    /**
     * time interval of shift key
     */
    protected Interval glskShiftKeyInterval;
    /**
     * country mrid
     */
    protected String subjectDomainmRID;
    /**
     * merit order position
     */
    protected int meritOrderPosition;
    /**
     * merit order direction
     */
    protected String flowDirection;
    /**
     * Maximum shift
     */
    protected double maximumShift = DEFAULT_MAXIMUM_SHIFT;

    /**
     * @return debug to string
     */
    public String glskShiftKeyToString() {
        return "\t==== GSK Shift Key ====\n" +
                "\tBusinessType = " + businessType + "\n" +
                "\tPsrType = " + psrType + "\n" +
                "\tQuantity = " + quantity + "\n" +
                "\tGlskShiftKeyInterval = " + glskShiftKeyInterval + "\n" +
                "\tRegisteredResource size = " + registeredResourceArrayList.size() + "\n";
    }

    /**
     * @return getter businesstype
     */
    public String getBusinessType() {
        return businessType;
    }

    /**
     * @param businessType setter business type
     */
    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    /**
     * @return getter psrType
     */
    public String getPsrType() {
        return psrType;
    }

    /**
     * @param psrType setter psrType
     */
    public void setPsrType(String psrType) {
        this.psrType = psrType;
    }

    /**
     * @return getter quantity
     */
    public Double getQuantity() {
        return quantity;
    }

    /**
     * @return get list of registered resources
     */
    public List<AbstractGlskRegisteredResource> getRegisteredResourceArrayList() {
        return registeredResourceArrayList;
    }

    /**
     * @param registeredResourceArrayList setter registered resources
     */
    public void setRegisteredResourceArrayList(List<AbstractGlskRegisteredResource> registeredResourceArrayList) {
        this.registeredResourceArrayList = registeredResourceArrayList;
    }

    /**
     * @return getter country mrid
     */
    public String getSubjectDomainmRID() {
        return subjectDomainmRID;
    }

    /**
     * @return getter merit order position
     */
    public int getMeritOrderPosition() {
        return meritOrderPosition;
    }

    /**
     * @return getter merit order direction
     */
    public String getFlowDirection() {
        return flowDirection;
    }

    public double getMaximumShift() {
        return maximumShift;
    }
}
