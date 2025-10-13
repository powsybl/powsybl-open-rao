/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.Action;
import com.powsybl.action.HvdcActionBuilder;
import com.powsybl.openrao.data.crac.api.networkaction.AcEmulationSwitchActionAdder;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;

import static com.powsybl.openrao.data.crac.api.networkaction.ActionType.ACTIVATE;
import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class AcEmulationSwitchActionAdderImpl extends AbstractSingleNetworkElementActionAdderImpl<AcEmulationSwitchActionAdder> implements AcEmulationSwitchActionAdder  {

    private ActionType actionType;

    AcEmulationSwitchActionAdderImpl(NetworkActionAdderImpl ownerAdder) {
        super(ownerAdder);
    }

    @Override
    public AcEmulationSwitchActionAdder withActionType(ActionType actionType) {
        this.actionType = actionType;
        return this;
    }

    protected Action buildAction() {
        // update setpoint too
        return new HvdcActionBuilder()
                .withId(String.format("%s_%s_%s", getActionName(), networkElementId, actionType))
                .withHvdcId(networkElementId)
                .withAcEmulationEnabled(actionType == ACTIVATE)
                .build();
    }

    protected void assertSpecificAttributes() {
        assertAttributeNotNull(actionType, getActionName(), "actionType", "withActionType()");
    }

    protected String getActionName() {
            return "AcEmulationSwitchAction";
        }

}