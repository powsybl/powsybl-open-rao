/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.swecneexporter;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracio.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.swecneexporter.xsd.MonitoredRegisteredResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for tie-line CNECs results handling
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class TieLineTest {
    private Network network;
    private SweMonitoredSeriesCreator monitoredSeriesCreator;

    @BeforeEach
    void setUp() {
        network = Network.read("SweTestCaseWith12NodesAndXnodes.uct", getClass().getResourceAsStream("/SweTestCaseWith12NodesAndXnodes.uct"));
        network.getDanglingLine("FFR2AA1  XES_FR11 1").setProperty("CGMES.TopologicalNode_Boundary", "XES_FR11_mRID");
        network.getDanglingLine("XES_FR11 EES3AA1  1").setProperty("CGMES.TopologicalNode_Boundary", "XES_FR11_mRID");
        network.getDanglingLine("EES2AA1  XES_PT11 1").setProperty("CGMES.TopologicalNode_Boundary", "XES_PT11_mRID");
        network.getDanglingLine("XES_PT11 PPT3AA1  1").setProperty("CGMES.TopologicalNode_Boundary", "XES_PT11_mRID");

        SweCneHelper helper = Mockito.mock(SweCneHelper.class);
        Mockito.when(helper.getNetwork()).thenReturn(network);

        monitoredSeriesCreator = new SweMonitoredSeriesCreator(helper, Mockito.mock(CimCracCreationContext.class));
    }

    @Test
    void testSetInOutAggregateNodesFrEs() {
        MonitoredRegisteredResource rr = new MonitoredRegisteredResource();

        monitoredSeriesCreator.setInOutAggregateNodes("FFR2AA1  XES_FR11 1 + XES_FR11 EES3AA1  1", "RTE_blabla", rr);
        assertEquals("FFR2AA1", rr.getInAggregateNodeMRID().getValue());
        assertEquals("XES_FR11_mRID", rr.getOutAggregateNodeMRID().getValue());

        monitoredSeriesCreator.setInOutAggregateNodes("FFR2AA1  XES_FR11 1 + XES_FR11 EES3AA1  1", "REEejcnc", rr);
        assertEquals("XES_FR11_mRID", rr.getInAggregateNodeMRID().getValue());
        assertEquals("EES3AA1", rr.getOutAggregateNodeMRID().getValue());
    }

    @Test
    void testSetInOutAggregateNodesPtEs() {
        MonitoredRegisteredResource rr = new MonitoredRegisteredResource();

        monitoredSeriesCreator.setInOutAggregateNodes("EES2AA1  XES_PT11 1 + XES_PT11 PPT3AA1  1", "REN_blabla", rr);
        assertEquals("XES_PT11_mRID", rr.getInAggregateNodeMRID().getValue());
        assertEquals("PPT3AA1", rr.getOutAggregateNodeMRID().getValue());

        monitoredSeriesCreator.setInOutAggregateNodes("EES2AA1  XES_PT11 1 + XES_PT11 PPT3AA1  1", "REE", rr);
        assertEquals("EES2AA1", rr.getInAggregateNodeMRID().getValue());
        assertEquals("XES_PT11_mRID", rr.getOutAggregateNodeMRID().getValue());
    }

    @Test
    void testSetInOutAggregateNodesNoProperty() {
        MonitoredRegisteredResource rr = new MonitoredRegisteredResource();
        network.getDanglingLine("EES2AA1  XES_PT11 1").removeProperty("CGMES.TopologicalNode_Boundary");

        monitoredSeriesCreator.setInOutAggregateNodes("EES2AA1  XES_PT11 1 + XES_PT11 PPT3AA1  1", "REN_blabla", rr);
        assertEquals("EES2AA1", rr.getInAggregateNodeMRID().getValue());
        assertEquals("PPT3AA1", rr.getOutAggregateNodeMRID().getValue());

        monitoredSeriesCreator.setInOutAggregateNodes("EES2AA1  XES_PT11 1 + XES_PT11 PPT3AA1  1", "REE", rr);
        assertEquals("EES2AA1", rr.getInAggregateNodeMRID().getValue());
        assertEquals("PPT3AA1", rr.getOutAggregateNodeMRID().getValue());
    }

    @Test
    void testSetInOutAggregateNodesInternalLine() {
        MonitoredRegisteredResource rr = new MonitoredRegisteredResource();
        monitoredSeriesCreator.setInOutAggregateNodes("EES1AA1  EES3AA1  1", "REN_blabla", rr);
        assertEquals("EES1AA1", rr.getInAggregateNodeMRID().getValue());
        assertEquals("EES3AA1", rr.getOutAggregateNodeMRID().getValue());
    }
}
