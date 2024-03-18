/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.SwitchPair;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;
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
        SwitchPair sp1 = new SwitchPairImpl(switch1, switch2);
        SwitchPair sp2 = new SwitchPairImpl(switch2, switch1);
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
        SwitchPair sp1 = new SwitchPairImpl(switch1, switch2);
        SwitchPair sp2 = new SwitchPairImpl(switch2, switch1);
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
        new SwitchPairImpl(switch1, switch2).apply(network);
        assertTrue(network.getSwitch(switch1.getId()).isOpen());
        assertFalse(network.getSwitch(switch2.getId()).isOpen());

        // re-apply
        new SwitchPairImpl(switch1, switch2).apply(network);
        assertTrue(network.getSwitch(switch1.getId()).isOpen());
        assertFalse(network.getSwitch(switch2.getId()).isOpen());

        // invert
        new SwitchPairImpl(switch2, switch1).apply(network);
        assertFalse(network.getSwitch(switch1.getId()).isOpen());
        assertTrue(network.getSwitch(switch2.getId()).isOpen());
    }

    @Test
    void testEquals() {
        SwitchPair switchPair = new SwitchPairImpl(switch1, switch2);
        assertNotNull(switchPair);
        assertNotEquals(Mockito.mock(TopologicalAction.class), switchPair);
        assertNotEquals(new SwitchPairImpl(switch2, switch1), switchPair);
        assertEquals(new SwitchPairImpl(switch1, switch2), switchPair);
        assertNotEquals(new SwitchPairImpl(switch1, new NetworkElementImpl("other")), switchPair);
        SwitchPairImpl switchPairImpl = new SwitchPairImpl(switch1, switch2);
        assertEquals(switchPair, switchPairImpl);
    }
}
