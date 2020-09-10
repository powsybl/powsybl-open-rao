/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.ActionType;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.Topology;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.rao_commons.*;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({RaoUtil.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class LeafTest {

    private static final String INITIAL_VARIANT_ID = "initial-variant-ID";

    private NetworkAction na1;
    private NetworkAction na2;

    private Network network;
    private SimpleCrac crac;
    private RaoData raoData;
    private RaoData raoDataMock;
    private RaoParameters raoParameters;
    private SystematicSensitivityComputation systematicSensitivityComputation;
    private IteratingLinearOptimizer iteratingLinearOptimizer;

    @Before
    public void setUp() {
        // network
        network = NetworkImportsUtil.import12NodesNetwork();

        // other mocks
        crac = CommonCracCreation.create();
        na1 = new Topology("topology1", crac.getNetworkElement("BBE2AA1  FFR3AA1  1"), ActionType.OPEN);
        na2 = new Topology("topology2", crac.getNetworkElement("FFR2AA1  DDE3AA1  1"), ActionType.OPEN);
        crac.addNetworkAction(na1);
        crac.addNetworkAction(na2);

        RaoInput.cleanCrac(crac, network);
        RaoInput.synchronize(crac, network);
        raoData = Mockito.spy(new RaoData(network, crac));
        RaoDataManager spiedRaoDataManager = Mockito.spy(raoData.getRaoDataManager());
        Mockito.when(raoData.getRaoDataManager()).thenReturn(spiedRaoDataManager);
        Mockito.doNothing().when(spiedRaoDataManager).fillCracResultsWithSensis(anyDouble(), anyDouble());

        raoDataMock = Mockito.mock(RaoData.class);
        Mockito.when(raoDataMock.getInitialVariantId()).thenReturn(INITIAL_VARIANT_ID);
        Mockito.when(raoDataMock.hasSensitivityValues()).thenReturn(true);

        systematicSensitivityComputation = Mockito.mock(SystematicSensitivityComputation.class);
        iteratingLinearOptimizer = Mockito.mock(IteratingLinearOptimizer.class);

        // rao parameters
        raoParameters = new RaoParameters();
        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        raoParameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);

        mockRaoUtil();
    }

    private void mockRaoUtil() {
        PowerMockito.mockStatic(RaoUtil.class);
        ObjectiveFunctionEvaluator costEvaluator = Mockito.mock(ObjectiveFunctionEvaluator.class);
        Mockito.when(costEvaluator.getCost(raoData)).thenAnswer(invocationOnMock -> 0.);
        Mockito.when(RaoUtil.createObjectiveFunction(raoParameters)).thenAnswer(invocationOnMock -> costEvaluator);
    }

    @Test
    public void testRootLeafDefinition() {
        Leaf rootLeaf = new Leaf(raoDataMock, raoParameters);
        assertTrue(rootLeaf.getNetworkActions().isEmpty());
        assertTrue(rootLeaf.isRoot());
        assertEquals(INITIAL_VARIANT_ID, rootLeaf.getInitialVariantId());
    }

    @Test
    public void testRootLeafDefinitionWithSensitivityValues() {
        Leaf rootLeaf = new Leaf(raoDataMock, raoParameters);
        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
    }

    @Test
    public void testRootLeafDefinitionWithoutSensitivityValues() {
        Mockito.when(raoDataMock.hasSensitivityValues()).thenReturn(false);
        Leaf rootLeaf = new Leaf(raoDataMock, raoParameters);
        assertEquals(Leaf.Status.CREATED, rootLeaf.getStatus());
    }

    @Test
    public void testLeafDefinition() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters);
        Leaf leaf = new Leaf(rootLeaf, na1, network, raoParameters);
        assertEquals(1, leaf.getNetworkActions().size());
        assertTrue(leaf.getNetworkActions().contains(na1));
        assertFalse(leaf.isRoot());
        assertEquals(Leaf.Status.CREATED, leaf.getStatus());
    }

    @Test
    public void testMultipleLeafDefinition() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters);
        Leaf leaf1 = new Leaf(rootLeaf, na1, network, raoParameters);
        Leaf leaf2 = new Leaf(leaf1, na2, network, raoParameters);
        assertEquals(2, leaf2.getNetworkActions().size());
        assertTrue(leaf2.getNetworkActions().contains(na1));
        assertTrue(leaf2.getNetworkActions().contains(na2));
        assertFalse(leaf2.isRoot());
    }

    @Test
    public void testMultipleLeafDefinitionWithSameNetworkAction() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters);
        Leaf leaf1 = new Leaf(rootLeaf, na1, network, raoParameters);
        Leaf leaf2 = new Leaf(leaf1, na1, network, raoParameters);
        assertEquals(1, leaf2.getNetworkActions().size());
        assertTrue(leaf2.getNetworkActions().contains(na1));
        assertFalse(leaf2.isRoot());
    }

    @Test
    public void testBloom() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters);
        Set<NetworkAction> networkActions = rootLeaf.bloom();
        assertEquals(2, networkActions.size());
        assertTrue(networkActions.contains(na1));
        assertTrue(networkActions.contains(na2));
    }

    @Test
    public void testEvaluateOk() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters, systematicSensitivityComputation);
        Mockito.doAnswer(invocationOnMock -> {
            raoData.setSystematicSensitivityAnalysisResult(Mockito.mock(SystematicSensitivityAnalysisResult.class));
            return null;
        }).when(systematicSensitivityComputation).run(eq(raoData), any());
        rootLeaf.evaluate();
        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
        assertTrue(rootLeaf.getRaoData().hasSensitivityValues());
    }

    @Test
    public void testEvaluateError() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters, systematicSensitivityComputation);
        Mockito.doThrow(new FaraoException()).when(systematicSensitivityComputation).run(eq(raoData), any());
        rootLeaf.evaluate();
        assertEquals(Leaf.Status.ERROR, rootLeaf.getStatus());
        assertFalse(rootLeaf.getRaoData().hasSensitivityValues());
    }

    @Test
    public void testOptimizeWithoutEvaluation() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters);
        rootLeaf.optimize();
        assertEquals(Leaf.Status.CREATED, rootLeaf.getStatus());
    }

    @Test
    public void testOptimizeWithoutRangeActions() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters, systematicSensitivityComputation);
        Mockito.doNothing().when(systematicSensitivityComputation).run(eq(raoData), any());
        rootLeaf.evaluate();
        rootLeaf.optimize();
        assertEquals(rootLeaf.getInitialVariantId(), rootLeaf.getBestVariantId());
        assertEquals(Leaf.Status.OPTIMIZED, rootLeaf.getStatus());
    }

    @Test
    public void testOptimizeWithRangeActions() {
        crac.addRangeAction(new PstWithRange("pst", new NetworkElement("test")));
        Mockito.when(iteratingLinearOptimizer.optimize(any())).thenReturn("successful");
        Leaf rootLeaf = new Leaf(raoData, raoParameters, systematicSensitivityComputation, iteratingLinearOptimizer);
        Mockito.doNothing().when(systematicSensitivityComputation).run(eq(raoData), any());
        rootLeaf.evaluate();
        rootLeaf.optimize();
        assertEquals("successful", rootLeaf.getBestVariantId());
        assertEquals(Leaf.Status.OPTIMIZED, rootLeaf.getStatus());
    }

    @Test
    public void testClearAllVariantsExceptInitialOne() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters, systematicSensitivityComputation, iteratingLinearOptimizer);
        String initialVariantId = rootLeaf.getInitialVariantId();
        rootLeaf.getRaoData().cloneWorkingVariant();
        rootLeaf.getRaoData().cloneWorkingVariant();
        rootLeaf.getRaoData().cloneWorkingVariant();
        rootLeaf.clearAllVariantsExceptInitialOne();
        assertEquals(1, rootLeaf.getRaoData().getVariantIds().size());
        assertEquals(initialVariantId, rootLeaf.getRaoData().getVariantIds().get(0));
    }
}
