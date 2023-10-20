/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.CurativeOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.GlobalOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.PreventiveOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPVariable;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class LinearProblemResultTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private LinearProblem linearProblem;
    private LinearProblemResult linearProblemResult;
    private State preventiveState;
    private State aCurativeState;
    private PstRangeAction pst1;
    private PstRangeAction pst2;
    private RangeAction<?> ra3;
    private RangeAction<?> ra4;
    private Map<State, Set<RangeAction<?>>> rangeActionsPerState;
    private RangeActionSetpointResult prePerimeterRangeActionSetpoints;

    @BeforeEach
    public void setUp() {
        preventiveState = Mockito.mock(State.class);
        Instant instantPrev = Mockito.mock(Instant.class);
        Mockito.when(instantPrev.getInstantKind()).thenReturn(InstantKind.PREVENTIVE);
        Mockito.when(preventiveState.getInstant()).thenReturn(instantPrev);
        Mockito.when(preventiveState.isPreventive()).thenReturn(true);
        Mockito.when(preventiveState.getId()).thenReturn("pState");
        aCurativeState = Mockito.mock(State.class);
        Instant instantCurative = Mockito.mock(Instant.class);
        Mockito.when(instantCurative.getInstantKind()).thenReturn(InstantKind.CURATIVE);
        Mockito.when(aCurativeState.getInstant()).thenReturn(instantCurative);
        Mockito.when(aCurativeState.getId()).thenReturn("cState");

        pst1 = Mockito.mock(PstRangeAction.class);
        Mockito.when(pst1.getId()).thenReturn("pst1");
        NetworkElement pst1NE = Mockito.mock(NetworkElement.class);
        Mockito.when(pst1NE.getId()).thenReturn("pst1NE");
        Mockito.when(pst1.getNetworkElements()).thenReturn(Set.of(pst1NE));
        pst2 = Mockito.mock(PstRangeAction.class);
        Mockito.when(pst2.getId()).thenReturn("pst2");
        NetworkElement pst2NE = Mockito.mock(NetworkElement.class);
        Mockito.when(pst2NE.getId()).thenReturn("pst2NE");
        Mockito.when(pst2.getNetworkElements()).thenReturn(Set.of(pst2NE));
        ra3 = Mockito.mock(RangeAction.class);
        Mockito.when(ra3.getId()).thenReturn("ra3");
        NetworkElement ra3NE = Mockito.mock(NetworkElement.class);
        Mockito.when(ra3NE.getId()).thenReturn("ra3NE");
        Mockito.when(ra3.getNetworkElements()).thenReturn(Set.of(ra3NE));
        ra4 = Mockito.mock(RangeAction.class);
        Mockito.when(ra4.getId()).thenReturn("ra4");
        NetworkElement ra4NE = Mockito.mock(NetworkElement.class);
        Mockito.when(ra4NE.getId()).thenReturn("ra4NE");
        Mockito.when(ra4.getNetworkElements()).thenReturn(Set.of(ra4NE));

        linearProblem = Mockito.mock(LinearProblem.class);
        rangeActionsPerState = Map.of(
            preventiveState, Set.of(pst1, pst2, ra4),
            aCurativeState, Set.of(pst1, ra3, ra4));

        prePerimeterRangeActionSetpoints = new RangeActionSetpointResultImpl(Map.of(
            pst1, 0.8,
            pst2, 5.4,
            ra3, 600.,
            ra4, -200.
        ));

        // pst1 activated in preventive
        // pst2 not activated
        // ra3 activated in curative
        // ra4 activated in preventive and curative

        Map<State, Map<RangeAction<?>, Double>> setPointPerRangeAction = Map.of(
            preventiveState, Map.of(
                pst1, 2.3,
                pst2, 5.4,
                ra4, -300.),
            aCurativeState, Map.of(
                pst1, 2.3,
                ra3, 200.,
                ra4, 700.));

        Map<State, Map<RangeAction<?>, Double>> setPointVariationPerRangeAction = Map.of(
            preventiveState, Map.of(
                pst1, 1.5,
                pst2, 0.0,
                ra4, 100.),
            aCurativeState, Map.of(
                pst1, 0.0,
                ra3, 400.,
                ra4, 1000.0));

        Map<State, Map<RangeAction<?>, FaraoMPVariable>> setPointVariablePerRangeAction = Map.of(
            preventiveState, Map.of(
                pst1, Mockito.mock(FaraoMPVariable.class),
                pst2, Mockito.mock(FaraoMPVariable.class),
                ra3, Mockito.mock(FaraoMPVariable.class),
                ra4, Mockito.mock(FaraoMPVariable.class)),
            aCurativeState, Map.of(
                pst1, Mockito.mock(FaraoMPVariable.class),
                pst2, Mockito.mock(FaraoMPVariable.class),
                ra3, Mockito.mock(FaraoMPVariable.class),
                ra4, Mockito.mock(FaraoMPVariable.class)));

        Map<State, Map<RangeAction<?>, FaraoMPVariable>> setPointVariationVariablePerRangeAction = Map.of(
            preventiveState, Map.of(
                pst1, Mockito.mock(FaraoMPVariable.class),
                pst2, Mockito.mock(FaraoMPVariable.class),
                ra3, Mockito.mock(FaraoMPVariable.class),
                ra4, Mockito.mock(FaraoMPVariable.class)),
            aCurativeState, Map.of(
                pst1, Mockito.mock(FaraoMPVariable.class),
                pst2, Mockito.mock(FaraoMPVariable.class),
                ra3, Mockito.mock(FaraoMPVariable.class),
                ra4, Mockito.mock(FaraoMPVariable.class)));

        rangeActionsPerState.forEach((state, rangeActions) -> rangeActions.forEach(ra -> {
            FaraoMPVariable setPointVariable = setPointVariablePerRangeAction.get(state).get(ra);
            Mockito.when(linearProblem.getRangeActionSetpointVariable(ra, state)).thenReturn(setPointVariable);
            Mockito.when(setPointVariable.solutionValue()).thenReturn(setPointPerRangeAction.get(state).get(ra));

            FaraoMPVariable setPointVariationVariable = setPointVariationVariablePerRangeAction.get(state).get(ra);
            Mockito.when(linearProblem.getAbsoluteRangeActionVariationVariable(ra, state)).thenReturn(setPointVariationVariable);
            Mockito.when(setPointVariationVariable.solutionValue()).thenReturn(setPointVariationPerRangeAction.get(state).get(ra));
        }));

        Mockito.when(pst1.convertAngleToTap(1.5)).thenReturn(3);
        Mockito.when(pst2.convertAngleToTap(5.4)).thenReturn(10);
    }

    @Test
    void testGetOptimizedSetPointPreventivePerimeter() {
        OptimizationPerimeter optimizationPerimeter = new PreventiveOptimizationPerimeter(
            preventiveState, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), rangeActionsPerState.get(preventiveState));

        linearProblemResult = new LinearProblemResult(linearProblem, prePerimeterRangeActionSetpoints, optimizationPerimeter);
        assertEquals(2.3, linearProblemResult.getOptimizedSetpoint(pst1, preventiveState), DOUBLE_TOLERANCE);
        assertEquals(5.4, linearProblemResult.getOptimizedSetpoint(pst2, preventiveState), DOUBLE_TOLERANCE);
        assertEquals(-300., linearProblemResult.getOptimizedSetpoint(ra4, preventiveState), DOUBLE_TOLERANCE);
        assertEquals(Set.of(pst1, ra4), linearProblemResult.getActivatedRangeActions(preventiveState));
    }

    @Test
    void testGetOptimizedSetPointCurativePerimeter() {
        OptimizationPerimeter optimizationPerimeter = new CurativeOptimizationPerimeter(
            aCurativeState, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), rangeActionsPerState.get(aCurativeState));

        linearProblemResult = new LinearProblemResult(linearProblem, prePerimeterRangeActionSetpoints, optimizationPerimeter);
        assertEquals(0.8, linearProblemResult.getOptimizedSetpoint(pst1, aCurativeState), DOUBLE_TOLERANCE);
        assertEquals(200., linearProblemResult.getOptimizedSetpoint(ra3, aCurativeState), DOUBLE_TOLERANCE);
        assertEquals(700., linearProblemResult.getOptimizedSetpoint(ra4, aCurativeState), DOUBLE_TOLERANCE);
        assertEquals(Set.of(ra3, ra4), linearProblemResult.getActivatedRangeActions(aCurativeState));
    }

    @Test
    void testGetOptimizedSetPointGlobalPerimeter() {
        OptimizationPerimeter optimizationPerimeter = new GlobalOptimizationPerimeter(
            aCurativeState, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), rangeActionsPerState);

        linearProblemResult = new LinearProblemResult(linearProblem, prePerimeterRangeActionSetpoints, optimizationPerimeter);
        assertEquals(2.3, linearProblemResult.getOptimizedSetpoint(pst1, preventiveState), DOUBLE_TOLERANCE);
        assertEquals(5.4, linearProblemResult.getOptimizedSetpoint(pst2, preventiveState), DOUBLE_TOLERANCE);
        assertEquals(600., linearProblemResult.getOptimizedSetpoint(ra3, preventiveState), DOUBLE_TOLERANCE);
        assertEquals(-300., linearProblemResult.getOptimizedSetpoint(ra4, preventiveState), DOUBLE_TOLERANCE);
        assertEquals(Set.of(pst1, ra4), linearProblemResult.getActivatedRangeActions(preventiveState));

        assertEquals(2.3, linearProblemResult.getOptimizedSetpoint(pst1, aCurativeState), DOUBLE_TOLERANCE);
        assertEquals(5.4, linearProblemResult.getOptimizedSetpoint(pst2, aCurativeState), DOUBLE_TOLERANCE);
        assertEquals(200., linearProblemResult.getOptimizedSetpoint(ra3, aCurativeState), DOUBLE_TOLERANCE);
        assertEquals(700., linearProblemResult.getOptimizedSetpoint(ra4, aCurativeState), DOUBLE_TOLERANCE);
        assertEquals(Set.of(ra3, ra4), linearProblemResult.getActivatedRangeActions(aCurativeState));
    }

}
