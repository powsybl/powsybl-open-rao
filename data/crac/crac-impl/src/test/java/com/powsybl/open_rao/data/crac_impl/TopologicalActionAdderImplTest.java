/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.network_action.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class TopologicalActionAdderImplTest {

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
        NetworkAction networkAction = networkActionAdder.newTopologicalAction()
            .withNetworkElement("branchNetworkElementId")
            .withActionType(ActionType.OPEN)
            .add()
            .add();

        TopologicalAction topologicalAction = (TopologicalAction) networkAction.getElementaryActions().iterator().next();
        assertEquals("branchNetworkElementId", topologicalAction.getNetworkElement().getId());
        assertEquals(ActionType.OPEN, topologicalAction.getActionType());

        // check that network element has been added in CracImpl
        assertEquals(1, ((CracImpl) crac).getNetworkElements().size());
        assertNotNull(((CracImpl) crac).getNetworkElement("branchNetworkElementId"));
    }

    @Test
    void testNoNetworkElement() {
        TopologicalActionAdder topologicalActionAdder = networkActionAdder.newTopologicalAction()
            .withActionType(ActionType.OPEN);
        assertThrows(FaraoException.class, topologicalActionAdder::add);
    }

    @Test
    void testNoActionType() {
        TopologicalActionAdder topologicalActionAdder = networkActionAdder.newTopologicalAction()
            .withNetworkElement("branchNetworkElementId");
        assertThrows(FaraoException.class, topologicalActionAdder::add);
    }
}
