/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Range;
import com.farao_community.farao.data.crac_api.RangeType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class RangeImpl implements Range {

    private RangeType rangeType;
    private double min;
    private double max;
    private Unit unit;

    @JsonCreator
    @Deprecated
    // TODO : convert to private package
    public RangeImpl(@JsonProperty("min") double min,
                    @JsonProperty("max") double max,
                    @JsonProperty("rangeType") RangeType rangeType,
                    @JsonProperty("unit") Unit unit) {
        this.min = min;
        this.max = max;
        this.rangeType = rangeType;
        this.unit = unit;
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public RangeType getRangeType() {
        return rangeType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Range otherRange = (Range) o;
        return rangeType.equals(otherRange.getRangeType())
                && min == otherRange.getMin()
                && max == otherRange.getMax();
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + rangeType.hashCode();
        result = 31 * result + (int) min;
        result = 31 * result + (int) max;
        return result;
    }
}
