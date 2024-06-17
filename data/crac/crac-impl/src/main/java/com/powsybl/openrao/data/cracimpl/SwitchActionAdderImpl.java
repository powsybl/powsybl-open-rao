/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.Action;
import com.powsybl.action.SwitchActionBuilder;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.SwitchActionAdder;

import static com.powsybl.openrao.data.cracimpl.AdderUtils.assertAttributeNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SwitchActionAdderImpl extends AbstractSingleNetworkElementActionAdderImpl<SwitchActionAdder> implements SwitchActionAdder {

    private ActionType actionType;

    SwitchActionAdderImpl(NetworkActionAdderImpl ownerAdder) {
        super(ownerAdder);
    }

    @Override
    public SwitchActionAdder withActionType(ActionType actionType) {
        this.actionType = actionType;
        return this;
    }

    protected Action buildAction() {
        return new SwitchActionBuilder()
            .withId(String.format("%s_%s_%s", getActionName(), networkElementId, actionType))
            .withNetworkElementId(networkElementId)
            .withOpen(actionType == ActionType.OPEN)
            .build();
    }

    protected void assertSpecificAttributes() {
        assertAttributeNotNull(actionType, getActionName(), "actionType", "withActionType()");
    }

    protected String getActionName() {
        return "SwitchAction";
    }
}
