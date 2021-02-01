/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.ActionType;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.Topology;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.usage_rule.OnStateImpl;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_util.CracCleaner;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.*;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({RaoUtil.class, SystematicSensitivityInterface.class, InitialSensitivityAnalysis.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class LeafTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private static final String INITIAL_VARIANT_ID = "initial-variant-ID";

    private NetworkAction na1;
    private NetworkAction na2;

    private Network network;
    private SimpleCrac crac;
    private RaoData raoData;
    private RaoData raoDataMock;
    private RaoParameters raoParameters;
    private TreeParameters treeParameters;
    private IteratingLinearOptimizer iteratingLinearOptimizer;
    ObjectiveFunctionEvaluator costEvaluatorMock;

    private SystematicSensitivityInterface systematicSensitivityInterface;
    private SystematicSensitivityResult systematicSensitivityResult;
    private SystematicSensitivityInterface.SystematicSensitivityInterfaceBuilder sensitivityBuilder;

    @Before
    public void setUp() {
        // network
        network = NetworkImportsUtil.import12NodesNetwork();

        // other mocks
        crac = CommonCracCreation.create();
        na1 = new Topology("topology1", crac.getNetworkElement("BBE2AA1  FFR3AA1  1"), ActionType.OPEN);
        na1.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState()));
        na2 = new Topology("topology2", crac.getNetworkElement("FFR2AA1  DDE3AA1  1"), ActionType.OPEN);
        na2.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState()));
        crac.addNetworkAction(na1);
        crac.addNetworkAction(na2);

        // rao parameters
        raoParameters = new RaoParameters();
        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        raoParameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        treeParameters = TreeParameters.buildForPreventivePerimeter(searchTreeRaoParameters);

        CracCleaner cracCleaner = new CracCleaner();
        cracCleaner.cleanCrac(crac, network);
        RaoInputHelper.synchronize(crac, network);
        raoData = Mockito.spy(new RaoData(network, crac, crac.getPreventiveState(),
                Collections.singleton(crac.getPreventiveState()), null, null, null, raoParameters));
        CracResultManager spiedCracResultManager = Mockito.spy(raoData.getCracResultManager());
        Mockito.when(raoData.getCracResultManager()).thenReturn(spiedCracResultManager);
        Mockito.doNothing().when(spiedCracResultManager).fillCnecResultWithFlows();

        raoDataMock = Mockito.mock(RaoData.class);
        Mockito.when(raoDataMock.getPreOptimVariantId()).thenReturn(INITIAL_VARIANT_ID);
        Mockito.when(raoDataMock.hasSensitivityValues()).thenReturn(true);

        systematicSensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        iteratingLinearOptimizer = Mockito.mock(IteratingLinearOptimizer.class);
        sensitivityBuilder = Mockito.mock(SystematicSensitivityInterface.SystematicSensitivityInterfaceBuilder.class);
        Mockito.when(sensitivityBuilder.build()).thenReturn(systematicSensitivityInterface);
        Mockito.when(sensitivityBuilder.withDefaultParameters(any())).thenReturn(sensitivityBuilder);
        Mockito.when(sensitivityBuilder.withFallbackParameters(any())).thenReturn(sensitivityBuilder);
        Mockito.when(sensitivityBuilder.withRangeActionSensitivities(any(), any(), any())).thenReturn(sensitivityBuilder);
        Mockito.when(sensitivityBuilder.withSensitivityProvider(any())).thenReturn(sensitivityBuilder);
        Mockito.when(sensitivityBuilder.withPtdfSensitivities(any(), any(), any())).thenReturn(sensitivityBuilder);

        try {
            PowerMockito.mockStatic(SystematicSensitivityInterface.class);
            PowerMockito.when(SystematicSensitivityInterface.builder()).thenAnswer(invocationOnMock -> sensitivityBuilder);
        } catch (Exception e) {
            e.printStackTrace();
        }
        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
    }

    private void mockRaoUtil() {
        try {
            PowerMockito.mockStatic(RaoUtil.class);
            PowerMockito.when(RaoUtil.createLinearOptimizer(Mockito.any(), Mockito.any())).thenAnswer(invocationOnMock -> iteratingLinearOptimizer);
            PowerMockito.when(RaoUtil.createSystematicSensitivityInterface(Mockito.any(), Mockito.any(), anyBoolean())).thenAnswer(invocationOnMock -> systematicSensitivityInterface);
        } catch (Exception e) {
            e.printStackTrace();
        }
        costEvaluatorMock = Mockito.mock(ObjectiveFunctionEvaluator.class);
        Mockito.when(costEvaluatorMock.getCost(raoData)).thenAnswer(invocationOnMock -> 0.);
        Mockito.when(costEvaluatorMock.getFunctionalCost(raoData)).thenAnswer(invocationOnMock -> 0.);
        Mockito.when(costEvaluatorMock.getVirtualCost(raoData)).thenAnswer(invocationOnMock -> 0.);
        Mockito.when(RaoUtil.createObjectiveFunction(raoParameters)).thenAnswer(invocationOnMock -> costEvaluatorMock);
    }

    private void mockSensitivityComputation() {
        Mockito.when(systematicSensitivityResult.isSuccess()).thenReturn(true);
        Mockito.doAnswer(invocationOnMock -> {
            raoData.setSystematicSensitivityResult(systematicSensitivityResult);
            return systematicSensitivityResult;
        }).when(systematicSensitivityInterface).run(any());
    }

    @Test
    public void testRootLeafDefinition() {
        Leaf rootLeaf = new Leaf(raoDataMock, raoParameters, treeParameters);
        assertTrue(rootLeaf.getNetworkActions().isEmpty());
        assertTrue(rootLeaf.isRoot());
        assertEquals(INITIAL_VARIANT_ID, rootLeaf.getPreOptimVariantId());
    }

    @Test
    public void testRootLeafDefinitionWithSensitivityValues() {
        Leaf rootLeaf = new Leaf(raoDataMock, raoParameters, treeParameters);
        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
    }

    @Test
    public void testRootLeafDefinitionWithoutSensitivityValues() {
        Mockito.when(raoDataMock.hasSensitivityValues()).thenReturn(false);
        Leaf rootLeaf = new Leaf(raoDataMock, raoParameters, treeParameters);
        assertEquals(Leaf.Status.CREATED, rootLeaf.getStatus());
    }

    @Test
    public void testLeafDefinition() {
        crac.getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setAbsolutePtdfSum(0.5);
        crac.getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setAbsolutePtdfSum(0.4);
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters);
        Leaf leaf = new Leaf(rootLeaf, na1, network, raoParameters, treeParameters);
        assertEquals(1, leaf.getNetworkActions().size());
        assertTrue(leaf.getNetworkActions().contains(na1));
        assertFalse(leaf.isRoot());
        assertEquals(Leaf.Status.CREATED, leaf.getStatus());
        assertEquals(0.5, leaf.getRaoData().getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).getAbsolutePtdfSum(), DOUBLE_TOLERANCE);
        assertEquals(0.4, leaf.getRaoData().getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).getAbsolutePtdfSum(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testMultipleLeafDefinition() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters);
        Leaf leaf1 = new Leaf(rootLeaf, na1, network, raoParameters, treeParameters);
        Leaf leaf2 = new Leaf(leaf1, na2, network, raoParameters, treeParameters);
        assertEquals(2, leaf2.getNetworkActions().size());
        assertTrue(leaf2.getNetworkActions().contains(na1));
        assertTrue(leaf2.getNetworkActions().contains(na2));
        assertFalse(leaf2.isRoot());
    }

    @Test
    public void testMultipleLeafDefinitionWithSameNetworkAction() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters);
        Leaf leaf1 = new Leaf(rootLeaf, na1, network, raoParameters, treeParameters);
        Leaf leaf2 = new Leaf(leaf1, na1, network, raoParameters, treeParameters);
        assertEquals(1, leaf2.getNetworkActions().size());
        assertTrue(leaf2.getNetworkActions().contains(na1));
        assertFalse(leaf2.isRoot());
    }

    @Test
    public void testBloom() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters);
        Set<NetworkAction> networkActions = rootLeaf.bloom();
        assertEquals(2, networkActions.size());
        assertTrue(networkActions.contains(na1));
        assertTrue(networkActions.contains(na2));
    }

    @Test
    public void testEvaluateOk() {
        mockSensitivityComputation();
        mockRaoUtil();

        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters);
        rootLeaf.evaluate();

        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
        assertTrue(rootLeaf.getRaoData().hasSensitivityValues());
    }

    @Test
    public void testReevaluate() {
        mockSensitivityComputation();
        mockRaoUtil();

        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters);
        rootLeaf.evaluate();
        double bestCost = rootLeaf.getBestCost();

        rootLeaf.evaluate();
        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
        assertEquals(bestCost, rootLeaf.getBestCost(), DOUBLE_TOLERANCE);

        Mockito.when(costEvaluatorMock.getFunctionalCost(raoData)).thenAnswer(invocationOnMock -> 10.);
        Mockito.when(costEvaluatorMock.getVirtualCost(raoData)).thenAnswer(invocationOnMock -> 2.);
        rootLeaf.evaluate();
        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
        assertEquals(12, rootLeaf.getBestCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testEvaluateError() {
        Mockito.when(systematicSensitivityResult.isSuccess()).thenReturn(false);
        Mockito.doThrow(new SensitivityAnalysisException("mock")).when(systematicSensitivityInterface).run(any());

        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters);
        rootLeaf.evaluate();

        assertEquals(Leaf.Status.ERROR, rootLeaf.getStatus());
        assertFalse(rootLeaf.getRaoData().hasSensitivityValues());
    }

    @Test
    public void testEvaluateWithLoopflows() {
        mockSensitivityComputation();

        raoParameters.setRaoWithLoopFlowLimitation(true);
        for (BranchCnec cnec : crac.getBranchCnecs(crac.getPreventiveState())) {
            CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension(100, Unit.PERCENT_IMAX);
            cnec.addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension);
        }
        raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, raoParameters);

        mockRaoUtil();
        CnecResult cnec1result = crac.getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId());
        CnecResult cnec2result = crac.getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId());
        cnec1result.setCommercialFlowInMW(10.0);
        cnec2result.setCommercialFlowInMW(-25.0);

        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters);
        rootLeaf.evaluate();

        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
        assertTrue(rootLeaf.getRaoData().hasSensitivityValues());
        assertEquals(-10, cnec1result.getLoopflowInMW(), DOUBLE_TOLERANCE);
        assertEquals(25, cnec2result.getLoopflowInMW(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testOptimizeWithoutEvaluation() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters);
        rootLeaf.optimize();
        assertEquals(Leaf.Status.CREATED, rootLeaf.getStatus());
    }

    @Test
    public void testOptimizeWithoutRangeActions() {
        mockRaoUtil();
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters);
        rootLeaf.evaluate();
        rootLeaf.optimize();
        assertEquals(rootLeaf.getPreOptimVariantId(), rootLeaf.getBestVariantId());
        assertEquals(Leaf.Status.OPTIMIZED, rootLeaf.getStatus());
    }

    @Test
    public void testOptimizeWithRangeActions() {
        RangeAction rangeAction = new PstWithRange("pst", new NetworkElement("test"));
        rangeAction.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState()));
        crac.addRangeAction(rangeAction);

        String newVariant = raoData.getCracVariantManager().cloneWorkingVariant();
        Mockito.doAnswer(invocationOnMock -> newVariant).when(iteratingLinearOptimizer).optimize(any());
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters);
        Mockito.doAnswer(invocationOnMock -> systematicSensitivityResult).when(systematicSensitivityInterface).run(any());

        mockRaoUtil();

        rootLeaf.evaluate();
        rootLeaf.optimize();
        assertEquals(newVariant, rootLeaf.getBestVariantId());
        assertEquals(Leaf.Status.OPTIMIZED, rootLeaf.getStatus());
    }

    @Test
    public void testClearAllVariantsExceptInitialOne() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters);
        String initialVariantId = rootLeaf.getPreOptimVariantId();
        rootLeaf.getRaoData().getCracVariantManager().cloneWorkingVariant();
        rootLeaf.getRaoData().getCracVariantManager().cloneWorkingVariant();
        rootLeaf.getRaoData().getCracVariantManager().cloneWorkingVariant();
        rootLeaf.clearAllVariantsExceptInitialOne();
        assertEquals(1, rootLeaf.getRaoData().getCracVariantManager().getVariantIds().size());
        assertEquals(initialVariantId, rootLeaf.getRaoData().getCracVariantManager().getVariantIds().get(0));
    }

    @Test
    public void testIsNetworkActionCloseToLocations() {
        raoParameters.setPtdfBoundariesFromCountryCodes(List.of("FR-DE", "DE-AT"));
        raoParameters.getExtension(SearchTreeRaoParameters.class).setMaxNumberOfBoundariesForSkippingNetworkActions(0);
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters);
        assertTrue(rootLeaf.isNetworkActionCloseToLocations(na1, List.of(Optional.empty())));
        assertTrue(rootLeaf.isNetworkActionCloseToLocations(na1, List.of(Optional.of(Country.FR))));
        assertTrue(rootLeaf.isNetworkActionCloseToLocations(na1, List.of(Optional.of(Country.BE))));
        assertFalse(rootLeaf.isNetworkActionCloseToLocations(na1, List.of(Optional.of(Country.DE))));
        raoParameters.getExtension(SearchTreeRaoParameters.class).setMaxNumberOfBoundariesForSkippingNetworkActions(1);
        assertTrue(rootLeaf.isNetworkActionCloseToLocations(na1, List.of(Optional.of(Country.DE))));
        assertFalse(rootLeaf.isNetworkActionCloseToLocations(na1, List.of(Optional.of(Country.AT))));
        raoParameters.getExtension(SearchTreeRaoParameters.class).setMaxNumberOfBoundariesForSkippingNetworkActions(2);
        assertTrue(rootLeaf.isNetworkActionCloseToLocations(na1, List.of(Optional.of(Country.AT))));

        mockRaoUtil();
        PowerMockito.when(RaoUtil.getNetworkActionLocation(Mockito.any(), Mockito.any())).thenAnswer(invocationOnMock -> List.of(Optional.of(Country.FR), Optional.empty()));
        raoParameters.getExtension(SearchTreeRaoParameters.class).setMaxNumberOfBoundariesForSkippingNetworkActions(0);
        assertTrue(rootLeaf.isNetworkActionCloseToLocations(na1, List.of(Optional.of(Country.AT))));
    }

    @Test
    public void testClearAllVariantsExceptOptimizedOne() {
        mockSensitivityComputation();
        mockRaoUtil();

        crac = CommonCracCreation.createWithPstRange();
        crac.synchronize(network);
        raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, raoParameters);

        String mockPostPreventiveVariantId = raoData.getCracVariantManager().cloneWorkingVariant();
        RaoData curativeRaoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, mockPostPreventiveVariantId, raoParameters);
        String mockPostCurativeVariantId = curativeRaoData.getCracVariantManager().cloneWorkingVariant();
        Mockito.when(iteratingLinearOptimizer.optimize(any())).thenAnswer(invocationOnMock -> mockPostCurativeVariantId);

        Leaf rootLeaf = new Leaf(curativeRaoData, raoParameters, treeParameters);
        rootLeaf.evaluate();
        rootLeaf.optimize();
        curativeRaoData.getCracVariantManager().setWorkingVariant(mockPostCurativeVariantId);

        rootLeaf.clearAllVariantsExceptOptimizedOne();
        assertEquals(1, curativeRaoData.getCracVariantManager().getVariantIds().size());
        assertEquals(mockPostCurativeVariantId, curativeRaoData.getCracVariantManager().getVariantIds().get(0));
    }
}
