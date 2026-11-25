/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.raoimpl.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.raoimpl.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.RangeActionSetpointBounds.RANGE_ACTION_SETPOINT_EPSILON;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RangeActionSetpointBoundsTest extends AbstractFillerTest {
    private static final double INFINITY = 1e20;
    private OptimizationPerimeter perimeter;
    private State preventiveState;
    private State curativeState;

    @BeforeEach
    void setUp() throws IOException {
        init();
        preventiveState = cnec1.getState();
        curativeState = cnec2.getState();
        perimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(perimeter.getMainOptimizationState()).thenReturn(preventiveState);
        Mockito.when(perimeter.getRangeActionsPerState()).thenReturn(Map.of(
            preventiveState, Set.<RangeAction<?>>of(pstRangeAction),
            curativeState, Set.<RangeAction<?>>of(pstRangeAction)));
    }

    @Test
    void firstAvailableStateUsesAdmissibleRangeAroundPrePerimeterSetpoint() {
        double prePerimeterSetpoint = pstRangeAction.convertTapToAngle(TAP_INITIAL);
        RangeActionSetpointResult prePerimeter = new RangeActionSetpointResultImpl(Map.of(pstRangeAction, prePerimeterSetpoint));

        RangeActionSetpointBounds.Interval bounds = RangeActionSetpointBounds.getSetpointVariableBounds(perimeter, pstRangeAction, preventiveState, prePerimeter, INFINITY);

        assertEquals(pstRangeAction.getMinAdmissibleSetpoint(prePerimeterSetpoint) - RANGE_ACTION_SETPOINT_EPSILON, bounds.min(), 1e-12);
        assertEquals(pstRangeAction.getMaxAdmissibleSetpoint(prePerimeterSetpoint) + RANGE_ACTION_SETPOINT_EPSILON, bounds.max(), 1e-12);
    }

    @Test
    void laterStateUsesAbsoluteRange() {
        RangeActionSetpointResult prePerimeter = new RangeActionSetpointResultImpl(Map.of(pstRangeAction, 0.));

        RangeActionSetpointBounds.Interval bounds = RangeActionSetpointBounds.getSetpointVariableBounds(perimeter, pstRangeAction, curativeState, prePerimeter, INFINITY);

        List<Double> minMax = RangeActionSetpointBounds.getMinAndMaxAbsoluteAndRelativeSetpoints(pstRangeAction, INFINITY);
        assertEquals(minMax.get(0) - RANGE_ACTION_SETPOINT_EPSILON, bounds.min(), 1e-12);
        assertEquals(minMax.get(1) + RANGE_ACTION_SETPOINT_EPSILON, bounds.max(), 1e-12);
        // PRA_PST_BE has an absolute range of taps [-15, 15]
        assertEquals(pstRangeAction.convertTapToAngle(-15) - RANGE_ACTION_SETPOINT_EPSILON, bounds.min(), 1e-12);
        assertEquals(pstRangeAction.convertTapToAngle(15) + RANGE_ACTION_SETPOINT_EPSILON, bounds.max(), 1e-12);
    }

    @Test
    void onlyRelativeRangeGivesInfiniteInterval() {
        StandardRange relativeRange = Mockito.mock(StandardRange.class);
        Mockito.when(relativeRange.getRangeType()).thenReturn(RangeType.RELATIVE_TO_PREVIOUS_INSTANT);
        Mockito.when(relativeRange.getMin()).thenReturn(-10.);
        Mockito.when(relativeRange.getMax()).thenReturn(10.);
        HvdcRangeAction hvdc = Mockito.mock(HvdcRangeAction.class);
        Mockito.when(hvdc.getId()).thenReturn("hvdc");
        Mockito.when(hvdc.getNetworkElementsIds()).thenReturn(Set.of("hvdc-element"));
        Mockito.when(hvdc.getRanges()).thenReturn(List.of(relativeRange));
        Mockito.when(perimeter.getRangeActionsPerState()).thenReturn(Map.of(
            preventiveState, Set.<RangeAction<?>>of(hvdc),
            curativeState, Set.<RangeAction<?>>of(hvdc)));
        RangeActionSetpointResult prePerimeter = new RangeActionSetpointResultImpl(Map.of(hvdc, 0.));

        RangeActionSetpointBounds.Interval bounds = RangeActionSetpointBounds.getSetpointVariableBounds(perimeter, hvdc, curativeState, prePerimeter, INFINITY);

        assertEquals(-INFINITY - RANGE_ACTION_SETPOINT_EPSILON, bounds.min(), 1e-12);
        assertEquals(INFINITY + RANGE_ACTION_SETPOINT_EPSILON, bounds.max(), 1e-12);
    }
}
