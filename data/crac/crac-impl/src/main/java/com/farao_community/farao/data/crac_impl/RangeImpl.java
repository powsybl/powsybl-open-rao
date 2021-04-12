/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.range_action.Range;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RangeImpl implements Range {

    private RangeType rangeType;

    private Unit unit;

    @JsonCreator
    RangeImpl(@JsonProperty("rangeType") RangeType rangeType,
              @JsonProperty("unit") Unit unit) {
        this.rangeType = rangeType;
        this.unit = unit;
    }

    @Override
    public Unit getUnit() {
        return unit;
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
        return rangeType.equals(otherRange.getRangeType());
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + rangeType.hashCode();
        return result;
    }
}
