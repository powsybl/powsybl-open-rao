/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation.createCracWithRemedialActions;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class TopologicalActionImplTest {

    private TopologicalActionImpl topologyOpen;
    private TopologicalActionImpl topologyClose;

    @BeforeEach
    public void setUp() {
        topologyOpen = new TopologicalActionImpl(
                new NetworkElementImpl("FFR2AA1  DDE3AA1  1"),
                ActionType.OPEN
        );
        topologyClose = new TopologicalActionImpl(
                new NetworkElementImpl("FFR2AA1  DDE3AA1  1"),
                ActionType.CLOSE
        );
    }

    @Test
    void basicMethods() {
        assertEquals(ActionType.OPEN, topologyOpen.getActionType());
        assertEquals("FFR2AA1  DDE3AA1  1", topologyOpen.getNetworkElement().getId());
        assertEquals(1, topologyOpen.getNetworkElements().size());
        assertTrue(topologyOpen.canBeApplied(Mockito.mock(Network.class)));
    }

    @Test
    void hasImpactOnNetworkForLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();

        assertTrue(topologyOpen.hasImpactOnNetwork(network));
        assertFalse(topologyClose.hasImpactOnNetwork(network));
    }

    @Test
    void applyOpenCloseLine() {
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
    void hasImpactOnNetworkForSwitch() {
        Network network = NetworkImportsUtil.import12NodesNetworkWithSwitch();
        String switchNetworkElementId = "NNL3AA11 NNL3AA12 1";

        NetworkElement networkElement = new NetworkElementImpl(switchNetworkElementId);
        TopologicalActionImpl openSwitchTopology = new TopologicalActionImpl(
            networkElement,
            ActionType.OPEN);

        assertTrue(openSwitchTopology.hasImpactOnNetwork(network));

        TopologicalActionImpl closeSwitchTopology = new TopologicalActionImpl(
            networkElement,
            ActionType.CLOSE);

        assertFalse(closeSwitchTopology.hasImpactOnNetwork(network));
    }

    @Test
    void switchTopology() {
        Network network = NetworkImportsUtil.import12NodesNetworkWithSwitch();
        String switchNetworkElementId = "NNL3AA11 NNL3AA12 1";

        NetworkElement networkElement = new NetworkElementImpl(switchNetworkElementId);
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

    @Test
    void applyOnUnsupportedElement() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        TopologicalActionImpl topologyOnNode = new TopologicalActionImpl(
                new NetworkElementImpl("FFR2AA1"),
                ActionType.OPEN);

        assertThrows(NotImplementedException.class, () -> topologyOnNode.apply(network));
    }

    @Test
    void equals() {
        assertEquals(topologyClose, topologyClose);
        assertNotEquals(topologyClose, topologyOpen);
    }

    @Test
    void compatibility() {
        Crac crac = createCracWithRemedialActions();
        TopologicalAction topologicalAction = (TopologicalAction) crac.getNetworkAction("open-switch-1").getElementaryActions().iterator().next();

        assertTrue(topologicalAction.isCompatibleWith(topologicalAction));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("open-switch-2").getElementaryActions().iterator().next()));
        assertFalse(topologicalAction.isCompatibleWith(crac.getNetworkAction("close-switch-1").getElementaryActions().iterator().next()));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("close-switch-2").getElementaryActions().iterator().next()));

        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("generator-1-75-mw").getElementaryActions().iterator().next()));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("generator-1-100-mw").getElementaryActions().iterator().next()));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("generator-2-75-mw").getElementaryActions().iterator().next()));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("generator-2-100-mw").getElementaryActions().iterator().next()));

        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("pst-1-tap-3").getElementaryActions().iterator().next()));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("pst-1-tap-8").getElementaryActions().iterator().next()));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("pst-2-tap-3").getElementaryActions().iterator().next()));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("pst-2-tap-8").getElementaryActions().iterator().next()));

        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("open-switch-1-close-switch-2").getElementaryActions().iterator().next()));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("open-switch-2-close-switch-1").getElementaryActions().iterator().next()));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("open-switch-3-close-switch-4").getElementaryActions().iterator().next()));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("open-switch-1-close-switch-3").getElementaryActions().iterator().next()));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("open-switch-3-close-switch-2").getElementaryActions().iterator().next()));
    }
}
