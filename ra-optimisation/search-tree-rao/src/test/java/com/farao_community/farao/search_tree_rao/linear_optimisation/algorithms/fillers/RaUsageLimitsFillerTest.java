/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.RangeActionLimitationParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.RangeActionParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemBuilder;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionActivationResultImpl;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class RaUsageLimitsFillerTest extends AbstractFillerTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;

    private PstRangeAction pst1;
    private PstRangeAction pst2;
    private PstRangeAction pst3;
    private HvdcRangeAction hvdc;
    private InjectionRangeAction injection;
    private Map<State, Set<RangeAction<?>>> rangeActionsPerState;
    private RangeActionActivationResult prePerimeterRangeActionActivationResult;
    private RangeActionSetpointResult prePerimeterRangeActionSetpointResult;
    private State state;

    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;

    @Before
    public void setup() {
        init();
        state = crac.getPreventiveState();

        pst1 = mock(PstRangeAction.class);
        when(pst1.getId()).thenReturn("pst1");
        when(pst1.getOperator()).thenReturn("opA");
        when(pst1.getTapToAngleConversionMap()).thenReturn(Map.of(-1, -5.0, 1, 1.9));

        pst2 = mock(PstRangeAction.class);
        when(pst2.getId()).thenReturn("pst2");
        when(pst2.getOperator()).thenReturn("opA");
        when(pst2.getTapToAngleConversionMap()).thenReturn(Map.of(0, 5.0, 1, 8.0));

        pst3 = mock(PstRangeAction.class);
        when(pst3.getId()).thenReturn("pst3");
        when(pst3.getOperator()).thenReturn("opB");
        when(pst3.getTapToAngleConversionMap()).thenReturn(Map.of(-10, -4.0, -7, -8.5));

        hvdc = mock(HvdcRangeAction.class);
        when(hvdc.getId()).thenReturn("hvdc");
        when(hvdc.getOperator()).thenReturn("opA");

        injection = mock(InjectionRangeAction.class);
        when(injection.getId()).thenReturn("injection");
        when(injection.getOperator()).thenReturn("opC");

        Set<RangeAction<?>> rangeActions = Set.of(pst1, pst2, pst3, hvdc, injection);

        prePerimeterRangeActionSetpointResult = mock(RangeActionSetpointResult.class);

        when(prePerimeterRangeActionSetpointResult.getRangeActions()).thenReturn(rangeActions);
        when(prePerimeterRangeActionSetpointResult.getSetpoint(pst1)).thenReturn(1.);
        when(prePerimeterRangeActionSetpointResult.getSetpoint(pst2)).thenReturn(2.);
        when(prePerimeterRangeActionSetpointResult.getSetpoint(pst3)).thenReturn(3.);
        when(prePerimeterRangeActionSetpointResult.getSetpoint(hvdc)).thenReturn(4.);
        when(prePerimeterRangeActionSetpointResult.getSetpoint(injection)).thenReturn(5.);

        prePerimeterRangeActionActivationResult = new RangeActionActivationResultImpl(prePerimeterRangeActionSetpointResult);

        rangeActions.forEach(ra -> {
            double min = -10 * prePerimeterRangeActionActivationResult.getOptimizedSetpoint(ra, state);
            double max = 20 * prePerimeterRangeActionActivationResult.getOptimizedSetpoint(ra, state);
            when(ra.getMinAdmissibleSetpoint(anyDouble())).thenReturn(min);
            when(ra.getMaxAdmissibleSetpoint(anyDouble())).thenReturn(max);
        });
        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);

        rangeActionsPerState = new HashMap<>();
        rangeActionsPerState.put(state, rangeActions);
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActionsPerState);

        RangeActionParameters rangeActionParameters = RangeActionParameters.buildFromRaoParameters(new RaoParameters());

        coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            prePerimeterRangeActionSetpointResult,
            prePerimeterRangeActionActivationResult,
            rangeActionParameters);
    }

    @Test
    public void testSkipFiller() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeAction(state, 5);
        raLimitationParameters.setMaxTso(state, 2);
        raLimitationParameters.setMaxTsoExclusion(state, Set.of("opA"));
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(mpSolver)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        rangeActionsPerState.get(state).forEach(ra -> assertNull(linearProblem.getRangeActionVariationBinary(ra, state)));
    }

    @Test
    public void testVariationVariableAndConstraints() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeAction(state, 1);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(mpSolver)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        rangeActionsPerState.get(state).forEach(ra -> {
            MPVariable binary = linearProblem.getRangeActionVariationBinary(ra, state);
            MPConstraint constraintUp = linearProblem.getIsVariationInDirectionConstraint(ra, state, LinearProblem.VariationReferenceExtension.PREPERIMETER, LinearProblem.VariationDirectionExtension.UPWARD);
            MPConstraint constraintDown = linearProblem.getIsVariationInDirectionConstraint(ra, state, LinearProblem.VariationReferenceExtension.PREPERIMETER, LinearProblem.VariationDirectionExtension.DOWNWARD);

            assertNotNull(binary);
            assertNotNull(constraintUp);
            assertNotNull(constraintDown);

            MPVariable setpointVariable = linearProblem.getRangeActionSetpointVariable(ra, state);
            double initialSetpoint = prePerimeterRangeActionActivationResult.getOptimizedSetpoint(ra, state);

            assertEquals(1, constraintUp.getCoefficient(setpointVariable), DOUBLE_TOLERANCE);
            assertEquals(-(ra.getMaxAdmissibleSetpoint(initialSetpoint) - initialSetpoint), constraintUp.getCoefficient(binary), DOUBLE_TOLERANCE);
            assertEquals(-LinearProblem.infinity(), constraintUp.lb(), DOUBLE_TOLERANCE);
            assertEquals(initialSetpoint + 1e-5, constraintUp.ub(), DOUBLE_TOLERANCE);

            assertEquals(1, constraintDown.getCoefficient(setpointVariable), DOUBLE_TOLERANCE);
            assertEquals(initialSetpoint - ra.getMinAdmissibleSetpoint(initialSetpoint), constraintDown.getCoefficient(binary), DOUBLE_TOLERANCE);
            assertEquals(initialSetpoint - 1e-5, constraintDown.lb(), DOUBLE_TOLERANCE);
            assertEquals(LinearProblem.infinity(), constraintDown.ub(), DOUBLE_TOLERANCE);
        });
    }

    @Test
    public void testVariationVariableAndConstraintsApproxPsts() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeAction(state, 1);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            true);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(mpSolver)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        rangeActionsPerState.get(state).forEach(ra -> {
            MPVariable binary = linearProblem.getRangeActionVariationBinary(ra, state);
            MPConstraint constraintUp = linearProblem.getIsVariationInDirectionConstraint(ra, state, LinearProblem.VariationReferenceExtension.PREPERIMETER, LinearProblem.VariationDirectionExtension.UPWARD);
            MPConstraint constraintDown = linearProblem.getIsVariationInDirectionConstraint(ra, state, LinearProblem.VariationReferenceExtension.PREPERIMETER, LinearProblem.VariationDirectionExtension.DOWNWARD);

            assertNotNull(binary);
            assertNotNull(constraintUp);
            assertNotNull(constraintDown);

            MPVariable setpointVariable = linearProblem.getRangeActionSetpointVariable(ra, state);
            double initialSetpoint = prePerimeterRangeActionActivationResult.getOptimizedSetpoint(ra, state);
            double relaxation = 1e-5;
            if (ra.getId().equals("pst1")) {
                relaxation = 0.3 * 6.9 / 2;
            } else if (ra.getId().equals("pst2")) {
                relaxation = 0.3 * 3;
            } else if (ra.getId().equals("pst3")) {
                relaxation = 0.3 * 4.5 / 3;
            }

            assertEquals(1, constraintUp.getCoefficient(setpointVariable), DOUBLE_TOLERANCE);
            assertEquals(-(ra.getMaxAdmissibleSetpoint(initialSetpoint) + 1e-5 - initialSetpoint - relaxation), constraintUp.getCoefficient(binary), DOUBLE_TOLERANCE);
            assertEquals(-LinearProblem.infinity(), constraintUp.lb(), DOUBLE_TOLERANCE);
            assertEquals(initialSetpoint + relaxation, constraintUp.ub(), DOUBLE_TOLERANCE);

            assertEquals(1, constraintDown.getCoefficient(setpointVariable), DOUBLE_TOLERANCE);
            assertEquals(initialSetpoint - relaxation - ra.getMinAdmissibleSetpoint(initialSetpoint) + 1e-5, constraintDown.getCoefficient(binary), DOUBLE_TOLERANCE);
            assertEquals(initialSetpoint - relaxation, constraintDown.lb(), DOUBLE_TOLERANCE);
            assertEquals(LinearProblem.infinity(), constraintDown.ub(), DOUBLE_TOLERANCE);
        });
    }

    @Test
    public void testSkipConstraints1() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeAction(state, 5);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(mpSolver)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        assertNull(linearProblem.getMaxTsoConstraint(state));
        assertNull(linearProblem.getMaxPstPerTsoConstraint("opA", state));
        assertNull(linearProblem.getMaxPstPerTsoConstraint("opB", state));
        assertNull(linearProblem.getMaxPstPerTsoConstraint("opC", state));
        assertNull(linearProblem.getMaxRaPerTsoConstraint("opA", state));
        assertNull(linearProblem.getMaxRaPerTsoConstraint("opB", state));
        assertNull(linearProblem.getMaxRaPerTsoConstraint("opC", state));
    }

    @Test
    public void testSkipConstraints2() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxTso(state, 3);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(mpSolver)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        assertNull(linearProblem.getMaxRaConstraint(state));
    }

    @Test
    public void testMaxRa() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeAction(state, 4);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false);

        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(mpSolver)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        MPConstraint constraint = linearProblem.getMaxRaConstraint(state);
        assertNotNull(constraint);
        assertEquals(0, constraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(4, constraint.ub(), DOUBLE_TOLERANCE);
        rangeActionsPerState.get(state).forEach(ra ->
            assertEquals(1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(ra, state)), DOUBLE_TOLERANCE));
    }

    private void checkTsoToRaConstraint(String tso, RangeAction<?> ra) {
        MPConstraint constraint = linearProblem.getTsoRaUsedConstraint(tso, ra, state);
        assertNotNull(constraint);
        assertEquals(0, constraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), constraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable(tso, state)), DOUBLE_TOLERANCE);
        assertEquals(-1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(ra, state)), DOUBLE_TOLERANCE);
    }

    @Test
    public void testMaxTso() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxTso(state, 2);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(mpSolver)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        MPConstraint constraint = linearProblem.getMaxTsoConstraint(state);
        assertNotNull(constraint);
        assertEquals(0, constraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(2, constraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable("opA", state)), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable("opB", state)), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable("opC", state)), DOUBLE_TOLERANCE);

        checkTsoToRaConstraint("opA", pst1);
        checkTsoToRaConstraint("opA", pst2);
        checkTsoToRaConstraint("opB", pst3);
        checkTsoToRaConstraint("opA", hvdc);
        checkTsoToRaConstraint("opC", injection);
    }

    @Test
    public void testMaxTsoWithExclusion() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxTso(state, 1);
        raLimitationParameters.setMaxTsoExclusion(state, Set.of("opC"));
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(mpSolver)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        MPConstraint constraint = linearProblem.getMaxTsoConstraint(state);
        assertNotNull(constraint);
        assertEquals(0, constraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable("opA", state)), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable("opB", state)), DOUBLE_TOLERANCE);
        assertNull(linearProblem.getTsoRaUsedVariable("opC", state));
    }

    @Test
    public void testMaxRaPerTso() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeActionPerTso(state, Map.of("opA", 2, "opC", 0));
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false);

        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(mpSolver)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        MPConstraint constraintA = linearProblem.getMaxRaPerTsoConstraint("opA", state);
        assertNotNull(constraintA);
        assertEquals(0, constraintA.lb(), DOUBLE_TOLERANCE);
        assertEquals(2, constraintA.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1, state)), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst2, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst3, state)), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(hvdc, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(injection, state)), DOUBLE_TOLERANCE);

        MPConstraint constraintC = linearProblem.getMaxRaPerTsoConstraint("opC", state);
        assertNotNull(constraintC);
        assertEquals(0, constraintC.lb(), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.ub(), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst2, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst3, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(hvdc, state)), DOUBLE_TOLERANCE);
        assertEquals(1, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(injection, state)), DOUBLE_TOLERANCE);

        assertNull(linearProblem.getMaxPstPerTsoConstraint("opB", state));
    }

    @Test
    public void testMaxPstPerTso() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxPstPerTso(state, Map.of("opA", 1, "opC", 3));
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false);

        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(mpSolver)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        MPConstraint constraintA = linearProblem.getMaxPstPerTsoConstraint("opA", state);
        assertNotNull(constraintA);
        assertEquals(0, constraintA.lb(), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1, state)), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst2, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst3, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(hvdc, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(injection, state)), DOUBLE_TOLERANCE);

        MPConstraint constraintC = linearProblem.getMaxPstPerTsoConstraint("opC", state);
        assertNotNull(constraintC);
        assertEquals(0, constraintC.lb(), DOUBLE_TOLERANCE);
        assertEquals(3, constraintC.ub(), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst2, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst3, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(hvdc, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(injection, state)), DOUBLE_TOLERANCE);

        assertNull(linearProblem.getMaxPstPerTsoConstraint("opB", state));
    }
}
