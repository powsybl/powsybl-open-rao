/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.TapRange;
import com.powsybl.openrao.data.crac.api.range.TapRangeAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeActionAdder;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class TapRangeAdderImpl implements TapRangeAdder {

    private Integer minTap;
    private Integer maxTap;
    private RangeType rangeType;
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
    public PstRangeActionAdder add() {
        AdderUtils.assertAttributeNotNull(minTap, CLASS_NAME, "min value", "withMin()");
        AdderUtils.assertAttributeNotNull(maxTap, CLASS_NAME, "max value", "withMax()");
        AdderUtils.assertAttributeNotNull(rangeType, CLASS_NAME, "range type", "withRangeType()");

        if (maxTap < minTap) {
            throw new OpenRaoException("Max tap of TapRange must be equal or greater than min tap.");
        }

        TapRange pstRange = new TapRangeImpl(minTap, maxTap, rangeType);

        ownerAdder.addRange(pstRange);
        return ownerAdder;
    }
}
