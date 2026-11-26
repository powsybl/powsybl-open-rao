/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.CostResult;
import com.powsybl.openrao.data.raoresult.api.extension.Metadata;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoimpl.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.raoimpl.commons.objectivefunctionevaluator.SensitivityFailureOvercostEvaluator;
import com.powsybl.openrao.raoimpl.result.api.*;
import com.powsybl.openrao.raoimpl.result.extension.TimeCoupledCostResult;
import com.powsybl.openrao.raoimpl.result.impl.GlobalLinearOptimizationResult;
import com.powsybl.openrao.raoimpl.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.raoimpl.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.raoimpl.result.impl.RemedialActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters;
import static com.powsybl.openrao.raoimpl.algorithms.MarmotUtils.runInitialSensitivityAnalysis;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
class MarmotUtilsTest {
    private Crac crac1;
    private Crac crac2;
    private Crac crac3;
    private final OffsetDateTime timestamp1 = OffsetDateTime.of(2024, 12, 10, 16, 21, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp2 = OffsetDateTime.of(2024, 12, 10, 17, 21, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp3 = OffsetDateTime.of(2024, 12, 10, 18, 21, 0, 0, ZoneOffset.UTC);
    RaoParameters parameters;
    TemporalData<RaoInput> inputs;
    private static final double DOUBLE_TOLERANCE = 1e-4;
    private static final double AMPERE_MEGAWATT_TOLERANCE = 1.0;

    @BeforeEach
    void setUp() throws IOException {
        Network network1 = Network.read("12Nodes_2_pst.uct", MarmotUtilsTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));
        Network network2 = Network.read("12Nodes_2_pst.uct", MarmotUtilsTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));
        Network network3 = Network.read("12Nodes_2_pst.uct", MarmotUtilsTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));

        crac1 = Crac.read("small-crac-2pst-1600.json", MarmotUtilsTest.class.getResourceAsStream("/crac/small-crac-2pst-1600.json"), network1);
        crac2 = Crac.read("small-crac-2pst-1700.json", MarmotUtilsTest.class.getResourceAsStream("/crac/small-crac-2pst-1700.json"), network2);
        crac3 = Crac.read("small-crac-2pst-1800.json", MarmotUtilsTest.class.getResourceAsStream("/crac/small-crac-2pst-1800.json"), network3);

        RaoInput raoInput1 = RaoInput.build(network1, crac1).build();
        RaoInput raoInput2 = RaoInput.build(network2, crac2).build();
        RaoInput raoInput3 = RaoInput.build(network3, crac3).build();

