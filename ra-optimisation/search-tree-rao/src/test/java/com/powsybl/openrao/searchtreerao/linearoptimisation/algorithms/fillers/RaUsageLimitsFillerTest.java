/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.*;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RaUsageLimitsFillerTest extends AbstractFillerTest {
    private static final double DOUBLE_TOLERANCE = 1e-5;
    private static final double RANGE_ACTION_SETPOINT_EPSILON = 1e-4;

    private PstRangeAction pst1;
    private PstRangeAction pst2;
    private PstRangeAction pst3;
    private HvdcRangeAction hvdc;
    private InjectionRangeAction injection;
    private Map<State, Set<RangeAction<?>>> rangeActionsPerState;
    private RangeActionResult prePerimeterRangeActionResult;
    private RangeActionResult prePerimeterRangeActionResult;
    private State state;

    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;

    @BeforeEach
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

        prePerimeterRangeActionResult = mock(RangeActionResult.class);

        when(prePerimeterRangeActionResult.getRangeActions()).thenReturn(rangeActions);
        when(prePerimeterRangeActionResult.getSetpoint(pst1)).thenReturn(1.);
        when(prePerimeterRangeActionResult.getSetpoint(pst2)).thenReturn(2.);
        when(prePerimeterRangeActionResult.getSetpoint(pst3)).thenReturn(3.);
        when(prePerimeterRangeActionResult.getSetpoint(hvdc)).thenReturn(4.);
        when(prePerimeterRangeActionResult.getSetpoint(injection)).thenReturn(5.);

        prePerimeterRangeActionResult = new RangeActionResultImpl(prePerimeterRangeActionResult);

        rangeActions.forEach(ra -> {
            double min = -10 * prePerimeterRangeActionResult.getOptimizedSetpoint(ra, state);
            double max = 20 * prePerimeterRangeActionResult.getOptimizedSetpoint(ra, state);
            when(ra.getMinAdmissibleSetpoint(anyDouble())).thenReturn(min);
            when(ra.getMaxAdmissibleSetpoint(anyDouble())).thenReturn(max);
        });
        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);

        rangeActionsPerState = new HashMap<>();
        rangeActionsPerState.put(state, rangeActions);
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActionsPerState);

        RangeActionsOptimizationParameters rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(new RaoParameters());

        coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            prePerimeterRangeActionResult,
            prePerimeterRangeActionResult,
            rangeActionParameters,
            Unit.MEGAWATT,
            false);
    }

    @Test
    void testSkipFiller() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionResult,
            raLimitationParameters,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        rangeActionsPerState.get(state).forEach(ra -> {
            Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getRangeActionVariationBinary(ra, state));
            assertEquals(String.format("Variable %s has not been created yet", LinearProblemIdGenerator.rangeActionBinaryVariableId(ra, state)), e.getMessage());
        });
    }

    @Test
    void testVariationVariableAndConstraints() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeAction(state, 1);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionResult,
            raLimitationParameters,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        rangeActionsPerState.get(state).forEach(ra -> {
            OpenRaoMPVariable binary = linearProblem.getRangeActionVariationBinary(ra, state);
            OpenRaoMPConstraint constraint = linearProblem.getIsVariationConstraint(ra, state);

            assertNotNull(binary);
            assertNotNull(constraint);

            OpenRaoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(ra, state);
            double initialSetpoint = prePerimeterRangeActionResult.getOptimizedSetpoint(ra, state);

            assertEquals(1, constraint.getCoefficient(absoluteVariationVariable), DOUBLE_TOLERANCE);
            assertEquals(-(ra.getMaxAdmissibleSetpoint(initialSetpoint) + RANGE_ACTION_SETPOINT_EPSILON - ra.getMinAdmissibleSetpoint(initialSetpoint)), constraint.getCoefficient(binary), DOUBLE_TOLERANCE);
            assertEquals(-LinearProblem.infinity(), constraint.lb(), INFINITY_TOLERANCE);
        });
    }

    @Test
    void testVariationVariableAndConstraintsApproxPsts() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeAction(state, 1);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionResult,
            raLimitationParameters,
            true);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        rangeActionsPerState.get(state).forEach(ra -> {
            OpenRaoMPVariable binary = linearProblem.getRangeActionVariationBinary(ra, state);
            OpenRaoMPConstraint constraint = linearProblem.getIsVariationConstraint(ra, state);

            assertNotNull(binary);
            assertNotNull(constraint);

            OpenRaoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(ra, state);
            double initialSetpoint = prePerimeterRangeActionResult.getOptimizedSetpoint(ra, state);
            double relaxation = 1e-5;
            if (ra.getId().equals("pst1")) {
                relaxation = 0.3 * 6.9 / 2;
            } else if (ra.getId().equals("pst2")) {
                relaxation = 0.3 * 3;
            } else if (ra.getId().equals("pst3")) {
                relaxation = 0.3 * 4.5 / 3;
            }

            assertEquals(1, constraint.getCoefficient(absoluteVariationVariable), DOUBLE_TOLERANCE);
            assertEquals(-(ra.getMaxAdmissibleSetpoint(initialSetpoint) + RANGE_ACTION_SETPOINT_EPSILON - ra.getMinAdmissibleSetpoint(initialSetpoint)), constraint.getCoefficient(binary), DOUBLE_TOLERANCE);
            assertEquals(-LinearProblem.infinity(), constraint.lb(), INFINITY_TOLERANCE);
        });
    }

    @Test
    void testSkipConstraints1() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeAction(state, 5);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionResult,
            raLimitationParameters,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxTsoConstraint(state));
        assertEquals("Constraint maxtso_preventive_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxPstPerTsoConstraint("opA", state));
        assertEquals("Constraint maxpstpertso_opA_preventive_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxPstPerTsoConstraint("opB", state));
        assertEquals("Constraint maxpstpertso_opB_preventive_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxPstPerTsoConstraint("opC", state));
        assertEquals("Constraint maxpstpertso_opC_preventive_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxRaPerTsoConstraint("opA", state));
        assertEquals("Constraint maxrapertso_opA_preventive_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxRaPerTsoConstraint("opB", state));
        assertEquals("Constraint maxrapertso_opB_preventive_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxRaPerTsoConstraint("opC", state));
        assertEquals("Constraint maxrapertso_opC_preventive_constraint has not been created yet", e.getMessage());
    }

    @Test
    void testSkipConstraints2() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxTso(state, 3);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionResult,
            raLimitationParameters,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxRaConstraint(state));
        assertEquals("Constraint maxra_preventive_constraint has not been created yet", e.getMessage());
    }

    @Test
    void testMaxRa() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeAction(state, 4);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionResult,
            raLimitationParameters,
            false);

        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        OpenRaoMPConstraint constraint = linearProblem.getMaxRaConstraint(state);
        assertNotNull(constraint);
        assertEquals(0, constraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(4, constraint.ub(), DOUBLE_TOLERANCE);
        rangeActionsPerState.get(state).forEach(ra ->
            assertEquals(1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(ra, state)), DOUBLE_TOLERANCE));
    }

    private void checkTsoToRaConstraint(String tso, RangeAction<?> ra) {
        OpenRaoMPConstraint constraint = linearProblem.getTsoRaUsedConstraint(tso, ra, state);
        assertNotNull(constraint);
        assertEquals(0, constraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), constraint.ub(), INFINITY_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable(tso, state)), DOUBLE_TOLERANCE);
        assertEquals(-1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(ra, state)), DOUBLE_TOLERANCE);
    }

    @Test
    void testMaxTso() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxTso(state, 2);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionResult,
            raLimitationParameters,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        OpenRaoMPConstraint constraint = linearProblem.getMaxTsoConstraint(state);
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
    void testMaxTsoWithExclusion() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxTso(state, 1);
        raLimitationParameters.setMaxTsoExclusion(state, Set.of("opC"));
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionResult,
            raLimitationParameters,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        OpenRaoMPConstraint constraint = linearProblem.getMaxTsoConstraint(state);
        assertNotNull(constraint);
        assertEquals(0, constraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable("opA", state)), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable("opB", state)), DOUBLE_TOLERANCE);
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getTsoRaUsedVariable("opC", state));
        assertEquals("Variable tsoraused_opC_preventive_variable has not been created yet", e.getMessage());
    }

    @Test
    void testMaxRaPerTso() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeActionPerTso(state, Map.of("opA", 2, "opC", 0));
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionResult,
            raLimitationParameters,
            false);

        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        OpenRaoMPConstraint constraintA = linearProblem.getMaxRaPerTsoConstraint("opA", state);
        assertNotNull(constraintA);
        assertEquals(0, constraintA.lb(), DOUBLE_TOLERANCE);
        assertEquals(2, constraintA.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1, state)), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst2, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst3, state)), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(hvdc, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(injection, state)), DOUBLE_TOLERANCE);

        OpenRaoMPConstraint constraintC = linearProblem.getMaxRaPerTsoConstraint("opC", state);
        assertNotNull(constraintC);
        assertEquals(0, constraintC.lb(), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.ub(), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst2, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst3, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(hvdc, state)), DOUBLE_TOLERANCE);
        assertEquals(1, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(injection, state)), DOUBLE_TOLERANCE);

        assertThrows(OpenRaoException.class, () -> linearProblem.getMaxPstPerTsoConstraint("opB", state));
    }

    @Test
    void testMaxPstPerTso() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxPstPerTso(state, Map.of("opA", 1, "opC", 3));
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionResult,
            raLimitationParameters,
            false);

        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        OpenRaoMPConstraint constraintA = linearProblem.getMaxPstPerTsoConstraint("opA", state);
        assertNotNull(constraintA);
        assertEquals(0, constraintA.lb(), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1, state)), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst2, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst3, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(hvdc, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(injection, state)), DOUBLE_TOLERANCE);

        OpenRaoMPConstraint constraintC = linearProblem.getMaxPstPerTsoConstraint("opC", state);
        assertNotNull(constraintC);
        assertEquals(0, constraintC.lb(), DOUBLE_TOLERANCE);
        assertEquals(3, constraintC.ub(), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst2, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst3, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(hvdc, state)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(injection, state)), DOUBLE_TOLERANCE);

        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxPstPerTsoConstraint("opB", state));
        assertEquals("Constraint maxpstpertso_opB_preventive_constraint has not been created yet", e.getMessage());
    }
}
