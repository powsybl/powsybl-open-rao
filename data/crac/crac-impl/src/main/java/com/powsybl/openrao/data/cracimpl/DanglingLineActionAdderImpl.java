/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.Action;
import com.powsybl.action.DanglingLineActionBuilder;
import com.powsybl.openrao.data.cracapi.networkaction.*;

import static com.powsybl.openrao.data.cracimpl.AdderUtils.assertAttributeNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class DanglingLineActionAdderImpl extends AbstractSingleNetworkElementActionAdderImpl<DanglingLineActionAdder> implements DanglingLineActionAdder {

    private Double activePowerValue;

    DanglingLineActionAdderImpl(NetworkActionAdderImpl ownerAdder) {
        super(ownerAdder);
    }

    @Override
    public DanglingLineActionAdder withActivePowerValue(double activePowerValue) {
        this.activePowerValue = activePowerValue;
        return this;
    }

    protected Action buildAction() {
        return new DanglingLineActionBuilder()
            .withId(String.format("%s_%s_%s", getActionName(), networkElementId, activePowerValue))
            .withNetworkElementId(networkElementId)
            .withRelativeValue(false)
            .withActivePowerValue(activePowerValue)
            .build();
    }

    protected void assertSpecificAttributes() {
        assertAttributeNotNull(activePowerValue, getActionName(), "activePowerValue", "withActivePowerValue()");
    }

    protected String getActionName() {
        return "DanglingLineAction";
    }
}
