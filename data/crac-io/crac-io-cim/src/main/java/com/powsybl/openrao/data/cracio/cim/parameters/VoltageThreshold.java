/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.cim.parameters;

import com.powsybl.openrao.commons.Unit;

import java.util.Objects;

import static java.lang.Math.abs;

/**
 * Utility threshold class used to read thresholds
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class VoltageThreshold {

    protected Unit unit;
    protected Double min;
    protected Double max;

    public VoltageThreshold(Unit unit, Double min, Double max) {
        this.unit = unit;
        this.min = min;
        this.max = max;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    public Unit getUnit() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VoltageThreshold otherT = (VoltageThreshold) o;
        return (unit == null && otherT.getUnit() == null || unit != null && unit.equals(otherT.getUnit()))
                && equalsDouble(max, otherT.getMax())
                && equalsDouble(min, otherT.getMin());
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + unit.hashCode();
        hash = 31 * hash + (Objects.isNull(min) ? 0 : min.intValue());
        hash = 31 * hash + (Objects.isNull(max) ? 0 : max.intValue());
        return hash;
    }

    private boolean equalsDouble(Double d1, Double d2) {
        return d1 == null && d2 == null
            || d1 != null && d2 != null && abs(d1 - d2) < 1e-6;
    }
}
