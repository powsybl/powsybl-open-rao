/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class TapRangeAdderImpl implements TapRangeAdder {

    private Integer minTap;
    private Integer maxTap;
    private RangeType rangeType;
    private TapConvention rangeDefinition;

    private PstRangeActionAdderImpl ownerAdder;

    TapRangeAdderImpl(PstRangeActionAdderImpl ownerAdder) {
        this.ownerAdder = ownerAdder;
        this.minTap = Integer.MIN_VALUE;
        this.maxTap = Integer.MAX_VALUE;
    }

    @Override
    public TapRangeAdder withMinTap(int minTap) {
        this.minTap = minTap;
        return this;
    }

    @Override
    public TapRangeAdder withMaxTap(int maxTap) {
        this.maxTap = maxTap;
        return this;
    }

    @Override
    public TapRangeAdder withRangeType(RangeType rangeType) {
        this.rangeType = rangeType;
        return this;
    }

    @Override
    public TapRangeAdder withTapConvention(TapConvention rangeDefinition) {
        this.rangeDefinition = rangeDefinition;
        return this;
    }

    @Override
    public PstRangeActionAdder add() {
        AdderUtils.assertAttributeNotNull(minTap, "TapRange", "min value", "withMin()");
        AdderUtils.assertAttributeNotNull(maxTap, "TapRange", "max value", "withMax()");
        AdderUtils.assertAttributeNotNull(rangeType, "TapRange", "range type", "withRangeType()");
        AdderUtils.assertAttributeNotNull(rangeDefinition, "TapRange", "range definition", "withTapConvention()");

        if (maxTap < minTap) {
            throw new FaraoException("Max tap of TapRange must be equal or greater than min tap.");
        }

        if (rangeDefinition.equals(TapConvention.STARTS_AT_ONE) && minTap < 1) {
            throw new FaraoException("TapRange with STARTS_AT_ONE must have a min and max taps higher than 1");
        }

        TapRange pstRange = new TapRangeImpl(minTap, maxTap, rangeType, rangeDefinition);

        ownerAdder.addRange(pstRange);
        return ownerAdder;
    }
}
