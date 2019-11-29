/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PexGraphTest {
    private Network testNetwork;
    private Map<Bus, Integer> busMapper;

    @Before
    public void setUp() {
        testNetwork = Importers.loadNetwork("testCase.xiidm", NetworkUtilTest.class.getResourceAsStream("/testCase.xiidm"));
        busMapper = NetworkIndexMapperUtil.generateBusMapping(testNetwork);
    }

    @Test
    public void testGraphInjectionSummed() {
        // Test while summing injections
        PexGraph pexGraph = new PexGraph(testNetwork, busMapper, FullLineDecompositionParameters.InjectionStrategy.SUM_INJECTIONS);
        assertEquals(testNetwork.getBusView().getBusStream().count(), pexGraph.vertexSet().size());
        assertEquals(testNetwork.getBranchCount(), pexGraph.edgeSet().size());

        PexGraphVertex pexGraphVertex = pexGraph.vertexSet()
                .stream()
                .filter(vertex -> vertex.getAssociatedBus().getId().equals("NNL2AA1_0"))
                .findAny().get();
        // load = 1000, gen = 500 --> if injection summed associated load = 500, associated gen = 0
        assertEquals(500., pexGraphVertex.getAssociatedLoad(), 0.);
        assertEquals(0., pexGraphVertex.getAssociatedGeneration(), 0.);

        PexGraphEdge pexGraphEdge = pexGraph.edgeSet()
                .stream()
                .filter(edge -> edge.getAssociatedBranch().getId().equals("BBE2AA1  FFR3AA1  1"))
                .findAny().get();
        assertEquals(324.6, pexGraphEdge.getAssociatedFlow(), 0.1);
    }

    @Test
    public void testGraphInjectionDecomposed() {
        // Test while summing injections
        PexGraph pexGraph = new PexGraph(testNetwork, busMapper, FullLineDecompositionParameters.InjectionStrategy.DECOMPOSE_INJECTIONS);
        assertEquals(testNetwork.getBusView().getBusStream().count(), pexGraph.vertexSet().size());
        assertEquals(testNetwork.getBranchCount(), pexGraph.edgeSet().size());

        PexGraphVertex pexGraphVertex = pexGraph.vertexSet()
                .stream()
                .filter(vertex -> vertex.getAssociatedBus().getId().equals("NNL2AA1_0"))
                .findAny().get();
        // load = 1000, gen = 500
        assertEquals(1000., pexGraphVertex.getAssociatedLoad(), 0.);
        assertEquals(500., pexGraphVertex.getAssociatedGeneration(), 0.);

        PexGraphEdge pexGraphEdge = pexGraph.edgeSet()
                .stream()
                .filter(edge -> edge.getAssociatedBranch().getId().equals("BBE2AA1  FFR3AA1  1"))
                .findAny().get();
        assertEquals(324.6, pexGraphEdge.getAssociatedFlow(), 0.1);
    }
}
