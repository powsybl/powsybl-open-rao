/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.Unit;

import java.util.Optional;

import static java.lang.Math.abs;

/**
 * Generic threshold (flow, voltage, etc.) in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class Threshold {

    protected Unit unit;
    protected Double min;
    protected Double max;

    Threshold(Unit unit, Double min, Double max) {
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

    public Unit getUnit() {
        return unit;
    }

    public Optional<Double> min() {
        // So that it returns Optional.empty() if min value is null or NaN
        return Optional.ofNullable(Double.isNaN(getMin()) ? null : min);
    }

    public Optional<Double> max() {
        // So that it returns Optional.empty() if max value is null or NaN
        return Optional.ofNullable(Double.isNaN(getMax()) ? null : max);
    }

    boolean limitsByMax() {
        return max().isPresent();
    }

    boolean limitsByMin() {
        return min().isPresent();
    }
}
