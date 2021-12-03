/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.network_action.SwitchPair;
import com.farao_community.farao.data.crac_api.network_action.TopologicalAction;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SwitchPairImplTest {

    private Network network;
    private NetworkElement switch1 = new NetworkElementImpl("NNL3AA11 NNL3AA12 1");
    private NetworkElement switch2 = new NetworkElementImpl("NNL3AA13 NNL3AA14 1");

    @Before
    public void setUp() {
        network = Importers.loadNetwork("TestCase12NodesWith2Switches.uct", getClass().getResourceAsStream("/TestCase12NodesWith2Switches.uct"));
    }

    @Test
    public void testCanBeApplied() {
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
    public void testApply() {
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
    public void testEquals() {
        assertNotEquals(new SwitchPairImpl(switch1, switch2), Mockito.mock(TopologicalAction.class));
        assertNotEquals(new SwitchPairImpl(switch1, switch2), new SwitchPairImpl(switch2, switch1));
        assertEquals(new SwitchPairImpl(switch1, switch2), new SwitchPairImpl(switch1, switch2));
        SwitchPairImpl switchPairImpl = new SwitchPairImpl(switch1, switch2);
        assertEquals(switchPairImpl, switchPairImpl);
    }
}
