/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.process.search_tree;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.powsybl.iidm.network.*;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class TreeTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void buildOutputTest() {

        // Get RaoComputationResults
        RaoResult rootRaoResult = new RaoResult(RaoResult.Status.SUCCESS);
        rootRaoResult.setPreOptimVariantId("rootPreOptim");
        rootRaoResult.setPostOptimVariantId("rootPostOptim");
        RaoResult optimalRaoResult = new RaoResult(RaoResult.Status.SUCCESS);
        optimalRaoResult.setPreOptimVariantId("leafPreOptim");
        optimalRaoResult.setPostOptimVariantId("leafPostOptim");

        // Mock root leaf
        Leaf rootLeaf = Mockito.mock(Leaf.class);
        Mockito.when(rootLeaf.getRaoResult()).thenReturn(rootRaoResult);

        // Mock optimal Leaf
        Leaf optimalLeaf = Mockito.mock(Leaf.class);
        Mockito.when(optimalLeaf.getRaoResult()).thenReturn(optimalRaoResult);

        // build output
        RaoResult result = Tree.buildOutput(rootLeaf, optimalLeaf);

        assertTrue(result.isOk());
        assertEquals("rootPreOptim", result.getPreOptimVariantId());
        assertEquals("leafPostOptim", result.getPostOptimVariantId());
    }

    @Test
    public void brokenRootSearchTest() {
        Network network = Mockito.mock(Network.class);
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        Mockito.when(network.getVariantManager()).thenReturn(variantManager);
        RaoResult result;
        try {
            result = Tree.search(network, Mockito.mock(Crac.class), "", Mockito.mock(RaoParameters.class)).get();
            assertEquals(RaoResult.Status.FAILURE, result.getStatus());
        } catch (Exception e) {
            throw new AssertionError();
        }
    }
}
