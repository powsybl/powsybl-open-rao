/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.networkaction.SwitchPair;
import com.powsybl.openrao.data.crac.api.networkaction.SwitchPairAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class SwitchPairAdderImplTest {

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
        NetworkAction networkAction = networkActionAdder.newSwitchPair()
            .withSwitchToOpen("open-id", "open-name")
            .withSwitchToClose("close-id", "close-name")
            .add()
            .add();

        SwitchPair switchPair = (SwitchPair) networkAction.getElementaryActions().iterator().next();
        assertEquals("open-id", switchPair.getSwitchToOpenId());
        assertEquals("close-id", switchPair.getSwitchToCloseId());

        assertEquals(Set.of("open-id", "close-id"), networkAction.getNetworkElementsIds());

        // check that network element has been added in CracImpl
        assertEquals(2, ((CracImpl) crac).getNetworkElementIds().size());
        assertTrue(((CracImpl) crac).hasNetworkElement("open-id"));
        assertTrue(((CracImpl) crac).hasNetworkElement("close-id"));
    }

    @Test
    void testNoName() {
        NetworkAction networkAction = networkActionAdder.newSwitchPair()
            .withSwitchToOpen("open-id")
            .withSwitchToClose("close-id")
            .add()
            .add();

        SwitchPair switchPair = (SwitchPair) networkAction.getElementaryActions().iterator().next();
        assertEquals("open-id", switchPair.getSwitchToOpenId());
        assertEquals("close-id", switchPair.getSwitchToCloseId());
    }

    @Test
    void testNoSwitchToOpen() {
        SwitchPairAdder switchPairAdder = networkActionAdder.newSwitchPair()
            .withSwitchToClose("test");
        assertThrows(OpenRaoException.class, switchPairAdder::add);
    }

    @Test
    void testNoSwitchToClose() {
        SwitchPairAdder switchPairAdder = networkActionAdder.newSwitchPair()
            .withSwitchToOpen("test");
        assertThrows(OpenRaoException.class, switchPairAdder::add);
    }

    @Test
    void testSameSwitch() {
        SwitchPairAdder switchPairAdder = networkActionAdder.newSwitchPair()
            .withSwitchToOpen("test")
            .withSwitchToClose("test");
        assertThrows(OpenRaoException.class, switchPairAdder::add);
    }
}
