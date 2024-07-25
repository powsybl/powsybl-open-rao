/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.range.StandardRange;
import com.powsybl.openrao.data.cracapi.range.StandardRangeAdder;
import com.powsybl.openrao.data.cracapi.rangeaction.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class StandardRangeAdderImpl<T extends StandardRangeActionAdder<T>> implements StandardRangeAdder<T> {

    private static final String CLASS_NAME = "StandardRange";
    private final AbstractStandardRangeActionAdder<T> ownerAdder;

    private Double min;
    private Double max;

    private RangeType rangeType;

    StandardRangeAdderImpl(AbstractStandardRangeActionAdder<T> ownerAdder) {
        this.ownerAdder = ownerAdder;
        this.min = Double.MIN_VALUE;
        this.max = Double.MAX_VALUE;
        this.rangeType = RangeType.ABSOLUTE;
    }

    @Override
    public StandardRangeAdder<T> withMin(double minSetpoint) {
        this.min = minSetpoint;
        return this;
    }

    @Override
    public StandardRangeAdder<T> withMax(double maxSetpoint) {
        this.max = maxSetpoint;
        return this;
    }

    @Override
    public StandardRangeAdder<T> withRangeType(RangeType rangeType) {
        this.rangeType = rangeType;
        return this;
    }

    @Override
    public T add() {
        AdderUtils.assertAttributeNotNull(min, CLASS_NAME, "min value", "withMin()");
        AdderUtils.assertAttributeNotNull(max, CLASS_NAME, "max value", "withMax()");
        AdderUtils.assertAttributeNotNull(rangeType, CLASS_NAME, "range type", "withRangeType()");

        if (max == Double.MAX_VALUE) {
            throw new OpenRaoException("StandardRange max value was not defined.");
        }
        if (min == Double.MIN_VALUE) {
            throw new OpenRaoException("StandardRange min value was not defined.");
        }
        if (max < min) {
            throw new OpenRaoException("Max value of StandardRange must be equal or greater than min value.");
        }

        StandardRange standardRange = new StandardRangeImpl(min, max, rangeType);

        ownerAdder.addRange(standardRange);
        return (T) ownerAdder;
    }
}
