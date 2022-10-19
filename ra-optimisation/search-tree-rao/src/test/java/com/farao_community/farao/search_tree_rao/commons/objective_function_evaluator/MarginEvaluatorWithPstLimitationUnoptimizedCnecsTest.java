/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class MarginEvaluatorWithPstLimitationUnoptimizedCnecsTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private final FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
    private final FlowResult currentFlowResult = Mockito.mock(FlowResult.class);
    private final RangeActionActivationResult rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
    private final SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);

    private final FlowCnec flowCnecInSeries = Mockito.mock(FlowCnec.class);
    private final PstRangeAction pstRangeActionInSeries = Mockito.mock(PstRangeAction.class);
    private Map<FlowCnec, PstRangeAction> flowCnecPstRangeActionMap;
    private final RangeActionSetpointResult rangeActionSetpointResult = Mockito.mock(RangeActionSetpointResult.class);
    private MarginEvaluatorWithPstLimitationUnoptimizedCnecs marginEvaluatorWithPstLimitationUnoptimizedCnecs;

    @Before
    public void setUp() {
        flowCnecPstRangeActionMap = Map.of(flowCnecInSeries, pstRangeActionInSeries);
        marginEvaluatorWithPstLimitationUnoptimizedCnecs =
                new MarginEvaluatorWithPstLimitationUnoptimizedCnecs(
                        new BasicMinMarginEvaluator(),
                        flowCnecPstRangeActionMap,
                        rangeActionSetpointResult
                );
    }

    @Test
    public void optimizedCnec() {
        when(currentFlowResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(200.);
        double margin = marginEvaluatorWithPstLimitationUnoptimizedCnecs.getMargin(currentFlowResult, flowCnec, rangeActionActivationResult, sensitivityResult, Unit.MEGAWATT);
        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    public void unoptimizedCnecTest1() {
        // Sensi > 0
        // AboveThresholdConstraint OK
        // BelowThresholdConstraint OK
        when(currentFlowResult.getMargin(flowCnecInSeries, Unit.MEGAWATT)).thenReturn(200.);
        when(sensitivityResult.getSensitivityValue(flowCnecInSeries, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(50.);
        when(pstRangeActionInSeries.getMinAdmissibleSetpoint(rangeActionSetpointResult.getSetpoint(pstRangeActionInSeries))).thenReturn(2.);
        when(pstRangeActionInSeries.getMaxAdmissibleSetpoint(rangeActionSetpointResult.getSetpoint(pstRangeActionInSeries))).thenReturn(10.);
        when(rangeActionActivationResult.getOptimizedSetpoint(pstRangeActionInSeries, flowCnecInSeries.getState())).thenReturn(5.);
        when(flowCnecInSeries.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(40.));
        when(flowCnecInSeries.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(100.));
        when(currentFlowResult.getFlow(flowCnecInSeries, Unit.MEGAWATT)).thenReturn(50.);
        double margin = marginEvaluatorWithPstLimitationUnoptimizedCnecs.getMargin(currentFlowResult, flowCnecInSeries, rangeActionActivationResult, sensitivityResult, Unit.MEGAWATT);

        assertEquals(Double.MAX_VALUE, margin, DOUBLE_TOLERANCE);
    }

    @Test
    public void unoptimizedCnecTest2() {
        // Sensi > 0
        // AboveThresholdConstraint OK
        // BelowThresholdConstraint NOK
        when(currentFlowResult.getMargin(flowCnecInSeries, Unit.MEGAWATT)).thenReturn(200.);

        when(sensitivityResult.getSensitivityValue(flowCnecInSeries, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(3.);
        when(pstRangeActionInSeries.getMinAdmissibleSetpoint(rangeActionSetpointResult.getSetpoint(pstRangeActionInSeries))).thenReturn(2.);
        when(pstRangeActionInSeries.getMaxAdmissibleSetpoint(rangeActionSetpointResult.getSetpoint(pstRangeActionInSeries))).thenReturn(10.);
        when(rangeActionActivationResult.getOptimizedSetpoint(pstRangeActionInSeries, flowCnecInSeries.getState())).thenReturn(5.);
        when(flowCnecInSeries.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(40.));
        when(flowCnecInSeries.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(100.));
        when(currentFlowResult.getFlow(flowCnecInSeries, Unit.MEGAWATT)).thenReturn(20.);
        double margin = marginEvaluatorWithPstLimitationUnoptimizedCnecs.getMargin(currentFlowResult, flowCnecInSeries, rangeActionActivationResult, sensitivityResult, Unit.MEGAWATT);

        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    public void unoptimizedCnecTest3() {
        // Sensi > 0
        // AboveThresholdConstraint NOK
        // BelowThresholdConstraint OK
        when(currentFlowResult.getMargin(flowCnecInSeries, Unit.MEGAWATT)).thenReturn(200.);

        when(sensitivityResult.getSensitivityValue(flowCnecInSeries, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(3.);
        when(pstRangeActionInSeries.getMinAdmissibleSetpoint(rangeActionSetpointResult.getSetpoint(pstRangeActionInSeries))).thenReturn(2.);
        when(pstRangeActionInSeries.getMaxAdmissibleSetpoint(rangeActionSetpointResult.getSetpoint(pstRangeActionInSeries))).thenReturn(10.);
        when(rangeActionActivationResult.getOptimizedSetpoint(pstRangeActionInSeries, flowCnecInSeries.getState())).thenReturn(5.);
        when(flowCnecInSeries.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(40.));
        when(flowCnecInSeries.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(100.));
        when(currentFlowResult.getFlow(flowCnecInSeries, Unit.MEGAWATT)).thenReturn(120.);
        double margin = marginEvaluatorWithPstLimitationUnoptimizedCnecs.getMargin(currentFlowResult, flowCnecInSeries, rangeActionActivationResult, sensitivityResult, Unit.MEGAWATT);

        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    public void unoptimizedCnecTest4() {
        // Sensi < 0
        // AboveThresholdConstraint OK
        // BelowThresholdConstraint OK
        when(currentFlowResult.getMargin(flowCnecInSeries, Unit.MEGAWATT)).thenReturn(200.);
        when(sensitivityResult.getSensitivityValue(flowCnecInSeries, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(-1.);
        when(pstRangeActionInSeries.getMinAdmissibleSetpoint(rangeActionSetpointResult.getSetpoint(pstRangeActionInSeries))).thenReturn(2.);
        when(pstRangeActionInSeries.getMaxAdmissibleSetpoint(rangeActionSetpointResult.getSetpoint(pstRangeActionInSeries))).thenReturn(10.);
        when(rangeActionActivationResult.getOptimizedSetpoint(pstRangeActionInSeries, flowCnecInSeries.getState())).thenReturn(5.);
        when(flowCnecInSeries.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(40.));
        when(flowCnecInSeries.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(100.));
        when(currentFlowResult.getFlow(flowCnecInSeries, Unit.MEGAWATT)).thenReturn(50.);
        double margin = marginEvaluatorWithPstLimitationUnoptimizedCnecs.getMargin(currentFlowResult, flowCnecInSeries, rangeActionActivationResult, sensitivityResult, Unit.MEGAWATT);

        assertEquals(Double.MAX_VALUE, margin, DOUBLE_TOLERANCE);
    }

    @Test
    public void unoptimizedCnecTest5() {
        // Sensi < 0
        // AboveThresholdConstraint OK
        // BelowThresholdConstraint NOK
        when(currentFlowResult.getMargin(flowCnecInSeries, Unit.MEGAWATT)).thenReturn(200.);

        when(sensitivityResult.getSensitivityValue(flowCnecInSeries, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(-3.);
        when(pstRangeActionInSeries.getMinAdmissibleSetpoint(rangeActionSetpointResult.getSetpoint(pstRangeActionInSeries))).thenReturn(2.);
        when(pstRangeActionInSeries.getMaxAdmissibleSetpoint(rangeActionSetpointResult.getSetpoint(pstRangeActionInSeries))).thenReturn(10.);
        when(rangeActionActivationResult.getOptimizedSetpoint(pstRangeActionInSeries, flowCnecInSeries.getState())).thenReturn(5.);
        when(flowCnecInSeries.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(40.));
        when(flowCnecInSeries.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(100.));
        when(currentFlowResult.getFlow(flowCnecInSeries, Unit.MEGAWATT)).thenReturn(120.);
        double margin = marginEvaluatorWithPstLimitationUnoptimizedCnecs.getMargin(currentFlowResult, flowCnecInSeries, rangeActionActivationResult, sensitivityResult, Unit.MEGAWATT);
        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    public void unoptimizedCnecTest6() {
        // Sensi < 0
        // AboveThresholdConstraint NOK
        // BelowThresholdConstraint OK
        when(currentFlowResult.getMargin(flowCnecInSeries, Unit.MEGAWATT)).thenReturn(200.);

        when(sensitivityResult.getSensitivityValue(flowCnecInSeries, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(-3.);
        when(pstRangeActionInSeries.getMinAdmissibleSetpoint(rangeActionSetpointResult.getSetpoint(pstRangeActionInSeries))).thenReturn(2.);
        when(pstRangeActionInSeries.getMaxAdmissibleSetpoint(rangeActionSetpointResult.getSetpoint(pstRangeActionInSeries))).thenReturn(10.);
        when(rangeActionActivationResult.getOptimizedSetpoint(pstRangeActionInSeries, flowCnecInSeries.getState())).thenReturn(5.);
        when(flowCnecInSeries.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(40.));
        when(flowCnecInSeries.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(100.));
        when(currentFlowResult.getFlow(flowCnecInSeries, Unit.MEGAWATT)).thenReturn(10.);
        double margin = marginEvaluatorWithPstLimitationUnoptimizedCnecs.getMargin(currentFlowResult, flowCnecInSeries, rangeActionActivationResult, sensitivityResult, Unit.MEGAWATT);

        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }
}
