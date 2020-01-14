/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.process.search_tree;

import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.ra_optimisation.json.JsonRaoComputationResult;

import com.farao_community.farao.search_tree_rao.SearchTreeRaoResult;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class TreeTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void buildOutputTest() {

        // Get RaoComputationResults
        RaoComputationResult raoResultRoot = JsonRaoComputationResult.read(getClass().getResourceAsStream("/RaoComputationResultRoot.json"));
        RaoComputationResult raoResultOptimal = JsonRaoComputationResult.read(getClass().getResourceAsStream("/RaoComputationResultOptimal.json"));

        // Mock root leaf
        Leaf leafRoot = Mockito.mock(Leaf.class);
        Mockito.when(leafRoot.getRaoResult()).thenReturn(raoResultRoot);

        // Mock optimal Leaf
        NetworkAction na = Mockito.mock(NetworkAction.class);
        Mockito.when(na.getId()).thenReturn("RA1");
        Mockito.when(na.getName()).thenReturn("topological_RA");

        Leaf leafOptimal = Mockito.mock(Leaf.class);
        Mockito.when(leafOptimal.getRaoResult()).thenReturn(raoResultOptimal);
        Mockito.when(leafOptimal.getNetworkActions()).thenReturn(Collections.singletonList(na));
        Mockito.when(leafOptimal.getCost()).thenReturn(1.0);

        // build output
        RaoComputationResult result = Tree.buildOutput(leafRoot, leafOptimal);

        assertEquals(2, result.getPreContingencyResult().getMonitoredBranchResults().size());
        assertEquals("MONITORED_BRANCH_1", result.getPreContingencyResult().getMonitoredBranchResults().get(0).getId());
        assertEquals(105.0, result.getPreContingencyResult().getMonitoredBranchResults().get(0).getPreOptimisationFlow(), DOUBLE_TOLERANCE);
        //assertEquals(95.0, result.getPreContingencyResult().getMonitoredBranchResults().get(0).getPostOptimisationFlow(), DOUBLE_TOLERANCE);

        assertEquals(2, result.getContingencyResults().size());
        assertEquals("CONTINGENCY_1", result.getContingencyResults().get(0).getId());
        assertEquals("MONITORED_BRANCH_1_CO_1", result.getContingencyResults().get(0).getMonitoredBranchResults().get(0).getId());
        assertEquals(115.0, result.getContingencyResults().get(0).getMonitoredBranchResults().get(0).getPreOptimisationFlow(), DOUBLE_TOLERANCE);
        //assertEquals(98.0, result.getContingencyResults().get(0).getMonitoredBranchResults().get(0).getPostOptimisationFlow(), DOUBLE_TOLERANCE);

        assertEquals(1, result.getPreContingencyResult().getRemedialActionResults().size());
        assertEquals("RA1", result.getPreContingencyResult().getRemedialActionResults().get(0).getId());

        assertNotNull(result.getExtension(SearchTreeRaoResult.class));
        assertEquals(SearchTreeRaoResult.ComputationStatus.SECURE, result.getExtension(SearchTreeRaoResult.class).getComputationStatus());
        assertEquals(SearchTreeRaoResult.StopCriterion.OPTIMIZATION_FINISHED, result.getExtension(SearchTreeRaoResult.class).getStopCriterion());
    }
}
