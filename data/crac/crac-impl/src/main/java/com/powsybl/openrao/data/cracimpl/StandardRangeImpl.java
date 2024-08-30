/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.range.StandardRange;
import com.powsybl.openrao.data.cracapi.range.RangeType;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class StandardRangeImpl extends AbstractRange implements StandardRange {

    private final double min;
    private final double max;

    // rangeType added to StandardRange to accept also RELATIVE_TO_PREVIOUS_TIME_STEP, and not only ABSOLUTE
    StandardRangeImpl(double min, double max, RangeType rangeType) {
        super(rangeType, Unit.MEGAWATT);
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
