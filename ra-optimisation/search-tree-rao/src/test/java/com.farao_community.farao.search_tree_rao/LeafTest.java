/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.CountryBoundary;
import com.farao_community.farao.commons.CountryGraph;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.ActionType;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.NetworkActionImpl;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.TopologicalActionImpl;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstRangeActionImpl;
import com.farao_community.farao.data.crac_impl.usage_rule.OnStateImpl;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.data.crac_util.CracCleaner;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_commons.*;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerOutput;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest({RaoUtil.class, SystematicSensitivityInterface.class, PrePerimeterSensitivityAnalysis.class, IteratingLinearOptimizer.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class LeafTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private static final String INITIAL_VARIANT_ID = "initial-variant-ID";
    private static final String PREPERIMETER_VARIANT_ID = "preperimeter-variant-ID";

    private NetworkAction na1;
    private NetworkAction na2;

    private Network network;
    private SimpleCrac crac;
    private RaoData raoData;
    private RaoData raoDataMock;
    private RaoParameters raoParameters;
    private TreeParameters treeParameters;
    private LinearOptimizerParameters linearOptimizerParameters;
    private IteratingLinearOptimizer iteratingLinearOptimizer;
    private ObjectiveFunctionEvaluator costEvaluatorMock;

    private SystematicSensitivityInterface systematicSensitivityInterface;
    private SystematicSensitivityResult systematicSensitivityResult;
    private SensitivityAndLoopflowResults sensitivityAndLoopflowResults;
    private SystematicSensitivityInterface.SystematicSensitivityInterfaceBuilder sensitivityBuilder;

    @Before
    public void setUp() {
        // network
        network = NetworkImportsUtil.import12NodesNetwork();

        // other mocks
        crac = CommonCracCreation.create();
        na1 = new NetworkActionImpl(
            "topology1",
            "topology1",
            "fr",
            Collections.singletonList(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState())),
            Collections.singleton(new TopologicalActionImpl(crac.getNetworkElement("BBE2AA1  FFR3AA1  1"), ActionType.OPEN)));

        na2 = new NetworkActionImpl(
            "topology2",
            "topology2",
            "fr",
            Collections.singletonList(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState())),
            Collections.singleton(new TopologicalActionImpl(crac.getNetworkElement("FFR2AA1  DDE3AA1  1"), ActionType.OPEN)));

        crac.addNetworkAction(na1);
        crac.addNetworkAction(na2);

        // rao parameters
        raoParameters = new RaoParameters();
        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        raoParameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        treeParameters = TreeParameters.buildForPreventivePerimeter(searchTreeRaoParameters);

        CnecResults initialCnecResults = new CnecResults();
        initialCnecResults.setCommercialFlowsInMW(new HashMap<>());
        initialCnecResults.setLoopflowThresholdInMW(new HashMap<>());
        initialCnecResults.setLoopflowsInMW(new HashMap<>());
        initialCnecResults.setFlowsInMW(new HashMap<>());
        initialCnecResults.setFlowsInA(new HashMap<>());
        initialCnecResults.setAbsolutePtdfSums(new HashMap<>());

        CracCleaner cracCleaner = new CracCleaner();
        cracCleaner.cleanCrac(crac, network);
        RaoInputHelper.synchronize(crac, network);
        raoData = Mockito.spy(new RaoData(network, crac, crac.getPreventiveState(),
                Collections.singleton(crac.getPreventiveState()), null, null, null, raoParameters));
        CracResultManager spiedCracResultManager = Mockito.spy(raoData.getCracResultManager());
        Mockito.when(raoData.getCracResultManager()).thenReturn(spiedCracResultManager);
        Mockito.doReturn(initialCnecResults).when(raoData).getInitialCnecResults();
        Mockito.doReturn(new HashMap<>()).when(raoData).getPrePerimeterMarginsInAbsoluteMW();
        Mockito.doNothing().when(spiedCracResultManager).fillCnecResultWithFlows();

        raoDataMock = Mockito.mock(RaoData.class);
        Mockito.when(raoDataMock.getPreOptimVariantId()).thenReturn(INITIAL_VARIANT_ID);
        Mockito.when(raoDataMock.getCrac()).thenReturn(crac);
        Mockito.when(raoDataMock.hasSensitivityValues()).thenReturn(true);
        crac.getExtension(ResultVariantManager.class).createVariant(PREPERIMETER_VARIANT_ID);
        crac.getExtension(ResultVariantManager.class).setPrePerimeterVariantId(PREPERIMETER_VARIANT_ID);

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
            PowerMockito.mockStatic(IteratingLinearOptimizer.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        costEvaluatorMock = Mockito.mock(ObjectiveFunctionEvaluator.class);
        Mockito.when(costEvaluatorMock.computeCost(any())).thenAnswer(invocationOnMock -> 0.);
        Mockito.when(costEvaluatorMock.computeFunctionalCost(any())).thenAnswer(invocationOnMock -> 0.);
        Mockito.when(costEvaluatorMock.computeVirtualCost(any())).thenAnswer(invocationOnMock -> 0.);
        Mockito.when(RaoUtil.createObjectiveFunction(raoData, linearOptimizerParameters, raoParameters.getFallbackOverCost())).thenAnswer(invocationOnMock -> costEvaluatorMock);
    }

    private void mockSensitivityComputation() {
        Mockito.when(raoDataMock.getSystematicSensitivityResult()).thenReturn(systematicSensitivityResult);
        Mockito.when(systematicSensitivityResult.isSuccess()).thenReturn(true);
        Mockito.when(systematicSensitivityResult.getReferenceFlow(any())).thenReturn(5.);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(any())).thenReturn(12.);
        Mockito.doAnswer(invocationOnMock -> {
            raoData.setSystematicSensitivityResult(systematicSensitivityResult);
            return systematicSensitivityResult;
        }).when(systematicSensitivityInterface).run(any());
    }

    private RangeAction addPst() {
        NetworkElement pstElement = new NetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name");
        PstRangeActionImpl pstRangeAction = new PstRangeActionImpl("pst", pstElement);
        pstRangeAction.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState()));
        crac.addRangeAction(pstRangeAction);
        crac.desynchronize();
        crac.synchronize(network);
        return pstRangeAction;
    }

    @Test
    public void testRootLeafDefinition() {
        Leaf rootLeaf = new Leaf(raoDataMock, raoParameters, treeParameters, linearOptimizerParameters);
        assertTrue(rootLeaf.getNetworkActions().isEmpty());
        assertTrue(rootLeaf.isRoot());
        assertEquals(INITIAL_VARIANT_ID, rootLeaf.getPreOptimVariantId());
    }

    @Test
    public void testRootLeafDefinitionWithSensitivityValues() {
        Leaf rootLeaf = new Leaf(raoDataMock, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
    }

    @Test
    public void testRootLeafDefinitionWithoutSensitivityValues() {
        Mockito.when(raoDataMock.hasSensitivityValues()).thenReturn(false);
        Leaf rootLeaf = new Leaf(raoDataMock, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(Leaf.Status.CREATED, rootLeaf.getStatus());
    }

    @Test
    public void testLeafDefinition() {
        crac.getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setAbsolutePtdfSum(0.5);
        crac.getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setAbsolutePtdfSum(0.4);
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        Leaf leaf = new Leaf(rootLeaf, na1, network, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(1, leaf.getNetworkActions().size());
        assertTrue(leaf.getNetworkActions().contains(na1));
        assertFalse(leaf.isRoot());
        assertEquals(Leaf.Status.CREATED, leaf.getStatus());
        assertEquals(0.5, leaf.getRaoData().getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).getAbsolutePtdfSum(), DOUBLE_TOLERANCE);
        assertEquals(0.4, leaf.getRaoData().getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).getAbsolutePtdfSum(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testMultipleLeafDefinition() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        Leaf leaf1 = new Leaf(rootLeaf, na1, network, raoParameters, treeParameters, linearOptimizerParameters);
        Leaf leaf2 = new Leaf(leaf1, na2, network, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(2, leaf2.getNetworkActions().size());
        assertTrue(leaf2.getNetworkActions().contains(na1));
        assertTrue(leaf2.getNetworkActions().contains(na2));
        assertFalse(leaf2.isRoot());
    }

    @Test
    public void testMultipleLeafDefinitionWithSameNetworkAction() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        Leaf leaf1 = new Leaf(rootLeaf, na1, network, raoParameters, treeParameters, linearOptimizerParameters);
        Leaf leaf2 = new Leaf(leaf1, na1, network, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(1, leaf2.getNetworkActions().size());
        assertTrue(leaf2.getNetworkActions().contains(na1));
        assertFalse(leaf2.isRoot());
    }

    @Test
    public void testBloom() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        Set<NetworkAction> networkActions = rootLeaf.bloom();
        assertEquals(2, networkActions.size());
        assertTrue(networkActions.contains(na1));
        assertTrue(networkActions.contains(na2));
    }

    @Test
    public void testEvaluateOk() {
        mockSensitivityComputation();
        mockRaoUtil();

        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        rootLeaf.evaluate();

        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
        assertTrue(rootLeaf.getRaoData().hasSensitivityValues());
        assertEquals(5., crac.getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(PREPERIMETER_VARIANT_ID).getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(12., crac.getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(PREPERIMETER_VARIANT_ID).getFlowInA(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testReevaluate() {
        mockSensitivityComputation();
        mockRaoUtil();

        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        rootLeaf.evaluate();
        double bestCost = rootLeaf.getBestCost();

        rootLeaf.evaluate();
        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
        assertEquals(bestCost, rootLeaf.getBestCost(), DOUBLE_TOLERANCE);

        Mockito.when(costEvaluatorMock.computeFunctionalCost(any())).thenAnswer(invocationOnMock -> 10.);
        Mockito.when(costEvaluatorMock.computeVirtualCost(any())).thenAnswer(invocationOnMock -> 2.);
        rootLeaf.evaluate();
        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
        assertEquals(12, rootLeaf.getBestCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testEvaluateError() {
        Mockito.when(systematicSensitivityResult.isSuccess()).thenReturn(false);
        Mockito.doThrow(new SensitivityAnalysisException("mock")).when(systematicSensitivityInterface).run(any());

        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
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

        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        rootLeaf.evaluate();

        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
        assertTrue(rootLeaf.getRaoData().hasSensitivityValues());
        assertEquals(-5, cnec1result.getLoopflowInMW(), DOUBLE_TOLERANCE);
        assertEquals(30, cnec2result.getLoopflowInMW(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testOptimizeWithoutEvaluation() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        rootLeaf.optimize();
        assertEquals(Leaf.Status.CREATED, rootLeaf.getStatus());
    }

    @Test
    public void testOptimizeWithoutRangeActions() {
        mockSensitivityComputation();
        mockRaoUtil();
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        rootLeaf.evaluate();
        rootLeaf.optimize();
        assertEquals(rootLeaf.getPreOptimVariantId(), rootLeaf.getBestVariantId());
        assertEquals(Leaf.Status.OPTIMIZED, rootLeaf.getStatus());
    }

    @Test
    public void testOptimizeWithRangeActions() {
        addPst();

        String newVariant = raoData.getCracVariantManager().cloneWorkingVariant();
        PowerMockito.mockStatic(IteratingLinearOptimizer.class);
        IteratingLinearOptimizerOutput iteratingLinearOptimizerOutput = Mockito.mock(IteratingLinearOptimizerOutput.class);

        PowerMockito.when(IteratingLinearOptimizer.optimize(any(), any(), anyDouble())).thenAnswer(invocationOnMock -> iteratingLinearOptimizerOutput);
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        Mockito.doAnswer(invocationOnMock -> systematicSensitivityResult).when(systematicSensitivityInterface).run(any());

        mockRaoUtil();

        rootLeaf.evaluate();
        rootLeaf.optimize();
        assertEquals(newVariant, rootLeaf.getBestVariantId());
        assertEquals(Leaf.Status.OPTIMIZED, rootLeaf.getStatus());
    }

    @Test
    public void testClearAllVariantsExceptInitialOne() {
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
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

        HashSet<CountryBoundary> boundaries = new HashSet<>();
        boundaries.add(new CountryBoundary(Country.FR, Country.BE));
        boundaries.add(new CountryBoundary(Country.FR, Country.DE));
        boundaries.add(new CountryBoundary(Country.DE, Country.AT));
        CountryGraph countryGraph = new CountryGraph(boundaries);

        raoParameters.getExtension(SearchTreeRaoParameters.class).setMaxNumberOfBoundariesForSkippingNetworkActions(0);
        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        assertTrue(rootLeaf.isNetworkActionCloseToLocations(na1, Set.of(Optional.empty()), countryGraph));
        assertTrue(rootLeaf.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.FR)), countryGraph));
        assertTrue(rootLeaf.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.BE)), countryGraph));
        assertFalse(rootLeaf.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.DE)), countryGraph));
        raoParameters.getExtension(SearchTreeRaoParameters.class).setMaxNumberOfBoundariesForSkippingNetworkActions(1);
        assertTrue(rootLeaf.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.DE)), countryGraph));
        assertFalse(rootLeaf.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.AT)), countryGraph));
        raoParameters.getExtension(SearchTreeRaoParameters.class).setMaxNumberOfBoundariesForSkippingNetworkActions(2);
        assertTrue(rootLeaf.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.AT)), countryGraph));

        mockRaoUtil();
        NetworkAction mockedNa = Mockito.mock(NetworkAction.class);
        Mockito.when(mockedNa.getLocation(Mockito.any())).thenAnswer(invocationOnMock -> Set.of(Optional.of(Country.FR), Optional.empty()));
        raoParameters.getExtension(SearchTreeRaoParameters.class).setMaxNumberOfBoundariesForSkippingNetworkActions(0);
        assertTrue(rootLeaf.isNetworkActionCloseToLocations(mockedNa, Set.of(Optional.of(Country.AT)), countryGraph));
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
        Mockito.when(iteratingLinearOptimizer.optimize(any(), any(), anyDouble())).thenAnswer(invocationOnMock -> mockPostCurativeVariantId);

        crac.getExtension(ResultVariantManager.class).createVariant(PREPERIMETER_VARIANT_ID);
        crac.getExtension(ResultVariantManager.class).setPrePerimeterVariantId(PREPERIMETER_VARIANT_ID);

        Leaf rootLeaf = new Leaf(curativeRaoData, raoParameters, treeParameters, linearOptimizerParameters);
        rootLeaf.evaluate();
        rootLeaf.optimize();
        curativeRaoData.getCracVariantManager().setWorkingVariant(mockPostCurativeVariantId);

        rootLeaf.clearAllVariantsExceptOptimizedOne();
        assertEquals(1, curativeRaoData.getCracVariantManager().getVariantIds().size());
        assertEquals(mockPostCurativeVariantId, curativeRaoData.getCracVariantManager().getVariantIds().get(0));
    }

    @Test
    public void testRemoveNetworkActionsIfMaxNumberReached() {
        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        Set<NetworkAction> networkActionsToFilter = raoData.getAvailableNetworkActions();
        Leaf rootLeaf;
        Leaf childLeaf1;
        Leaf childLeaf2;

        // no filter
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(Map.of("be", 0));
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(2, rootLeaf.removeNetworkActionsIfMaxNumberReached(networkActionsToFilter).size());
        childLeaf1 = new Leaf(rootLeaf, na1, network, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(2, childLeaf1.removeNetworkActionsIfMaxNumberReached(networkActionsToFilter).size());
        childLeaf2 = new Leaf(childLeaf1, na2, network, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(2, childLeaf2.removeNetworkActionsIfMaxNumberReached(networkActionsToFilter).size());

        // no filter
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(Map.of("fr", 3));
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(2, rootLeaf.removeNetworkActionsIfMaxNumberReached(networkActionsToFilter).size());
        childLeaf1 = new Leaf(rootLeaf, na1, network, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(2, childLeaf1.removeNetworkActionsIfMaxNumberReached(networkActionsToFilter).size());
        childLeaf2 = new Leaf(childLeaf1, na2, network, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(2, childLeaf2.removeNetworkActionsIfMaxNumberReached(networkActionsToFilter).size());

        // keep 2 network actions
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(Map.of("fr", 2));
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(2, rootLeaf.removeNetworkActionsIfMaxNumberReached(networkActionsToFilter).size());
        childLeaf1 = new Leaf(rootLeaf, na1, network, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(2, childLeaf1.removeNetworkActionsIfMaxNumberReached(networkActionsToFilter).size());
        childLeaf2 = new Leaf(childLeaf1, na2, network, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(0, childLeaf2.removeNetworkActionsIfMaxNumberReached(networkActionsToFilter).size());

        // keep 1 network action
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(Map.of("fr", 1));
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(2, rootLeaf.removeNetworkActionsIfMaxNumberReached(networkActionsToFilter).size());
        childLeaf1 = new Leaf(rootLeaf, na1, network, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(0, childLeaf1.removeNetworkActionsIfMaxNumberReached(networkActionsToFilter).size());

        // filter out all topo
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(Map.of("fr", 0));
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        assertEquals(0, rootLeaf.removeNetworkActionsIfMaxNumberReached(networkActionsToFilter).size());
    }

    @Test
    public void testGetMaxPstPerTso() {
        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        Leaf rootLeaf;
        Leaf childLeaf1;
        Leaf childLeaf2;
        Leaf childLeaf3;
        Map<String, Integer> maxPstPerTso;

        // no filter
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        assertTrue(rootLeaf.getMaxPstPerTso().isEmpty());

        // only max pst parameter
        searchTreeRaoParameters.setMaxCurativeRaPerTso(null);
        searchTreeRaoParameters.setMaxCurativePstPerTso(Map.of("fr", 9));
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        maxPstPerTso = rootLeaf.getMaxPstPerTso();
        assertEquals(1, maxPstPerTso.size());
        assertEquals(9, (int) maxPstPerTso.getOrDefault("fr", 0));

        // only max cra parameter
        searchTreeRaoParameters.setMaxCurativeRaPerTso(Map.of("fr", 76));
        searchTreeRaoParameters.setMaxCurativePstPerTso(null);
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        maxPstPerTso = rootLeaf.getMaxPstPerTso();
        assertEquals(1, maxPstPerTso.size());
        assertEquals(76, (int) maxPstPerTso.getOrDefault("fr", 0));

        // two parameters, no network action 1
        searchTreeRaoParameters.setMaxCurativePstPerTso(Map.of("fr", 9));
        searchTreeRaoParameters.setMaxCurativeRaPerTso(Map.of("fr", 76));
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        maxPstPerTso = rootLeaf.getMaxPstPerTso();
        assertEquals(1, maxPstPerTso.size());
        assertEquals(9, (int) maxPstPerTso.getOrDefault("fr", 0));

        // two parameters, no network action 2
        searchTreeRaoParameters.setMaxCurativePstPerTso(Map.of("fr", 90));
        searchTreeRaoParameters.setMaxCurativeRaPerTso(Map.of("fr", 67));
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        maxPstPerTso = rootLeaf.getMaxPstPerTso();
        assertEquals(1, maxPstPerTso.size());
        assertEquals(67, (int) maxPstPerTso.getOrDefault("fr", 0));

        // two parameters, network actions used
        searchTreeRaoParameters.setMaxCurativePstPerTso(Map.of("fr", 5));
        searchTreeRaoParameters.setMaxCurativeRaPerTso(Map.of("fr", 5));
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        maxPstPerTso = rootLeaf.getMaxPstPerTso();
        assertEquals(1, maxPstPerTso.size());
        assertEquals(5, (int) maxPstPerTso.getOrDefault("fr", 0));
        childLeaf1 = new Leaf(rootLeaf, na1, network, raoParameters, treeParameters, linearOptimizerParameters);
        maxPstPerTso = childLeaf1.getMaxPstPerTso();
        assertEquals(1, maxPstPerTso.size());
        assertEquals(4, (int) maxPstPerTso.getOrDefault("fr", 0));
        childLeaf2 = new Leaf(childLeaf1, na2, network, raoParameters, treeParameters, linearOptimizerParameters);
        maxPstPerTso = childLeaf2.getMaxPstPerTso();
        assertEquals(1, maxPstPerTso.size());
        assertEquals(3, (int) maxPstPerTso.getOrDefault("fr", 0));
        childLeaf3 = new Leaf(childLeaf2, na2, network, raoParameters, treeParameters, linearOptimizerParameters);
        maxPstPerTso = childLeaf3.getMaxPstPerTso();
        assertEquals(1, maxPstPerTso.size());
        assertEquals(3, (int) maxPstPerTso.getOrDefault("fr", 0));
    }

    @Test
    public void testIsRangeActionActivated() {
        RangeAction rangeAction = addPst();

        RaoData raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, raoParameters);
        String preOptimVariantId = raoData.getPreOptimVariantId();
        raoData.getCrac().getExtension(ResultVariantManager.class).setPrePerimeterVariantId(preOptimVariantId);
        String workingVariantId = raoData.getCracVariantManager().cloneWorkingVariant();
        raoData.getCracVariantManager().setWorkingVariant(workingVariantId);

        Leaf rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);

        rangeAction.getExtension(RangeActionResultExtension.class).getVariant(workingVariantId).setSetPoint(crac.getPreventiveState().getId(), 0.0);
        assertFalse(rootLeaf.isRangeActionActivated(rangeAction));

        rangeAction.getExtension(RangeActionResultExtension.class).getVariant(workingVariantId).setSetPoint(crac.getPreventiveState().getId(), 10.0);
        assertTrue(rootLeaf.isRangeActionActivated(rangeAction));

        rangeAction.getExtension(RangeActionResultExtension.class).getVariant(workingVariantId).setSetPoint(crac.getPreventiveState().getId(), Double.NaN);
        assertFalse(rootLeaf.isRangeActionActivated(rangeAction));

        rangeAction.getExtension(RangeActionResultExtension.class).getVariant(workingVariantId).setSetPoint(crac.getPreventiveState().getId(), 0.0);
        rangeAction.getExtension(RangeActionResultExtension.class).getVariant(preOptimVariantId).setSetPoint(crac.getPreventiveState().getId(), Double.NaN);
        assertTrue(rootLeaf.isRangeActionActivated(rangeAction));
    }

    @Test
    public void testGetMaxTopoPerTso() {
        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        Leaf rootLeaf;
        Leaf childLeaf1;
        Map<String, Integer> maxTopoPerTso;

        RangeAction rangeAction = addPst();
        RaoData raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, raoParameters);
        String preOptimVariantId = raoData.getPreOptimVariantId();
        raoData.getCrac().getExtension(ResultVariantManager.class).setPrePerimeterVariantId(preOptimVariantId);
        String workingVariantId = raoData.getCracVariantManager().cloneWorkingVariant();
        raoData.getCracVariantManager().setWorkingVariant(workingVariantId);
        rangeAction.getExtension(RangeActionResultExtension.class).getVariant(workingVariantId).setSetPoint(crac.getPreventiveState().getId(), 0.0);

        // no filter
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        assertTrue(rootLeaf.getMaxTopoPerTso().isEmpty());

        // only max topo parameter
        searchTreeRaoParameters.setMaxCurativeRaPerTso(null);
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(Map.of("fr", 9));
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        maxTopoPerTso = rootLeaf.getMaxTopoPerTso();
        assertEquals(1, maxTopoPerTso.size());
        assertEquals(9, (int) maxTopoPerTso.getOrDefault("fr", 0));

        // only max cra parameter
        searchTreeRaoParameters.setMaxCurativeRaPerTso(Map.of("fr", 76));
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(null);
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        maxTopoPerTso = rootLeaf.getMaxTopoPerTso();
        assertEquals(1, maxTopoPerTso.size());
        assertEquals(76, (int) maxTopoPerTso.getOrDefault("fr", 0));

        // two parameters, no range action 1
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(Map.of("fr", 9));
        searchTreeRaoParameters.setMaxCurativeRaPerTso(Map.of("fr", 76));
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        maxTopoPerTso = rootLeaf.getMaxTopoPerTso();
        assertEquals(1, maxTopoPerTso.size());
        assertEquals(9, (int) maxTopoPerTso.getOrDefault("fr", 0));

        // two parameters, no range action 2
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(Map.of("fr", 90));
        searchTreeRaoParameters.setMaxCurativeRaPerTso(Map.of("fr", 67));
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        maxTopoPerTso = rootLeaf.getMaxTopoPerTso();
        assertEquals(1, maxTopoPerTso.size());
        assertEquals(67, (int) maxTopoPerTso.getOrDefault("fr", 0));

        // two parameters, range action is used
        rangeAction.getExtension(RangeActionResultExtension.class).getVariant(preOptimVariantId).setSetPoint(crac.getPreventiveState().getId(), 10.0);
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(Map.of("fr", 5));
        searchTreeRaoParameters.setMaxCurativeRaPerTso(Map.of("fr", 5));
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, .0);
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters, linearOptimizerParameters);
        maxTopoPerTso = rootLeaf.getMaxTopoPerTso();
        assertEquals(1, maxTopoPerTso.size());
        assertEquals(4, (int) maxTopoPerTso.getOrDefault("fr", 0));
        childLeaf1 = new Leaf(rootLeaf, na1, network, raoParameters, treeParameters, linearOptimizerParameters);
        maxTopoPerTso = childLeaf1.getMaxTopoPerTso();
        assertEquals(1, maxTopoPerTso.size());
        assertEquals(4, (int) maxTopoPerTso.getOrDefault("fr", 0));
    }
}
