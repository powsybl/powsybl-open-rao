/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_api.range_action.TapRange;
import com.farao_community.farao.data.crac_api.range_action.TapRangeAdder;

import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class TapRangeAdderImpl implements TapRangeAdder {

    private Integer minTap;
    private Integer maxTap;
    private RangeType rangeType;
    private TapConvention tapConvention;
    private static final String CLASS_NAME = "TapRange";

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
        this.tapConvention = rangeDefinition;
        return this;
    }

    @Override
    public PstRangeActionAdder add() {
        AdderUtils.assertAttributeNotNull(minTap, CLASS_NAME, "min value", "withMin()");
        AdderUtils.assertAttributeNotNull(maxTap, CLASS_NAME, "max value", "withMax()");
        AdderUtils.assertAttributeNotNull(rangeType, CLASS_NAME, "range type", "withRangeType()");

        if (rangeType.equals(RangeType.ABSOLUTE) && Objects.isNull(tapConvention)) {
            throw new FaraoException("A tapRange with RangeType ABSOLUTE must contain a tap convention");
        }

        if (maxTap < minTap) {
            throw new FaraoException("Max tap of TapRange must be equal or greater than min tap.");
        }

        if (minTap != Integer.MIN_VALUE && rangeType.equals(RangeType.ABSOLUTE) && tapConvention.equals(TapConvention.STARTS_AT_ONE) && minTap < 1) {
            throw new FaraoException("TapRange with STARTS_AT_ONE must have a min tap higher than 1");
        }

        if (rangeType.equals(RangeType.ABSOLUTE) && tapConvention.equals(TapConvention.STARTS_AT_ONE) &&  maxTap < 1) {
            throw new FaraoException("TapRange with STARTS_AT_ONE must have a max tap higher than 1");
        }

        TapRange pstRange = new TapRangeImpl(minTap, maxTap, rangeType, tapConvention);

        ownerAdder.addRange(pstRange);
        return ownerAdder;
    }
}
