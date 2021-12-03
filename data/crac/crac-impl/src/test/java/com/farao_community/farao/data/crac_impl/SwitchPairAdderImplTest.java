/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.network_action.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SwitchPairAdderImplTest {

    private Crac crac;
    private NetworkActionAdder networkActionAdder;

    @Before
    public void setUp() {
        crac = new CracImplFactory().create("cracId");
        networkActionAdder = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator");
    }

    @Test
    public void testOk() {
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
    public void testNoName() {
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

    @Test (expected = FaraoException.class)
    public void testNoSwitchToOpen() {
        networkActionAdder.newSwitchPair()
            .withSwitchToClose("test")
            .add()
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNoSwitchToClose() {
        networkActionAdder.newSwitchPair()
            .withSwitchToOpen("test")
            .add()
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testSameSwitch() {
        networkActionAdder.newSwitchPair()
            .withSwitchToOpen("test")
            .withSwitchToClose("test")
            .add()
            .add();
    }
}
