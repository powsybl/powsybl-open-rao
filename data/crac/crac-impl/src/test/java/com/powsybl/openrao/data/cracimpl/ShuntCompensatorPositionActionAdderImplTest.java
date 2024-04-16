/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.ShuntCompensatorPositionAction;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
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
