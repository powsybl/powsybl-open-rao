/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.ShuntCompensatorPositionAction;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.networkaction.ShuntCompensatorPositionActionAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
class ShuntCompensatorPositionActionAdderImplTest {

    private Crac crac;
    private NetworkActionAdder networkActionAdder;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId");
        networkActionAdder = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator");
    }

    @Test
    void testOk() {
        NetworkAction networkAction = networkActionAdder.newShuntCompensatorPositionAction()
            .withNetworkElement("groupNetworkElementId")
            .withSectionCount(3)
            .add()
            .add();

        ShuntCompensatorPositionAction shuntCompensatorPositionAction = (ShuntCompensatorPositionAction) networkAction.getElementaryActions().iterator().next();
        assertEquals("groupNetworkElementId", shuntCompensatorPositionAction.getShuntCompensatorId());
        assertEquals(3, shuntCompensatorPositionAction.getSectionCount());

        // check that network element have been added to CracImpl
        assertEquals(1, ((CracImpl) crac).getNetworkElements().size());
        assertNotNull(((CracImpl) crac).getNetworkElement("groupNetworkElementId"));
    }

    @Test
    void testNoNetworkElement() {
        ShuntCompensatorPositionActionAdder shuntCompensatorPositionActionAdder = networkActionAdder.newShuntCompensatorPositionAction()
            .withSectionCount(3);
        Exception e = assertThrows(OpenRaoException.class, shuntCompensatorPositionActionAdder::add);
        assertEquals("Cannot add ShuntCompensatorPositionAction without a network element. Please use withNetworkElement() with a non null value", e.getMessage());
    }

    @Test
    void testNoSetpoint() {
        ShuntCompensatorPositionActionAdder shuntCompensatorPositionActionAdder = networkActionAdder.newShuntCompensatorPositionAction()
            .withNetworkElement("groupNetworkElementId");
        Exception e = assertThrows(OpenRaoException.class, shuntCompensatorPositionActionAdder::add);
        assertEquals("Cannot add ShuntCompensatorPositionAction without a sectionCount. Please use withSectionCount() with a non null value", e.getMessage());
    }

    @Test
    void testNegativeSetPointWithSectionCount() {
        ShuntCompensatorPositionActionAdder shuntCompensatorPositionActionAdder = networkActionAdder.newShuntCompensatorPositionAction()
                .withNetworkElement("groupNetworkElementId").withSectionCount(-100);
        Exception e = assertThrows(OpenRaoException.class, shuntCompensatorPositionActionAdder::add);
        assertEquals("Section count should be a positive integer", e.getMessage());
    }

}
