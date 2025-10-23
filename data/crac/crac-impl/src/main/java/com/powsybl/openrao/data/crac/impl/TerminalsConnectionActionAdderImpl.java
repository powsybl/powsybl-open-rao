/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.Action;
import com.powsybl.action.TerminalsConnectionActionBuilder;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.TerminalsConnectionActionAdder;

import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
public class TerminalsConnectionActionAdderImpl extends AbstractSingleNetworkElementActionAdderImpl<TerminalsConnectionActionAdder> implements TerminalsConnectionActionAdder {

    private ActionType actionType;

    TerminalsConnectionActionAdderImpl(NetworkActionAdderImpl ownerAdder) {
        super(ownerAdder);
    }

    @Override
    public TerminalsConnectionActionAdder withActionType(ActionType actionType) {
        this.actionType = actionType;
        return this;
    }

    protected Action buildAction() {
        return new TerminalsConnectionActionBuilder()
            .withId(String.format("%s_%s_%s", getActionName(), networkElementId, actionType))
            .withNetworkElementId(networkElementId)
            .withOpen(actionType == ActionType.OPEN)
            .build();
    }

    protected void assertSpecificAttributes() {
        assertAttributeNotNull(actionType, getActionName(), "actionType", "withActionType()");
    }

    protected String getActionName() {
        return "TerminalsConnectionAction";
    }
}
