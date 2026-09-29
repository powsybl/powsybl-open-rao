/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.range.ConnectedAreaAdder;
import com.powsybl.openrao.data.crac.api.range.ConnectedAreaBorderRangeAdder;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.StandardRange;

/**
 * @author Pedro Tobarra {@literal <pedro.tobarra at artelys.com>}
 */
class ConnectedAreaBorderRangeAdderImpl implements ConnectedAreaBorderRangeAdder {

    private static final String CLASS_NAME = "ConnectedAreaBorderRange";
    private final ConnectedAreaAdderImpl ownerAdder;

    private Double min;
    private Double max;
    private RangeType rangeType;

    ConnectedAreaBorderRangeAdderImpl(ConnectedAreaAdderImpl ownerAdder) {
        this.ownerAdder = ownerAdder;
        this.min = Double.MIN_VALUE;
        this.max = Double.MAX_VALUE;
        this.rangeType = RangeType.ABSOLUTE;
    }

    @Override
    public ConnectedAreaBorderRangeAdder withMin(double minSetpoint) {
        this.min = minSetpoint;
        return this;
    }

    @Override
    public ConnectedAreaBorderRangeAdder withMax(double maxSetpoint) {
        this.max = maxSetpoint;
        return this;
    }

    @Override
    public ConnectedAreaBorderRangeAdder withRangeType(RangeType rangeType) {
        this.rangeType = rangeType;
        return this;
    }

    @Override
    public ConnectedAreaAdder add() {
        AdderUtils.assertAttributeNotNull(min, CLASS_NAME, "min value", "withMin()");
        AdderUtils.assertAttributeNotNull(max, CLASS_NAME, "max value", "withMax()");
        AdderUtils.assertAttributeNotNull(rangeType, CLASS_NAME, "range type", "withRangeType()");

        if (max == Double.MAX_VALUE && rangeType.equals(RangeType.ABSOLUTE)) {
            throw new OpenRaoException("ConnectedAreaBorderRange max value was not defined for absolute range.");
        }
        if (min == Double.MIN_VALUE && rangeType.equals(RangeType.ABSOLUTE)) {
            throw new OpenRaoException("ConnectedAreaBorderRange min value was not defined for absolute range.");
        }
        if (max < min) {
            throw new OpenRaoException("Max value of ConnectedAreaBorderRange must be equal or greater than min value.");
        }
        StandardRange borderRange = new StandardRangeImpl(min, max, rangeType);

        ownerAdder.addBorderRange(borderRange);
        return ownerAdder;
    }
}
