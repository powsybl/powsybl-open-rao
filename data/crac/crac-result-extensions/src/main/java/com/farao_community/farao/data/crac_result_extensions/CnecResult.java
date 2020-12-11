/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.commons.Unit;
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

    private double loopflowInMW; //loopflow value in MW
    private double loopflowThresholdInMW; //loopflow threshold in MW. Normally = max(Tso input, initial calculated lp)
    private double commercialFlowInMW;

    private double absolutePtdfSum;

    @JsonCreator
    public CnecResult(@JsonProperty("flowInMW") double flowInMW,
                      @JsonProperty("flowInA") double flowInA,
                      @JsonProperty("absolutePtdfSum") double absolutePtdfSum) {
        this.flowInMW = flowInMW;
        this.flowInA = flowInA;
        this.minThresholdInMW = Double.NaN;
        this.maxThresholdInMW = Double.NaN;
        this.minThresholdInA = Double.NaN;
        this.maxThresholdInA = Double.NaN;
        this.loopflowInMW = Double.NaN;
        this.loopflowThresholdInMW = Double.NaN;
        this.commercialFlowInMW = Double.NaN;
        this.absolutePtdfSum = absolutePtdfSum;
    }

    public CnecResult(double flowInMW, double flowInA) {
        this(flowInMW, flowInA, Double.NaN);
    }

    public CnecResult(double flowInMW) {
        this(flowInMW, Double.NaN, Double.NaN);
    }

    public CnecResult() {
        this(Double.NaN, Double.NaN, Double.NaN);
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

    public void setThresholds(BranchCnec cnec) {
        minThresholdInMW = cnec.getLowerBound(Side.LEFT, Unit.MEGAWATT).orElse(Double.NEGATIVE_INFINITY);
        maxThresholdInMW = cnec.getUpperBound(Side.LEFT, Unit.MEGAWATT).orElse(Double.POSITIVE_INFINITY);
        minThresholdInA = cnec.getLowerBound(Side.LEFT, Unit.AMPERE).orElse(Double.NEGATIVE_INFINITY);
        maxThresholdInA = cnec.getUpperBound(Side.LEFT, Unit.AMPERE).orElse(Double.POSITIVE_INFINITY);
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

    public double getLoopflowInMW() {
        return loopflowInMW;
    }

    public void setLoopflowInMW(double loopflowInMW) {
        this.loopflowInMW = loopflowInMW;
    }

    public double getLoopflowThresholdInMW() {
        return loopflowThresholdInMW;
    }

    public void setLoopflowThresholdInMW(double loopflowThresholdInMW) {
        this.loopflowThresholdInMW = loopflowThresholdInMW;
    }

    public double getCommercialFlowInMW() {
        return commercialFlowInMW;
    }

    public void setCommercialFlowInMW(double commercialFlowInMW) {
        this.commercialFlowInMW = commercialFlowInMW;
    }

    public double getAbsolutePtdfSum() {
        return absolutePtdfSum;
    }

    public void setAbsolutePtdfSum(double absolutePtdfSum) {
        this.absolutePtdfSum = absolutePtdfSum;
    }
}
