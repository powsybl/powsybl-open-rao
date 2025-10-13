/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.Action;
import com.powsybl.action.PhaseTapChangerTapPositionActionBuilder;
import com.powsybl.openrao.data.crac.api.networkaction.PhaseTapChangerTapPositionActionAdder;

import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
public class PhaseTapChangerTapPositionActionAdderImpl extends AbstractSingleNetworkElementActionAdderImpl<PhaseTapChangerTapPositionActionAdder> implements PhaseTapChangerTapPositionActionAdder {

    private Integer tapPosition;

    PhaseTapChangerTapPositionActionAdderImpl(NetworkActionAdderImpl ownerAdder) {
        super(ownerAdder);
    }

    @Override
    public PhaseTapChangerTapPositionActionAdder withTapPosition(int tapPosition) {
        this.tapPosition = tapPosition;
        return this;
    }

    protected Action buildAction() {
        return new PhaseTapChangerTapPositionActionBuilder()
            .withId(String.format("%s_%s_%s", getActionName(), networkElementId, tapPosition))
            .withNetworkElementId(networkElementId)
            .withTapPosition(tapPosition)
            .withRelativeValue(false)
            .build();
    }

    protected void assertSpecificAttributes() {
        assertAttributeNotNull(tapPosition, getActionName(), "tapPosition", "withTapPosition()");
    }

    protected String getActionName() {
        return "PhaseTapChangerTapPositionAction";
    }
}
