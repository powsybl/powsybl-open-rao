/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.range.Range;
import com.powsybl.openrao.data.crac.api.range.RangeType;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public abstract class AbstractRange implements Range {

    private RangeType rangeType;

    private Unit unit;

    AbstractRange(RangeType rangeType, Unit unit) {
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
