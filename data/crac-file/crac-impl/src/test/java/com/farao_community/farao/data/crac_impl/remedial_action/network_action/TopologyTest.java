/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.ActionType;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class TopologyTest {

    @Test
    public void basicMethods() {
        Topology topologyOpen = new Topology(
            new NetworkElement("FFR2AA1  DDE3AA1  1", "FFR2AA1  DDE3AA1  1"),
            ActionType.OPEN
        );
        assertEquals(ActionType.OPEN, topologyOpen.getActionType());

        topologyOpen.setActionType(ActionType.CLOSE);
        assertEquals(ActionType.CLOSE, topologyOpen.getActionType());
    }

    @Test
    public void applyOpenCloseLine() {
        Network network = Importers.loadNetwork(
            "TestCase12Nodes.uct",
            getClass().getResourceAsStream("/TestCase12Nodes.uct")
        );
        Topology topologyOpen = new Topology(
            new NetworkElement("FFR2AA1  DDE3AA1  1", "FFR2AA1  DDE3AA1  1"),
            ActionType.OPEN
        );
        Topology topologyClose = new Topology(
            new NetworkElement("FFR2AA1  DDE3AA1  1", "FFR2AA1  DDE3AA1  1"),
            ActionType.CLOSE
        );

        LoadFlow.run(network);
        assertNotEquals(0, network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal1().getP());
        assertNotEquals(0, network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal2().getP());

        topologyOpen.apply(network);
        LoadFlow.run(network);
        assertTrue(Double.isNaN(network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal1().getP()));
        assertTrue(Double.isNaN(network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal2().getP()));

        topologyClose.apply(network);
        LoadFlow.run(network);
        assertNotEquals(0, network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal1().getP());
        assertNotEquals(0, network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal2().getP());
    }

    @Test
    public void applyOnUnsupportedElement() {
        Network network = Importers.loadNetwork(
            "TestCase12Nodes.uct",
            getClass().getResourceAsStream("/TestCase12Nodes.uct")
        );
        Topology topologyOnNode = new Topology(
            new NetworkElement("FFR2AA1", "FFR2AA1"),
            ActionType.OPEN
        );

        try {
            topologyOnNode.apply(network);
            fail();
        } catch (NotImplementedException ignored) {

        }
    }
}