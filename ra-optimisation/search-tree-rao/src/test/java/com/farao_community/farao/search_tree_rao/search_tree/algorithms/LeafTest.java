/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.search_tree.algorithms;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_api.parameters.ObjectiveFunctionParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.SensitivityComputer;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.TreeParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.IteratingLinearOptimizer;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.commons.NetworkActionCombination;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemBuilder;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.farao_community.farao.search_tree_rao.result.impl.IteratingLinearOptimizationResultImpl;
import com.farao_community.farao.search_tree_rao.search_tree.inputs.SearchTreeInput;
import com.farao_community.farao.search_tree_rao.search_tree.parameters.SearchTreeParameters;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static com.farao_community.farao.data.crac_api.cnec.Side.RIGHT;
import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static com.farao_community.farao.commons.Unit.*;
import static org.mockito.Mockito.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class LeafTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;

    private NetworkAction na1;
    private NetworkAction na2;

    private Network network;
    private IteratingLinearOptimizer iteratingLinearOptimizer;
    private ObjectiveFunction costEvaluatorMock;
    private SensitivityComputer sensitivityComputer;
    private OptimizationPerimeter optimizationPerimeter;
    private PrePerimeterResult prePerimeterResult;
    private AppliedRemedialActions appliedRemedialActions;
    private State optimizedState;

    private String virtualCostName;

    private MockedStatic<LinearProblem> linearProblemMockedStatic;
    private MockedStatic<IteratingLinearOptimizer> iteratingLinearOptimizerMockedStatic;

    @BeforeEach
    public void setUp() {
        // network
        network = NetworkImportsUtil.import12NodesNetwork();

        // network actions
        na1 = Mockito.mock(NetworkAction.class);
        na2 = Mockito.mock(NetworkAction.class);
        when(na1.apply(any())).thenReturn(true);
        when(na2.apply(any())).thenReturn(true);

        // rao parameters
        RaoParameters raoParameters = new RaoParameters();

        iteratingLinearOptimizer = Mockito.mock(IteratingLinearOptimizer.class);
        sensitivityComputer = Mockito.mock(SensitivityComputer.class);
        costEvaluatorMock = Mockito.mock(ObjectiveFunction.class);
        optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        when(optimizationPerimeter.getMainOptimizationState()).thenReturn(optimizedState);
        prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        appliedRemedialActions = Mockito.mock(AppliedRemedialActions.class);
        optimizedState = Mockito.mock(State.class);

        virtualCostName = "VirtualCost";
        linearProblemMockedStatic = mockStatic(LinearProblem.class);
        iteratingLinearOptimizerMockedStatic = mockStatic(IteratingLinearOptimizer.class);
    }

    @AfterEach
    public void tearDown() {
        linearProblemMockedStatic.close();
        iteratingLinearOptimizerMockedStatic.close();
    }

    private Leaf buildNotEvaluatedRootLeaf() {
        RangeActionActivationResult rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        return new Leaf(optimizationPerimeter, network, new HashSet<>(), new NetworkActionCombination(na1), rangeActionActivationResult, prePerimeterResult, appliedRemedialActions);
    }

    private void prepareLinearProblemBuilder(IteratingLinearOptimizationResultImpl linearOptimizationResult) {
        LinearProblemBuilder linearProblemBuilder = Mockito.mock(LinearProblemBuilder.class);
        LinearProblem linearProblem = Mockito.mock(LinearProblem.class);
        when(linearProblemBuilder.buildFromInputsAndParameters(Mockito.any(), Mockito.any())).thenReturn(linearProblem);
        linearProblemMockedStatic.when(LinearProblem::create).thenReturn(linearProblemBuilder);
        iteratingLinearOptimizerMockedStatic.when(() -> IteratingLinearOptimizer.optimize(Mockito.any(), Mockito.any())).thenReturn(linearOptimizationResult);

    }

    @Test
    void testRootLeafDefinition() {
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        assertTrue(rootLeaf.getActivatedNetworkActions().isEmpty());
        assert rootLeaf.getActivatedNetworkActions().isEmpty();
        assertTrue(rootLeaf.isRoot());
        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
    }

    @Test
    void testRootLeafDefinitionWithoutSensitivityValues() {
        Leaf rootLeaf = buildNotEvaluatedRootLeaf();
        assertEquals(Leaf.Status.CREATED, rootLeaf.getStatus());
    }

    @Test
    void testMultipleLeafsDefinition() {
        RangeActionActivationResult rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        Leaf leaf1 = new Leaf(optimizationPerimeter, network, rootLeaf.getActivatedNetworkActions(), new NetworkActionCombination(na1), rangeActionActivationResult, prePerimeterResult, appliedRemedialActions);
        Leaf leaf2 = new Leaf(optimizationPerimeter, network, leaf1.getActivatedNetworkActions(), new NetworkActionCombination(na2), rangeActionActivationResult, prePerimeterResult, appliedRemedialActions);
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
    void testMultipleLeafDefinitionWithSameNetworkAction() {
        RangeActionActivationResult rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        Leaf leaf1 = new Leaf(optimizationPerimeter, network, rootLeaf.getActivatedNetworkActions(), new NetworkActionCombination(na1), rangeActionActivationResult, prePerimeterResult, appliedRemedialActions);
        Leaf leaf2 = new Leaf(optimizationPerimeter, network, leaf1.getActivatedNetworkActions(), new NetworkActionCombination(na1), rangeActionActivationResult, prePerimeterResult, appliedRemedialActions);
        assertEquals(1, leaf2.getActivatedNetworkActions().size());
        assertTrue(leaf2.getActivatedNetworkActions().contains(na1));
        assertFalse(leaf2.isRoot());
    }

    @Test
    void evaluateAnAlreadyEvaluatedLeaf() {
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        ObjectiveFunctionResult preOptimObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        when(costEvaluatorMock.evaluate(prePerimeterResult, rootLeaf.getRangeActionActivationResult(), prePerimeterResult, sensitivityStatus)).thenReturn(preOptimObjectiveFunctionResult);
        rootLeaf.evaluate(costEvaluatorMock, sensitivityComputer);
        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
    }

    private Leaf prepareLeafForEvaluation(NetworkAction networkAction, ComputationStatus expectedSensitivityStatus, FlowResult expectedFlowResult, double expectedCost, List<FlowCnec> mostLimitingCnecs) {
        when(networkAction.apply(any())).thenReturn(true);
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        RangeActionActivationResult rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        Leaf leaf = new Leaf(optimizationPerimeter, network, rootLeaf.getActivatedNetworkActions(), new NetworkActionCombination(networkAction), rangeActionActivationResult, prePerimeterResult, appliedRemedialActions);
        SensitivityResult expectedSensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityComputer.getSensitivityResult()).thenReturn(expectedSensitivityResult);
        when(expectedSensitivityResult.getSensitivityStatus()).thenReturn(expectedSensitivityStatus);
        when(sensitivityComputer.getBranchResult(network)).thenReturn(expectedFlowResult);
        ObjectiveFunctionResult expectedObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        when(costEvaluatorMock.evaluate(any(), any(), any(), any())).thenReturn(expectedObjectiveFunctionResult);
        when(expectedObjectiveFunctionResult.getFunctionalCost()).thenReturn(expectedCost / 2);
        when(expectedObjectiveFunctionResult.getVirtualCost()).thenReturn(expectedCost / 2);
        when(expectedObjectiveFunctionResult.getVirtualCost(virtualCostName)).thenReturn(expectedCost / 2);
        if (!mostLimitingCnecs.isEmpty()) {
            when(expectedObjectiveFunctionResult.getMostLimitingElements(mostLimitingCnecs.size())).thenReturn(mostLimitingCnecs);
            when(expectedObjectiveFunctionResult.getCostlyElements(virtualCostName, mostLimitingCnecs.size())).thenReturn(mostLimitingCnecs);
        }
        when(expectedObjectiveFunctionResult.getVirtualCostNames()).thenReturn(Collections.singleton(virtualCostName));
        return leaf;
    }

    private Leaf prepareLeafForEvaluation(NetworkAction na1, ComputationStatus expectedSensitivityStatus, FlowResult expectedFlowResult, double expectedCost) {
        return prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost, Collections.emptyList());
    }

    @Test
    void evaluateAChildLeaf() {
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
    void testReevaluate() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf1 = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost);

        leaf1.evaluate(costEvaluatorMock, sensitivityComputer);

        ListAppender<ILoggingEvent> listAppender = getTechnicalLogs();

        leaf1.evaluate(costEvaluatorMock, sensitivityComputer);
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertEquals("[DEBUG] Leaf has already been evaluated", logsList.get(0).toString());

    }

    private ListAppender<ILoggingEvent> getLogs(Class clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    private ListAppender<ILoggingEvent> getTechnicalLogs() {
        return getLogs(FaraoLoggerProvider.TECHNICAL_LOGS.getClass());
    }

    private ListAppender<ILoggingEvent> getBusinessWarns() {
        return getLogs(FaraoLoggerProvider.BUSINESS_WARNS.getClass());
    }

    @Test
    void testEvaluateError() {
        Leaf rootLeaf = buildNotEvaluatedRootLeaf();
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        Mockito.when(sensitivityComputer.getSensitivityResult()).thenReturn(sensitivityResult);
        Mockito.when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        Mockito.doNothing().when(sensitivityComputer).compute(network);

        rootLeaf.evaluate(costEvaluatorMock, sensitivityComputer);

        assertEquals(Leaf.Status.ERROR, rootLeaf.getStatus());
    }

    @Test
    void testOptimizeWithoutEvaluation() {
        Leaf rootLeaf = buildNotEvaluatedRootLeaf();
        assertEquals(Leaf.Status.CREATED, rootLeaf.getStatus());
        ListAppender<ILoggingEvent> listAppender = getBusinessWarns();
        SearchTreeInput searchTreeInput = Mockito.mock(SearchTreeInput.class);
        SearchTreeParameters searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        rootLeaf.optimize(searchTreeInput, searchTreeParameters);
        assertEquals(1, listAppender.list.size());
        String expectedLog = String.format("[WARN] Impossible to optimize leaf: %s\n because evaluation has not been performed", rootLeaf);
        assertEquals(expectedLog, listAppender.list.get(0).toString());
    }

    @Test
    void testOptimizeWithError() {
        Leaf rootLeaf = buildNotEvaluatedRootLeaf();
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        Mockito.when(sensitivityComputer.getSensitivityResult()).thenReturn(sensitivityResult);
        Mockito.when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        Mockito.doNothing().when(sensitivityComputer).compute(network);
        rootLeaf.evaluate(costEvaluatorMock, sensitivityComputer);
        ListAppender<ILoggingEvent> listAppender = getBusinessWarns();
        SearchTreeInput searchTreeInput = Mockito.mock(SearchTreeInput.class);
        SearchTreeParameters searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        rootLeaf.optimize(searchTreeInput, searchTreeParameters);
        assertEquals(1, listAppender.list.size());
        String expectedLog = String.format("[WARN] Impossible to optimize leaf: %s\n because evaluation failed", rootLeaf);
        assertEquals(expectedLog, listAppender.list.get(0).toString());
    }

    @Test
    void optimize() {
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        prepareLinearProblemBuilder(Mockito.mock(IteratingLinearOptimizationResultImpl.class));
        SearchTreeInput searchTreeInput = Mockito.mock(SearchTreeInput.class);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunction.class));
        SearchTreeParameters searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class));
        when(searchTreeParameters.getTreeParameters()).thenReturn(Mockito.mock(TreeParameters.class));
        rootLeaf.optimize(searchTreeInput, searchTreeParameters);
        assertEquals(Leaf.Status.OPTIMIZED, rootLeaf.getStatus());
    }

    @Test
    void getFlowsAndPtdfsOnFlowCnecAfterEvaluation() {
        //prepare leaf
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost);

        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);

        FlowResult flowResult = Mockito.mock(FlowResult.class);
        when(sensitivityComputer.getBranchResult(network)).thenReturn(flowResult);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);

        double expectedFlow = 3.;
        Unit unit = MEGAWATT;
        when(flowResult.getFlow(flowCnec, LEFT, unit)).thenReturn(expectedFlow);
        when(flowResult.getCommercialFlow(flowCnec, LEFT, unit)).thenReturn(expectedFlow);
        assertEquals(expectedFlow, leaf.getFlow(flowCnec, LEFT, unit), DOUBLE_TOLERANCE);
        assertEquals(expectedFlow, leaf.getCommercialFlow(flowCnec, LEFT, unit), DOUBLE_TOLERANCE);

        double expectedPtdf = 4.;
        when(flowResult.getPtdfZonalSum(flowCnec, LEFT)).thenReturn(expectedPtdf);
        assertEquals(expectedPtdf, leaf.getPtdfZonalSum(flowCnec, LEFT), DOUBLE_TOLERANCE);

        Map<FlowCnec, Map<Side, Double>> expectedPtdfZonalSums = new HashMap<>();
        when(flowResult.getPtdfZonalSums()).thenReturn(expectedPtdfZonalSums);
        assertEquals(expectedPtdfZonalSums, leaf.getPtdfZonalSums());
    }

    @Test
    void getFlowsAndPtdfsOnFlowCnecAfterOptimization() {
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        IteratingLinearOptimizationResultImpl linearOptimizationResult = Mockito.mock(IteratingLinearOptimizationResultImpl.class);
        SearchTreeInput searchTreeInput = Mockito.mock(SearchTreeInput.class);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunction.class));
        SearchTreeParameters searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class));
        when(searchTreeParameters.getTreeParameters()).thenReturn(Mockito.mock(TreeParameters.class));
        prepareLinearProblemBuilder(linearOptimizationResult);
        leaf.optimize(searchTreeInput, searchTreeParameters);

        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);

        double expectedFlow = 3.;
        Unit unit = MEGAWATT;
        when(linearOptimizationResult.getFlow(flowCnec, LEFT, unit)).thenReturn(expectedFlow);
        when(linearOptimizationResult.getCommercialFlow(flowCnec, LEFT, unit)).thenReturn(expectedFlow);
        assertEquals(expectedFlow, leaf.getFlow(flowCnec, LEFT, unit), DOUBLE_TOLERANCE);
        assertEquals(expectedFlow, leaf.getCommercialFlow(flowCnec, LEFT, unit), DOUBLE_TOLERANCE);

        double expectedPtdf = 4.;
        when(linearOptimizationResult.getPtdfZonalSum(flowCnec, LEFT)).thenReturn(expectedPtdf);
        assertEquals(expectedPtdf, leaf.getPtdfZonalSum(flowCnec, LEFT), DOUBLE_TOLERANCE);

        Map<FlowCnec, Map<Side, Double>> expectedPtdfZonalSums = new HashMap<>();
        when(linearOptimizationResult.getPtdfZonalSums()).thenReturn(expectedPtdfZonalSums);
        assertEquals(expectedPtdfZonalSums, leaf.getPtdfZonalSums());

    }

    @Test
    void getFlowOnFlowCnecBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        assertThrows(FaraoException.class, () -> leaf.getFlow(flowCnec, LEFT, MEGAWATT));
    }

    @Test
    void getCommercialFlowOnFlowCnecBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        assertThrows(FaraoException.class, () -> leaf.getCommercialFlow(flowCnec, LEFT, MEGAWATT));
    }

    @Test
    void getPtdfZonalSumOnCnecBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        assertThrows(FaraoException.class, () -> leaf.getPtdfZonalSum(flowCnec, LEFT));
    }

    @Test
    void getPtdfZonalSumsBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        assertThrows(FaraoException.class, leaf::getPtdfZonalSums);
    }

    @Test
    void getFunctionalCostAfterEvaluation() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);
        assertEquals(expectedCost / 2, leaf.getFunctionalCost(), DOUBLE_TOLERANCE);
    }

    @Test
    void getFunctionalCostAfterOptimization() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        IteratingLinearOptimizationResultImpl linearOptimizationResult = Mockito.mock(IteratingLinearOptimizationResultImpl.class);
        SearchTreeInput searchTreeInput = Mockito.mock(SearchTreeInput.class);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunction.class));
        SearchTreeParameters searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class));
        when(searchTreeParameters.getTreeParameters()).thenReturn(Mockito.mock(TreeParameters.class));
        prepareLinearProblemBuilder(linearOptimizationResult);
        leaf.optimize(searchTreeInput, searchTreeParameters);
        double expectedFunctionalCost = 3.;
        when(linearOptimizationResult.getFunctionalCost()).thenReturn(expectedFunctionalCost);
        assertEquals(expectedFunctionalCost, leaf.getFunctionalCost(), DOUBLE_TOLERANCE);
    }

    @Test
    void getFunctionalCostBeforeOptimization() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        assertThrows(FaraoException.class, leaf::getFunctionalCost);
    }

    @Test
    void getVirtualCostAfterEvaluation() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        when(na1.apply(any())).thenReturn(true);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);
        assertEquals(expectedCost / 2, leaf.getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(expectedCost / 2, leaf.getVirtualCost(virtualCostName), DOUBLE_TOLERANCE);
    }

    @Test
    void getVirtualCostAfterOptimization() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        IteratingLinearOptimizationResultImpl linearOptimizationResult = Mockito.mock(IteratingLinearOptimizationResultImpl.class);
        SearchTreeInput searchTreeInput = Mockito.mock(SearchTreeInput.class);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunction.class));
        SearchTreeParameters searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class));
        when(searchTreeParameters.getTreeParameters()).thenReturn(Mockito.mock(TreeParameters.class));
        prepareLinearProblemBuilder(linearOptimizationResult);
        leaf.optimize(searchTreeInput, searchTreeParameters);
        double expectedVirtualCost = 3.;
        when(linearOptimizationResult.getVirtualCost()).thenReturn(expectedVirtualCost);
        assertEquals(expectedVirtualCost, leaf.getVirtualCost(), DOUBLE_TOLERANCE);
        when(linearOptimizationResult.getVirtualCost(virtualCostName)).thenReturn(expectedVirtualCost);
        assertEquals(expectedVirtualCost, leaf.getVirtualCost(virtualCostName), DOUBLE_TOLERANCE);
    }

    @Test
    void getVirtualCostBeforeOptimization() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        assertThrows(FaraoException.class, leaf::getVirtualCost);
    }

    @Test
    void getSpecificVirtualCostBeforeOptimization() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        assertThrows(FaraoException.class, () -> leaf.getVirtualCost(virtualCostName));
    }

    @Test
    void getCostlyAndMostLimitingElementsAfterEvaluation() {
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
    void getCostlyAndMostLimitingElementsAfterOptimization() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        IteratingLinearOptimizationResultImpl linearOptimizationResult = Mockito.mock(IteratingLinearOptimizationResultImpl.class);
        SearchTreeInput searchTreeInput = Mockito.mock(SearchTreeInput.class);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunction.class));
        SearchTreeParameters searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class));
        when(searchTreeParameters.getTreeParameters()).thenReturn(Mockito.mock(TreeParameters.class));
        prepareLinearProblemBuilder(linearOptimizationResult);
        leaf.optimize(searchTreeInput, searchTreeParameters);
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        List<FlowCnec> flowCnecs = Collections.singletonList(flowCnec);
        when(linearOptimizationResult.getMostLimitingElements(flowCnecs.size())).thenReturn(flowCnecs);
        assertEquals(flowCnecs, leaf.getMostLimitingElements(flowCnecs.size()));
        when(linearOptimizationResult.getCostlyElements(virtualCostName, flowCnecs.size())).thenReturn(flowCnecs);
        assertEquals(flowCnecs, leaf.getCostlyElements(virtualCostName, flowCnecs.size()));
    }

    @Test
    void getMostLimitingElementsBeforeOptimization() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        assertThrows(FaraoException.class, () -> leaf.getMostLimitingElements(0));
    }

    @Test
    void getCostlyElementsBeforeOptimization() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        assertThrows(FaraoException.class, () -> leaf.getCostlyElements(virtualCostName, 0));
    }

    @Test
    void getVirtualCostNames() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        ObjectiveFunctionResult objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        when(costEvaluatorMock.evaluate(any(), any(), any(), any())).thenReturn(objectiveFunctionResult);
        Set<String> virtualCostNames = new HashSet<>();
        virtualCostNames.add(virtualCostName);
        when(objectiveFunctionResult.getVirtualCostNames()).thenReturn(virtualCostNames);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);
        assertEquals(virtualCostNames, leaf.getVirtualCostNames());
    }

    @Test
    void getRangeActionsAfterEvaluation() {
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        when(pstRangeAction.getId()).thenReturn("pstRa");
        when(pstRangeAction.convertAngleToTap(3.)).thenReturn(3);
        when(pstRangeAction.getNetworkElements()).thenReturn(Set.of(Mockito.mock(NetworkElement.class)));
        int optimalTap = 3;
        double optimalSetpoint = 3.;
        Map<PstRangeAction, Integer> optimizedTaps = new HashMap<>();
        optimizedTaps.put(pstRangeAction, optimalTap);
        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        when(rangeAction.getId()).thenReturn("ra");
        when(pstRangeAction.getNetworkElements()).thenReturn(Set.of(Mockito.mock(NetworkElement.class)));
        Map<RangeAction<?>, Double> optimizedSetPoints = new HashMap<>();
        optimizedSetPoints.put(rangeAction, optimalSetpoint);
        optimizedSetPoints.put(pstRangeAction, optimalSetpoint);
        Set<RangeAction<?>> rangeActions = new HashSet<>();
        rangeActions.add(pstRangeAction);
        rangeActions.add(rangeAction);
        when(prePerimeterResult.getRangeActions()).thenReturn(rangeActions);
        when(prePerimeterResult.getTap(pstRangeAction)).thenReturn(optimalTap);
        when(prePerimeterResult.getSetpoint(rangeAction)).thenReturn(optimalSetpoint);
        when(prePerimeterResult.getSetpoint(pstRangeAction)).thenReturn(optimalSetpoint);

        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);

        assertEquals(rangeActions, leaf.getRangeActions());
        assertEquals(optimalTap, leaf.getOptimizedTap(pstRangeAction, optimizedState));
        assertEquals(optimizedTaps, leaf.getOptimizedTapsOnState(optimizedState));
        assertEquals(optimalSetpoint, leaf.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);
        assertEquals(optimizedSetPoints, leaf.getOptimizedSetpointsOnState(optimizedState));
    }

    @Test
    void getRangeActionsAfterOptimization() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        IteratingLinearOptimizationResultImpl linearOptimizationResult = Mockito.mock(IteratingLinearOptimizationResultImpl.class);
        SearchTreeInput searchTreeInput = Mockito.mock(SearchTreeInput.class);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunction.class));
        SearchTreeParameters searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class));
        when(searchTreeParameters.getTreeParameters()).thenReturn(Mockito.mock(TreeParameters.class));
        prepareLinearProblemBuilder(linearOptimizationResult);
        leaf.optimize(searchTreeInput, searchTreeParameters);

        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        int optimalTap = 3;
        Map<PstRangeAction, Integer> optimizedTaps = new HashMap<>();
        optimizedTaps.put(pstRangeAction, optimalTap);
        double optimalSetpoint = 3.;
        Map<RangeAction<?>, Double> optimizedSetPoints = new HashMap<>();
        optimizedSetPoints.put(rangeAction, optimalSetpoint);
        Set<RangeAction<?>> rangeActions = new HashSet<>();
        rangeActions.add(pstRangeAction);
        rangeActions.add(rangeAction);

        when(linearOptimizationResult.getRangeActions()).thenReturn(rangeActions);
        assertEquals(rangeActions, leaf.getRangeActions());

        when(linearOptimizationResult.getOptimizedTap(pstRangeAction, optimizedState)).thenReturn(optimalTap);
        assertEquals(optimalTap, leaf.getOptimizedTap(pstRangeAction, optimizedState));

        when(linearOptimizationResult.getOptimizedTapsOnState(optimizedState)).thenReturn(optimizedTaps);
        assertEquals(optimizedTaps, leaf.getOptimizedTapsOnState(optimizedState));

        when(linearOptimizationResult.getOptimizedSetpoint(rangeAction, optimizedState)).thenReturn(optimalSetpoint);
        assertEquals(optimalSetpoint, leaf.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);

        when(linearOptimizationResult.getOptimizedSetpointsOnState(optimizedState)).thenReturn(optimizedSetPoints);
        assertEquals(optimizedSetPoints, leaf.getOptimizedSetpointsOnState(optimizedState));
    }

    @Test
    void getRangeActionsBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        assertThrows(FaraoException.class, leaf::getRangeActions);
    }

    @Test
    void getOptimizedTapBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        assertThrows(FaraoException.class, () -> leaf.getOptimizedTap(pstRangeAction, optimizedState));
    }

    @Test
    void getOptimizedTapsBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        assertThrows(FaraoException.class, () -> leaf.getOptimizedTapsOnState(optimizedState));
    }

    @Test
    void getOptimizedSetpointsBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        assertThrows(FaraoException.class, () -> leaf.getOptimizedSetpointsOnState(optimizedState));
    }

    @Test
    void getOptimizedSetPointBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        assertThrows(FaraoException.class, () -> leaf.getOptimizedSetpoint(rangeAction, optimizedState));
    }

    @Test
    void getSensitivityStatusAfterEvaluation() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        assertEquals(sensitivityStatus, leaf.getSensitivityStatus());
    }

    @Test
    void getSensitivityStatusAfterOptimization() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        IteratingLinearOptimizationResultImpl linearOptimizationResult = Mockito.mock(IteratingLinearOptimizationResultImpl.class);
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        when(linearOptimizationResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        prepareLinearProblemBuilder(linearOptimizationResult);

        SearchTreeInput searchTreeInput = Mockito.mock(SearchTreeInput.class);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunction.class));
        SearchTreeParameters searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class));
        when(searchTreeParameters.getTreeParameters()).thenReturn(Mockito.mock(TreeParameters.class));
        leaf.optimize(searchTreeInput, searchTreeParameters);

        assertEquals(sensitivityStatus, leaf.getSensitivityStatus());
    }

    @Test
    void getSensitivityStatusBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        assertThrows(FaraoException.class, leaf::getSensitivityStatus);
    }

    @Test
    void getSensitivityValueAfterEvaluation() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);

        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        SensitivityVariableSet linearGlsk = Mockito.mock(SensitivityVariableSet.class);
        double expectedSensi = 3.;

        when(prePerimeterResult.getSensitivityValue(flowCnec, RIGHT, rangeAction, MEGAWATT)).thenReturn(expectedSensi);
        when(prePerimeterResult.getSensitivityValue(flowCnec, RIGHT, linearGlsk, MEGAWATT)).thenReturn(expectedSensi);

        assertEquals(expectedSensi, leaf.getSensitivityValue(flowCnec, RIGHT, rangeAction, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(expectedSensi, leaf.getSensitivityValue(flowCnec, RIGHT, linearGlsk, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void getSensitivityValueAfterOptimization() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        IteratingLinearOptimizationResultImpl linearOptimizationResult = Mockito.mock(IteratingLinearOptimizationResultImpl.class);
        SearchTreeInput searchTreeInput = Mockito.mock(SearchTreeInput.class);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunction.class));
        SearchTreeParameters searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class));
        when(searchTreeParameters.getTreeParameters()).thenReturn(Mockito.mock(TreeParameters.class));
        prepareLinearProblemBuilder(linearOptimizationResult);
        leaf.optimize(searchTreeInput, searchTreeParameters);

        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        SensitivityVariableSet linearGlsk = Mockito.mock(SensitivityVariableSet.class);
        double expectedSensi = 3.;

        when(linearOptimizationResult.getSensitivityValue(flowCnec, RIGHT, rangeAction, MEGAWATT)).thenReturn(expectedSensi);
        when(linearOptimizationResult.getSensitivityValue(flowCnec, RIGHT, linearGlsk, MEGAWATT)).thenReturn(expectedSensi);
        when(linearOptimizationResult.getRangeActions()).thenReturn(Set.of(rangeAction));

        assertEquals(expectedSensi, leaf.getSensitivityValue(flowCnec, RIGHT, rangeAction, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(expectedSensi, leaf.getSensitivityValue(flowCnec, RIGHT, linearGlsk, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void getSensitivityValueOnRangeActionBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        assertThrows(FaraoException.class, () -> leaf.getSensitivityValue(flowCnec, RIGHT, rangeAction, MEGAWATT));
    }

    @Test
    void getSensitivityValueOnLinearGlskBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        SensitivityVariableSet linearGlsk = Mockito.mock(SensitivityVariableSet.class);
        assertThrows(FaraoException.class, () -> leaf.getSensitivityValue(flowCnec, RIGHT, linearGlsk, MEGAWATT));
    }

    @Test
    void getObjectiveFunctionBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        assertThrows(FaraoException.class, leaf::getObjectiveFunction);
    }

    @Test
    void getObjectiveFunctionAfterOptimization() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        IteratingLinearOptimizationResultImpl linearOptimizationResult = Mockito.mock(IteratingLinearOptimizationResultImpl.class);
        SearchTreeInput searchTreeInput = Mockito.mock(SearchTreeInput.class);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunction.class));
        SearchTreeParameters searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class));
        when(searchTreeParameters.getTreeParameters()).thenReturn(Mockito.mock(TreeParameters.class));
        prepareLinearProblemBuilder(linearOptimizationResult);
        leaf.optimize(searchTreeInput, searchTreeParameters);

        ObjectiveFunction objectiveFunction = Mockito.mock(ObjectiveFunction.class);
        doReturn(objectiveFunction).when(linearOptimizationResult).getObjectiveFunction();

        assertEquals(objectiveFunction, leaf.getObjectiveFunction());
    }

    @Test
    void testFinalize() {
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        SearchTreeInput searchTreeInput = Mockito.mock(SearchTreeInput.class);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunction.class));
        SearchTreeParameters searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class));
        when(searchTreeParameters.getTreeParameters()).thenReturn(Mockito.mock(TreeParameters.class));
        prepareLinearProblemBuilder(Mockito.mock(IteratingLinearOptimizationResultImpl.class));
        rootLeaf.optimize(searchTreeInput, searchTreeParameters);
        rootLeaf.finalizeOptimization();
        assertThrows(FaraoException.class, () -> rootLeaf.optimize(searchTreeInput, searchTreeParameters));
    }

    @Test
    void testNonapplicableNa() {
        RangeActionActivationResult rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        NetworkActionCombination naCombinationToApply = Mockito.mock(NetworkActionCombination.class);
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        NetworkAction na2 = Mockito.mock(NetworkAction.class);
        when(na1.apply(any())).thenReturn(true);
        when(na2.apply(any())).thenReturn(false);
        when(naCombinationToApply.getNetworkActionSet()).thenReturn(Set.of(na1, na2));
        Set<NetworkAction> alreadyAppliedNetworkActions = Set.of();
        assertThrows(FaraoException.class, () -> new Leaf(optimizationPerimeter, network, alreadyAppliedNetworkActions, naCombinationToApply, rangeActionActivationResult, prePerimeterResult, appliedRemedialActions));
    }

    @Test
    void testToStringOnRootLeaf() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        IteratingLinearOptimizationResultImpl linearOptimizationResult = Mockito.mock(IteratingLinearOptimizationResultImpl.class);
        when(iteratingLinearOptimizer.optimize(Mockito.any(), Mockito.any())).thenReturn(linearOptimizationResult);
        SearchTreeInput searchTreeInput = Mockito.mock(SearchTreeInput.class);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunction.class));
        SearchTreeParameters searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class));
        when(searchTreeParameters.getTreeParameters()).thenReturn(Mockito.mock(TreeParameters.class));
        prepareLinearProblemBuilder(linearOptimizationResult);
        leaf.optimize(searchTreeInput, searchTreeParameters);
        when(linearOptimizationResult.getCost()).thenReturn(-100.5);
        when(linearOptimizationResult.getFunctionalCost()).thenReturn(-160.);
        // With virtual cost
        when(linearOptimizationResult.getVirtualCost()).thenReturn(59.5);
        when(linearOptimizationResult.getVirtualCostNames()).thenReturn(Set.of("mnec-violation-cost", "loopflow-violation-cost"));
        when(linearOptimizationResult.getVirtualCost("mnec-violation-cost")).thenReturn(42.2);
        when(linearOptimizationResult.getVirtualCost("loopflow-violation-cost")).thenReturn(17.3);
        assertEquals("Root leaf, no range action(s) activated, cost: -100.50 (functional: -160.00, virtual: 59.50 {mnec-violation-cost=42.2, loopflow-violation-cost=17.3})", leaf.toString());
        // Without virtual cost
        when(linearOptimizationResult.getVirtualCost()).thenReturn(0.);
        when(linearOptimizationResult.getVirtualCost("mnec-violation-cost")).thenReturn(0.);
        when(linearOptimizationResult.getVirtualCost("loopflow-violation-cost")).thenReturn(0.);
        assertEquals("Root leaf, no range action(s) activated, cost: -160.00 (functional: -160.00, virtual: 0.00)", leaf.toString());

    }
}
