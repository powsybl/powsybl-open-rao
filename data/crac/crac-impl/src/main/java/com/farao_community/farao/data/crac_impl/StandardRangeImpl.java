/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.range.StandardRange;
import com.farao_community.farao.data.crac_api.range.RangeType;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class StandardRangeImpl extends AbstractRange implements StandardRange {

    private final double min;
    private final double max;

    StandardRangeImpl(double min, double max) {
        super(RangeType.ABSOLUTE, Unit.MEGAWATT);
        this.min = min;
        this.max = max;
    }

    @Override
    public double getMin() {
        return min;
    }

    @Override
    public double getMax() {
        return max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StandardRangeImpl otherRange = (StandardRangeImpl) o;
        return super.equals(otherRange)
            && max == otherRange.getMax()
            && min == otherRange.getMin();
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) min;
        result = 31 * result + (int) max;
        return result;
    }

}
