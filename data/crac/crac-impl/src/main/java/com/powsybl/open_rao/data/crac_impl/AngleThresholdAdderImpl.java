/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.PhysicalParameter;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.cnec.AngleCnecAdder;
import com.powsybl.open_rao.data.crac_api.threshold.AngleThresholdAdder;

import java.util.Objects;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class AngleThresholdAdderImpl extends AbstractThresholdAdderImpl<AngleThresholdAdder> implements AngleThresholdAdder {

    private final AngleCnecAdderImpl ownerAdder;

    AngleThresholdAdderImpl(AngleCnecAdder ownerAdder) {
        Objects.requireNonNull(ownerAdder);
        this.ownerAdder = (AngleCnecAdderImpl) ownerAdder;
    }

    @Override
    public AngleThresholdAdderImpl withUnit(Unit unit) {
        unit.checkPhysicalParameter(PhysicalParameter.ANGLE);
        this.unit = unit;
        return this;
    }

    @Override
    public AngleCnecAdder add() {
        super.checkThreshold();

        ownerAdder.addThreshold(new ThresholdImpl(unit, min, max));
        return ownerAdder;
    }
}
