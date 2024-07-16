/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.IteratingLinearOptimizer;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.MultiStateRemedialActionResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PerimeterResultWithCnecs;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.SearchTreeResult;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
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
import static com.powsybl.iidm.network.TwoSides.TWO;
import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.openrao.commons.Unit.*;
import static org.mockito.Mockito.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class LeafTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;

    private NetworkAction na1;
    private NetworkAction na2;

    private Network network;
    private ObjectiveFunction costEvaluatorMock;
    private SensitivityComputer sensitivityComputer;
    private OptimizationPerimeter optimizationPerimeter;
    private PerimeterResultWithCnecs prePerimeterResult;
    private AppliedRemedialActions appliedRemedialActions;
    private State optimizedState;
    private SearchTreeInput searchTreeInput;
    private SearchTreeParameters searchTreeParameters;

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

        sensitivityComputer = Mockito.mock(SensitivityComputer.class);
        costEvaluatorMock = Mockito.mock(ObjectiveFunction.class);
        optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        optimizedState = Mockito.mock(State.class);
        when(optimizationPerimeter.getMainOptimizationState()).thenReturn(optimizedState);
        when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(Map.of(optimizedState, new HashSet<>()));
        prePerimeterResult = Mockito.mock(PerimeterResultWithCnecs.class);
        appliedRemedialActions = Mockito.mock(AppliedRemedialActions.class);
        Instant instant = Mockito.mock(Instant.class);
        when(optimizedState.getInstant()).thenReturn(instant);
        when(instant.getId()).thenReturn("curative");
        searchTreeInput = Mockito.mock(SearchTreeInput.class);
        when(searchTreeInput.getOptimizationPerimeter()).thenReturn(optimizationPerimeter);
        when(searchTreeInput.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunction.class));
        Instant outageInstant = Mockito.mock(Instant.class);
        when(searchTreeInput.getOutageInstant()).thenReturn(outageInstant);
        searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class));
        when(searchTreeParameters.getTreeParameters()).thenReturn(Mockito.mock(TreeParameters.class));

        virtualCostName = "VirtualCost";
        linearProblemMockedStatic = mockStatic(LinearProblem.class);
        iteratingLinearOptimizerMockedStatic = mockStatic(IteratingLinearOptimizer.class);
    }

    @AfterEach
    public void tearDown() {
        linearProblemMockedStatic.close();
        iteratingLinearOptimizerMockedStatic.close();
    }

    private void prepareLinearProblemBuilder(SearchTreeResult linearOptimizationResult) {
        LinearProblemBuilder linearProblemBuilder = Mockito.mock(LinearProblemBuilder.class);
        LinearProblem linearProblem = Mockito.mock(LinearProblem.class);
        when(linearProblemBuilder.buildFromInputsAndParameters(Mockito.any(), Mockito.any())).thenReturn(linearProblem);
        linearProblemMockedStatic.when(LinearProblem::create).thenReturn(linearProblemBuilder);
        iteratingLinearOptimizerMockedStatic.when(() -> IteratingLinearOptimizer.optimize(Mockito.any(), Mockito.any())).thenReturn(linearOptimizationResult);
    }

    @Test
    void testRootLeafDefinition() {
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions, costEvaluatorMock);
        assertTrue(rootLeaf.getResult().getPerimeterResultWithCnecs().getActivatedNetworkActions().isEmpty());
        assertTrue(rootLeaf.isRoot());
        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
    }

    @Test
    void testMultipleLeavesDefinition() {
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(sensitivityComputer.getSensitivityResult()).thenReturn(sensitivityResult);
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions, costEvaluatorMock);
        Leaf leaf1 = new Leaf(optimizationPerimeter, network, prePerimeterResult, rootLeaf.getResult(), new NetworkActionCombination(na1), appliedRemedialActions, true);
        leaf1.evaluate(costEvaluatorMock, sensitivityComputer);
        Leaf leaf2 = new Leaf(optimizationPerimeter, network, prePerimeterResult, leaf1.getResult(), new NetworkActionCombination(na2), appliedRemedialActions, true);
        leaf2.evaluate(costEvaluatorMock, sensitivityComputer);

        assertFalse(leaf1.isRoot());
        assertFalse(leaf2.isRoot());

        assertEquals(Leaf.Status.EVALUATED, leaf1.getStatus());
        assertEquals(Leaf.Status.EVALUATED, leaf2.getStatus());

        assertTrue(leaf1.getResult().getPerimeterResultWithCnecs().isActivated(na1));
        assertFalse(leaf1.getResult().getPerimeterResultWithCnecs().isActivated(na2));
        assertTrue(leaf2.getResult().getPerimeterResultWithCnecs().isActivated(na1));
        assertTrue(leaf2.getResult().getPerimeterResultWithCnecs().isActivated(na2));
    }

    @Test
    void testMultipleLeafDefinitionWithSameNetworkAction() {
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf1 = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost);

        leaf1.evaluate(costEvaluatorMock, sensitivityComputer);
        Leaf leaf2 = new Leaf(optimizationPerimeter, network, prePerimeterResult, leaf1.getResult(), new NetworkActionCombination(na1), appliedRemedialActions, true);
        leaf2.evaluate(costEvaluatorMock, sensitivityComputer);

        assertEquals(1, leaf2.getResult().getPerimeterResultWithCnecs().getActivatedNetworkActions().size());
        assertTrue(leaf2.getResult().getPerimeterResultWithCnecs().isActivated(na1));
        assertFalse(leaf2.isRoot());
    }

    @Test
    void evaluateAnAlreadyEvaluatedLeaf() {
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions, costEvaluatorMock);
        ObjectiveFunctionResult preOptimObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        when(costEvaluatorMock.evaluate(prePerimeterResult, prePerimeterResult)).thenReturn(preOptimObjectiveFunctionResult);

        rootLeaf.evaluate(costEvaluatorMock, sensitivityComputer);
        assertEquals(Leaf.Status.EVALUATED, rootLeaf.getStatus());
    }

    private Leaf prepareLeafForEvaluation(NetworkAction networkAction, ComputationStatus expectedSensitivityStatus, FlowResult expectedFlowResult, double expectedCost, List<FlowCnec> mostLimitingCnecs) {
        when(networkAction.apply(any())).thenReturn(true);
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions, costEvaluatorMock);
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, rootLeaf.getResult(), new NetworkActionCombination(networkAction), appliedRemedialActions, false);
        SensitivityResult expectedSensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityComputer.getSensitivityResult()).thenReturn(expectedSensitivityResult);
        when(expectedSensitivityResult.getSensitivityStatus()).thenReturn(expectedSensitivityStatus);
        when(sensitivityComputer.getBranchResult(network)).thenReturn(expectedFlowResult);
        ObjectiveFunctionResult expectedObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        when(costEvaluatorMock.evaluate(any(), any())).thenReturn(expectedObjectiveFunctionResult);
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
        PerimeterResultWithCnecs expectedFlowResult = Mockito.mock(PerimeterResultWithCnecs.class);
        double expectedCost = 5.;
        Leaf leaf1 = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost);

        leaf1.evaluate(costEvaluatorMock, sensitivityComputer);

        assertEquals(Leaf.Status.EVALUATED, leaf1.getStatus());
        assertEquals(expectedSensitivityStatus, leaf1.getResult().getPerimeterResultWithCnecs().getSensitivityStatus());
        assertEquals(expectedCost, leaf1.getResult().getPerimeterResultWithCnecs().getCost(), DOUBLE_TOLERANCE);
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

    private ListAppender<ILoggingEvent> getLogs(Class<?> clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    private ListAppender<ILoggingEvent> getTechnicalLogs() {
        return getLogs(OpenRaoLoggerProvider.TECHNICAL_LOGS.getClass());
    }

    private ListAppender<ILoggingEvent> getBusinessWarns() {
        return getLogs(OpenRaoLoggerProvider.BUSINESS_WARNS.getClass());
    }

    @Test
    void testEvaluateError() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost);

        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        Mockito.when(sensitivityComputer.getSensitivityResult()).thenReturn(sensitivityResult);
        Mockito.when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        Mockito.doNothing().when(sensitivityComputer).compute(network);

        leaf.evaluate(costEvaluatorMock, sensitivityComputer);

        assertEquals(Leaf.Status.ERROR, leaf.getStatus());
    }

    @Test
    void testOptimizeWithoutEvaluation() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost);

        assertEquals(Leaf.Status.CREATED, leaf.getStatus());
        ListAppender<ILoggingEvent> listAppender = getBusinessWarns();
        leaf.optimize(searchTreeInput, searchTreeParameters);
        assertEquals(1, listAppender.list.size());
        String expectedLog = String.format("[WARN] Impossible to optimize leaf: %s\n because evaluation has not been performed", leaf);
        assertEquals(expectedLog, listAppender.list.get(0).toString());
    }

    @Test
    void testOptimizeWithError() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost);

        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        Mockito.when(sensitivityComputer.getSensitivityResult()).thenReturn(sensitivityResult);
        Mockito.when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        Mockito.doNothing().when(sensitivityComputer).compute(network);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);
        ListAppender<ILoggingEvent> listAppender = getBusinessWarns();
        leaf.optimize(searchTreeInput, searchTreeParameters);
        assertEquals(1, listAppender.list.size());
        String expectedLog = String.format("[WARN] Impossible to optimize leaf: %s\n because evaluation failed", leaf);
        assertEquals(expectedLog, listAppender.list.get(0).toString());
    }

    @Test
    void optimize() {
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions, costEvaluatorMock);
        prepareLinearProblemBuilder(Mockito.mock(SearchTreeResult.class));
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
        PerimeterResultWithCnecs perimeterResultWithCnecs = leaf.getResult().getPerimeterResultWithCnecs();

        double expectedFlow = 3.;
        Unit unit = MEGAWATT;
        when(flowResult.getFlow(flowCnec, ONE, unit)).thenReturn(expectedFlow);
        when(flowResult.getCommercialFlow(flowCnec, ONE, unit)).thenReturn(expectedFlow);
        assertEquals(expectedFlow, perimeterResultWithCnecs.getFlow(flowCnec, ONE, unit), DOUBLE_TOLERANCE);
        assertEquals(expectedFlow, perimeterResultWithCnecs.getCommercialFlow(flowCnec, ONE, unit), DOUBLE_TOLERANCE);

        double expectedPtdf = 4.;
        when(flowResult.getPtdfZonalSum(flowCnec, ONE)).thenReturn(expectedPtdf);
        assertEquals(expectedPtdf, perimeterResultWithCnecs.getPtdfZonalSum(flowCnec, ONE), DOUBLE_TOLERANCE);

        Map<FlowCnec, Map<TwoSides, Double>> expectedPtdfZonalSums = new HashMap<>();
        when(flowResult.getPtdfZonalSums()).thenReturn(expectedPtdfZonalSums);
        assertEquals(expectedPtdfZonalSums, perimeterResultWithCnecs.getPtdfZonalSums());
    }

    @Test
    void getResultAfterOptimization() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions, costEvaluatorMock);
        SearchTreeResult linearOptimizationResult = Mockito.mock(SearchTreeResult.class);
        prepareLinearProblemBuilder(linearOptimizationResult);
        leaf.optimize(searchTreeInput, searchTreeParameters);

        assertEquals(linearOptimizationResult, leaf.getResult());
    }

    @Test
    void getFunctionalCostAfterEvaluation() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf = prepareLeafForEvaluation(na1, expectedSensitivityStatus, expectedFlowResult, expectedCost);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);
        assertEquals(expectedCost / 2, leaf.getResult().getPerimeterResultWithCnecs().getFunctionalCost(), DOUBLE_TOLERANCE);
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
        assertEquals(expectedCost / 2, leaf.getResult().getPerimeterResultWithCnecs().getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(expectedCost / 2, leaf.getResult().getPerimeterResultWithCnecs().getVirtualCost(virtualCostName), DOUBLE_TOLERANCE);
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
        assertEquals(flowCnecs, leaf.getResult().getPerimeterResultWithCnecs().getMostLimitingElements(flowCnecs.size()));
        assertEquals(flowCnecs, leaf.getResult().getPerimeterResultWithCnecs().getCostlyElements(virtualCostName, flowCnecs.size()));
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
        when(prePerimeterResult.getOptimizedSetpoints()).thenReturn(optimizedSetPoints);
        when(optimizationPerimeter.getRangeActions()).thenReturn(rangeActions);
        when(prePerimeterResult.getOptimizedTap(pstRangeAction)).thenReturn(optimalTap);
        when(prePerimeterResult.getOptimizedSetpoint(rangeAction)).thenReturn(optimalSetpoint);
        when(prePerimeterResult.getOptimizedSetpoint(pstRangeAction)).thenReturn(optimalSetpoint);

        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions, costEvaluatorMock);

        assertEquals(rangeActions, leaf.getResult().getPerimeterResultWithCnecs().getRangeActions());
        assertEquals(optimalTap, leaf.getResult().getPerimeterResultWithCnecs().getOptimizedTap(pstRangeAction));
        assertEquals(optimizedTaps, leaf.getResult().getPerimeterResultWithCnecs().getOptimizedTaps());
        assertEquals(optimalSetpoint, leaf.getResult().getPerimeterResultWithCnecs().getOptimizedSetpoint(rangeAction), DOUBLE_TOLERANCE);
        assertEquals(optimizedSetPoints, leaf.getResult().getPerimeterResultWithCnecs().getOptimizedSetpoints());
    }

    @Test
    void getSensitivityStatusAfterEvaluation() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions, costEvaluatorMock);
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        assertEquals(sensitivityStatus, leaf.getResult().getPerimeterResultWithCnecs().getSensitivityStatus());
    }

    @Test
    void getSensitivityValueAfterEvaluation() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions, costEvaluatorMock);

        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        SensitivityVariableSet linearGlsk = Mockito.mock(SensitivityVariableSet.class);
        double expectedSensi = 3.;

        when(prePerimeterResult.getSensitivityValue(flowCnec, TWO, rangeAction, MEGAWATT)).thenReturn(expectedSensi);
        when(prePerimeterResult.getSensitivityValue(flowCnec, TWO, linearGlsk, MEGAWATT)).thenReturn(expectedSensi);

        assertEquals(expectedSensi, leaf.getResult().getPerimeterResultWithCnecs().getSensitivityValue(flowCnec, TWO, rangeAction, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(expectedSensi, leaf.getResult().getPerimeterResultWithCnecs().getSensitivityValue(flowCnec, TWO, linearGlsk, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testFinalize() {
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions, costEvaluatorMock);
        prepareLinearProblemBuilder(Mockito.mock(SearchTreeResult.class));
        rootLeaf.optimize(searchTreeInput, searchTreeParameters);
        rootLeaf.finalizeOptimization();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> rootLeaf.optimize(searchTreeInput, searchTreeParameters));
        assertEquals("Cannot optimize leaf, because optimization data has been deleted", exception.getMessage());
    }

    @Test
    void testNonapplicableNa() {
        NetworkActionCombination naCombinationToApply = Mockito.mock(NetworkActionCombination.class);
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        NetworkAction na2 = Mockito.mock(NetworkAction.class);
        when(na1.apply(any())).thenReturn(true);
        when(na2.apply(any())).thenReturn(false);
        when(naCombinationToApply.getNetworkActionSet()).thenReturn(Set.of(na1, na2));
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions, costEvaluatorMock);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new Leaf(optimizationPerimeter, network, prePerimeterResult, rootLeaf.getResult(), naCombinationToApply, appliedRemedialActions, false));
        assertEquals("null could not be applied on the network", exception.getMessage());
    }

    @Test
    void testToStringOnRootLeaf() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions, costEvaluatorMock);
        SearchTreeResult linearOptimizationResult = Mockito.mock(SearchTreeResult.class);
        MultiStateRemedialActionResultImpl multiStateRemedialActionResult = Mockito.mock(MultiStateRemedialActionResultImpl.class);
        when(linearOptimizationResult.getAllStatesRemedialActionResult()).thenReturn(multiStateRemedialActionResult);
        RangeActionResultImpl rangeActionResult = Mockito.mock(RangeActionResultImpl.class);
        when(multiStateRemedialActionResult.getRangeActionResultOnState(any())).thenReturn(rangeActionResult);
        PerimeterResultWithCnecs perimeterResultWithCnecs = Mockito.mock(PerimeterResultWithCnecs.class);
        when(linearOptimizationResult.getPerimeterResultWithCnecs()).thenReturn(perimeterResultWithCnecs);
        when(IteratingLinearOptimizer.optimize(Mockito.any(), Mockito.any())).thenReturn(linearOptimizationResult);
        prepareLinearProblemBuilder(linearOptimizationResult);
        leaf.optimize(searchTreeInput, searchTreeParameters);

        when(perimeterResultWithCnecs.getFunctionalCost()).thenReturn(-160.);

        // With virtual cost
        when(perimeterResultWithCnecs.getVirtualCost()).thenReturn(59.5);
        when(perimeterResultWithCnecs.getCost()).thenReturn(-100.5);
        when(perimeterResultWithCnecs.getVirtualCostNames()).thenReturn(Set.of("mnec-violation-cost", "loopflow-violation-cost"));
        when(perimeterResultWithCnecs.getVirtualCost("mnec-violation-cost")).thenReturn(42.2);
        when(perimeterResultWithCnecs.getVirtualCost("loopflow-violation-cost")).thenReturn(17.3);
        assertEquals("Root leaf, no range action(s) activated, cost: -100.50 (functional: -160.00, virtual: 59.50 {mnec-violation-cost=42.2, loopflow-violation-cost=17.3})", leaf.toString());

        // Without virtual cost
        when(perimeterResultWithCnecs.getVirtualCost()).thenReturn(0.);
        when(perimeterResultWithCnecs.getCost()).thenReturn(-160.);
        when(perimeterResultWithCnecs.getVirtualCost("mnec-violation-cost")).thenReturn(0.);
        when(perimeterResultWithCnecs.getVirtualCost("loopflow-violation-cost")).thenReturn(0.);
        assertEquals("Root leaf, no range action(s) activated, cost: -160.00 (functional: -160.00, virtual: 0.00)", leaf.toString());
    }

    @Test
    void testRaLimitations() {
        Instant instant = Mockito.mock(Instant.class);
        when(optimizedState.getInstant()).thenReturn(instant);
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions, costEvaluatorMock);
        RaUsageLimits raUsageLimits = new RaUsageLimits();
        raUsageLimits.setMaxRa(3);

        // test for instant not present in searchTreeParameters
        Instant curativeInstant = Mockito.mock(Instant.class);
        when(curativeInstant.getId()).thenReturn("curative");
        Map<Instant, RaUsageLimits> raUsageLimitsMapForCurative = Map.of(curativeInstant, raUsageLimits);
        when(searchTreeParameters.getRaLimitationParameters()).thenReturn(raUsageLimitsMapForCurative);
        when(instant.getId()).thenReturn("preventive");
        assertNull(leaf.getRaLimitationParameters(optimizationPerimeter, searchTreeParameters));

        // test for preventive without topological actions
        Map<Instant, RaUsageLimits> raUsageLimitsMap = Map.of(instant, raUsageLimits);
        when(searchTreeParameters.getRaLimitationParameters()).thenReturn(raUsageLimitsMap);
        RangeActionLimitationParameters raLimitationParameters = leaf.getRaLimitationParameters(optimizationPerimeter, searchTreeParameters);
        assertEquals(3, raLimitationParameters.getMaxRangeActions(optimizedState));

        // test for preventive with 1 topological actions
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions, costEvaluatorMock);
        Leaf leaftWith1Topo = new Leaf(optimizationPerimeter, network, prePerimeterResult, rootLeaf.getResult(), new NetworkActionCombination(na1), appliedRemedialActions, false);
        raLimitationParameters = leaftWith1Topo.getRaLimitationParameters(optimizationPerimeter, searchTreeParameters);
        assertEquals(2, raLimitationParameters.getMaxRangeActions(optimizedState));

        // test for 2nd preventive
        OptimizationPerimeter secondPreventivePerimeter = Mockito.mock(GlobalOptimizationPerimeter.class);
        when(secondPreventivePerimeter.getRangeActionOptimizationStates()).thenReturn(Set.of(optimizedState));
        when(secondPreventivePerimeter.getMainOptimizationState()).thenReturn(optimizedState);
        when(instant.isCurative()).thenReturn(true);
        when(appliedRemedialActions.getAppliedNetworkActions(optimizedState)).thenReturn(Set.of(na1, na2));
        Leaf leaf2ndPreventive = new Leaf(secondPreventivePerimeter, network, prePerimeterResult, appliedRemedialActions, costEvaluatorMock);
        raLimitationParameters = leaf2ndPreventive.getRaLimitationParameters(secondPreventivePerimeter, searchTreeParameters);
        assertEquals(1, raLimitationParameters.getMaxRangeActions(optimizedState));

        // test for curative
        raUsageLimitsMap = Map.of(curativeInstant, raUsageLimits);
        when(searchTreeParameters.getRaLimitationParameters()).thenReturn(raUsageLimitsMap);
        when(optimizedState.getInstant()).thenReturn(curativeInstant);
        raLimitationParameters = leaf.getRaLimitationParameters(optimizationPerimeter, searchTreeParameters);
        assertEquals(3, raLimitationParameters.getMaxRangeActions(optimizedState));
    }
}
