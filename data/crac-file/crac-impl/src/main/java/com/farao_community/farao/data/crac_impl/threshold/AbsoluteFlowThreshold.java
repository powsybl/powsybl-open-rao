/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Limits of a flow (in MEGAWATT or AMPERE) through a branch. Given as
 * an absolute value.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("absolute-flow-threshold")
public class AbsoluteFlowThreshold extends AbstractFlowThreshold {
    private double maxValue;

    @JsonCreator
    public AbsoluteFlowThreshold(@JsonProperty("unit") Unit unit,
                                 @JsonProperty("side") Side side,
                                 @JsonProperty("direction") Direction direction,
                                 @JsonProperty("maxValue") double maxValue) {
        super(unit, side, direction);
        initMaxValue(maxValue);
    }

    public AbsoluteFlowThreshold(Unit unit, NetworkElement networkElement, Side side, Direction direction, double maxValue) {
        super(unit, networkElement, side, direction);
        initMaxValue(maxValue);
    }

    private void initMaxValue(double maxValue) {
        if (maxValue < 0) {
            throw new FaraoException("MaxValue of AbsoluteFlowThresholds must be positive.");
        }
        this.maxValue = maxValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    @Override
    protected double getAbsoluteMax() {
        return maxValue;
    }

    @Override
    public AbstractThreshold copy() {
        AbsoluteFlowThreshold copiedAbsoluteFlowThreshold = new AbsoluteFlowThreshold(unit, networkElement, side, direction, maxValue);
        if (isSynchronized()) {
            copiedAbsoluteFlowThreshold.isSynchronized = isSynchronized;
            copiedAbsoluteFlowThreshold.voltageLevel = voltageLevel;
        }
        return copiedAbsoluteFlowThreshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbsoluteFlowThreshold threshold = (AbsoluteFlowThreshold) o;
        return super.equals(threshold) && maxValue == threshold.maxValue;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) maxValue;
        return result;
    }
}
