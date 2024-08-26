/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */

class OptimizationResultImplTest {

    private OptimizationResultImpl optimizationResult;
    private ObjectiveFunctionResult objectiveFunctionResult;
    private FlowResult flowResult;
    private SensitivityResult sensitivityResult;
    private NetworkActionsResult networkActionsResult;
    private RangeActionActivationResult rangeActionActivationResult;

    @BeforeEach
    void setUp() {
        objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        flowResult = Mockito.mock(FlowResult.class);
        sensitivityResult = Mockito.mock(SensitivityResult.class);
        networkActionsResult = Mockito.mock(NetworkActionsResult.class);
        rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        optimizationResult = new OptimizationResultImpl(objectiveFunctionResult, flowResult, sensitivityResult, networkActionsResult, rangeActionActivationResult);
    }

    @Test
    void testObjectiveFunctionResultMethods() {
        double functionalCost = 10.1;
        List<FlowCnec> limitingCnecs = List.of(Mockito.mock(FlowCnec.class));
        double virtualCost = 4.2;
        Set<String> virtualCostNames = Set.of("vc1", "vc2");
        double vc1Cost = 2.3;
        List<FlowCnec> vc1CostlyElements = List.of(Mockito.mock(FlowCnec.class), Mockito.mock(FlowCnec.class));
        ObjectiveFunction objectiveFunction = Mockito.mock(ObjectiveFunction.class);

        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(functionalCost);
        when(objectiveFunctionResult.getMostLimitingElements(1)).thenReturn(limitingCnecs);
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(virtualCost);
        when(objectiveFunctionResult.getVirtualCostNames()).thenReturn(virtualCostNames);
        when(objectiveFunctionResult.getVirtualCost("vc1")).thenReturn(vc1Cost);
        when(objectiveFunctionResult.getCostlyElements("vc1", 2)).thenReturn(vc1CostlyElements);
        when(objectiveFunctionResult.getObjectiveFunction()).thenReturn(objectiveFunction);

        assertEquals(functionalCost, optimizationResult.getFunctionalCost());
        assertEquals(limitingCnecs, optimizationResult.getMostLimitingElements(1));
        assertEquals(virtualCost, optimizationResult.getVirtualCost());
        assertEquals(virtualCostNames, optimizationResult.getVirtualCostNames());
        assertEquals(vc1Cost, optimizationResult.getVirtualCost("vc1"));
        assertEquals(vc1CostlyElements, optimizationResult.getCostlyElements("vc1", 2));
        assertEquals(objectiveFunction, optimizationResult.getObjectiveFunction());
    }

    @Test
    void testFlowResultMethods() {
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        TwoSides side = TwoSides.ONE;
        Unit unit = Unit.MEGAWATT;
        Instant instant = Mockito.mock(Instant.class);

        double flow = 24.5;
        double flowWithInstant = 21.4;
        double margin = 15.5;
        double commercialFlow = 14.5;
        double ptdfZonalSum = 0.13;
        Map<FlowCnec, Map<TwoSides, Double>> ptdfZonalSums = Map.of(flowCnec, Map.of(side, 0.07));

        when(flowResult.getFlow(flowCnec, side, unit)).thenReturn(flow);
        when(flowResult.getFlow(flowCnec, side, unit, instant)).thenReturn(flowWithInstant);
        when(flowResult.getMargin(flowCnec, unit)).thenReturn(margin);
        when(flowResult.getCommercialFlow(flowCnec, side, unit)).thenReturn(commercialFlow);
        when(flowResult.getPtdfZonalSum(flowCnec, side)).thenReturn(ptdfZonalSum);
        when(flowResult.getPtdfZonalSums()).thenReturn(ptdfZonalSums);

        assertEquals(flow, optimizationResult.getFlow(flowCnec, side, unit));
        assertEquals(flowWithInstant, optimizationResult.getFlow(flowCnec, side, unit, instant));
        assertEquals(margin, optimizationResult.getMargin(flowCnec, unit));
        assertEquals(commercialFlow, optimizationResult.getCommercialFlow(flowCnec, side, unit));
        assertEquals(ptdfZonalSum, optimizationResult.getPtdfZonalSum(flowCnec, side));
        assertEquals(ptdfZonalSums, optimizationResult.getPtdfZonalSums());
    }

