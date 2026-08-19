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
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.CostResult;
import com.powsybl.openrao.data.raoresult.api.extension.Metadata;
import com.powsybl.openrao.searchtreerao.marmot.TestsUtils;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class TimeCoupledRaoResultImplTest {
    private State stateTimestamp1;
    private State stateTimestamp2;
    private State stateTimestamp3;
    private Instant instant;
    private FlowCnec flowCnecTimestamp1;
    private FlowCnec flowCnecTimestamp2;
    private FlowCnec flowCnecTimestamp3;
    private PstRangeAction pstRangeAction;
    private NetworkAction networkAction;
    private TimeCoupledRaoResultImpl timeCoupledRaoResult;

    @BeforeEach
    void setUp() {
        stateTimestamp1 = TestsUtils.mockState(TestsUtils.TIMESTAMP_1);
        stateTimestamp2 = TestsUtils.mockState(TestsUtils.TIMESTAMP_2);
        stateTimestamp3 = TestsUtils.mockState(TestsUtils.TIMESTAMP_3);

        instant = Mockito.mock(Instant.class);
        Mockito.when(instant.isPreventive()).thenReturn(true);

        flowCnecTimestamp1 = TestsUtils.mockFlowCnec(stateTimestamp1);
        flowCnecTimestamp2 = TestsUtils.mockFlowCnec(stateTimestamp2);
        flowCnecTimestamp3 = TestsUtils.mockFlowCnec(stateTimestamp3);

        pstRangeAction = Mockito.mock(PstRangeAction.class);
        networkAction = Mockito.mock(NetworkAction.class);

        ObjectiveFunctionResult initialObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        Mockito.when(initialObjectiveFunctionResult.getFunctionalCost()).thenReturn(0.);
        Mockito.when(initialObjectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        Mockito.when(initialObjectiveFunctionResult.getVirtualCostNames()).thenReturn(Set.of("virtual"));
        Mockito.when(initialObjectiveFunctionResult.getVirtualCost("virtual")).thenReturn(0.);

        ObjectiveFunctionResult objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        Mockito.when(objectiveFunctionResult.getFunctionalCost()).thenReturn(900.);
        Mockito.when(objectiveFunctionResult.getVirtualCost()).thenReturn(100.);
        Mockito.when(objectiveFunctionResult.getVirtualCostNames()).thenReturn(Set.of("virtual"));
        Mockito.when(objectiveFunctionResult.getVirtualCost("virtual")).thenReturn(100.);

        final RaoResult raoResultTimestamp1 = mockRaoResult("RAO 1 succeeded.", 450., 0., flowCnecTimestamp1, 850., 10., stateTimestamp1, 0, 0, 0., 0., true);
        final RaoResult raoResultTimestamp2 = mockRaoResult("RAO 2 succeeded.", 250., 90., flowCnecTimestamp2, 510., 45., stateTimestamp2, 0, 5, 0., 10.2, false);
        final RaoResult raoResultTimestamp3 = mockRaoResult("RAO 3 failed.", 200., 10., flowCnecTimestamp3, 1000., -60., stateTimestamp3, 0, 16, 0., 35.32, true);

        timeCoupledRaoResult = new TimeCoupledRaoResultImpl(
            initialObjectiveFunctionResult,
            objectiveFunctionResult,
            new TemporalDataImpl<>(Map.of(
                TestsUtils.TIMESTAMP_1, raoResultTimestamp1,
                TestsUtils.TIMESTAMP_2, raoResultTimestamp2,
                TestsUtils.TIMESTAMP_3, raoResultTimestamp3
            ))
        );
    }

    @Test
    void testTimestamps() {
        assertEquals(List.of(TestsUtils.TIMESTAMP_1, TestsUtils.TIMESTAMP_2, TestsUtils.TIMESTAMP_3), timeCoupledRaoResult.getTimestamps());
    }

    @Test
    void testFlow() {
        assertEquals(850., timeCoupledRaoResult.getFlow(instant, flowCnecTimestamp1, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(510., timeCoupledRaoResult.getFlow(instant, flowCnecTimestamp2, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(1000., timeCoupledRaoResult.getFlow(instant, flowCnecTimestamp3, TwoSides.ONE, Unit.MEGAWATT));
    }

    @Test
    void testMargin() {
        assertEquals(10., timeCoupledRaoResult.getMargin(instant, flowCnecTimestamp1, Unit.MEGAWATT));
        assertEquals(45., timeCoupledRaoResult.getMargin(instant, flowCnecTimestamp2, Unit.MEGAWATT));
        assertEquals(-60., timeCoupledRaoResult.getMargin(instant, flowCnecTimestamp3, Unit.MEGAWATT));
    }

    private RaoResult mockRaoResult(String executionDetails,
                                    double functionalCost,
                                    double virtualCost,
                                    FlowCnec flowCnec,
                                    double flow,
                                    double margin,
                                    State state,
                                    int initialTap,
                                    int optimizedTap,
                                    double initialSetPoint,
                                    double optimizedSetPoint,
                                    boolean isNetworkActionActivated) {
        RaoResult raoResult = Mockito.mock(RaoResult.class);
        Mockito.when(raoResult.getFlow(instant, flowCnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(flow);
        Mockito.when(raoResult.getMargin(instant, flowCnec, Unit.MEGAWATT)).thenReturn(margin);
        Mockito.when(raoResult.getPreOptimizationTapOnState(state, pstRangeAction)).thenReturn(initialTap);
        Mockito.when(raoResult.getOptimizedTapOnState(state, pstRangeAction)).thenReturn(optimizedTap);
        Mockito.when(raoResult.getOptimizedTapsOnState(state)).thenReturn(Map.of(pstRangeAction, optimizedTap));
        Mockito.when(raoResult.isActivatedDuringState(state, pstRangeAction)).thenReturn(initialTap != optimizedTap);
        Mockito.when(raoResult.getPreOptimizationSetPointOnState(state, pstRangeAction)).thenReturn(initialSetPoint);
        Mockito.when(raoResult.getOptimizedSetPointOnState(state, pstRangeAction)).thenReturn(optimizedSetPoint);
        Mockito.when(raoResult.getOptimizedSetPointsOnState(state)).thenReturn(Map.of(pstRangeAction, optimizedSetPoint));
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(state)).thenReturn(isNetworkActionActivated ? Set.of(networkAction) : Set.of());
        Mockito.when(raoResult.isActivatedDuringState(state, networkAction)).thenReturn(isNetworkActionActivated);

        // mock cost extension
        CostResult costResult = new CostResult();
        costResult.addFunctionalCostResult(instant, functionalCost);
        costResult.addVirtualCostResult(instant, "virtual", virtualCost);
        Mockito.when(raoResult.getExtension(CostResult.class)).thenReturn(costResult);

        // mock metadata extension
        Metadata metadata = new Metadata();
        metadata.setExecutionDetails(executionDetails);
        Mockito.when(raoResult.getExtension(Metadata.class)).thenReturn(metadata);

        return raoResult;
    }

    @Test
    void testRangeActionActivation() {
        assertFalse(timeCoupledRaoResult.isActivatedDuringState(stateTimestamp1, pstRangeAction));
        assertEquals(0, timeCoupledRaoResult.getPreOptimizationTapOnState(stateTimestamp1, pstRangeAction));
        assertEquals(0, timeCoupledRaoResult.getOptimizedTapOnState(stateTimestamp1, pstRangeAction));
        assertEquals(Map.of(pstRangeAction, 0), timeCoupledRaoResult.getOptimizedTapsOnState(stateTimestamp1));
        assertEquals(0., timeCoupledRaoResult.getPreOptimizationSetPointOnState(stateTimestamp1, pstRangeAction));
        assertEquals(0., timeCoupledRaoResult.getOptimizedSetPointOnState(stateTimestamp1, pstRangeAction));
        assertEquals(Map.of(pstRangeAction, 0.), timeCoupledRaoResult.getOptimizedSetPointsOnState(stateTimestamp1));

        assertTrue(timeCoupledRaoResult.isActivatedDuringState(stateTimestamp2, pstRangeAction));
        assertEquals(0, timeCoupledRaoResult.getPreOptimizationTapOnState(stateTimestamp2, pstRangeAction));
        assertEquals(5, timeCoupledRaoResult.getOptimizedTapOnState(stateTimestamp2, pstRangeAction));
        assertEquals(Map.of(pstRangeAction, 5), timeCoupledRaoResult.getOptimizedTapsOnState(stateTimestamp2));
        assertEquals(0., timeCoupledRaoResult.getPreOptimizationSetPointOnState(stateTimestamp2, pstRangeAction));
        assertEquals(10.2, timeCoupledRaoResult.getOptimizedSetPointOnState(stateTimestamp2, pstRangeAction));
        assertEquals(Map.of(pstRangeAction, 10.2), timeCoupledRaoResult.getOptimizedSetPointsOnState(stateTimestamp2));

        assertTrue(timeCoupledRaoResult.isActivatedDuringState(stateTimestamp3, pstRangeAction));
        assertEquals(0, timeCoupledRaoResult.getPreOptimizationTapOnState(stateTimestamp3, pstRangeAction));
        assertEquals(16, timeCoupledRaoResult.getOptimizedTapOnState(stateTimestamp3, pstRangeAction));
        assertEquals(Map.of(pstRangeAction, 16), timeCoupledRaoResult.getOptimizedTapsOnState(stateTimestamp3));
        assertEquals(0., timeCoupledRaoResult.getPreOptimizationSetPointOnState(stateTimestamp3, pstRangeAction));
        assertEquals(35.32, timeCoupledRaoResult.getOptimizedSetPointOnState(stateTimestamp3, pstRangeAction));
        assertEquals(Map.of(pstRangeAction, 35.32), timeCoupledRaoResult.getOptimizedSetPointsOnState(stateTimestamp3));
    }

    @Test
    void testNetworkActionActivation() {
        assertTrue(timeCoupledRaoResult.isActivatedDuringState(stateTimestamp1, networkAction));
        assertEquals(Set.of(networkAction), timeCoupledRaoResult.getActivatedNetworkActionsDuringState(stateTimestamp1));

        assertFalse(timeCoupledRaoResult.isActivatedDuringState(stateTimestamp2, networkAction));
        assertTrue(timeCoupledRaoResult.getActivatedNetworkActionsDuringState(stateTimestamp2).isEmpty());

        assertTrue(timeCoupledRaoResult.isActivatedDuringState(stateTimestamp3, networkAction));
        assertEquals(Set.of(networkAction), timeCoupledRaoResult.getActivatedNetworkActionsDuringState(stateTimestamp3));
    }
}
