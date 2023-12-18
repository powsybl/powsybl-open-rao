/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.network_action.NetworkAction;
import com.powsybl.open_rao.data.crac_api.network_action.NetworkActionAdder;
import com.powsybl.open_rao.data.crac_api.network_action.PstSetpoint;
import com.powsybl.open_rao.data.crac_api.network_action.PstSetpointAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class PstSetpointAdderImplTest {

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

        NetworkAction networkAction = networkActionAdder.newPstSetPoint()
            .withNetworkElement("pstNetworkElementId")
            .withSetpoint(0)
            .add()
            .add();

        PstSetpoint pstSetpoint = (PstSetpoint) networkAction.getElementaryActions().iterator().next();
        assertEquals("pstNetworkElementId", pstSetpoint.getNetworkElement().getId());
        assertEquals(0, pstSetpoint.getSetpoint(), 1e-3);

        // check that network element has been created in CracImpl
        assertEquals(1, ((CracImpl) crac).getNetworkElements().size());
        assertNotNull(((CracImpl) crac).getNetworkElement("pstNetworkElementId"));
    }

    @Test
    void testNoNetworkElement() {
        PstSetpointAdder pstSetpointAdder = networkActionAdder.newPstSetPoint()
            .withSetpoint(0);
        assertThrows(FaraoException.class, pstSetpointAdder::add);
    }

    @Test
    void testNoSetpoint() {
        PstSetpointAdder pstSetpointAdder = networkActionAdder.newPstSetPoint()
            .withNetworkElement("pstNetworkElementId");
        assertThrows(FaraoException.class, pstSetpointAdder::add);
    }
}
