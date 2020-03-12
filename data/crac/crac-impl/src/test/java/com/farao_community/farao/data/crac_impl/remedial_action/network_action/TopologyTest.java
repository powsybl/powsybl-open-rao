/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.ActionType;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.AbstractRemedialActionTest;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class TopologyTest extends AbstractRemedialActionTest {

    private String networkElementId = "FFR2AA1  DDE3AA1  1";
    private Topology topologyOpen;
    private Topology topologyClose;
    private ArrayList<UsageRule> usageRules;

    @Before
    public void setUp() throws Exception {
        usageRules = createUsageRules();
        Topology topologyOpen = new Topology(
                "topology_id",
                "topology_name",
                "topology_operator",
                usageRules,
                new NetworkElement(networkElementId, networkElementId),
                ActionType.OPEN
        );
        Topology topologyClose = new Topology(
                "topology_id",
                "topology_name",
                "topology_operator",
                usageRules,
                new NetworkElement(networkElementId, networkElementId),
                ActionType.CLOSE
        );
        this.topologyClose = topologyClose;
        this.topologyOpen = topologyOpen;
    }

    @Test
    public void basicMethods() {

        assertEquals(ActionType.OPEN, topologyOpen.getActionType());
        topologyOpen.setActionType(ActionType.CLOSE);
        assertEquals(ActionType.CLOSE, topologyOpen.getActionType());
    }

    @Test
    public void applyOpenCloseLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();

        assertTrue(network.getBranch(networkElementId).getTerminal1().isConnected());
        assertTrue(network.getBranch(networkElementId).getTerminal2().isConnected());

        topologyOpen.apply(network);
        assertFalse(network.getBranch(networkElementId).getTerminal1().isConnected());
        assertFalse(network.getBranch(networkElementId).getTerminal2().isConnected());

        topologyClose.apply(network);
        assertTrue(network.getBranch(networkElementId).getTerminal1().isConnected());
        assertTrue(network.getBranch(networkElementId).getTerminal2().isConnected());
    }

    @Test
    public void applyOnUnsupportedElement() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        String nodeId = "FFR2AA1";
        Topology topologyOnNode = new Topology(
                "on_node_id",
                "on_node_name",
                "on_node_operator",
                usageRules,
                new NetworkElement(nodeId, nodeId),
                ActionType.OPEN
        );

        try {
            topologyOnNode.apply(network);
            fail();
        } catch (NotImplementedException ignored) {

        }
    }

    @Test
    public void equals() {
        assertEquals(topologyClose, topologyClose);
        assertNotEquals(topologyClose, topologyOpen);
    }

}
