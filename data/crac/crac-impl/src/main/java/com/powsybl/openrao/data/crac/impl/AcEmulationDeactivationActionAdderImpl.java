/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.Action;
import com.powsybl.action.HvdcActionBuilder;
import com.powsybl.openrao.data.crac.api.networkaction.AcEmulationDeactivationActionAdder;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;

import static com.powsybl.openrao.data.crac.api.networkaction.ActionType.ACTIVATE;
import static com.powsybl.openrao.data.crac.api.networkaction.ActionType.DEACTIVATE;
import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class AcEmulationDeactivationActionAdderImpl extends AbstractSingleNetworkElementActionAdderImpl<AcEmulationDeactivationActionAdder> implements AcEmulationDeactivationActionAdder {


    AcEmulationDeactivationActionAdderImpl(NetworkActionAdderImpl ownerAdder) {
        super(ownerAdder);
    }

    protected Action buildAction() {
        return new HvdcActionBuilder()
                .withId(String.format("%s_%s_%s", getActionName(), networkElementId, DEACTIVATE))
                .withHvdcId(networkElementId)
                .withAcEmulationEnabled(false)
                .build();
    }

    protected void assertSpecificAttributes() {
        assertAttributeNotNull(DEACTIVATE, getActionName(), "actionType", "withActionType()");
    }

    protected String getActionName() {
        return "AcEmulationDeactivationAction";
    }
}
