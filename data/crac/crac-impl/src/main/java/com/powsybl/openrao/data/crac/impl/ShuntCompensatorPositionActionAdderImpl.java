/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.Action;
import com.powsybl.action.ShuntCompensatorPositionActionBuilder;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.networkaction.ShuntCompensatorPositionActionAdder;

import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
public class ShuntCompensatorPositionActionAdderImpl extends AbstractSingleNetworkElementActionAdderImpl<ShuntCompensatorPositionActionAdder> implements ShuntCompensatorPositionActionAdder {

    private Integer sectionCount;

    ShuntCompensatorPositionActionAdderImpl(NetworkActionAdderImpl ownerAdder) {
        super(ownerAdder);
    }

    @Override
    public ShuntCompensatorPositionActionAdder withSectionCount(int sectionCount) {
        this.sectionCount = sectionCount;
        return this;
    }

    protected Action buildAction() {
        return new ShuntCompensatorPositionActionBuilder()
            .withId(String.format("%s_%s_%s", getActionName(), networkElementId, sectionCount))
            .withNetworkElementId(networkElementId)
            .withSectionCount(sectionCount)
            .build();
    }

    protected void assertSpecificAttributes() {
        assertAttributeNotNull(sectionCount, getActionName(), "sectionCount", "withSectionCount()");
        if (sectionCount < 0) {
            throw new OpenRaoException("Section count should be a positive integer");
        }
    }

    protected String getActionName() {
        return "ShuntCompensatorPositionAction";
    }
}
