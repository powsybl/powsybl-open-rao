/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation.createCracWithRemedialActions;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
class SwitchActionImplTest {
    private NetworkAction topologyOpen;
    private NetworkAction topologyClose;
    private Network network;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetworkWithSwitch();
        Crac crac = new CracImplFactory().create("cracId");
        topologyOpen = crac.newNetworkAction()
            .withId("topologyOpen")
            .newSwitchAction()
                .withNetworkElement("NNL3AA11 NNL3AA12 1")
                .withActionType(ActionType.OPEN)
                .add()
            .add();
        topologyClose = crac.newNetworkAction()
            .withId("topologyClose")
            .newSwitchAction()
                .withNetworkElement("NNL3AA11 NNL3AA12 1")
                .withActionType(ActionType.CLOSE)
                .add()
            .add();
    }

    @Test
    void basicMethods() {
        assertEquals(1, topologyOpen.getNetworkElements().size());
        assertEquals("NNL3AA11 NNL3AA12 1", topologyOpen.getNetworkElements().iterator().next().getId());
        assertTrue(topologyOpen.canBeApplied(network));
    }

    @Test
    void hasImpactOnNetworkForSwitch() {
        String switchNetworkElementId = "NNL3AA11 NNL3AA12 1";

        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction openSwitchTopology = crac.newNetworkAction()
            .withId("openSwitchTopology")
            .newSwitchAction()
                .withNetworkElement(switchNetworkElementId)
                .withActionType(ActionType.OPEN)
                .add()
            .add();

        assertTrue(openSwitchTopology.hasImpactOnNetwork(network));

        NetworkAction closeSwitchTopology = crac.newNetworkAction()
            .withId("closeSwitchTopology")
            .newSwitchAction()
            .withNetworkElement(switchNetworkElementId)
            .withActionType(ActionType.CLOSE)
            .add()
            .add();

        assertFalse(closeSwitchTopology.hasImpactOnNetwork(network));
    }

    @Test
    void switchTopology() {
        String switchNetworkElementId = "NNL3AA11 NNL3AA12 1";
        Crac crac = new CracImplFactory().create("cracId");

        NetworkAction openSwitchTopology = crac.newNetworkAction()
            .withId("openSwitchTopology")
            .newSwitchAction()
            .withNetworkElement(switchNetworkElementId)
            .withActionType(ActionType.OPEN)
            .add()
            .add();

        openSwitchTopology.apply(network);
        assertTrue(network.getSwitch(switchNetworkElementId).isOpen());

        NetworkAction closeSwitchTopology = crac.newNetworkAction()
            .withId("closeSwitchTopology")
            .newSwitchAction()
            .withNetworkElement(switchNetworkElementId)
            .withActionType(ActionType.CLOSE)
            .add()
            .add();

        closeSwitchTopology.apply(network);
        assertFalse(network.getSwitch(switchNetworkElementId).isOpen());
    }

    @Test
    void equals() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction similarTopologyClose = crac.newNetworkAction()
            .withId("topologyClose")
            .newSwitchAction()
            .withNetworkElement("NNL3AA11 NNL3AA12 1")
            .withActionType(ActionType.CLOSE)
            .add()
            .add();
        assertEquals(similarTopologyClose, topologyClose);
        assertNotEquals(topologyClose, topologyOpen);
    }

    @Test
    void compatibility() {
        Crac crac = createCracWithRemedialActions();
        NetworkAction topologicalAction = crac.getNetworkAction("open-switch-1");

        assertTrue(topologicalAction.isCompatibleWith(topologicalAction));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("open-switch-2")));
        assertFalse(topologicalAction.isCompatibleWith(crac.getNetworkAction("close-switch-1")));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("close-switch-2")));

        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("generator-1-75-mw")));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("generator-1-100-mw")));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("generator-2-75-mw")));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("generator-2-100-mw")));

        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("pst-1-tap-3")));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("pst-1-tap-8")));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("pst-2-tap-3")));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("pst-2-tap-8")));

        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("open-switch-1-close-switch-2")));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("open-switch-2-close-switch-1")));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("open-switch-3-close-switch-4")));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("open-switch-1-close-switch-3")));
        assertTrue(topologicalAction.isCompatibleWith(crac.getNetworkAction("open-switch-3-close-switch-2")));
    }
}
