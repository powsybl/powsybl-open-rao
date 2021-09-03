/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.range_action.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class HvdcRangeAdderImpl implements HvdcRangeAdder {

    private Double min;
    private Double max;
    private static final String CLASS_NAME = "HvdcRange";

    private HvdcRangeActionAdderImpl ownerAdder;

    HvdcRangeAdderImpl(HvdcRangeActionAdderImpl ownerAdder) {
        this.ownerAdder = ownerAdder;
        this.min = Double.MIN_VALUE;
        this.max = Double.MAX_VALUE;
    }

    @Override
    public HvdcRangeAdder withMin(double minSetpoint) {
        this.min = minSetpoint;
        return this;
    }

    @Override
    public HvdcRangeAdder withMax(double maxSetpoint) {
        this.max = maxSetpoint;
        return this;
    }

    @Override
    public HvdcRangeActionAdder add() {
        AdderUtils.assertAttributeNotNull(min, CLASS_NAME, "min value", "withMin()");
        AdderUtils.assertAttributeNotNull(max, CLASS_NAME, "max value", "withMax()");

        if (max == Double.MAX_VALUE) {
            throw new FaraoException("HVDC max range was not defined.");
        }
        if (min == Double.MIN_VALUE) {
            throw new FaraoException("HVDC min range was not defined.");
        }
        if (max < min) {
            throw new FaraoException("Max setpoint of HvdcRange must be equal or greater than min setpoint.");
        }

        HvdcRange hvdcRange = new HvdcRangeImpl(min, max);

        ownerAdder.addRange(hvdcRange);
        return ownerAdder;
    }
}