        inputs = new TemporalDataImpl<>(Map.of(timestamp1, raoInput1, timestamp2, raoInput2, timestamp3, raoInput3));
        parameters = new RaoParameters(ReportNode.NO_OP);
        parameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters(ReportNode.NO_OP));
        getSensitivityWithLoadFlowParameters(parameters).getLoadFlowParameters().setDc(true);

        parameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters().setDc(true);
    }

    @Test
    void testRunInitialSensitivityAnalysis() {
        FlowCnec preventiveCnecTimestamp1 = crac1.getFlowCnec("cnecDeNlPrev - 1600");
        FlowCnec outageCnecTimestamp1 = crac1.getFlowCnec("cnecDeNlOut - 1600");
        FlowCnec preventiveCnecTimestamp2 = crac2.getFlowCnec("cnecDeNlPrev - 1700");
        FlowCnec outageCnecTimestamp2 = crac2.getFlowCnec("cnecDeNlOut - 1700");
        FlowCnec preventiveCnecTimestamp3 = crac3.getFlowCnec("cnecDeNlPrev - 1800");
        FlowCnec outageCnecTimestamp3 = crac3.getFlowCnec("cnecDeNlOut - 1800");

        RangeAction<?> pstBeTimestamp1 = crac1.getRangeAction("pstBe - 1600");
        RangeAction<?> pstBeTimestamp2 = crac2.getRangeAction("pstBe - 1700");
        RangeAction<?> pstBeTimestamp3 = crac3.getRangeAction("pstBe - 1800");
        RangeAction<?> pstDeTimestamp3 = crac3.getRangeAction("pstDe - 1800");

        // Timestamp 1
        PrePerimeterResult prePerimeterResult1 = runInitialSensitivityAnalysis(inputs.getData(timestamp1).get(), parameters, ReportNode.NO_OP);

        FlowResult flowResultTimestamp1 = prePerimeterResult1.getFlowResult();
        assertFlowValueMw(flowResultTimestamp1, preventiveCnecTimestamp1, -382.0);
        assertFlowValueMw(flowResultTimestamp1, outageCnecTimestamp1, -1000.0);

        SensitivityResult sensitivityResultTimestamp1 = prePerimeterResult1.getSensitivityResult();
        assertSensitivityValue(sensitivityResultTimestamp1, preventiveCnecTimestamp1, pstBeTimestamp1, 25.202534);
        assertSensitivityValue(sensitivityResultTimestamp1, outageCnecTimestamp1, pstBeTimestamp1, 0.0);

        RangeActionSetpointResult setPointResultTimestamp1 = prePerimeterResult1.getRangeActionSetpointResult();
        // assertEquals(12, setPointResultTimestamp1.getTap((PstRangeAction) pstBeTimestamp1));

        // Timestamp 2
        PrePerimeterResult prePerimeterResult2 = runInitialSensitivityAnalysis(inputs.getData(timestamp2).get(), parameters, ReportNode.NO_OP);

        FlowResult flowResultTimestamp2 = prePerimeterResult2.getFlowResult();
        assertFlowValueMw(flowResultTimestamp2, preventiveCnecTimestamp2, -382.0);
        assertFlowValueMw(flowResultTimestamp2, outageCnecTimestamp2, -1000.0);

        SensitivityResult sensitivityResultTimestamp2 = prePerimeterResult2.getSensitivityResult();
        assertSensitivityValue(sensitivityResultTimestamp2, preventiveCnecTimestamp2, pstBeTimestamp2, 25.202534);
        assertSensitivityValue(sensitivityResultTimestamp2, outageCnecTimestamp2, pstBeTimestamp2, 0.0);

        RangeActionSetpointResult setPointResultTimestamp2 = prePerimeterResult2.getRangeActionSetpointResult();
        // assertEquals(12, setPointResultTimestamp2.getTap((PstRangeAction) pstBeTimestamp2));

        // Timestamp 3
        PrePerimeterResult prePerimeterResult3 = runInitialSensitivityAnalysis(inputs.getData(timestamp3).get(), parameters, ReportNode.NO_OP);

        FlowResult flowResultTimestamp3 = prePerimeterResult3.getFlowResult();
        assertFlowValueMw(flowResultTimestamp3, preventiveCnecTimestamp3, -382.0);
        assertFlowValueMw(flowResultTimestamp3, outageCnecTimestamp3, -1000.0);

        SensitivityResult sensitivityResultTimestamp3 = prePerimeterResult3.getSensitivityResult();
        assertSensitivityValue(sensitivityResultTimestamp3, preventiveCnecTimestamp3, pstBeTimestamp3, 25.202534);
        assertSensitivityValue(sensitivityResultTimestamp3, outageCnecTimestamp3, pstBeTimestamp3, 0.0);
        // assertSensitivityValue(sensitivityResultTimestamp3, preventiveCnecTimestamp3, pstDeTimestamp3, 25.202534);
        assertSensitivityValue(sensitivityResultTimestamp3, outageCnecTimestamp3, pstDeTimestamp3, 0.0);

        RangeActionSetpointResult setPointResultTimestamp3 = prePerimeterResult3.getRangeActionSetpointResult();
        // assertEquals(12, setPointResultTimestamp3.getTap((PstRangeAction) pstBeTimestamp3));
        // assertEquals(0, setPointResultTimestamp3.getTap((PstRangeAction) pstDeTimestamp3));

    }

    @Test
    void testBuildCurativeActionsToApply() {
        // issue #1893: no curative remedial action should be applied on a state that diverged initially
        State curativeState = crac1.getStates().stream()
            .filter(state -> state.getInstant().isCurative())
            .findFirst()
            .orElseThrow();
        RangeAction<?> pstDe = crac1.getRangeAction("pstDe - 1600");

        AppliedRemedialActions topoCurativeActions = new AppliedRemedialActions();
        topoCurativeActions.addAppliedRangeAction(curativeState, pstDe, 5.0);

        LinearOptimizationResult filteredResult = Mockito.mock(LinearOptimizationResult.class);
        when(filteredResult.getActivatedRangeActions(curativeState)).thenReturn(Set.of(pstDe));
        when(filteredResult.getOptimizedSetpoint(pstDe, curativeState)).thenReturn(5.0);

        PrePerimeterResult initialResult = Mockito.mock(PrePerimeterResult.class);

        // state did not diverge in the initial sensitivity computation: all curative RAs are applied
        when(initialResult.getSensitivityStatus(curativeState)).thenReturn(ComputationStatus.DEFAULT);
        AppliedRemedialActions appliedActions = Marmot.buildCurativeActionsToApply(crac1, topoCurativeActions, filteredResult, initialResult);
        assertEquals(Map.of(pstDe, 5.0), appliedActions.getAppliedRangeActions(curativeState));

        // state diverged in the initial sensitivity computation: no curative RA is applied
        when(initialResult.getSensitivityStatus(curativeState)).thenReturn(ComputationStatus.FAILURE);
        AppliedRemedialActions noActions = Marmot.buildCurativeActionsToApply(crac1, topoCurativeActions, filteredResult, initialResult);
        assertTrue(noActions.getAppliedRangeActions(curativeState).isEmpty());
        assertTrue(noActions.getAppliedNetworkActions(curativeState).isEmpty());
    }

    @Test
    void testBuildCurativeActionsWithMixedSensitivityStatuses() {
        State healthy = crac1.getStates().stream().filter(state -> state.getInstant().isCurative()).findFirst().orElseThrow();
        State failed = crac2.getStates().stream().filter(state -> state.getInstant().isCurative()).findFirst().orElseThrow();
        State preventive = crac1.getPreventiveState();
        Crac crac = Mockito.mock(Crac.class);
        when(crac.getStates()).thenReturn(Set.of(healthy, failed, preventive));
        NetworkAction networkAction = Mockito.mock(NetworkAction.class);
        RangeAction<?> rangeAction = crac1.getRangeAction("pstDe - 1600");
        AppliedRemedialActions topologicalActions = new AppliedRemedialActions();
        topologicalActions.addAppliedNetworkAction(healthy, networkAction);
        topologicalActions.addAppliedNetworkAction(failed, networkAction);
        LinearOptimizationResult filteredResult = Mockito.mock(LinearOptimizationResult.class);
        when(filteredResult.getActivatedRangeActions(healthy)).thenReturn(Set.of(rangeAction));
        when(filteredResult.getActivatedRangeActions(failed)).thenReturn(Set.of(rangeAction));
        when(filteredResult.getOptimizedSetpoint(rangeAction, healthy)).thenReturn(7.5);
        when(filteredResult.getOptimizedSetpoint(rangeAction, failed)).thenReturn(8.5);
        PrePerimeterResult initialResult = Mockito.mock(PrePerimeterResult.class);
        when(initialResult.getSensitivityStatus(healthy)).thenReturn(ComputationStatus.DEFAULT);
        when(initialResult.getSensitivityStatus(failed)).thenReturn(ComputationStatus.FAILURE);

        AppliedRemedialActions actions = Marmot.buildCurativeActionsToApply(crac, topologicalActions, filteredResult, initialResult);

        assertEquals(Set.of(networkAction), actions.getAppliedNetworkActions(healthy));
        assertEquals(Map.of(rangeAction, 7.5), actions.getAppliedRangeActions(healthy));
        assertTrue(actions.getAppliedNetworkActions(failed).isEmpty());
        assertTrue(actions.getAppliedRangeActions(failed).isEmpty());
        verify(filteredResult, never()).getActivatedRangeActions(failed);
        verify(initialResult, never()).getSensitivityStatus(preventive);
    }

    @Test
    void testReportingActionsSkipsOnlyFailedStates() throws Exception {
        State healthy = crac1.getStates().stream().filter(state -> state.getInstant().isCurative()).findFirst().orElseThrow();
        State failed = crac2.getStates().stream().filter(state -> state.getInstant().isCurative()).findFirst().orElseThrow();
        State preventive = crac1.getPreventiveState();
        Crac crac = Mockito.mock(Crac.class);
        when(crac.getStates()).thenReturn(Set.of(healthy, failed, preventive));
        NetworkAction networkAction = Mockito.mock(NetworkAction.class);
        RangeAction<?> rangeAction = crac1.getRangeAction("pstDe - 1600");
        NetworkActionsResult networkActions = Mockito.mock(NetworkActionsResult.class);
        when(networkActions.getActivatedNetworkActionsPerState()).thenReturn(Map.of(
            healthy, Set.of(networkAction), failed, Set.of(networkAction), preventive, Set.of(networkAction)));
        GlobalLinearOptimizationResult linearResult = Mockito.mock(GlobalLinearOptimizationResult.class);
        when(linearResult.getActivatedRangeActions(healthy)).thenReturn(Set.of(rangeAction));
        when(linearResult.getActivatedRangeActions(failed)).thenReturn(Set.of(rangeAction));
        when(linearResult.getActivatedRangeActions(preventive)).thenReturn(Set.of(rangeAction));
        when(linearResult.getOptimizedSetpoint(rangeAction, healthy)).thenReturn(7.5);
        when(linearResult.getOptimizedSetpoint(rangeAction, preventive)).thenReturn(2.5);
        PrePerimeterResult initialResult = Mockito.mock(PrePerimeterResult.class);
        when(initialResult.getSensitivityStatus(healthy)).thenReturn(ComputationStatus.DEFAULT);
        when(initialResult.getSensitivityStatus(preventive)).thenReturn(ComputationStatus.DEFAULT);
        when(initialResult.getSensitivityStatus(failed)).thenReturn(ComputationStatus.FAILURE);
        RaoResultImpl.RaoResultImplBuilder builder = RaoResultImpl.builder(crac1);

        Method method = MarmotUtils.class.getDeclaredMethod("fillWithActivatedRemedialActions", GlobalLinearOptimizationResult.class,
            NetworkActionsResult.class, RaoResultImpl.RaoResultImplBuilder.class, Crac.class, PrePerimeterResult.class);
        method.setAccessible(true);
        method.invoke(null, linearResult, networkActions, builder, crac, initialResult);
        RaoResultImpl result = builder.build();

        assertTrue(result.isActivatedDuringState(healthy, networkAction));
        assertTrue(result.isActivatedDuringState(healthy, rangeAction));
        assertEquals(7.5, result.getOptimizedSetPointOnState(healthy, rangeAction), DOUBLE_TOLERANCE);
        assertTrue(result.isActivatedDuringState(preventive, networkAction));
        assertTrue(result.isActivatedDuringState(preventive, rangeAction));
        assertEquals(2.5, result.getOptimizedSetPointOnState(preventive, rangeAction), DOUBLE_TOLERANCE);
        assertFalse(result.isActivatedDuringState(failed, networkAction));
        assertFalse(result.isActivatedDuringState(failed, rangeAction));
        verify(linearResult, never()).getActivatedRangeActions(failed);
    }

    @Test
    void testInitiallyFailedStateWithSuccessfulFinalResult() throws Exception {
        OffsetDateTime timestamp = crac1.getTimestamp().orElseThrow();
        State preventive = crac1.getPreventiveState();
        State failed = crac1.getStates().stream().filter(state -> state.getInstant().isCurative()).findFirst().orElseThrow();
        RangeAction<?> rangeAction = crac1.getRangeAction("pstBe - 1600");
        String failureCost = SensitivityFailureOvercostEvaluator.getEvaluatorName();
        double overcost = 23456.0;
        parameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters().setSensitivityFailureOvercost(overcost);

        // Model the two sensitivity outcomes independently; this does not simulate a physical convergence recovery.
        PrePerimeterResult finalResult = runInitialSensitivityAnalysis(inputs.getData(timestamp1).orElseThrow(), parameters, ReportNode.NO_OP);
        assertEquals(ComputationStatus.DEFAULT, finalResult.getComputationStatus(failed));
        PrePerimeterResult initialResult = spy(finalResult);
        doReturn(ComputationStatus.FAILURE).when(initialResult).getSensitivityStatus(failed);
        doReturn(ComputationStatus.FAILURE).when(initialResult).getComputationStatus(failed);
        doReturn(Set.of(failureCost)).when(initialResult).getVirtualCostNames();
        doReturn(overcost).when(initialResult).getVirtualCost(failureCost);

        RangeActionActivationResultImpl activations = new RangeActionActivationResultImpl(initialResult);
        double setpoint = MarmotUtils.getInitialSetPoint(rangeAction) + 2.5;
        activations.putResult(rangeAction, preventive, setpoint);
        assertTrue(activations.getActivatedRangeActions(preventive).contains(rangeAction));
        assertFalse(activations.getActivatedRangeActions(failed).contains(rangeAction));
        assertEquals(setpoint, activations.getOptimizedSetpoint(rangeAction, failed), DOUBLE_TOLERANCE);

        TemporalData<Crac> cracs = new TemporalDataImpl<>(Map.of(timestamp, crac1));
        TemporalData<PrePerimeterResult> initialResults = new TemporalDataImpl<>(Map.of(timestamp, initialResult));
        NetworkActionsResult networkActions = new NetworkActionsResultImpl(Map.of());
        ObjectiveFunction objective = ObjectiveFunction.build(crac1.getFlowCnecs(), Set.of(), initialResult, initialResult, Set.of(), parameters, crac1.getStates());
        ObjectiveFunctionResult initialGlobalCost = objective.evaluate(initialResult, RemedialActionActivationResultImpl.empty(initialResult), ReportNode.NO_OP);
        assertEquals(overcost, initialGlobalCost.getVirtualCost(failureCost), DOUBLE_TOLERANCE);
        GlobalLinearOptimizationResult globalResult = new GlobalLinearOptimizationResult(
            new TemporalDataImpl<>(Map.of(timestamp, finalResult)),
            new TemporalDataImpl<>(Map.of(timestamp, finalResult.getSensitivityResult())),
            new TemporalDataImpl<>(Map.of(timestamp, activations)),
            new TemporalDataImpl<>(Map.of(timestamp, networkActions)), objective, LinearProblemStatus.OPTIMAL, ReportNode.NO_OP);

        Method merge = Marmot.class.getDeclaredMethod("mergeTopologicalAndLinearOptimizationResults", TemporalData.class,
            TemporalData.class, ObjectiveFunctionResult.class, GlobalLinearOptimizationResult.class, TemporalData.class,
            TemporalData.class, TemporalData.class, Set.class, RaoParameters.class, ReportNode.class);
        merge.setAccessible(true);
        RaoResult result = (RaoResult) merge.invoke(null, cracs, initialResults, initialGlobalCost, globalResult,
            new TemporalDataImpl<>(Map.of(timestamp, Set.of())),
            new TemporalDataImpl<>(Map.of(timestamp, new AppliedRemedialActions())),
            new TemporalDataImpl<>(Map.of(timestamp, Set.of())), Set.of(), parameters, ReportNode.NO_OP);

        assertEquals(ComputationStatus.DEFAULT, result.getExtension(Metadata.class).getComputationStatus(failed));
        assertTrue(result.isActivatedDuringState(preventive, rangeAction));
        assertFalse(result.isActivatedDuringState(failed, rangeAction));
        assertEquals(setpoint, result.getOptimizedSetPointOnState(failed, rangeAction), DOUBLE_TOLERANCE);
        CostResult costs = result.getExtension(CostResult.class);
        assertEquals(overcost, costs.getVirtualCost(null, failureCost), DOUBLE_TOLERANCE);
        assertEquals(0.0, costs.getVirtualCost(crac1.getPreventiveInstant(), failureCost), DOUBLE_TOLERANCE);
        TimeCoupledCostResult timestampCosts = result.getExtension(TimeCoupledCostResult.class);
        assertEquals(overcost, timestampCosts.getVirtualCost(null, failureCost, timestamp), DOUBLE_TOLERANCE);
        assertEquals(0.0, timestampCosts.getVirtualCost(crac1.getLastInstant(), failureCost, timestamp), DOUBLE_TOLERANCE);
    }

    private static void assertFlowValueMw(FlowResult flowResult, FlowCnec flowCnec, double expectedFlowMw) {
        assertEquals(expectedFlowMw, flowResult.getFlow(flowCnec, TwoSides.ONE, Unit.MEGAWATT), AMPERE_MEGAWATT_TOLERANCE);
    }

    private static void assertSensitivityValue(SensitivityResult sensitivityResult, FlowCnec flowCnec, RangeAction<?> rangeAction, double expectedSensitivityValue) {
        assertEquals(expectedSensitivityValue, sensitivityResult.getSensitivityValue(flowCnec, TwoSides.ONE, rangeAction, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }
}
