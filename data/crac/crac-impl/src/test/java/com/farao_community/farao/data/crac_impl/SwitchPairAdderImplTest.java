/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.network_action.SwitchPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals("open-id", switchPair.getSwitchToOpen().getId());
        assertEquals("open-name", switchPair.getSwitchToOpen().getName());
        assertEquals("close-id", switchPair.getSwitchToClose().getId());
        assertEquals("close-name", switchPair.getSwitchToClose().getName());

        assertEquals(Set.of(switchPair.getSwitchToOpen(), switchPair.getSwitchToClose()), switchPair.getNetworkElements());

        // check that network element has been added in CracImpl
        assertEquals(2, ((CracImpl) crac).getNetworkElements().size());
        assertNotNull(((CracImpl) crac).getNetworkElement("open-id"));
        assertNotNull(((CracImpl) crac).getNetworkElement("close-id"));
    }

    @Test
    void testNoName() {
        NetworkAction networkAction = networkActionAdder.newSwitchPair()
            .withSwitchToOpen("open-id")
            .withSwitchToClose("close-id")
            .add()
            .add();

        SwitchPair switchPair = (SwitchPair) networkAction.getElementaryActions().iterator().next();
        assertEquals("open-id", switchPair.getSwitchToOpen().getId());
        assertEquals("open-id", switchPair.getSwitchToOpen().getName());
        assertEquals("close-id", switchPair.getSwitchToClose().getId());
        assertEquals("close-id", switchPair.getSwitchToClose().getName());
    }

    @Test
    void testNoSwitchToOpen() {
        assertThrows(FaraoException.class, () ->
            networkActionAdder.newSwitchPair()
                .withSwitchToClose("test")
                .add()
                .add());
    }

    @Test
    void testNoSwitchToClose() {
        assertThrows(FaraoException.class, () ->
            networkActionAdder.newSwitchPair()
                .withSwitchToOpen("test")
                .add()
                .add());
    }

    @Test
    void testSameSwitch() {
        assertThrows(FaraoException.class, () ->
            networkActionAdder.newSwitchPair()
                .withSwitchToOpen("test")
                .withSwitchToClose("test")
                .add()
                .add());
    }
}
