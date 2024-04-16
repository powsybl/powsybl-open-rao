/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.TerminalsConnectionAction;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class TerminalsConnectionActionAdderImplTest {

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
        NetworkAction networkAction = networkActionAdder.newTerminalsConnectionAction()
            .withNetworkElement("branchNetworkElementId")
            .withActionType(ActionType.OPEN)
            .add()
            .add();

        TerminalsConnectionAction terminalsConnectionAction = (TerminalsConnectionAction) networkAction.getElementaryActions().iterator().next();
        assertEquals("branchNetworkElementId", terminalsConnectionAction.getElementId());
        assertTrue(terminalsConnectionAction.isOpen());

        // check that network element has been added in CracImpl
        assertEquals(1, ((CracImpl) crac).getNetworkElements().size());
        assertNotNull(((CracImpl) crac).getNetworkElement("branchNetworkElementId"));
    }

    @Test
    void testNoNetworkElement() {
        TerminalsConnectionActionAdder terminalsConnectionActionAdder = networkActionAdder.newTerminalsConnectionAction()
            .withActionType(ActionType.OPEN);
        assertThrows(OpenRaoException.class, terminalsConnectionActionAdder::add);
    }

    @Test
    void testNoActionType() {
        TerminalsConnectionActionAdder terminalsConnectionActionAdder = networkActionAdder.newTerminalsConnectionAction()
            .withNetworkElement("branchNetworkElementId");
        assertThrows(OpenRaoException.class, terminalsConnectionActionAdder::add);
    }
}
