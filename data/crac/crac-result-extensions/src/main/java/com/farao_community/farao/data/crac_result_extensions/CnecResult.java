/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Unit;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@JsonTypeName("cnec-result")
public class CnecResult implements Result {

    private double flowInMW;
    private double flowInA;

    private double minThresholdInMW;
    private double maxThresholdInMW;
    private double minThresholdInA;
    private double maxThresholdInA;

    private double loopflow; //loopflow value

    @JsonCreator
    public CnecResult(@JsonProperty("flowInMW") double flowInMW, @JsonProperty("flowInA") double flowInA) {
        this.flowInMW = flowInMW;
        this.flowInA = flowInA;
        this.minThresholdInMW = Double.NaN;
        this.maxThresholdInMW = Double.NaN;
        this.minThresholdInA = Double.NaN;
        this.maxThresholdInA = Double.NaN;
        this.loopflow = Double.NaN;
    }

    public CnecResult(double flowInMW) {
        this.flowInMW = flowInMW;
        this.flowInA = Double.NaN;
        this.minThresholdInMW = Double.NaN;
        this.maxThresholdInMW = Double.NaN;
        this.minThresholdInA = Double.NaN;
        this.maxThresholdInA = Double.NaN;
        this.loopflow = Double.NaN;
    }

    public CnecResult() {
        this.flowInMW = Double.NaN;
        this.flowInA = Double.NaN;
        this.minThresholdInMW = Double.NaN;
        this.maxThresholdInMW = Double.NaN;
        this.minThresholdInA = Double.NaN;
        this.maxThresholdInA = Double.NaN;
        this.loopflow = Double.NaN;
    }

    public void setFlowInMW(double flow) {
        this.flowInMW = flow;
    }

    public double getFlowInMW() {
        return flowInMW;
    }

    public void setFlowInA(double flow) {
        this.flowInA = flow;
    }

    public double getFlowInA() {
        return flowInA;
    }

    public void setThresholds(Cnec cnec) {
        minThresholdInMW = cnec.getMinThreshold(Unit.MEGAWATT).orElse(Double.NEGATIVE_INFINITY);
        maxThresholdInMW = cnec.getMaxThreshold(Unit.MEGAWATT).orElse(Double.POSITIVE_INFINITY);
        minThresholdInA = cnec.getMinThreshold(Unit.AMPERE).orElse(Double.NEGATIVE_INFINITY);
        maxThresholdInA = cnec.getMaxThreshold(Unit.AMPERE).orElse(Double.POSITIVE_INFINITY);
    }

    public double getMinThresholdInMW() {
        return minThresholdInMW;
    }

    public void setMinThresholdInMW(double minThresholdInMW) {
        this.minThresholdInMW = minThresholdInMW;
    }

    public double getMaxThresholdInMW() {
        return maxThresholdInMW;
    }

    public void setMaxThresholdInMW(double maxThresholdInMW) {
        this.maxThresholdInMW = maxThresholdInMW;
    }

    public double getMinThresholdInA() {
        return minThresholdInA;
    }

    public void setMinThresholdInA(double minThresholdInA) {
        this.minThresholdInA = minThresholdInA;
    }

    public double getMaxThresholdInA() {
        return maxThresholdInA;
    }

    public void setMaxThresholdInA(double maxThresholdInA) {
        this.maxThresholdInA = maxThresholdInA;
    }

    public double getLoopflow() {
        return loopflow;
    }

    public void setLoopflow(double loopflow) {
        this.loopflow = loopflow;
    }
}
