/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.PhysicalParameter;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnecAdder;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_api.threshold.BranchThresholdAdder;

import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class BranchThresholdAdderImpl extends AbstractThresholdAdderImpl<BranchThresholdAdder> implements BranchThresholdAdder {

    private final FlowCnecAdderImpl ownerAdder;
    private Side side;

    BranchThresholdAdderImpl(FlowCnecAdder ownerAdder) {
        Objects.requireNonNull(ownerAdder);
        this.ownerAdder = (FlowCnecAdderImpl) ownerAdder;
    }

    @Override
    public BranchThresholdAdderImpl withUnit(Unit unit) {
        unit.checkPhysicalParameter(PhysicalParameter.FLOW);
        this.unit = unit;
        return this;
    }

    @Override
    public BranchThresholdAdder withSide(Side side) {
        this.side = side;
        return this;
    }

    @Override
    public FlowCnecAdder add() {
        super.checkThreshold();
        AdderUtils.assertAttributeNotNull(this.side, "BranchThreshold", "Side", "withSide()");

        ownerAdder.addThreshold(new BranchThresholdImpl(side, unit, min, max));
        return ownerAdder;
    }
}
