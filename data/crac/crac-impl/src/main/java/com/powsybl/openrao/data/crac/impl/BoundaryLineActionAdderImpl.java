/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.Action;
import com.powsybl.action.BoundaryLineActionBuilder;
import com.powsybl.openrao.data.crac.api.networkaction.BoundaryLineActionAdder;

import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
public class BoundaryLineActionAdderImpl extends AbstractSingleNetworkElementActionAdderImpl<BoundaryLineActionAdder> implements BoundaryLineActionAdder {

    private Double activePowerValue;

    BoundaryLineActionAdderImpl(NetworkActionAdderImpl ownerAdder) {
        super(ownerAdder);
    }

    @Override
    public BoundaryLineActionAdder withActivePowerValue(double activePowerValue) {
        this.activePowerValue = activePowerValue;
        return this;
    }

    protected Action buildAction() {
        return new BoundaryLineActionBuilder()
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
        return "BoundaryLineAction";
    }
}
