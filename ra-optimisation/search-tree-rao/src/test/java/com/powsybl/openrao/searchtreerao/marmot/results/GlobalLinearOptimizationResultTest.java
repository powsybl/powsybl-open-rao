/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.marmot.TestsUtils;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearProblemStatus;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class GlobalLinearOptimizationResultTest {
    private State stateTimestamp1;
    private State stateTimestamp2;
    private State stateTimestamp3;
    private FlowCnec flowCnecTimestamp1;
    private FlowCnec flowCnecTimestamp2;
    private FlowCnec flowCnecTimestamp3;
    private FlowResult flowResultTimestamp1;
    private FlowResult flowResultTimestamp2;
    private PstRangeAction pstRangeActionTimestamp1;
    private PstRangeAction pstRangeActionTimestamp2;
    private PstRangeAction pstRangeActionTimestamp3;
    private SensitivityResult sensitivityResultTimestamp1;
    private SensitivityResult sensitivityResultTimestamp2;
    private RangeActionActivationResult rangeActionActivationResultTimestamp1;
    private RangeActionActivationResult rangeActionActivationResultTimestamp2;
    private ObjectiveFunction objectiveFunction;
    private GlobalLinearOptimizationResult globalLinearOptimizationResult;

    @BeforeEach
    void setUp() {
        stateTimestamp1 = TestsUtils.mockState(TestsUtils.TIMESTAMP_1);
        stateTimestamp2 = TestsUtils.mockState(TestsUtils.TIMESTAMP_2);
        stateTimestamp3 = TestsUtils.mockState(TestsUtils.TIMESTAMP_3);

        // mock flow result

        flowCnecTimestamp1 = TestsUtils.mockFlowCnec(stateTimestamp1);
        flowCnecTimestamp2 = TestsUtils.mockFlowCnec(stateTimestamp2);
        flowCnecTimestamp3 = TestsUtils.mockFlowCnec(stateTimestamp3);

        flowResultTimestamp1 = TestsUtils.createMockedFlowResult(ComputationStatus.DEFAULT);
        TestsUtils.mockFlowResult(flowResultTimestamp1, flowCnecTimestamp1, 100., 10.);

        flowResultTimestamp2 = TestsUtils.createMockedFlowResult(ComputationStatus.PARTIAL_FAILURE);
        TestsUtils.mockFlowResult(flowResultTimestamp2, flowCnecTimestamp2, 200., 20.);

        FlowResult flowResultTimestamp3 = TestsUtils.createMockedFlowResult(ComputationStatus.FAILURE);
        TestsUtils.mockFlowResult(flowResultTimestamp3, flowCnecTimestamp3, 300., 30.);

        // mock sensitivity result

        pstRangeActionTimestamp1 = Mockito.mock(PstRangeAction.class);
        pstRangeActionTimestamp2 = Mockito.mock(PstRangeAction.class);
        pstRangeActionTimestamp3 = Mockito.mock(PstRangeAction.class);

        sensitivityResultTimestamp1 = TestsUtils.createMockedSensitivityResult(ComputationStatus.DEFAULT);
        TestsUtils.mockSensitivityResult(sensitivityResultTimestamp1, flowCnecTimestamp1, pstRangeActionTimestamp1, 15.);

        sensitivityResultTimestamp2 = TestsUtils.createMockedSensitivityResult(ComputationStatus.PARTIAL_FAILURE);
        TestsUtils.mockSensitivityResult(sensitivityResultTimestamp2, flowCnecTimestamp2, pstRangeActionTimestamp2, 30.);

        SensitivityResult sensitivityResultTimestamp3 = TestsUtils.createMockedSensitivityResult(ComputationStatus.FAILURE);
        TestsUtils.mockSensitivityResult(sensitivityResultTimestamp3, flowCnecTimestamp3, pstRangeActionTimestamp3, 45.);

        // mock range action activation result

        rangeActionActivationResultTimestamp1 = TestsUtils.mockRangeActionActivationResult(stateTimestamp1, pstRangeActionTimestamp1, 5, 6.22);
        rangeActionActivationResultTimestamp2 = TestsUtils.mockRangeActionActivationResult(stateTimestamp2, pstRangeActionTimestamp2, 8, 12.11);
        RangeActionActivationResult rangeActionActivationResultTimestamp3 = TestsUtils.mockRangeActionActivationResult(stateTimestamp3, pstRangeActionTimestamp3, 1, 0.55);

        // mock objective function result

        ObjectiveFunctionResult objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        Mockito.when(objectiveFunctionResult.getFunctionalCost()).thenReturn(900.);
        Mockito.when(objectiveFunctionResult.getVirtualCost()).thenReturn(100.);

        objectiveFunction = Mockito.mock(ObjectiveFunction.class);
        Mockito.when(objectiveFunction.evaluate(Mockito.any(FlowResult.class), Mockito.any(RemedialActionActivationResult.class))).thenReturn(objectiveFunctionResult);

        // create global linear optimization result

        globalLinearOptimizationResult = new GlobalLinearOptimizationResult(
            new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, flowResultTimestamp1, TestsUtils.TIMESTAMP_2, flowResultTimestamp2, TestsUtils.TIMESTAMP_3, flowResultTimestamp3)),
            new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, sensitivityResultTimestamp1, TestsUtils.TIMESTAMP_2, sensitivityResultTimestamp2, TestsUtils.TIMESTAMP_3, sensitivityResultTimestamp3)),
            new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, rangeActionActivationResultTimestamp1, TestsUtils.TIMESTAMP_2, rangeActionActivationResultTimestamp2, TestsUtils.TIMESTAMP_3, rangeActionActivationResultTimestamp3)),
            new TemporalDataImpl<>(),
            objectiveFunction,
            LinearProblemStatus.OPTIMAL
        );
    }

    @Test
    void testStatus() {
        GlobalLinearOptimizationResult linearOptimizationResult = new GlobalLinearOptimizationResult(new TemporalDataImpl<>(), new TemporalDataImpl<>(), new TemporalDataImpl<>(), new TemporalDataImpl<>(), objectiveFunction, LinearProblemStatus.OPTIMAL);
        assertEquals(LinearProblemStatus.OPTIMAL, linearOptimizationResult.getStatus());
        linearOptimizationResult.setStatus(LinearProblemStatus.FEASIBLE);
        assertEquals(LinearProblemStatus.FEASIBLE, linearOptimizationResult.getStatus());
    }

    @Test
    void testCost() {
        LinearOptimizationResult linearOptimizationResult = new GlobalLinearOptimizationResult(new TemporalDataImpl<>(), new TemporalDataImpl<>(), new TemporalDataImpl<>(), new TemporalDataImpl<>(), objectiveFunction, LinearProblemStatus.OPTIMAL);
        assertEquals(1000., linearOptimizationResult.getCost());
        assertEquals(900., linearOptimizationResult.getFunctionalCost());
        assertEquals(100., linearOptimizationResult.getVirtualCost());
    }

    @Test
    void testFlow() {
        assertEquals(100., globalLinearOptimizationResult.getFlow(flowCnecTimestamp1, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(200., globalLinearOptimizationResult.getFlow(flowCnecTimestamp2, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(300., globalLinearOptimizationResult.getFlow(flowCnecTimestamp3, TwoSides.ONE, Unit.MEGAWATT));
    }

    @Test
    void testMargin() {
        assertEquals(10., globalLinearOptimizationResult.getMargin(flowCnecTimestamp1, Unit.MEGAWATT));
        assertEquals(20., globalLinearOptimizationResult.getMargin(flowCnecTimestamp2, Unit.MEGAWATT));
        assertEquals(30., globalLinearOptimizationResult.getMargin(flowCnecTimestamp3, Unit.MEGAWATT));
    }

    @Test
    void testComputationOrSensitivityStatusWithFailure() {
        assertEquals(ComputationStatus.FAILURE, globalLinearOptimizationResult.getComputationStatus());
        assertEquals(ComputationStatus.FAILURE, globalLinearOptimizationResult.getSensitivityStatus());
    }

    @Test
    void testComputationStatusWithPartialFailure() {
        GlobalLinearOptimizationResult partialLinearOptimizationResult = new GlobalLinearOptimizationResult(
            new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, flowResultTimestamp1, TestsUtils.TIMESTAMP_2, flowResultTimestamp2)),
            new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, sensitivityResultTimestamp1, TestsUtils.TIMESTAMP_2, sensitivityResultTimestamp2)),
            new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, rangeActionActivationResultTimestamp1, TestsUtils.TIMESTAMP_2, rangeActionActivationResultTimestamp2)),
            new TemporalDataImpl<>(),
            objectiveFunction,
            LinearProblemStatus.OPTIMAL
        );
        assertEquals(ComputationStatus.PARTIAL_FAILURE, partialLinearOptimizationResult.getComputationStatus());
        assertEquals(ComputationStatus.PARTIAL_FAILURE, partialLinearOptimizationResult.getSensitivityStatus());
    }

    @Test
    void testComputationStatusDefault() {
        GlobalLinearOptimizationResult partialLinearOptimizationResult = new GlobalLinearOptimizationResult(
            new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, flowResultTimestamp1)),
            new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, sensitivityResultTimestamp1)),
            new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, rangeActionActivationResultTimestamp1)),
            new TemporalDataImpl<>(),
            objectiveFunction,
            LinearProblemStatus.OPTIMAL
        );
        assertEquals(ComputationStatus.DEFAULT, partialLinearOptimizationResult.getComputationStatus());
        assertEquals(ComputationStatus.DEFAULT, partialLinearOptimizationResult.getSensitivityStatus());
    }

    @Test
    void testSensitivityValue() {
        assertEquals(15., globalLinearOptimizationResult.getSensitivityValue(flowCnecTimestamp1, TwoSides.ONE, pstRangeActionTimestamp1, Unit.MEGAWATT));
        assertEquals(30., globalLinearOptimizationResult.getSensitivityValue(flowCnecTimestamp2, TwoSides.ONE, pstRangeActionTimestamp2, Unit.MEGAWATT));
        assertEquals(45., globalLinearOptimizationResult.getSensitivityValue(flowCnecTimestamp3, TwoSides.ONE, pstRangeActionTimestamp3, Unit.MEGAWATT));
    }

    @Test
    void testRangeActions() {
        assertEquals(Set.of(pstRangeActionTimestamp1, pstRangeActionTimestamp2, pstRangeActionTimestamp3), globalLinearOptimizationResult.getRangeActions());
    }

    @Test
    void testActivatedRangeActions() {
        assertEquals(Set.of(pstRangeActionTimestamp1), globalLinearOptimizationResult.getActivatedRangeActions(stateTimestamp1));
        assertEquals(Set.of(pstRangeActionTimestamp2), globalLinearOptimizationResult.getActivatedRangeActions(stateTimestamp2));
        assertEquals(Set.of(pstRangeActionTimestamp3), globalLinearOptimizationResult.getActivatedRangeActions(stateTimestamp3));
    }

    @Test
    void testOptimalTap() {
        assertEquals(5, globalLinearOptimizationResult.getOptimizedTap(pstRangeActionTimestamp1, stateTimestamp1));
        assertEquals(8, globalLinearOptimizationResult.getOptimizedTap(pstRangeActionTimestamp2, stateTimestamp2));
        assertEquals(1, globalLinearOptimizationResult.getOptimizedTap(pstRangeActionTimestamp3, stateTimestamp3));
    }

    @Test
    void testTapVariation() {
        assertEquals(5, globalLinearOptimizationResult.getTapVariation(pstRangeActionTimestamp1, stateTimestamp1));
        assertEquals(8, globalLinearOptimizationResult.getTapVariation(pstRangeActionTimestamp2, stateTimestamp2));
        assertEquals(1, globalLinearOptimizationResult.getTapVariation(pstRangeActionTimestamp3, stateTimestamp3));
    }

    @Test
    void testOptimizedTapsOnState() {
        assertEquals(Map.of(pstRangeActionTimestamp1, 5), globalLinearOptimizationResult.getOptimizedTapsOnState(stateTimestamp1));
        assertEquals(Map.of(pstRangeActionTimestamp2, 8), globalLinearOptimizationResult.getOptimizedTapsOnState(stateTimestamp2));
        assertEquals(Map.of(pstRangeActionTimestamp3, 1), globalLinearOptimizationResult.getOptimizedTapsOnState(stateTimestamp3));
    }

    @Test
    void testOptimalSetPoint() {
        assertEquals(6.22, globalLinearOptimizationResult.getOptimizedSetpoint(pstRangeActionTimestamp1, stateTimestamp1));
        assertEquals(12.11, globalLinearOptimizationResult.getOptimizedSetpoint(pstRangeActionTimestamp2, stateTimestamp2));
        assertEquals(0.55, globalLinearOptimizationResult.getOptimizedSetpoint(pstRangeActionTimestamp3, stateTimestamp3));
    }

    @Test
    void testSetPointVariation() {
        assertEquals(6.22, globalLinearOptimizationResult.getSetPointVariation(pstRangeActionTimestamp1, stateTimestamp1));
        assertEquals(12.11, globalLinearOptimizationResult.getSetPointVariation(pstRangeActionTimestamp2, stateTimestamp2));
        assertEquals(0.55, globalLinearOptimizationResult.getSetPointVariation(pstRangeActionTimestamp3, stateTimestamp3));
    }

    @Test
    void testOptimizedSetPointsOnState() {
        assertEquals(Map.of(pstRangeActionTimestamp1, 6.22), globalLinearOptimizationResult.getOptimizedSetpointsOnState(stateTimestamp1));
        assertEquals(Map.of(pstRangeActionTimestamp2, 12.11), globalLinearOptimizationResult.getOptimizedSetpointsOnState(stateTimestamp2));
        assertEquals(Map.of(pstRangeActionTimestamp3, 0.55), globalLinearOptimizationResult.getOptimizedSetpointsOnState(stateTimestamp3));
    }
}
