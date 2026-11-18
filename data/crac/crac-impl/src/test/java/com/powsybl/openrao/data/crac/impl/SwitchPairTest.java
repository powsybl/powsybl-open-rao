/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.SwitchPair;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This actually belongs in crac-api, but we need crac-impl to test it (bc of CracFactory)
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class SwitchPairTest {

    private Network network;
    private final String switch1 = "NNL3AA11 NNL3AA12 1";
    private final String switch2 = "NNL3AA13 NNL3AA14 1";

    @BeforeEach
    public void setUp() {
        network = Network.read("TestCase12NodesWith2Switches.uct", getClass().getResourceAsStream("/TestCase12NodesWith2Switches.uct"));
    }

    @Test
    void testGetters() {
        SwitchPair switchPair = new SwitchPair("id", switch1, switch2);
        assertEquals("id", switchPair.getId());
        assertEquals(switch1, switchPair.getSwitchToOpenId());
        assertEquals(switch2, switchPair.getSwitchToCloseId());
        assertEquals(Set.of(switch1, switch2), NetworkAction.getNetworkElementIds(switchPair));
    }

    @Test
    void hasImpactOnNetwork() {
        Crac crac = CracFactory.findDefault().create("cracId");
        NetworkAction sp1 = crac.newNetworkAction()
            .withId("sp1")
            .newSwitchPair()
            .withSwitchToOpen("NNL3AA11 NNL3AA12 1")
            .withSwitchToClose("NNL3AA13 NNL3AA14 1")
            .add()
            .add();
        NetworkAction sp2 = crac.newNetworkAction()
            .withId("sp2")
            .newSwitchPair()
            .withSwitchToOpen("NNL3AA13 NNL3AA14 1")
            .withSwitchToClose("NNL3AA11 NNL3AA12 1")
            .add()
            .add();
        assertEquals(Set.of("NNL3AA11 NNL3AA12 1", "NNL3AA13 NNL3AA14 1"), sp1.getNetworkElementsIds());
        assertEquals(Set.of("NNL3AA11 NNL3AA12 1", "NNL3AA13 NNL3AA14 1"), sp2.getNetworkElementsIds());

        network.getSwitch(switch1).setOpen(true);
        network.getSwitch(switch2).setOpen(false);
        assertFalse(sp1.hasImpactOnNetwork(network));
        assertTrue(sp2.hasImpactOnNetwork(network));

        network.getSwitch(switch1).setOpen(true);
        network.getSwitch(switch2).setOpen(true);
        assertTrue(sp1.hasImpactOnNetwork(network));
        assertTrue(sp2.hasImpactOnNetwork(network));

        network.getSwitch(switch1).setOpen(false);
        network.getSwitch(switch2).setOpen(false);
        assertTrue(sp1.hasImpactOnNetwork(network));
        assertTrue(sp2.hasImpactOnNetwork(network));
    }

    @Test
    void testCanBeApplied() {
        Crac crac = CracFactory.findDefault().create("cracId");
        NetworkAction sp1 = crac.newNetworkAction()
            .withId("sp1")
            .newSwitchPair()
            .withSwitchToOpen("NNL3AA11 NNL3AA12 1")
            .withSwitchToClose("NNL3AA13 NNL3AA14 1")
            .add()
            .add();
        NetworkAction sp2 = crac.newNetworkAction()
            .withId("sp2")
            .newSwitchPair()
            .withSwitchToOpen("NNL3AA13 NNL3AA14 1")
            .withSwitchToClose("NNL3AA11 NNL3AA12 1")
            .add()
            .add();
        assertEquals(Set.of("NNL3AA11 NNL3AA12 1", "NNL3AA13 NNL3AA14 1"), sp1.getNetworkElementsIds());
        assertEquals(Set.of("NNL3AA11 NNL3AA12 1", "NNL3AA13 NNL3AA14 1"), sp2.getNetworkElementsIds());

        network.getSwitch(switch1).setOpen(true);
        network.getSwitch(switch2).setOpen(false);
        assertTrue(sp1.canBeApplied(network));
        assertTrue(sp2.canBeApplied(network));

        network.getSwitch(switch1).setOpen(true);
        network.getSwitch(switch2).setOpen(true);
        assertFalse(sp1.canBeApplied(network));
        assertFalse(sp2.canBeApplied(network));

        network.getSwitch(switch1).setOpen(false);
        network.getSwitch(switch2).setOpen(false);
        assertFalse(sp1.canBeApplied(network));
        assertFalse(sp2.canBeApplied(network));
    }

    @Test
    void testApply() {
        network.getSwitch(switch1).setOpen(false);
        network.getSwitch(switch2).setOpen(true);

        // apply
        new SwitchPair("id", switch1, switch2).toModification().apply(network);
        assertTrue(network.getSwitch(switch1).isOpen());
        assertFalse(network.getSwitch(switch2).isOpen());

        // re-apply
        new SwitchPair("id", switch1, switch2).toModification().apply(network);
        assertTrue(network.getSwitch(switch1).isOpen());
        assertFalse(network.getSwitch(switch2).isOpen());

        // invert
        new SwitchPair("id", switch2, switch1).toModification().apply(network);
        assertFalse(network.getSwitch(switch1).isOpen());
        assertTrue(network.getSwitch(switch2).isOpen());
    }

    @Test
    void testEquals() {
        SwitchPair switchPair = new SwitchPair("id", switch1, switch2);
        assertNotNull(switchPair);
        assertNotEquals(Mockito.mock(com.powsybl.action.SwitchAction.class), switchPair);
        assertNotEquals(new SwitchPair("id", switch2, switch1), switchPair);
        assertEquals(new SwitchPair("id", switch1, switch2), switchPair);
        assertNotEquals(new SwitchPair("id", switch1, "other"), switchPair);
        SwitchPair switchPair2 = new SwitchPair("id", switch1, switch2);
        assertEquals(switchPair, switchPair2);
    }

    @Test
    void compatibility() {
        Crac crac = CommonCracCreation.createCracWithRemedialActions();
        NetworkAction switchPair = crac.getNetworkAction("open-switch-1-close-switch-2");

        assertFalse(switchPair.isCompatibleWith(switchPair));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("open-switch-2")));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("close-switch-1")));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("close-switch-2")));

        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("generator-1-75-mw")));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("generator-1-100-mw")));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("generator-2-75-mw")));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("generator-2-100-mw")));

        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("pst-1-tap-3")));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("pst-1-tap-8")));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("pst-2-tap-3")));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("pst-2-tap-8")));

        assertFalse(switchPair.isCompatibleWith(crac.getNetworkAction("open-switch-1-close-switch-2")));
        assertFalse(switchPair.isCompatibleWith(crac.getNetworkAction("open-switch-2-close-switch-1")));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("open-switch-3-close-switch-4")));
        assertFalse(switchPair.isCompatibleWith(crac.getNetworkAction("open-switch-1-close-switch-3")));
        assertFalse(switchPair.isCompatibleWith(crac.getNetworkAction("open-switch-3-close-switch-2")));
    }
}
