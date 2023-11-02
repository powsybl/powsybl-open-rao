/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.threshold.Threshold;

import java.util.Optional;

import static java.lang.Math.abs;

/**
 * Generic threshold (flow, voltage, etc.) in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class ThresholdImpl implements Threshold {

    protected Unit unit;
    protected Double min;
    protected Double max;

    ThresholdImpl(Unit unit, Double min, Double max) {
        this.unit = unit;
        this.min = min;
        this.max = max;
    }

    public double getMin() {
        return min == null ? Double.NaN : min;
    }

    public double getMax() {
        return max == null ? Double.NaN : max;
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    @Override
    public Optional<Double> min() {
        // So that it returns Optional.empty() if min value is null or NaN
        return Optional.ofNullable(Double.isNaN(getMin()) ? null : min);
    }

    @Override
    public Optional<Double> max() {
        // So that it returns Optional.empty() if max value is null or NaN
        return Optional.ofNullable(Double.isNaN(getMax()) ? null : max);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ThresholdImpl otherT = (ThresholdImpl) o;
        return ((unit == null && otherT.getUnit() == null) || (unit != null && unit.equals(otherT.getUnit())))
                && equalsDouble(max, otherT.getMax())
                && equalsDouble(min, otherT.getMin());
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + unit.hashCode();
        hash = 31 * hash + (Double.isNaN(getMin()) ? 0 : (int) getMin());
        hash = 31 * hash + (Double.isNaN(getMax()) ? 0 : (int) getMax());
        return hash;
    }

    private boolean equalsDouble(Double d1, Double d2) {
        boolean isD1null = d1 == null || Double.isNaN(d1);
        boolean isD2null = d2 == null || Double.isNaN(d2);
        return (isD1null && isD2null) || (!isD1null && !isD2null && abs(d1 - d2) < 1e-6);
    }
}
