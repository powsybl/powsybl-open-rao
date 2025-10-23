/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.SwitchAction;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.SwitchPair;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class SwitchPairImplTest {

    private Network network;
    private NetworkElement switch1 = new NetworkElementImpl("NNL3AA11 NNL3AA12 1");
    private NetworkElement switch2 = new NetworkElementImpl("NNL3AA13 NNL3AA14 1");

    @BeforeEach
    public void setUp() {
        network = Network.read("TestCase12NodesWith2Switches.uct", getClass().getResourceAsStream("/TestCase12NodesWith2Switches.uct"));
    }

    @Test
    void hasImpactOnNetwork() {
        Crac crac = new CracImplFactory().create("cracId");
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
        assertEquals(Set.of(switch1, switch2), sp1.getNetworkElements());
        assertEquals(Set.of(switch1, switch2), sp2.getNetworkElements());

        network.getSwitch(switch1.getId()).setOpen(true);
        network.getSwitch(switch2.getId()).setOpen(false);
        assertFalse(sp1.hasImpactOnNetwork(network));
        assertTrue(sp2.hasImpactOnNetwork(network));

        network.getSwitch(switch1.getId()).setOpen(true);
        network.getSwitch(switch2.getId()).setOpen(true);
        assertTrue(sp1.hasImpactOnNetwork(network));
        assertTrue(sp2.hasImpactOnNetwork(network));

        network.getSwitch(switch1.getId()).setOpen(false);
        network.getSwitch(switch2.getId()).setOpen(false);
        assertTrue(sp1.hasImpactOnNetwork(network));
        assertTrue(sp2.hasImpactOnNetwork(network));
    }

    @Test
    void testCanBeApplied() {
        Crac crac = new CracImplFactory().create("cracId");
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
        assertEquals(Set.of(switch1, switch2), sp1.getNetworkElements());
        assertEquals(Set.of(switch1, switch2), sp2.getNetworkElements());

        network.getSwitch(switch1.getId()).setOpen(true);
        network.getSwitch(switch2.getId()).setOpen(false);
        assertTrue(sp1.canBeApplied(network));
        assertTrue(sp2.canBeApplied(network));

        network.getSwitch(switch1.getId()).setOpen(true);
        network.getSwitch(switch2.getId()).setOpen(true);
        assertFalse(sp1.canBeApplied(network));
        assertFalse(sp2.canBeApplied(network));

        network.getSwitch(switch1.getId()).setOpen(false);
        network.getSwitch(switch2.getId()).setOpen(false);
        assertFalse(sp1.canBeApplied(network));
        assertFalse(sp2.canBeApplied(network));
    }

    @Test
    void testApply() {
        network.getSwitch(switch1.getId()).setOpen(false);
        network.getSwitch(switch2.getId()).setOpen(true);

        // apply
        new SwitchPairImpl("id", switch1, switch2).toModification().apply(network);
        assertTrue(network.getSwitch(switch1.getId()).isOpen());
        assertFalse(network.getSwitch(switch2.getId()).isOpen());

        // re-apply
        new SwitchPairImpl("id", switch1, switch2).toModification().apply(network);
        assertTrue(network.getSwitch(switch1.getId()).isOpen());
        assertFalse(network.getSwitch(switch2.getId()).isOpen());

        // invert
        new SwitchPairImpl("id", switch2, switch1).toModification().apply(network);
        assertFalse(network.getSwitch(switch1.getId()).isOpen());
        assertTrue(network.getSwitch(switch2.getId()).isOpen());
    }

    @Test
    void testEquals() {
        SwitchPair switchPair = new SwitchPairImpl("id", switch1, switch2);
        assertNotNull(switchPair);
        assertNotEquals(Mockito.mock(SwitchAction.class), switchPair);
        assertNotEquals(new SwitchPairImpl("id", switch2, switch1), switchPair);
        assertEquals(new SwitchPairImpl("id", switch1, switch2), switchPair);
        assertNotEquals(new SwitchPairImpl("id", switch1, new NetworkElementImpl("other")), switchPair);
        SwitchPairImpl switchPairImpl = new SwitchPairImpl("id", switch1, switch2);
        assertEquals(switchPair, switchPairImpl);
    }

    @Test
    void compatibility() {
        Crac crac = CommonCracCreation.createCracWithRemedialActions();
        SwitchPair switchPair = (SwitchPair) crac.getNetworkAction("open-switch-1-close-switch-2").getElementaryActions().iterator().next();

        assertTrue(switchPair.isCompatibleWith(switchPair));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("open-switch-2").getElementaryActions().iterator().next()));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("close-switch-1").getElementaryActions().iterator().next()));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("close-switch-2").getElementaryActions().iterator().next()));

        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("generator-1-75-mw").getElementaryActions().iterator().next()));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("generator-1-100-mw").getElementaryActions().iterator().next()));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("generator-2-75-mw").getElementaryActions().iterator().next()));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("generator-2-100-mw").getElementaryActions().iterator().next()));

        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("pst-1-tap-3").getElementaryActions().iterator().next()));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("pst-1-tap-8").getElementaryActions().iterator().next()));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("pst-2-tap-3").getElementaryActions().iterator().next()));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("pst-2-tap-8").getElementaryActions().iterator().next()));

        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("open-switch-1-close-switch-2").getElementaryActions().iterator().next()));
        assertFalse(switchPair.isCompatibleWith(crac.getNetworkAction("open-switch-2-close-switch-1").getElementaryActions().iterator().next()));
        assertTrue(switchPair.isCompatibleWith(crac.getNetworkAction("open-switch-3-close-switch-4").getElementaryActions().iterator().next()));
        assertFalse(switchPair.isCompatibleWith(crac.getNetworkAction("open-switch-1-close-switch-3").getElementaryActions().iterator().next()));
        assertFalse(switchPair.isCompatibleWith(crac.getNetworkAction("open-switch-3-close-switch-2").getElementaryActions().iterator().next()));
    }
}