    @Test
    void testNetworkActionResultMethods() {
        NetworkAction activatedNetworkAction = Mockito.mock(NetworkAction.class);
        NetworkAction unactivatedNetworkAction = Mockito.mock(NetworkAction.class);
        Set<NetworkAction> activatedNetworkActions = Set.of(activatedNetworkAction);

        when(networkActionsResult.isActivated(activatedNetworkAction)).thenReturn(true);
        when(networkActionsResult.isActivated(unactivatedNetworkAction)).thenReturn(false);
        when(networkActionsResult.getActivatedNetworkActions()).thenReturn(activatedNetworkActions);

        assertTrue(optimizationResult.isActivated(activatedNetworkAction));
        assertFalse(optimizationResult.isActivated(unactivatedNetworkAction));
        assertEquals(activatedNetworkActions, optimizationResult.getActivatedNetworkActions());
    }

    @Test
    void testRangeActionActivationResultMethods() {
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        State state = Mockito.mock(State.class);

        Set<RangeAction<?>> rangeActions = Set.of(pstRangeAction);
        double optimizedSetpoint = 2.3;
        Map<RangeAction<?>, Double> optimizedSetpoints = Map.of(pstRangeAction, 2.3);
        int optimizedTap = 3;
        Map<PstRangeAction, Integer> optimizedTaps = Map.of(pstRangeAction, 3);

        when(rangeActionActivationResult.getRangeActions()).thenReturn(rangeActions);
        when(rangeActionActivationResult.getOptimizedSetpoint(pstRangeAction, state)).thenReturn(optimizedSetpoint);
        when(rangeActionActivationResult.getOptimizedSetpointsOnState(state)).thenReturn(optimizedSetpoints);
        when(rangeActionActivationResult.getOptimizedTap(pstRangeAction, state)).thenReturn(optimizedTap);
        when(rangeActionActivationResult.getOptimizedTapsOnState(state)).thenReturn(optimizedTaps);

        assertEquals(rangeActions, optimizationResult.getRangeActions());
        assertEquals(optimizedSetpoint, optimizationResult.getOptimizedSetpoint(pstRangeAction, state));
        assertEquals(optimizedSetpoints, optimizationResult.getOptimizedSetpointsOnState(state));
        assertEquals(optimizedTap, optimizationResult.getOptimizedTap(pstRangeAction, state));
        assertEquals(optimizedTaps, optimizationResult.getOptimizedTapsOnState(state));
    }

    @Test
    void testSensitivityResultMethods() {
        State state = Mockito.mock(State.class);
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        TwoSides side = TwoSides.ONE;
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        Unit unit = Unit.MEGAWATT;
        SensitivityVariableSet glsk = Mockito.mock(SensitivityVariableSet.class);

        ComputationStatus status = ComputationStatus.PARTIAL_FAILURE;
        ComputationStatus statusForState = ComputationStatus.FAILURE;
        Set<String> contingencyIds = Set.of("cont1", "cont2");
        double sensitivityValueForPst = 0.34;
        double sensitivityValueForGlsk = 0.13;

        when(sensitivityResult.getSensitivityStatus()).thenReturn(status);
        when(sensitivityResult.getSensitivityStatus(state)).thenReturn(statusForState);
        when(sensitivityResult.getContingencies()).thenReturn(contingencyIds);
        when(sensitivityResult.getSensitivityValue(flowCnec, side, pstRangeAction, unit)).thenReturn(sensitivityValueForPst);
        when(sensitivityResult.getSensitivityValue(flowCnec, side, glsk, unit)).thenReturn(sensitivityValueForGlsk);

        assertEquals(status, optimizationResult.getSensitivityStatus());
        assertEquals(statusForState, optimizationResult.getSensitivityStatus(state));
        assertEquals(contingencyIds, optimizationResult.getContingencies());
        assertEquals(sensitivityValueForPst, optimizationResult.getSensitivityValue(flowCnec, side, pstRangeAction, unit));
        assertEquals(sensitivityValueForGlsk, optimizationResult.getSensitivityValue(flowCnec, side, glsk, unit));
    }
}
