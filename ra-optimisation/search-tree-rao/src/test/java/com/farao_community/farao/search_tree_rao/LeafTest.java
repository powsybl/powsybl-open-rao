/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_commons.PrePerimeterSensitivityAnalysis;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.rao_commons.SensitivityComputer;
import com.farao_community.farao.rao_commons.linear_optimisation.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.rao_commons.result_api.*;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@PrepareForTest({RaoUtil.class, SystematicSensitivityInterface.class, PrePerimeterSensitivityAnalysis.class, IteratingLinearOptimizer.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class LeafTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;

    private NetworkAction na1;
    private NetworkAction na2;

    private Network network;
    private IteratingLinearOptimizer iteratingLinearOptimizer;
    private ObjectiveFunction costEvaluatorMock;
    private SensitivityComputer sensitivityComputer;

    private String virtualCostName;

    @Before
    public void setUp() {
        // network
        network = NetworkImportsUtil.import12NodesNetwork();

        // network actions
        na1 = Mockito.mock(NetworkAction.class);
        na2 = Mockito.mock(NetworkAction.class);

        // rao parameters
        RaoParameters raoParameters = new RaoParameters();
        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        raoParameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);

        iteratingLinearOptimizer = Mockito.mock(IteratingLinearOptimizer.class);
        sensitivityComputer = Mockito.mock(SensitivityComputer.class);
        costEvaluatorMock = Mockito.mock(ObjectiveFunction.class);

        virtualCostName = "VirtualCost";
    }

    private Leaf buildNotEvaluatedRootLeaf() {
        RangeActionResult rangeActionResult = Mockito.mock(RangeActionResult.class);
        return new Leaf(network, new HashSet<>(), null, rangeActionResult);
    }

    @Test
    public void testRootLeafDefinition() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Leaf rootLeaf = new Leaf(network, prePerimeterResult);
        assertTrue(rootLeaf.getActivatedNetworkActions().isEmpty());
        assert rootLeaf.getActivatedNetworkActions().isEmpty();
        assertTrue(rootLeaf.isRoot());
        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
    }

    @Test
    public void testRootLeafDefinitionWithoutSensitivityValues() {
        Leaf rootLeaf = buildNotEvaluatedRootLeaf();
        assertEquals(Leaf.Status.CREATED, rootLeaf.getStatus());
    }

    @Test
    public void testMultipleLeafsDefinition() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Leaf rootLeaf = new Leaf(network, prePerimeterResult);
        RangeActionResult rangeActionResult = Mockito.mock(RangeActionResult.class);
        Leaf leaf1 = new Leaf(network, rootLeaf.getActivatedNetworkActions(), new NetworkActionCombination(na1), rangeActionResult);
        Leaf leaf2 = new Leaf(network, leaf1.getActivatedNetworkActions(), new NetworkActionCombination(na2), rangeActionResult);
        assertEquals(1, leaf1.getActivatedNetworkActions().size());
        assertEquals(2, leaf2.getActivatedNetworkActions().size());
        assertTrue(leaf1.getActivatedNetworkActions().contains(na1));
        assertTrue(leaf2.getActivatedNetworkActions().contains(na1));
        assertTrue(leaf2.getActivatedNetworkActions().contains(na2));
        assertFalse(leaf1.isRoot());
        assertFalse(leaf2.isRoot());
        assertEquals(Leaf.Status.CREATED, leaf1.getStatus());
        assertEquals(Leaf.Status.CREATED, leaf2.getStatus());
        assert leaf1.isActivated(na1);
        assertFalse(leaf1.isActivated(na2));
    }

    @Test
    public void testMultipleLeafDefinitionWithSameNetworkAction() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Leaf rootLeaf = new Leaf(network, prePerimeterResult);
        RangeActionResult rangeActionResult = Mockito.mock(RangeActionResult.class);
        Leaf leaf1 = new Leaf(network, rootLeaf.getActivatedNetworkActions(), new NetworkActionCombination(na1), rangeActionResult);
        Leaf leaf2 = new Leaf(network, leaf1.getActivatedNetworkActions(), new NetworkActionCombination(na1), rangeActionResult);
        assertEquals(1, leaf2.getActivatedNetworkActions().size());
        assertTrue(leaf2.getActivatedNetworkActions().contains(na1));
        assertFalse(leaf2.isRoot());
    }

    @Test
    public void evaluateAnAlreadyEvaluatedLeaf() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        Mockito.when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        Leaf rootLeaf = new Leaf(network, prePerimeterResult);
        ObjectiveFunctionResult preOptimObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        Mockito.when(costEvaluatorMock.evaluate(prePerimeterResult, sensitivityStatus)).thenReturn(preOptimObjectiveFunctionResult);
        rootLeaf.evaluate(costEvaluatorMock, sensitivityComputer);
        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
    }

    private Leaf prepareLeafForEvaluation(NetworkAction networkAction, ComputationStatus expectedSensitivityStatus, FlowResult expectedFlowResult, double expectedCost, List<FlowCnec> mostLimitingCnecs) {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Leaf rootLeaf = new Leaf(network, prePerimeterResult);
        RangeActionResult rangeActionResult = Mockito.mock(RangeActionResult.class);
        Leaf leaf = new Leaf(network, rootLeaf.getActivatedNetworkActions(), new NetworkActionCombination(networkAction), rangeActionResult);
        SensitivityResult expectedSensitivityResult = Mockito.mock(SensitivityResult.class);
        Mockito.when(sensitivityComputer.getSensitivityResult()).thenReturn(expectedSensitivityResult);
        Mockito.when(expectedSensitivityResult.getSensitivityStatus()).thenReturn(expectedSensitivityStatus);
        Mockito.when(sensitivityComputer.getBranchResult()).thenReturn(expectedFlowResult);
        ObjectiveFunctionResult expectedObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        Mockito.when(costEvaluatorMock.evaluate(any(), any())).thenReturn(expectedObjectiveFunctionResult);
        Mockito.when(expectedObjectiveFunctionResult.getFunctionalCost()).thenReturn(expectedCost / 2);
        Mockito.when(expectedObjectiveFunctionResult.getVirtualCost()).thenReturn(expectedCost / 2);
        Mockito.when(expectedObjectiveFunctionResult.getVirtualCost(virtualCostName)).thenReturn(expectedCost / 2);
        if (!mostLimitingCnecs.isEmpty()) {
            Mockito.when(expectedObjectiveFunctionResult.getMostLimitingElements(mostLimitingCnecs.size())).thenReturn(mostLimitingCnecs);
            Mockito.when(expectedObjectiveFunctionResult.getCostlyElements(virtualCostName, mostLimitingCnecs.size())).thenReturn(mostLimitingCnecs);
        }
        Mockito.when(expectedObjectiveFunctionResult.getVirtualCostNames()).thenReturn(Collections.singleton(virtualCostName));
        return leaf;
    }

    private Leaf prepareLeafForEvaluation(NetworkAction na1, ComputationStatus expectedSensitivityStatus, FlowResult expectedFlowResult, double expectedCost) {
        return prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost, Collections.emptyList());
    }

    @Test
    public void evaluateAChildLeaf() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf1 = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost);

        leaf1.evaluate(costEvaluatorMock, sensitivityComputer);

        assertEquals(Leaf.Status.EVALUATED, leaf1.getStatus());
        assertEquals(expectedFlowResult, leaf1.getPreOptimBranchResult());
        assertEquals(expectedSensitivityStatus, leaf1.getSensitivityStatus());
        assertEquals(expectedCost, leaf1.getCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testReevaluate() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf1 = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost);

        leaf1.evaluate(costEvaluatorMock, sensitivityComputer);

        ListAppender<ILoggingEvent> listAppender = getLeafLogs();

        leaf1.evaluate(costEvaluatorMock, sensitivityComputer);
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertEquals("[DEBUG] Leaf has already been evaluated", logsList.get(0).toString());

    }

    private ListAppender<ILoggingEvent> getLeafLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(Leaf.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    @Test
    public void testEvaluateError() {
        Leaf rootLeaf = buildNotEvaluatedRootLeaf();
        String message = "MockSensiFail";
        Mockito.doThrow(new FaraoException(message)).when(sensitivityComputer).compute(network);

        rootLeaf.evaluate(costEvaluatorMock, sensitivityComputer);

        assertEquals(Leaf.Status.ERROR, rootLeaf.getStatus());
    }

    @Test
    public void testOptimizeWithoutEvaluation() {
        Leaf rootLeaf = buildNotEvaluatedRootLeaf();
        assertEquals(Leaf.Status.CREATED, rootLeaf.getStatus());
        LeafProblem leafProblem = Mockito.mock(LeafProblem.class);
        ListAppender<ILoggingEvent> listAppender = getLeafLogs();
        rootLeaf.optimize(iteratingLinearOptimizer, sensitivityComputer, leafProblem);
        assertEquals(1, listAppender.list.size());
        String expectedLog = String.format("[WARN] Impossible to optimize leaf: %s\n because evaluation has not been performed", rootLeaf);
        assertEquals(expectedLog, listAppender.list.get(0).toString());
    }

    @Test
    public void testOptimizeWithError() {
        Leaf rootLeaf = buildNotEvaluatedRootLeaf();
        Mockito.doThrow(new FaraoException()).when(sensitivityComputer).compute(network);
        rootLeaf.evaluate(costEvaluatorMock, sensitivityComputer);
        LeafProblem leafProblem = Mockito.mock(LeafProblem.class);
        ListAppender<ILoggingEvent> listAppender = getLeafLogs();
        rootLeaf.optimize(iteratingLinearOptimizer, sensitivityComputer, leafProblem);
        assertEquals(1, listAppender.list.size());
        String expectedLog = String.format("[WARN] Impossible to optimize leaf: %s\n because evaluation failed", rootLeaf);
        assertEquals(expectedLog, listAppender.list.get(0).toString());
    }

    @Test
    public void optimize() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        Mockito.when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        Leaf rootLeaf = new Leaf(network, prePerimeterResult);
        LeafProblem leafProblem = Mockito.mock(LeafProblem.class);
        rootLeaf.optimize(iteratingLinearOptimizer, sensitivityComputer, leafProblem);
        assertEquals(Leaf.Status.OPTIMIZED, rootLeaf.getStatus());
    }

    @Test
    public void getFlowsAndPtdfsOnFlowCnecAfterEvaluation() {
        //prepare leaf
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost);

        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);

        FlowResult flowResult = Mockito.mock(FlowResult.class);
        Mockito.when(sensitivityComputer.getBranchResult()).thenReturn(flowResult);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);

        double expectedFlow = 3.;
        Unit unit = Unit.MEGAWATT;
        Mockito.when(flowResult.getFlow(flowCnec, unit)).thenReturn(expectedFlow);
        Mockito.when(flowResult.getCommercialFlow(flowCnec, unit)).thenReturn(expectedFlow);
        assertEquals(expectedFlow, leaf.getFlow(flowCnec, unit), DOUBLE_TOLERANCE);
        assertEquals(expectedFlow, leaf.getCommercialFlow(flowCnec, unit), DOUBLE_TOLERANCE);

        double expectedPtdf = 4.;
        Mockito.when(flowResult.getPtdfZonalSum(flowCnec)).thenReturn(expectedPtdf);
        assertEquals(expectedPtdf, leaf.getPtdfZonalSum(flowCnec), DOUBLE_TOLERANCE);

        Map<FlowCnec, Double> expectedPtdfZonalSums = new HashMap<>();
        Mockito.when(flowResult.getPtdfZonalSums()).thenReturn(expectedPtdfZonalSums);
        assertEquals(expectedPtdfZonalSums, leaf.getPtdfZonalSums());
    }

    @Test
    public void getFlowsAndPtdfsOnFlowCnecAfterOptimization() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        Mockito.when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        Leaf leaf = new Leaf(network, prePerimeterResult);
        LeafProblem leafProblem = Mockito.mock(LeafProblem.class);
        LinearOptimizationResult linearOptimizationResult = Mockito.mock(LinearOptimizationResult.class);
        Mockito.when(iteratingLinearOptimizer.optimize(any(), any(), any(), any(), any(), any())).thenReturn(linearOptimizationResult);
        leaf.optimize(iteratingLinearOptimizer, sensitivityComputer, leafProblem);

        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);

        double expectedFlow = 3.;
        Unit unit = Unit.MEGAWATT;
        Mockito.when(linearOptimizationResult.getFlow(flowCnec, unit)).thenReturn(expectedFlow);
        Mockito.when(linearOptimizationResult.getCommercialFlow(flowCnec, unit)).thenReturn(expectedFlow);
        assertEquals(expectedFlow, leaf.getFlow(flowCnec, unit), DOUBLE_TOLERANCE);
        assertEquals(expectedFlow, leaf.getCommercialFlow(flowCnec, unit), DOUBLE_TOLERANCE);

        double expectedPtdf = 4.;
        Mockito.when(linearOptimizationResult.getPtdfZonalSum(flowCnec)).thenReturn(expectedPtdf);
        assertEquals(expectedPtdf, leaf.getPtdfZonalSum(flowCnec), DOUBLE_TOLERANCE);

        Map<FlowCnec, Double> expectedPtdfZonalSums = new HashMap<>();
        Mockito.when(linearOptimizationResult.getPtdfZonalSums()).thenReturn(expectedPtdfZonalSums);
        assertEquals(expectedPtdfZonalSums, leaf.getPtdfZonalSums());

    }

    @Test(expected = FaraoException.class)
    public void getFlowOnFlowCnecBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        leaf.getFlow(flowCnec, Unit.MEGAWATT);
    }

    @Test(expected = FaraoException.class)
    public void getCommercialFlowOnFlowCnecBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        leaf.getCommercialFlow(flowCnec, Unit.MEGAWATT);
    }

    @Test(expected = FaraoException.class)
    public void getPtdfZonalSumOnCnecBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        leaf.getPtdfZonalSum(flowCnec);
    }

    @Test(expected = FaraoException.class)
    public void getPtdfZonalSumsBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        leaf.getPtdfZonalSums();
    }

    @Test
    public void getFunctionalCostAfterEvaluation() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);
        assertEquals(expectedCost / 2, leaf.getFunctionalCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void getFunctionalCostAfterOptimization() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Leaf leaf = new Leaf(network, prePerimeterResult);
        LeafProblem leafProblem = Mockito.mock(LeafProblem.class);
        LinearOptimizationResult linearOptimizationResult = Mockito.mock(LinearOptimizationResult.class);
        Mockito.when(iteratingLinearOptimizer.optimize(any(), any(), any(), any(), any(), any())).thenReturn(linearOptimizationResult);
        leaf.optimize(iteratingLinearOptimizer, sensitivityComputer, leafProblem);
        double expectedFunctionalCost = 3.;
        Mockito.when(linearOptimizationResult.getFunctionalCost()).thenReturn(expectedFunctionalCost);
        assertEquals(expectedFunctionalCost, leaf.getFunctionalCost(), DOUBLE_TOLERANCE);
    }

    @Test(expected = FaraoException.class)
    public void getFunctionalCostBeforeOptimization() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        leaf.getFunctionalCost();
    }

    @Test
    public void getVirtualCostAfterEvaluation() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);
        assertEquals(expectedCost / 2, leaf.getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(expectedCost / 2, leaf.getVirtualCost(virtualCostName), DOUBLE_TOLERANCE);
    }

    @Test
    public void getVirtualCostAfterOptimization() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Leaf leaf = new Leaf(network, prePerimeterResult);
        LeafProblem leafProblem = Mockito.mock(LeafProblem.class);
        LinearOptimizationResult linearOptimizationResult = Mockito.mock(LinearOptimizationResult.class);
        Mockito.when(iteratingLinearOptimizer.optimize(any(), any(), any(), any(), any(), any())).thenReturn(linearOptimizationResult);
        leaf.optimize(iteratingLinearOptimizer, sensitivityComputer, leafProblem);
        double expectedVirtualCost = 3.;
        Mockito.when(linearOptimizationResult.getVirtualCost()).thenReturn(expectedVirtualCost);
        assertEquals(expectedVirtualCost, leaf.getVirtualCost(), DOUBLE_TOLERANCE);
        Mockito.when(linearOptimizationResult.getVirtualCost(virtualCostName)).thenReturn(expectedVirtualCost);
        assertEquals(expectedVirtualCost, leaf.getVirtualCost(virtualCostName), DOUBLE_TOLERANCE);
    }

    @Test(expected = FaraoException.class)
    public void getVirtualCostBeforeOptimization() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        leaf.getVirtualCost();
    }

    @Test(expected = FaraoException.class)
    public void getSpecificVirtualCostBeforeOptimization() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        leaf.getVirtualCost(virtualCostName);
    }

    @Test
    public void getCostlyAndMostLimitingElementsAfterEvaluation() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        List<FlowCnec> flowCnecs = Collections.singletonList(flowCnec);
        Leaf leaf = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost, flowCnecs);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);
        assertEquals(flowCnecs, leaf.getMostLimitingElements(flowCnecs.size()));
        assertEquals(flowCnecs, leaf.getCostlyElements(virtualCostName, flowCnecs.size()));
    }

    @Test
    public void getCostlyAndMostLimitingElementsAfterOptimization() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Leaf leaf = new Leaf(network, prePerimeterResult);
        LeafProblem leafProblem = Mockito.mock(LeafProblem.class);
        LinearOptimizationResult linearOptimizationResult = Mockito.mock(LinearOptimizationResult.class);
        Mockito.when(iteratingLinearOptimizer.optimize(any(), any(), any(), any(), any(), any())).thenReturn(linearOptimizationResult);
        leaf.optimize(iteratingLinearOptimizer, sensitivityComputer, leafProblem);
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        List<FlowCnec> flowCnecs = Collections.singletonList(flowCnec);
        Mockito.when(linearOptimizationResult.getMostLimitingElements(flowCnecs.size())).thenReturn(flowCnecs);
        assertEquals(flowCnecs, leaf.getMostLimitingElements(flowCnecs.size()));
        Mockito.when(linearOptimizationResult.getCostlyElements(virtualCostName, flowCnecs.size())).thenReturn(flowCnecs);
        assertEquals(flowCnecs, leaf.getCostlyElements(virtualCostName, flowCnecs.size()));
    }

    @Test(expected = FaraoException.class)
    public void getMostLimitingElementsBeforeOptimization() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        leaf.getMostLimitingElements(0);
    }

    @Test(expected = FaraoException.class)
    public void getCostlyElementsBeforeOptimization() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        leaf.getCostlyElements(virtualCostName, 0);
    }

    @Test
    public void getVirtualCostNames() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Leaf leaf = new Leaf(network, prePerimeterResult);
        ObjectiveFunctionResult objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        Mockito.when(costEvaluatorMock.evaluate(any(), any())).thenReturn(objectiveFunctionResult);
        Set<String> virtualCostNames = new HashSet<>();
        virtualCostNames.add(virtualCostName);
        Mockito.when(objectiveFunctionResult.getVirtualCostNames()).thenReturn(virtualCostNames);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);
        assertEquals(virtualCostNames, leaf.getVirtualCostNames());
    }

    @Test
    public void getRangeActionsAfterEvaluation() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Leaf leaf = new Leaf(network, prePerimeterResult);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);

        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        int optimalTap = 3;
        Map<PstRangeAction, Integer> optimizedTaps = new HashMap<>();
        optimizedTaps.put(pstRangeAction, optimalTap);
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        double optimalSetpoint = 3.;
        Map<RangeAction, Double> optimizedSetPoints = new HashMap<>();
        optimizedSetPoints.put(rangeAction, optimalSetpoint);
        Set<RangeAction> rangeActions = new HashSet<>();
        rangeActions.add(pstRangeAction);
        rangeActions.add(rangeAction);

        Mockito.when(prePerimeterResult.getRangeActions()).thenReturn(rangeActions);
        assertEquals(rangeActions, leaf.getRangeActions());

        Mockito.when(prePerimeterResult.getOptimizedTap(pstRangeAction)).thenReturn(optimalTap);
        assertEquals(optimalTap, leaf.getOptimizedTap(pstRangeAction));

        Mockito.when(prePerimeterResult.getOptimizedTaps()).thenReturn(optimizedTaps);
        assertEquals(optimizedTaps, leaf.getOptimizedTaps());

        Mockito.when(prePerimeterResult.getOptimizedSetPoint(rangeAction)).thenReturn(optimalSetpoint);
        assertEquals(optimalSetpoint, leaf.getOptimizedSetPoint(rangeAction), DOUBLE_TOLERANCE);

        Mockito.when(prePerimeterResult.getOptimizedSetPoints()).thenReturn(optimizedSetPoints);
        assertEquals(optimizedSetPoints, leaf.getOptimizedSetPoints());
    }

    @Test
    public void getRangeActionsAfterOptimization() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Leaf leaf = new Leaf(network, prePerimeterResult);
        LeafProblem leafProblem = Mockito.mock(LeafProblem.class);
        LinearOptimizationResult linearOptimizationResult = Mockito.mock(LinearOptimizationResult.class);
        Mockito.when(iteratingLinearOptimizer.optimize(any(), any(), any(), any(), any(), any())).thenReturn(linearOptimizationResult);
        leaf.optimize(iteratingLinearOptimizer, sensitivityComputer, leafProblem);

        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        int optimalTap = 3;
        Map<PstRangeAction, Integer> optimizedTaps = new HashMap<>();
        optimizedTaps.put(pstRangeAction, optimalTap);
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        double optimalSetpoint = 3.;
        Map<RangeAction, Double> optimizedSetPoints = new HashMap<>();
        optimizedSetPoints.put(rangeAction, optimalSetpoint);
        Set<RangeAction> rangeActions = new HashSet<>();
        rangeActions.add(pstRangeAction);
        rangeActions.add(rangeAction);

        Mockito.when(linearOptimizationResult.getRangeActions()).thenReturn(rangeActions);
        assertEquals(rangeActions, leaf.getRangeActions());

        Mockito.when(linearOptimizationResult.getOptimizedTap(pstRangeAction)).thenReturn(optimalTap);
        assertEquals(optimalTap, leaf.getOptimizedTap(pstRangeAction));

        Mockito.when(linearOptimizationResult.getOptimizedTaps()).thenReturn(optimizedTaps);
        assertEquals(optimizedTaps, leaf.getOptimizedTaps());

        Mockito.when(linearOptimizationResult.getOptimizedSetPoint(rangeAction)).thenReturn(optimalSetpoint);
        assertEquals(optimalSetpoint, leaf.getOptimizedSetPoint(rangeAction), DOUBLE_TOLERANCE);

        Mockito.when(linearOptimizationResult.getOptimizedSetPoints()).thenReturn(optimizedSetPoints);
        assertEquals(optimizedSetPoints, leaf.getOptimizedSetPoints());
    }

    @Test(expected = FaraoException.class)
    public void getRangeActionsBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        leaf.getRangeActions();
    }

    @Test(expected = FaraoException.class)
    public void getOptimizedTapBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        leaf.getOptimizedTap(pstRangeAction);
    }

    @Test(expected = FaraoException.class)
    public void getOptimizedTapsBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        leaf.getOptimizedTaps();
    }

    @Test(expected = FaraoException.class)
    public void getOptimizedSetpointsBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        leaf.getOptimizedSetPoints();
    }

    @Test(expected = FaraoException.class)
    public void getOptimizedSetPointBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        leaf.getOptimizedSetPoint(rangeAction);
    }

    @Test
    public void getSensitivityStatusAfterEvaluation() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Leaf leaf = new Leaf(network, prePerimeterResult);
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        Mockito.when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        assertEquals(sensitivityStatus, leaf.getSensitivityStatus());
    }

    @Test
    public void getSensitivityStatusAfterOptimization() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Leaf leaf = new Leaf(network, prePerimeterResult);
        LeafProblem leafProblem = Mockito.mock(LeafProblem.class);
        LinearOptimizationResult linearOptimizationResult = Mockito.mock(LinearOptimizationResult.class);
        Mockito.when(iteratingLinearOptimizer.optimize(any(), any(), any(), any(), any(), any())).thenReturn(linearOptimizationResult);
        leaf.optimize(iteratingLinearOptimizer, sensitivityComputer, leafProblem);
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        Mockito.when(linearOptimizationResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        assertEquals(sensitivityStatus, leaf.getSensitivityStatus());
    }

    @Test(expected = FaraoException.class)
    public void getSensitivityStatusBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        leaf.getSensitivityStatus();
    }

    @Test
    public void getSensitivityValueAfterEvaluation() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Leaf leaf = new Leaf(network, prePerimeterResult);

        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        LinearGlsk linearGlsk = Mockito.mock(LinearGlsk.class);
        double expectedSensi = 3.;

        Mockito.when(prePerimeterResult.getSensitivityValue(flowCnec, rangeAction, Unit.MEGAWATT)).thenReturn(expectedSensi);
        Mockito.when(prePerimeterResult.getSensitivityValue(flowCnec, linearGlsk, Unit.MEGAWATT)).thenReturn(expectedSensi);

        assertEquals(expectedSensi, leaf.getSensitivityValue(flowCnec, rangeAction, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(expectedSensi, leaf.getSensitivityValue(flowCnec, linearGlsk, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void getSensitivityValueAfterOptimization() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Leaf leaf = new Leaf(network, prePerimeterResult);
        LeafProblem leafProblem = Mockito.mock(LeafProblem.class);
        LinearOptimizationResult linearOptimizationResult = Mockito.mock(LinearOptimizationResult.class);
        Mockito.when(iteratingLinearOptimizer.optimize(any(), any(), any(), any(), any(), any())).thenReturn(linearOptimizationResult);
        leaf.optimize(iteratingLinearOptimizer, sensitivityComputer, leafProblem);

        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        LinearGlsk linearGlsk = Mockito.mock(LinearGlsk.class);
        double expectedSensi = 3.;

        Mockito.when(linearOptimizationResult.getSensitivityValue(flowCnec, rangeAction, Unit.MEGAWATT)).thenReturn(expectedSensi);
        Mockito.when(linearOptimizationResult.getSensitivityValue(flowCnec, linearGlsk, Unit.MEGAWATT)).thenReturn(expectedSensi);
        Mockito.when(linearOptimizationResult.getRangeActions()).thenReturn(Set.of(rangeAction));

        assertEquals(expectedSensi, leaf.getSensitivityValue(flowCnec, rangeAction, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(expectedSensi, leaf.getSensitivityValue(flowCnec, linearGlsk, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test(expected = FaraoException.class)
    public void getSensitivityValueOnRangeActionBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        leaf.getSensitivityValue(flowCnec, rangeAction, Unit.MEGAWATT);
    }

    @Test(expected = FaraoException.class)
    public void getSensitivityValueOnLinearGlskBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        LinearGlsk linearGlsk = Mockito.mock(LinearGlsk.class);
        leaf.getSensitivityValue(flowCnec, linearGlsk, Unit.MEGAWATT);
    }

    @Test
    public void testFinalize() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        Mockito.when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        Leaf rootLeaf = new Leaf(network, prePerimeterResult);
        LeafProblem leafProblem = Mockito.mock(LeafProblem.class);
        rootLeaf.optimize(iteratingLinearOptimizer, sensitivityComputer, leafProblem);
        rootLeaf.finalizeOptimization();
        assertThrows(FaraoException.class, () -> rootLeaf.optimize(iteratingLinearOptimizer, sensitivityComputer, leafProblem));
    }
}
