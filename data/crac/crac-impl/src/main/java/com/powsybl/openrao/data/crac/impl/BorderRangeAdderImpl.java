/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.rangeaction.BorderRangeAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.ConnectedAreaAdder;

/**
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public class BorderRangeAdderImpl implements BorderRangeAdder {

    private static final String CLASS_NAME = "BorderRange";
    private final ConnectedAreaAdderImpl ownerAdder;

    private Double min;
    private Double max;
    private RangeType rangeType;

    BorderRangeAdderImpl(ConnectedAreaAdderImpl ownerAdder) {
        this.ownerAdder = ownerAdder;
        this.min = Double.MIN_VALUE;
        this.max = Double.MAX_VALUE;
        this.rangeType = RangeType.ABSOLUTE;
    }

    @Override
    public BorderRangeAdder withMin(double minSetpoint) {
        this.min = minSetpoint;
        return this;
    }

    @Override
    public BorderRangeAdder withMax(double maxSetpoint) {
        this.max = maxSetpoint;
        return this;
    }

    @Override
    public BorderRangeAdder withRangeType(RangeType rangeType) {
        this.rangeType = rangeType;
        return this;
    }

    @Override
    public ConnectedAreaAdder add() {
        AdderUtils.assertAttributeNotNull(min, CLASS_NAME, "min value", "withMin()");
        AdderUtils.assertAttributeNotNull(max, CLASS_NAME, "max value", "withMax()");
        AdderUtils.assertAttributeNotNull(rangeType, CLASS_NAME, "range type", "withRangeType()");

        if (max == Double.MAX_VALUE && rangeType.equals(RangeType.ABSOLUTE)) {
            throw new OpenRaoException("BorderRange max value was not defined for absolute range.");
        }
        if (min == Double.MIN_VALUE && rangeType.equals(RangeType.ABSOLUTE)) {
            throw new OpenRaoException("BorderRange min value was not defined for absolute range.");
        }
        if (max < min) {
            throw new OpenRaoException("Max value of BorderRange must be equal or greater than min value.");
        }
        StandardRange borderRange = new StandardRangeImpl(min, max, rangeType);

        ownerAdder.addBorderRange(borderRange);
        return ownerAdder;
    }
}
