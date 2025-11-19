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
import com.powsybl.action.Action;
import com.powsybl.action.HvdcActionBuilder;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.openrao.data.crac.impl.NetworkActionImpl;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.IteratingLinearOptimizer;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.IteratingLinearOptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
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

import static com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil.import16NodesNetworkWithAngleDroopHvdcs;
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
    private Action ea11;
    private Action ea12;
    private Action ea21;
    private Action ea22;
    private Action ea23;
    private RangeAction rangeAction;

    private Network network;
    private ObjectiveFunction costEvaluatorMock;
    private SensitivityComputer sensitivityComputer;
    private OptimizationPerimeter optimizationPerimeter;
    private PrePerimeterResult prePerimeterResult;
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
        when(na1.getOperator()).thenReturn("TSO1");
        when(na2.getOperator()).thenReturn("TSO2");
        ea11 = Mockito.mock(Action.class);
        ea12 = Mockito.mock(Action.class);
        ea21 = Mockito.mock(Action.class);
        ea22 = Mockito.mock(Action.class);
        ea23 = Mockito.mock(Action.class);
        when(na1.getElementaryActions()).thenReturn(Set.of(ea11, ea12));
        when(na2.getElementaryActions()).thenReturn(Set.of(ea21, ea22, ea23));

        sensitivityComputer = Mockito.mock(SensitivityComputer.class);
        costEvaluatorMock = Mockito.mock(ObjectiveFunction.class);
        optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        optimizedState = Mockito.mock(State.class);
        when(optimizationPerimeter.getMainOptimizationState()).thenReturn(optimizedState);
        when(optimizationPerimeter.copyWithFilteredAvailableHvdcRangeAction(network)).thenReturn(optimizationPerimeter);
        rangeAction = Mockito.mock(RangeAction.class);
        when(optimizationPerimeter.getRangeActions()).thenReturn(Set.of(rangeAction));
        prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
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
    void testLeafDefinitionWithAcEmulationDeactivationNetworkAction() {
        // An ac emulation deactivation action is activated.
        Network networkWithAngleDroop = import16NodesNetworkWithAngleDroopHvdcs();
        Leaf rootLeaf = new Leaf(optimizationPerimeter, networkWithAngleDroop, prePerimeterResult, appliedRemedialActions);
        RangeActionActivationResult rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        UsageRule usageRule = Mockito.mock(UsageRule.class);
        NetworkElement networkElement = Mockito.mock(NetworkElement.class);
        NetworkAction na1 = new NetworkActionImpl("na1", "na1", "TSO1", Set.of(usageRule),
            Set.of(new HvdcActionBuilder()
                .withId(String.format("%s_%s_%s", "acEmulation", "BBE2AA11 FFR3AA11 1", "DEACTIVATE"))
                .withHvdcId("BBE2AA11 FFR3AA11 1")
                .withAcEmulationEnabled(false)
                .build()), 1, 1.0, Set.of(networkElement));

        // before creation of the leaf, AC emulation is still enabled
        assertEquals(0, networkWithAngleDroop.getHvdcLine("BBE2AA11 FFR3AA11 1").getActivePowerSetpoint(), 1e-2);
        assertTrue(networkWithAngleDroop.getHvdcLine("BBE2AA11 FFR3AA11 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        // Set the active power setpoint before deactivating AC emulation. This is done in the root of the search tree.
        networkWithAngleDroop.getHvdcLine("BBE2AA11 FFR3AA11 1").setActivePowerSetpoint(812.28);

        new Leaf(optimizationPerimeter, networkWithAngleDroop, rootLeaf.getActivatedNetworkActions(), new NetworkActionCombination(na1), rangeActionActivationResult, prePerimeterResult, appliedRemedialActions);

        // AC emumation is deactivated but the active power setpoint is the one we set before hand.
        assertFalse(networkWithAngleDroop.getHvdcLine("BBE2AA11 FFR3AA11 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertEquals(812.28, networkWithAngleDroop.getHvdcLine("BBE2AA11 FFR3AA11 1").getActivePowerSetpoint(), 1e-2);
    }

    @Test
    void evaluateAnAlreadyEvaluatedLeaf() {
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        ObjectiveFunctionResult preOptimObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        when(costEvaluatorMock.evaluate(prePerimeterResult, null)).thenReturn(preOptimObjectiveFunctionResult);
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
        NetworkAction na = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf1 = prepareLeafForEvaluation(na, expectedSensitivityStatus, expectedFlowResult, expectedCost);

        leaf1.evaluate(costEvaluatorMock, sensitivityComputer);

        assertEquals(Leaf.Status.EVALUATED, leaf1.getStatus());
        assertEquals(expectedFlowResult, leaf1.getPreOptimBranchResult());
        assertEquals(expectedSensitivityStatus, leaf1.getSensitivityStatus());
        assertEquals(expectedCost, leaf1.getCost(), DOUBLE_TOLERANCE);
    }

    @Test
    void testReevaluate() {
        NetworkAction na = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf1 = prepareLeafForEvaluation(na, expectedSensitivityStatus, expectedFlowResult, expectedCost);

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
        return getLogs(OpenRaoLoggerProvider.TECHNICAL_LOGS.getClass());
    }

    private ListAppender<ILoggingEvent> getBusinessWarns() {
        return getLogs(OpenRaoLoggerProvider.BUSINESS_WARNS.getClass());
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
        rootLeaf.optimize(searchTreeInput, searchTreeParameters);
        assertEquals(1, listAppender.list.size());
        String expectedLog = String.format("[WARN] Impossible to optimize leaf: %s because evaluation has not been performed", rootLeaf);
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
        rootLeaf.optimize(searchTreeInput, searchTreeParameters);
        assertEquals(1, listAppender.list.size());
        String expectedLog = String.format("[WARN] Impossible to optimize leaf: %s because evaluation failed", rootLeaf);
        assertEquals(expectedLog, listAppender.list.get(0).toString());
    }

    @Test
    void optimize() {
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        prepareLinearProblemBuilder(Mockito.mock(IteratingLinearOptimizationResultImpl.class));
        rootLeaf.optimize(searchTreeInput, searchTreeParameters);
        assertEquals(Leaf.Status.OPTIMIZED, rootLeaf.getStatus());
    }

    @Test
    void getFlowsAndPtdfsOnFlowCnecAfterEvaluation() {
        //prepare leaf
        NetworkAction na = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf = prepareLeafForEvaluation(na, expectedSensitivityStatus, expectedFlowResult, expectedCost);

        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);

        FlowResult flowResult = Mockito.mock(FlowResult.class);
        when(sensitivityComputer.getBranchResult(network)).thenReturn(flowResult);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);

        double expectedFlow = 3.;
        Unit unit = MEGAWATT;
        when(flowResult.getFlow(flowCnec, ONE, unit)).thenReturn(expectedFlow);
        when(flowResult.getCommercialFlow(flowCnec, ONE, unit)).thenReturn(expectedFlow);
        assertEquals(expectedFlow, leaf.getFlow(flowCnec, ONE, unit), DOUBLE_TOLERANCE);
        assertEquals(expectedFlow, leaf.getCommercialFlow(flowCnec, ONE, unit), DOUBLE_TOLERANCE);

        double expectedPtdf = 4.;
        when(flowResult.getPtdfZonalSum(flowCnec, ONE)).thenReturn(expectedPtdf);
        assertEquals(expectedPtdf, leaf.getPtdfZonalSum(flowCnec, ONE), DOUBLE_TOLERANCE);

        Map<FlowCnec, Map<TwoSides, Double>> expectedPtdfZonalSums = new HashMap<>();
        when(flowResult.getPtdfZonalSums()).thenReturn(expectedPtdfZonalSums);
        assertEquals(expectedPtdfZonalSums, leaf.getPtdfZonalSums());
    }

    @Test
    void getFlowsAndPtdfsOnFlowCnecAfterOptimization() {
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        IteratingLinearOptimizationResultImpl linearOptimizationResult = Mockito.mock(IteratingLinearOptimizationResultImpl.class);
        prepareLinearProblemBuilder(linearOptimizationResult);
        leaf.optimize(searchTreeInput, searchTreeParameters);

        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);

        double expectedFlow = 3.;
        Unit unit = MEGAWATT;
        when(linearOptimizationResult.getFlow(flowCnec, ONE, unit)).thenReturn(expectedFlow);
        when(linearOptimizationResult.getCommercialFlow(flowCnec, ONE, unit)).thenReturn(expectedFlow);
        assertEquals(expectedFlow, leaf.getFlow(flowCnec, ONE, unit), DOUBLE_TOLERANCE);
        assertEquals(expectedFlow, leaf.getCommercialFlow(flowCnec, ONE, unit), DOUBLE_TOLERANCE);

        double expectedPtdf = 4.;
        when(linearOptimizationResult.getPtdfZonalSum(flowCnec, ONE)).thenReturn(expectedPtdf);
        assertEquals(expectedPtdf, leaf.getPtdfZonalSum(flowCnec, ONE), DOUBLE_TOLERANCE);

        Map<FlowCnec, Map<TwoSides, Double>> expectedPtdfZonalSums = new HashMap<>();
        when(linearOptimizationResult.getPtdfZonalSums()).thenReturn(expectedPtdfZonalSums);
        assertEquals(expectedPtdfZonalSums, leaf.getPtdfZonalSums());

    }

    @Test
    void getFlowOnFlowCnecBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> leaf.getFlow(flowCnec, ONE, MEGAWATT));
        assertEquals("No results available.", exception.getMessage());
    }

    @Test
    void getCommercialFlowOnFlowCnecBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> leaf.getCommercialFlow(flowCnec, ONE, MEGAWATT));
        assertEquals("No results available.", exception.getMessage());
    }

    @Test
    void getPtdfZonalSumOnCnecBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> leaf.getPtdfZonalSum(flowCnec, ONE));
        assertEquals("No results available.", exception.getMessage());
    }

    @Test
    void getPtdfZonalSumsBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        OpenRaoException exception = assertThrows(OpenRaoException.class, leaf::getPtdfZonalSums);
        assertEquals("No results available.", exception.getMessage());
    }

    @Test
    void getFunctionalCostAfterEvaluation() {
        NetworkAction na = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf = prepareLeafForEvaluation(na, expectedSensitivityStatus, expectedFlowResult, expectedCost);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);
        assertEquals(expectedCost / 2, leaf.getFunctionalCost(), DOUBLE_TOLERANCE);
    }

    @Test
    void getFunctionalCostAfterOptimization() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        IteratingLinearOptimizationResultImpl linearOptimizationResult = Mockito.mock(IteratingLinearOptimizationResultImpl.class);
        prepareLinearProblemBuilder(linearOptimizationResult);
        leaf.optimize(searchTreeInput, searchTreeParameters);
        double expectedFunctionalCost = 3.;
        when(linearOptimizationResult.getFunctionalCost()).thenReturn(expectedFunctionalCost);
        assertEquals(expectedFunctionalCost, leaf.getFunctionalCost(), DOUBLE_TOLERANCE);
    }

    @Test
    void getFunctionalCostBeforeOptimization() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        OpenRaoException exception = assertThrows(OpenRaoException.class, leaf::getFunctionalCost);
        assertEquals("No results available.", exception.getMessage());
    }

    @Test
    void getVirtualCostAfterEvaluation() {
        NetworkAction na = Mockito.mock(NetworkAction.class);
        when(na1.apply(any())).thenReturn(true);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        Leaf leaf = prepareLeafForEvaluation(na, expectedSensitivityStatus, expectedFlowResult, expectedCost);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);
        assertEquals(expectedCost / 2, leaf.getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(expectedCost / 2, leaf.getVirtualCost(virtualCostName), DOUBLE_TOLERANCE);
    }

    @Test
    void getVirtualCostAfterOptimization() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        IteratingLinearOptimizationResultImpl linearOptimizationResult = Mockito.mock(IteratingLinearOptimizationResultImpl.class);
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
        OpenRaoException exception = assertThrows(OpenRaoException.class, leaf::getVirtualCost);
        assertEquals("No results available.", exception.getMessage());
    }

    @Test
    void getSpecificVirtualCostBeforeOptimization() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> leaf.getVirtualCost(virtualCostName));
        assertEquals("No results available.", exception.getMessage());
    }

    @Test
    void getCostlyAndMostLimitingElementsAfterEvaluation() {
        NetworkAction na = Mockito.mock(NetworkAction.class);
        ComputationStatus expectedSensitivityStatus = Mockito.mock(ComputationStatus.class);
        FlowResult expectedFlowResult = Mockito.mock(FlowResult.class);
        double expectedCost = 5.;
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        List<FlowCnec> flowCnecs = Collections.singletonList(flowCnec);
        Leaf leaf = prepareLeafForEvaluation(na, expectedSensitivityStatus, expectedFlowResult, expectedCost, flowCnecs);
        leaf.evaluate(costEvaluatorMock, sensitivityComputer);
        assertEquals(flowCnecs, leaf.getMostLimitingElements(flowCnecs.size()));
        assertEquals(flowCnecs, leaf.getCostlyElements(virtualCostName, flowCnecs.size()));
    }

    @Test
    void getCostlyAndMostLimitingElementsAfterOptimization() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        IteratingLinearOptimizationResultImpl linearOptimizationResult = Mockito.mock(IteratingLinearOptimizationResultImpl.class);
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
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> leaf.getMostLimitingElements(0));
        assertEquals("No results available.", exception.getMessage());
    }

    @Test
    void getCostlyElementsBeforeOptimization() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> leaf.getCostlyElements(virtualCostName, 0));
        assertEquals("No results available.", exception.getMessage());
    }

    @Test
    void getVirtualCostNames() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        ObjectiveFunctionResult objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        when(costEvaluatorMock.evaluate(any(), any())).thenReturn(objectiveFunctionResult);
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
        when(optimizationPerimeter.getRangeActions()).thenReturn(rangeActions);
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

        when(optimizationPerimeter.getRangeActions()).thenReturn(rangeActions);
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
        assertEquals(Leaf.Status.CREATED, leaf.getStatus());
        assertEquals(Set.of(rangeAction), leaf.getRangeActions());
    }

    @Test
    void getOptimizedTapBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> leaf.getOptimizedTap(pstRangeAction, optimizedState));
        assertEquals("No results available.", exception.getMessage());
    }

    @Test
    void getOptimizedTapsBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> leaf.getOptimizedTapsOnState(optimizedState));
        assertEquals("No results available.", exception.getMessage());
    }

    @Test
    void getOptimizedSetpointsBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> leaf.getOptimizedSetpointsOnState(optimizedState));
        assertEquals("No results available.", exception.getMessage());
    }

    @Test
    void getOptimizedSetPointBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> leaf.getOptimizedSetpoint(rangeAction, optimizedState));
        assertEquals("No results available.", exception.getMessage());
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

        leaf.optimize(searchTreeInput, searchTreeParameters);

        assertEquals(sensitivityStatus, leaf.getSensitivityStatus());
    }

    @Test
    void getSensitivityStatusBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        OpenRaoException exception = assertThrows(OpenRaoException.class, leaf::getSensitivityStatus);
        assertEquals("No results available.", exception.getMessage());
    }

    @Test
    void getSensitivityValueAfterEvaluation() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);

        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        SensitivityVariableSet linearGlsk = Mockito.mock(SensitivityVariableSet.class);
        double expectedSensi = 3.;

        when(prePerimeterResult.getSensitivityValue(flowCnec, TWO, rangeAction, MEGAWATT)).thenReturn(expectedSensi);
        when(prePerimeterResult.getSensitivityValue(flowCnec, TWO, linearGlsk, MEGAWATT)).thenReturn(expectedSensi);

        assertEquals(expectedSensi, leaf.getSensitivityValue(flowCnec, TWO, rangeAction, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(expectedSensi, leaf.getSensitivityValue(flowCnec, TWO, linearGlsk, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void getSensitivityValueAfterOptimization() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        IteratingLinearOptimizationResultImpl linearOptimizationResult = Mockito.mock(IteratingLinearOptimizationResultImpl.class);
        prepareLinearProblemBuilder(linearOptimizationResult);
        leaf.optimize(searchTreeInput, searchTreeParameters);

        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        SensitivityVariableSet linearGlsk = Mockito.mock(SensitivityVariableSet.class);
        double expectedSensi = 3.;

        when(linearOptimizationResult.getSensitivityValue(flowCnec, TWO, rangeAction, MEGAWATT)).thenReturn(expectedSensi);
        when(linearOptimizationResult.getSensitivityValue(flowCnec, TWO, linearGlsk, MEGAWATT)).thenReturn(expectedSensi);
        when(linearOptimizationResult.getRangeActions()).thenReturn(Set.of(rangeAction));

        assertEquals(expectedSensi, leaf.getSensitivityValue(flowCnec, TWO, rangeAction, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(expectedSensi, leaf.getSensitivityValue(flowCnec, TWO, linearGlsk, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void getSensitivityValueOnRangeActionBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> leaf.getSensitivityValue(flowCnec, TWO, rangeAction, MEGAWATT));
        assertEquals("No results available.", exception.getMessage());
    }

    @Test
    void getSensitivityValueOnLinearGlskBeforeEvaluation() {
        Leaf leaf = buildNotEvaluatedRootLeaf();
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        SensitivityVariableSet linearGlsk = Mockito.mock(SensitivityVariableSet.class);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> leaf.getSensitivityValue(flowCnec, TWO, linearGlsk, MEGAWATT));
        assertEquals("No results available.", exception.getMessage());
    }

    @Test
    void testFinalize() {
        ComputationStatus sensitivityStatus = Mockito.mock(ComputationStatus.class);
        when(prePerimeterResult.getSensitivityStatus()).thenReturn(sensitivityStatus);
        Leaf rootLeaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        prepareLinearProblemBuilder(Mockito.mock(IteratingLinearOptimizationResultImpl.class));
        rootLeaf.optimize(searchTreeInput, searchTreeParameters);
        rootLeaf.finalizeOptimization();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> rootLeaf.optimize(searchTreeInput, searchTreeParameters));
        assertEquals("Cannot optimize leaf, because optimization data has been deleted", exception.getMessage());
    }

    @Test
    void testNonapplicableNa() {
        RangeActionActivationResult rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        NetworkActionCombination naCombinationToApply = Mockito.mock(NetworkActionCombination.class);
        when(na1.apply(any())).thenReturn(true);
        when(na2.apply(any())).thenReturn(false);
        when(naCombinationToApply.getNetworkActionSet()).thenReturn(Set.of(na1, na2));
        Set<NetworkAction> alreadyAppliedNetworkActions = Set.of();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new Leaf(optimizationPerimeter, network, alreadyAppliedNetworkActions, naCombinationToApply, rangeActionActivationResult, prePerimeterResult, appliedRemedialActions));
        assertEquals("null could not be applied on the network", exception.getMessage());
    }

    @Test
    void testToStringOnRootLeaf() {
        Leaf leaf = new Leaf(optimizationPerimeter, network, prePerimeterResult, appliedRemedialActions);
        IteratingLinearOptimizationResultImpl linearOptimizationResult = Mockito.mock(IteratingLinearOptimizationResultImpl.class);
        when(IteratingLinearOptimizer.optimize(Mockito.any(), Mockito.any())).thenReturn(linearOptimizationResult);
        prepareLinearProblemBuilder(linearOptimizationResult);
        leaf.optimize(searchTreeInput, searchTreeParameters);
        when(linearOptimizationResult.getCost()).thenReturn(-100.5);
        when(linearOptimizationResult.getFunctionalCost()).thenReturn(-160.);
        // With virtual cost
        when(linearOptimizationResult.getVirtualCost()).thenReturn(59.5);
        when(linearOptimizationResult.getVirtualCostNames()).thenReturn(Set.of("mnec-violation-cost", "loopflow-violation-cost"));
        when(linearOptimizationResult.getVirtualCost("mnec-violation-cost")).thenReturn(42.2);
        when(linearOptimizationResult.getVirtualCost("loopflow-violation-cost")).thenReturn(17.3);
        assertEquals("Root leaf, no range action(s) activated, cost: -100.5 (functional: -160.0, virtual: 59.5 {mnec-violation-cost=42.2, loopflow-violation-cost=17.3})", leaf.toString());
        // Without virtual cost
        when(linearOptimizationResult.getVirtualCost()).thenReturn(0.);
        when(linearOptimizationResult.getVirtualCost("mnec-violation-cost")).thenReturn(0.);
        when(linearOptimizationResult.getVirtualCost("loopflow-violation-cost")).thenReturn(0.);
        assertEquals("Root leaf, no range action(s) activated, cost: -160.0 (functional: -160.0, virtual: 0.0)", leaf.toString());
    }

    @Test
    void testRaLimitationsMaxRa() {
        Instant primaryInstant = optimizedState.getInstant();

        Instant secondaryInstant = Mockito.mock(Instant.class);
        State secondaryStateWithActions = Mockito.mock(State.class);
        when(secondaryStateWithActions.getInstant()).thenReturn(secondaryInstant);
        State secondaryStateWithoutActions = Mockito.mock(State.class);
        when(secondaryStateWithoutActions.getInstant()).thenReturn(secondaryInstant);

        Instant secondaryInstantWithoutLimit = Mockito.mock(Instant.class);
        State nonLimitedState = Mockito.mock(State.class);
        when(nonLimitedState.getInstant()).thenReturn(secondaryInstantWithoutLimit);

        when(optimizationPerimeter.getRangeActionOptimizationStates()).thenReturn(Set.of(optimizedState, secondaryStateWithoutActions, secondaryStateWithActions, nonLimitedState));

        RaUsageLimits primaryInstantRaUsageLimits = new RaUsageLimits();
        primaryInstantRaUsageLimits.setMaxRa(3);
        RaUsageLimits secondaryInstantRaUsageLimits = new RaUsageLimits();
        secondaryInstantRaUsageLimits.setMaxRa(8);
        Map<Instant, RaUsageLimits> raUsageLimitsMap = Map.of(primaryInstant, primaryInstantRaUsageLimits,
            secondaryInstant, secondaryInstantRaUsageLimits);
        when(searchTreeParameters.getRaLimitationParameters()).thenReturn(raUsageLimitsMap);

        NetworkActionCombination networkActionCombination = new NetworkActionCombination(Set.of(na1));
        when(appliedRemedialActions.getAppliedNetworkActions(secondaryStateWithActions)).thenReturn(Set.of(na1, na2));
        Leaf leaf = new Leaf(optimizationPerimeter, network, new HashSet<>(), networkActionCombination,
            Mockito.mock(RangeActionActivationResultImpl.class), prePerimeterResult, appliedRemedialActions);

        RangeActionLimitationParameters raLimitationParameters = leaf.getRaLimitationParameters(optimizationPerimeter, searchTreeParameters);
        assertEquals(null, raLimitationParameters.getMaxRangeActions(nonLimitedState));
        //3 - 1 from na combination
        assertEquals(2, raLimitationParameters.getMaxRangeActions(optimizedState));
        //8 - 2 from applied remedial actions
        assertEquals(6, raLimitationParameters.getMaxRangeActions(secondaryStateWithActions));
        //8
        assertEquals(8, raLimitationParameters.getMaxRangeActions(secondaryStateWithoutActions));
    }

    @Test
    void testRaLimitationsMaxRaPerTso() {
        Instant primaryInstant = optimizedState.getInstant();

        Instant secondaryInstant = Mockito.mock(Instant.class);
        State secondaryStateWithActions = Mockito.mock(State.class);
        when(secondaryStateWithActions.getInstant()).thenReturn(secondaryInstant);
        State secondaryStateWithoutActions = Mockito.mock(State.class);
        when(secondaryStateWithoutActions.getInstant()).thenReturn(secondaryInstant);

        Instant secondaryInstantWithoutLimit = Mockito.mock(Instant.class);
        State nonLimitedState = Mockito.mock(State.class);
        when(nonLimitedState.getInstant()).thenReturn(secondaryInstantWithoutLimit);

        when(optimizationPerimeter.getRangeActionOptimizationStates()).thenReturn(Set.of(optimizedState, secondaryStateWithoutActions, secondaryStateWithActions, nonLimitedState));

        RaUsageLimits primaryInstantRaUsageLimits = new RaUsageLimits();
        primaryInstantRaUsageLimits.setMaxRaPerTso(Map.of("TSO1", 3, "TSO2", 45));
        RaUsageLimits secondaryInstantRaUsageLimits = new RaUsageLimits();
        secondaryInstantRaUsageLimits.setMaxRaPerTso(Map.of("TSO1", 23, "TSO2", 8));
        Map<Instant, RaUsageLimits> raUsageLimitsMap = Map.of(primaryInstant, primaryInstantRaUsageLimits,
            secondaryInstant, secondaryInstantRaUsageLimits);
        when(searchTreeParameters.getRaLimitationParameters()).thenReturn(raUsageLimitsMap);

        NetworkActionCombination networkActionCombination = new NetworkActionCombination(Set.of(na1));
        when(appliedRemedialActions.getAppliedNetworkActions(secondaryStateWithActions)).thenReturn(Set.of(na1, na2));
        Leaf leaf = new Leaf(optimizationPerimeter, network, new HashSet<>(), networkActionCombination,
            Mockito.mock(RangeActionActivationResultImpl.class), prePerimeterResult, appliedRemedialActions);

        RangeActionLimitationParameters raLimitationParameters = leaf.getRaLimitationParameters(optimizationPerimeter, searchTreeParameters);
        assertEquals(null, raLimitationParameters.getMaxRangeActionPerTso(nonLimitedState).get("TSO1"));
        assertEquals(null, raLimitationParameters.getMaxRangeActionPerTso(nonLimitedState).get("TSO2"));
        //3 - 1 from na combination
        assertEquals(2, raLimitationParameters.getMaxRangeActionPerTso(optimizedState).get("TSO1"));
        assertEquals(45, raLimitationParameters.getMaxRangeActionPerTso(optimizedState).get("TSO2"));
        //23 - 1 from applied remedial actions
        //8 - 1
        assertEquals(22, raLimitationParameters.getMaxRangeActionPerTso(secondaryStateWithActions).get("TSO1"));
        assertEquals(7, raLimitationParameters.getMaxRangeActionPerTso(secondaryStateWithActions).get("TSO2"));
        //8
        assertEquals(23, raLimitationParameters.getMaxRangeActionPerTso(secondaryStateWithoutActions).get("TSO1"));
        assertEquals(8, raLimitationParameters.getMaxRangeActionPerTso(secondaryStateWithoutActions).get("TSO2"));
    }

    @Test
    void testRaLimitationsMaxElementaryActionsPerTso() {
        Instant primaryInstant = optimizedState.getInstant();

        Instant secondaryInstant = Mockito.mock(Instant.class);
        State secondaryStateWithActions = Mockito.mock(State.class);
        when(secondaryStateWithActions.getInstant()).thenReturn(secondaryInstant);
        State secondaryStateWithoutActions = Mockito.mock(State.class);
        when(secondaryStateWithoutActions.getInstant()).thenReturn(secondaryInstant);

        Instant secondaryInstantWithoutLimit = Mockito.mock(Instant.class);
        State nonLimitedState = Mockito.mock(State.class);
        when(nonLimitedState.getInstant()).thenReturn(secondaryInstantWithoutLimit);

        when(optimizationPerimeter.getRangeActionOptimizationStates()).thenReturn(Set.of(optimizedState, secondaryStateWithoutActions, secondaryStateWithActions, nonLimitedState));

        RaUsageLimits primaryInstantRaUsageLimits = new RaUsageLimits();
        primaryInstantRaUsageLimits.setMaxElementaryActionsPerTso(Map.of("TSO1", 3, "TSO2", 45));
        RaUsageLimits secondaryInstantRaUsageLimits = new RaUsageLimits();
        secondaryInstantRaUsageLimits.setMaxElementaryActionsPerTso(Map.of("TSO1", 23, "TSO2", 8));
        Map<Instant, RaUsageLimits> raUsageLimitsMap = Map.of(primaryInstant, primaryInstantRaUsageLimits,
            secondaryInstant, secondaryInstantRaUsageLimits);
        when(searchTreeParameters.getRaLimitationParameters()).thenReturn(raUsageLimitsMap);

        NetworkActionCombination networkActionCombination = new NetworkActionCombination(Set.of(na1));
        when(appliedRemedialActions.getAppliedNetworkActions(secondaryStateWithActions)).thenReturn(Set.of(na1, na2));
        Leaf leaf = new Leaf(optimizationPerimeter, network, new HashSet<>(), networkActionCombination,
            Mockito.mock(RangeActionActivationResultImpl.class), prePerimeterResult, appliedRemedialActions);

        RangeActionLimitationParameters raLimitationParameters = leaf.getRaLimitationParameters(optimizationPerimeter, searchTreeParameters);
        assertEquals(null, raLimitationParameters.getMaxElementaryActionsPerTso(nonLimitedState).get("TSO1"));
        assertEquals(null, raLimitationParameters.getMaxElementaryActionsPerTso(nonLimitedState).get("TSO2"));
        //3 - 2 elementary actions from na combination
        assertEquals(1, raLimitationParameters.getMaxElementaryActionsPerTso(optimizedState).get("TSO1"));
        assertEquals(45, raLimitationParameters.getMaxElementaryActionsPerTso(optimizedState).get("TSO2"));
        //23 - 2 elementary actions from applied remedial actions
        //8 - 3 elementary actions from applied remedial actions
        assertEquals(21, raLimitationParameters.getMaxElementaryActionsPerTso(secondaryStateWithActions).get("TSO1"));
        assertEquals(5, raLimitationParameters.getMaxElementaryActionsPerTso(secondaryStateWithActions).get("TSO2"));
        //8
        assertEquals(23, raLimitationParameters.getMaxElementaryActionsPerTso(secondaryStateWithoutActions).get("TSO1"));
        assertEquals(8, raLimitationParameters.getMaxElementaryActionsPerTso(secondaryStateWithoutActions).get("TSO2"));
    }
}
