/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.HvdcAction;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.networkaction.AcEmulationDeactivationActionAdder;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class AcEmulationDeactivationActionAdderImplTest {
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
        NetworkAction networkAction = networkActionAdder.newAcEmulationDeactivationAction()
            .withNetworkElement("hvdcLineElementId")
            .add()
            .add();

        HvdcAction hvdcAction = (HvdcAction) networkAction.getElementaryActions().iterator().next();
        assertEquals("hvdcLineElementId", hvdcAction.getHvdcId());
        assertFalse(hvdcAction.isAcEmulationEnabled().get());

        // check that network element has been added in CracImpl
        assertEquals(1, ((CracImpl) crac).getNetworkElements().size());
        assertNotNull(((CracImpl) crac).getNetworkElement("hvdcLineElementId"));
    }

    @Test
    void testNoNetworkElement() {
        AcEmulationDeactivationActionAdder acEmulationDeactivationActionAdder = networkActionAdder.newAcEmulationDeactivationAction();
        assertThrows(OpenRaoException.class, acEmulationDeactivationActionAdder::add);
    }

}
