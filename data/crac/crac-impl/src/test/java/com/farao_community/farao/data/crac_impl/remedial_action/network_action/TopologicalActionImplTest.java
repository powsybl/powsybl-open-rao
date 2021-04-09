/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.ActionType;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_impl.AbstractRemedialActionTest;
import com.farao_community.farao.data.crac_impl.TopologicalActionImpl;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class TopologicalActionImplTest extends AbstractRemedialActionTest {

    private TopologicalActionImpl topologyOpen;
    private TopologicalActionImpl topologyClose;

    @Before
    public void setUp() {
        topologyOpen = new TopologicalActionImpl(
                new NetworkElement("FFR2AA1  DDE3AA1  1"),
                ActionType.OPEN
        );
        topologyClose = new TopologicalActionImpl(
                new NetworkElement("FFR2AA1  DDE3AA1  1"),
                ActionType.CLOSE
        );
    }

    @Test
    public void basicMethods() {
        assertEquals(ActionType.OPEN, topologyOpen.getActionType());
        assertEquals("FFR2AA1  DDE3AA1  1", topologyOpen.getNetworkElement().getId());
    }

    @Test
    public void applyOpenCloseLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        assertTrue(network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal1().isConnected());
        assertTrue(network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal2().isConnected());

        topologyOpen.apply(network);
        assertFalse(network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal1().isConnected());
        assertFalse(network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal2().isConnected());

        topologyClose.apply(network);
        assertTrue(network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal1().isConnected());
        assertTrue(network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal2().isConnected());
    }

    @Test
    public void switchTopology() {
        Network network = NetworkImportsUtil.import12NodesNetworkWithSwitch();
        String switchNetworkElementId = "NNL3AA11 NNL3AA12 1";

        NetworkElement networkElement = new NetworkElement(switchNetworkElementId);
        TopologicalActionImpl openSwitchTopology = new TopologicalActionImpl(
            networkElement,
            ActionType.OPEN);

        openSwitchTopology.apply(network);
        assertTrue(network.getSwitch(switchNetworkElementId).isOpen());

        TopologicalActionImpl closeSwitchTopology = new TopologicalActionImpl(
            networkElement,
            ActionType.CLOSE);

        closeSwitchTopology.apply(network);
        assertFalse(network.getSwitch(switchNetworkElementId).isOpen());
    }

    @Test (expected = NotImplementedException.class)
    public void applyOnUnsupportedElement() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        TopologicalActionImpl topologyOnNode = new TopologicalActionImpl(
                new NetworkElement("FFR2AA1"),
                ActionType.OPEN);

        topologyOnNode.apply(network);
    }

    @Test
    public void equals() {
        assertEquals(topologyClose, topologyClose);
        assertNotEquals(topologyClose, topologyOpen);
    }
}
