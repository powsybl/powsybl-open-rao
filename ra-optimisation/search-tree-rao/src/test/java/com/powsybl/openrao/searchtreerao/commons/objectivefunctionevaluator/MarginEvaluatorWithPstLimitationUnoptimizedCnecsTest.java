/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class MarginEvaluatorWithPstLimitationUnoptimizedCnecsTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private final FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
    private final FlowResult currentFlowResult = Mockito.mock(FlowResult.class);
    private final RangeActionResult rangeActionResult = Mockito.mock(RangeActionResult.class);
    private final SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);

    private final FlowCnec flowCnecInSeries = Mockito.mock(FlowCnec.class);
    private final PstRangeAction pstRangeActionInSeries = Mockito.mock(PstRangeAction.class);
    private final Map<FlowCnec, RangeAction<?>> flowCnecPstRangeActionMap = Map.of(flowCnecInSeries, pstRangeActionInSeries);
    private final RangeActionResult rangeActionResult = Mockito.mock(RangeActionResult.class);
    private final MarginEvaluatorWithPstLimitationUnoptimizedCnecs marginEvaluatorWithPstLimitationUnoptimizedCnecs =
            new MarginEvaluatorWithPstLimitationUnoptimizedCnecs(
                    new BasicMarginEvaluator(),
                    flowCnecPstRangeActionMap,
                    rangeActionResult
            );

    @BeforeEach
    public void setUp() {
        when(flowCnec.getMonitoredSides()).thenReturn(Set.of(Side.LEFT));
        when(flowCnecInSeries.getMonitoredSides()).thenReturn(Set.of(Side.LEFT));
        when(currentFlowResult.getMargin(flowCnec, Side.LEFT, Unit.MEGAWATT)).thenReturn(200.);
        when(currentFlowResult.getMargin(flowCnecInSeries, Side.LEFT, Unit.MEGAWATT)).thenReturn(200.);
    }

    @Test
    void optimizedCnec() {
        double margin = marginEvaluatorWithPstLimitationUnoptimizedCnecs.getMargin(currentFlowResult, flowCnec, rangeActionResult, sensitivityResult, Unit.MEGAWATT);
        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    void unoptimizedCnecTest1() {
        // Sensi > 0
        when(sensitivityResult.getSensitivityValue(flowCnecInSeries, Side.LEFT, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(50.);
        when(pstRangeActionInSeries.getMinAdmissibleSetpoint(rangeActionResult.getSetpoint(pstRangeActionInSeries))).thenReturn(2.);
        when(pstRangeActionInSeries.getMaxAdmissibleSetpoint(rangeActionResult.getSetpoint(pstRangeActionInSeries))).thenReturn(10.);
        when(rangeActionResult.getOptimizedSetpointsOnState(flowCnecInSeries.getState())).thenReturn(Map.of(pstRangeActionInSeries, 5.));
        when(flowCnecInSeries.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(40.));
        when(flowCnecInSeries.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(100.));
        when(currentFlowResult.getFlow(flowCnecInSeries, Side.LEFT, Unit.MEGAWATT)).thenReturn(50.);
        double margin = marginEvaluatorWithPstLimitationUnoptimizedCnecs.getMargin(currentFlowResult, flowCnecInSeries, rangeActionResult, sensitivityResult, Unit.MEGAWATT);

        assertEquals(Double.MAX_VALUE, margin, DOUBLE_TOLERANCE);
    }

    @Test
    void unoptimizedCnecTest2() {
        // Sensi > 0
        when(sensitivityResult.getSensitivityValue(flowCnecInSeries, Side.LEFT, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(3.);
        when(pstRangeActionInSeries.getMinAdmissibleSetpoint(rangeActionResult.getSetpoint(pstRangeActionInSeries))).thenReturn(2.);
        when(pstRangeActionInSeries.getMaxAdmissibleSetpoint(rangeActionResult.getSetpoint(pstRangeActionInSeries))).thenReturn(10.);
        when(rangeActionResult.getOptimizedSetpointsOnState(flowCnecInSeries.getState())).thenReturn(Map.of(pstRangeActionInSeries, 5.));
        when(flowCnecInSeries.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(40.));
        when(flowCnecInSeries.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(100.));
        when(currentFlowResult.getFlow(flowCnecInSeries, Side.LEFT, Unit.MEGAWATT)).thenReturn(20.);
        double margin = marginEvaluatorWithPstLimitationUnoptimizedCnecs.getMargin(currentFlowResult, flowCnecInSeries, rangeActionResult, sensitivityResult, Unit.MEGAWATT);

        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    void unoptimizedCnecTest3() {
        // Sensi > 0
        when(sensitivityResult.getSensitivityValue(flowCnecInSeries, Side.LEFT, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(3.);
        when(pstRangeActionInSeries.getMinAdmissibleSetpoint(rangeActionResult.getSetpoint(pstRangeActionInSeries))).thenReturn(2.);
        when(pstRangeActionInSeries.getMaxAdmissibleSetpoint(rangeActionResult.getSetpoint(pstRangeActionInSeries))).thenReturn(10.);
        when(rangeActionResult.getOptimizedSetpointsOnState(flowCnecInSeries.getState())).thenReturn(Map.of(pstRangeActionInSeries, 5.));
        when(flowCnecInSeries.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(40.));
        when(flowCnecInSeries.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(100.));
        when(currentFlowResult.getFlow(flowCnecInSeries, Side.LEFT, Unit.MEGAWATT)).thenReturn(120.);
        double margin = marginEvaluatorWithPstLimitationUnoptimizedCnecs.getMargin(currentFlowResult, flowCnecInSeries, rangeActionResult, sensitivityResult, Unit.MEGAWATT);

        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    void unoptimizedCnecTest4() {
        // Sensi < 0
        when(sensitivityResult.getSensitivityValue(flowCnecInSeries, Side.LEFT, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(-1.);
        when(pstRangeActionInSeries.getMinAdmissibleSetpoint(rangeActionResult.getSetpoint(pstRangeActionInSeries))).thenReturn(2.);
        when(pstRangeActionInSeries.getMaxAdmissibleSetpoint(rangeActionResult.getSetpoint(pstRangeActionInSeries))).thenReturn(10.);
        when(rangeActionResult.getOptimizedSetpointsOnState(flowCnecInSeries.getState())).thenReturn(Map.of(pstRangeActionInSeries, 5.));
        when(flowCnecInSeries.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(40.));
        when(flowCnecInSeries.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(100.));
        when(currentFlowResult.getFlow(flowCnecInSeries, Side.LEFT, Unit.MEGAWATT)).thenReturn(50.);
        double margin = marginEvaluatorWithPstLimitationUnoptimizedCnecs.getMargin(currentFlowResult, flowCnecInSeries, rangeActionResult, sensitivityResult, Unit.MEGAWATT);

        assertEquals(Double.MAX_VALUE, margin, DOUBLE_TOLERANCE);
    }

    @Test
    void unoptimizedCnecTest5() {
        // Sensi < 0
        when(sensitivityResult.getSensitivityValue(flowCnecInSeries, Side.LEFT, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(-3.);
        when(pstRangeActionInSeries.getMinAdmissibleSetpoint(rangeActionResult.getSetpoint(pstRangeActionInSeries))).thenReturn(2.);
        when(pstRangeActionInSeries.getMaxAdmissibleSetpoint(rangeActionResult.getSetpoint(pstRangeActionInSeries))).thenReturn(10.);
        when(rangeActionResult.getOptimizedSetpointsOnState(flowCnecInSeries.getState())).thenReturn(Map.of(pstRangeActionInSeries, 5.));
        when(flowCnecInSeries.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(40.));
        when(flowCnecInSeries.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(100.));
        when(currentFlowResult.getFlow(flowCnecInSeries, Side.LEFT, Unit.MEGAWATT)).thenReturn(120.);
        double margin = marginEvaluatorWithPstLimitationUnoptimizedCnecs.getMargin(currentFlowResult, flowCnecInSeries, rangeActionResult, sensitivityResult, Unit.MEGAWATT);
        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    void unoptimizedCnecTest6() {
        // Sensi < 0
        when(sensitivityResult.getSensitivityValue(flowCnecInSeries, Side.LEFT, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(-3.);
        when(pstRangeActionInSeries.getMinAdmissibleSetpoint(rangeActionResult.getSetpoint(pstRangeActionInSeries))).thenReturn(2.);
        when(pstRangeActionInSeries.getMaxAdmissibleSetpoint(rangeActionResult.getSetpoint(pstRangeActionInSeries))).thenReturn(10.);
        when(rangeActionResult.getOptimizedSetpointsOnState(flowCnecInSeries.getState())).thenReturn(Map.of(pstRangeActionInSeries, 5.));
        when(flowCnecInSeries.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(40.));
        when(flowCnecInSeries.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(100.));
        when(currentFlowResult.getFlow(flowCnecInSeries, Side.LEFT, Unit.MEGAWATT)).thenReturn(10.);
        double margin = marginEvaluatorWithPstLimitationUnoptimizedCnecs.getMargin(currentFlowResult, flowCnecInSeries, rangeActionResult, sensitivityResult, Unit.MEGAWATT);

        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }
}
