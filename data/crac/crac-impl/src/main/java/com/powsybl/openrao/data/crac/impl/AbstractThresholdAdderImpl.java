/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.threshold.ThresholdAdder;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractThresholdAdderImpl<I extends ThresholdAdder<I>> implements ThresholdAdder<I> {

    protected Unit unit;
    protected Double max;
    protected Double min;

    AbstractThresholdAdderImpl() {
    }

    @Override
    public I withMax(Double max) {
        this.max = max;
        return (I) this;
    }

    @Override
    public I withMin(Double min) {
        this.min = min;
        return (I) this;
    }

    protected void checkThreshold() {
        AdderUtils.assertAttributeNotNull(this.unit, "Threshold", "Unit", "withUnit()");
        if (min == null && max == null) {
            throw new OpenRaoException("Cannot add a threshold without min nor max values. Please use withMin() or withMax().");
        }
    }
}
