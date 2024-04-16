/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.SwitchAction;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class SwitchActionAdderImplTest {

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
        NetworkAction networkAction = networkActionAdder.newSwitchAction()
            .withNetworkElement("branchNetworkElementId")
            .withActionType(ActionType.OPEN)
            .add()
            .add();

        SwitchAction switchAction = (SwitchAction) networkAction.getElementaryActions().iterator().next();
        assertEquals("branchNetworkElementId", switchAction.getSwitchId());
        assertTrue(switchAction.isOpen());

        // check that network element has been added in CracImpl
        assertEquals(1, ((CracImpl) crac).getNetworkElements().size());
        assertNotNull(((CracImpl) crac).getNetworkElement("branchNetworkElementId"));
    }

    @Test
    void testNoNetworkElement() {
        SwitchActionAdder switchActionAdder = networkActionAdder.newSwitchAction()
            .withActionType(ActionType.OPEN);
        assertThrows(OpenRaoException.class, switchActionAdder::add);
    }

    @Test
    void testNoActionType() {
        SwitchActionAdder switchActionAdder = networkActionAdder.newSwitchAction()
            .withNetworkElement("branchNetworkElementId");
        assertThrows(OpenRaoException.class, switchActionAdder::add);
    }
}
