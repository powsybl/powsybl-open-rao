/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.threshold.adder.ThresholdAdder;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ThresholdAdderImpl<I extends ThresholdAdder<I>> implements ThresholdAdder<I> {

    protected Unit unit;
    protected Double max;
    protected Double min;

    @Override
    public I setUnit(Unit unit) {
        unit.checkPhysicalParameter(PhysicalParameter.FLOW);
        this.unit = unit;
        return (I) this;
    }

    @Override
    public I setMax(Double max) {
        this.max = max;
        return (I) this;
    }

    @Override
    public I setMin(Double min) {
        this.min = min;
        return (I) this;
    }

    protected void checkThreshold() {
        if (unit == null) {
            throw new FaraoException("Cannot add a threshold without a unit. Please use setUnit.");
        }
        if (min == null && max == null) {
            throw new FaraoException("Cannot add a threshold without min nor max values. Please use setMin or setMax.");
        }
    }
}
