/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl.range_domain;

import com.farao_community.farao.data.crac_api.RangeDefinition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class Range {

    private RangeDefinition rangeDefinition;
    private RangeType rangeType;
    private double min;
    private double max;

    @JsonCreator
    public Range(@JsonProperty("min") double min,
                 @JsonProperty("max") double max,
                 @JsonProperty("rangeType") RangeType rangeType,
                 @JsonProperty("rangeDefinition") RangeDefinition rangeDefinition) {
        this.min = min;
        this.max = max;
        this.rangeType = rangeType;
        this.rangeDefinition = rangeDefinition;
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

    public RangeDefinition getRangeDefinition() {
        return rangeDefinition;
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
        return rangeDefinition.equals(otherRange.rangeDefinition)
                && rangeType.equals(otherRange.rangeType)
                && min == otherRange.min
                && max == otherRange.max;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + rangeDefinition.hashCode();
        result = 31 * result + rangeType.hashCode();
        result = 31 * result + (int) min;
        result = 31 * result + (int) max;
        return result;
    }
}
