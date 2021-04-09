/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PstRangeAdderImpl implements PstRangeAdder {

    private Double minValue;
    private Double maxValue;
    private RangeType rangeType;
    private RangeDefinition rangeDefinition;
    private Unit unit;

    private PstRangeActionAdderImpl ownerAdder;

    PstRangeAdderImpl(PstRangeActionAdderImpl ownerAdder) {
        this.ownerAdder = ownerAdder;
        this.minValue = Double.NEGATIVE_INFINITY;
        this.minValue = Double.POSITIVE_INFINITY;
    }

    @Override
    public PstRangeAdder withMin(double minValue) {
        this.minValue = minValue;
        return this;
    }

    @Override
    public PstRangeAdder withMax(double maxValue) {
        this.maxValue = maxValue;
        return this;
    }

    @Override
    public PstRangeAdder withRangeType(RangeType rangeType) {
        this.rangeType = rangeType;
        return this;
    }

    @Override
    public PstRangeAdder withRangeDefinition(RangeDefinition rangeDefinition) {
        this.rangeDefinition = rangeDefinition;
        return this;
    }

    @Override
    public PstRangeAdder withUnit(Unit unit) {
        this.unit = unit;
        return this;
    }

    @Override
    public PstRangeActionAdder add() {
        AdderUtils.assertAttributeNotNull(minValue, "PstRange", "min value", "withMin()");
        AdderUtils.assertAttributeNotNull(maxValue, "PstRange", "max value", "withMax()");
        AdderUtils.assertAttributeNotNull(rangeType, "PstRange", "range type", "withRangeType()");
        AdderUtils.assertAttributeNotNull(rangeDefinition, "PstRange", "range definition", "withRangeDefinition()");
        AdderUtils.assertAttributeNotNull(unit, "PstRange", "unit", "withUnit()");

        if (!unit.equals(Unit.TAP)) {
            throw new FaraoException("Unit of PstRange must be Unit.TAP");
        } else if (minValue % 1 > 1e-3 || maxValue % 1 > 1e-3) {
            throw new FaraoException("PstRange with TAP unit must have an integer min and a max value");
        }

        if (maxValue < minValue) {
            throw new FaraoException("Max value of PstRange must be equal or greater than min value.");
        }

        if (rangeDefinition.equals(RangeDefinition.STARTS_AT_ONE) && minValue < 1) {
            throw new FaraoException("PstRange with STARTS_AT_ONE must have a min and a max value higher than 1");
        }

        PstRange pstRange = new PstRangeImpl(minValue, maxValue, rangeType, rangeDefinition);

        ownerAdder.addPstRange(pstRange);
        return ownerAdder;
    }
}
