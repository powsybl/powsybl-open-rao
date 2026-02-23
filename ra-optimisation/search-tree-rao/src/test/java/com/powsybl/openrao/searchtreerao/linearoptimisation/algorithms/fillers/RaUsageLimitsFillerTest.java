/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.PstModel;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.Solver;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.*;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;

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
    private RangeActionActivationResult prePerimeterRangeActionActivationResult;
    private RangeActionSetpointResult prePerimeterRangeActionSetpointResult;
    private State state;
    private Set<RangeAction<?>> rangeActions;

    // for 2P with multi curative tests
    State co1Curative1;
    State co1Curative2;
    State co2Curative2;
    State preventiveState;
    private Map<State, Set<RangeAction<?>>> rangeActionsPerStateMultiCurative;

    private LinearProblem linearProblem;
    private MarginCoreProblemFiller coreProblemFiller;

    @BeforeEach
    public void setup() throws IOException {
        init();
        state = crac.getPreventiveState();

        pst1 = mock(PstRangeAction.class);
        when(pst1.getId()).thenReturn("pst1");
        when(pst1.getOperator()).thenReturn("opA");
        when(pst1.getTapToAngleConversionMap()).thenReturn(Map.of(-1, -5.0, 0, -2.3, 1, 1.9));

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

        rangeActions = Set.of(pst1, pst2, pst3, hvdc, injection);

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

        RangeActionsOptimizationParameters rangeActionParameters = (new RaoParameters()).getRangeActionsOptimizationParameters();

        coreProblemFiller = new MarginCoreProblemFiller(
            optimizationPerimeter,
            prePerimeterRangeActionSetpointResult,
            rangeActionParameters,
            null,
            Unit.MEGAWATT,
            false,
            PstModel.CONTINUOUS,
            null);
    }

    void setUpMultiCurativeIn2P() {

        // modify the setUp to mock a multi-curative situation in 2P.
        Instant preventive = Mockito.mock(Instant.class);
        when(preventive.getOrder()).thenReturn(0);
        Instant curative1 = Mockito.mock(Instant.class);
        Instant curative2 = Mockito.mock(Instant.class);
        when(curative1.getOrder()).thenReturn(1);
        when(curative2.getOrder()).thenReturn(2);
        when(curative1.comesBefore(curative2)).thenReturn(true);
        when(curative2.comesBefore(curative1)).thenReturn(false);
        when(curative1.isCurative()).thenReturn(true);
        when(curative2.isCurative()).thenReturn(true);
        when(preventive.isCurative()).thenReturn(false);
        when(preventive.isPreventive()).thenReturn(true);
        Contingency co1 = Mockito.mock(Contingency.class);
        Contingency co2 = Mockito.mock(Contingency.class);

        co1Curative1 = Mockito.mock(State.class);
        when(co1Curative1.getInstant()).thenReturn(curative1);
        when(co1Curative1.getContingency()).thenReturn(Optional.of(co1));
        when(co1Curative1.getId()).thenReturn("co1Curative1");

        co1Curative2 = Mockito.mock(State.class);
        when(co1Curative2.getInstant()).thenReturn(curative2);
        when(co1Curative2.getContingency()).thenReturn(Optional.of(co1));
        when(co1Curative2.getId()).thenReturn("co1Curative2");

        co2Curative2 = Mockito.mock(State.class);
        when(co2Curative2.getInstant()).thenReturn(curative2);
        when(co2Curative2.getContingency()).thenReturn(Optional.of(co2));
        when(co2Curative2.getId()).thenReturn("co2Curative2");

        preventiveState = Mockito.mock(State.class);
        when(preventiveState.getInstant()).thenReturn(preventive);
        when(preventiveState.getContingency()).thenReturn(Optional.empty());
        when(preventiveState.getId()).thenReturn("preventiveState");
        when(preventiveState.isPreventive()).thenReturn(true);


        // add multi-curative states
        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        rangeActionsPerStateMultiCurative = Map.of(co1Curative1, Set.of(pst1, hvdc, injection), co1Curative2, Set.of(pst1, pst2, pst3), co2Curative2, Set.of(pst2, pst3), preventiveState, Set.of(injection));
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActionsPerStateMultiCurative);
        RangeActionsOptimizationParameters rangeActionParameters = (new RaoParameters()).getRangeActionsOptimizationParameters();

        coreProblemFiller = new MarginCoreProblemFiller(
            optimizationPerimeter,
            prePerimeterRangeActionSetpointResult,
            rangeActionParameters,
            null,
            Unit.MEGAWATT,
            false,
            PstModel.CONTINUOUS,
            null);
    }

    @Test
    void testSkipFiller() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
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
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        rangeActionsPerState.get(state).forEach(ra -> {
            OpenRaoMPVariable binary = linearProblem.getRangeActionVariationBinary(ra, state);
            OpenRaoMPConstraint constraint = linearProblem.getIsVariationConstraint(ra, state);

            assertNotNull(binary);
            assertNotNull(constraint);

            OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(ra, state, LinearProblem.VariationDirectionExtension.UPWARD);
            OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(ra, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
            double initialSetpoint = prePerimeterRangeActionActivationResult.getOptimizedSetpoint(ra, state);

            assertEquals(1, constraint.getCoefficient(upwardVariationVariable), DOUBLE_TOLERANCE);
            assertEquals(1, constraint.getCoefficient(downwardVariationVariable), DOUBLE_TOLERANCE);
            assertEquals(-(ra.getMaxAdmissibleSetpoint(initialSetpoint) + RANGE_ACTION_SETPOINT_EPSILON - ra.getMinAdmissibleSetpoint(initialSetpoint)), constraint.getCoefficient(binary), DOUBLE_TOLERANCE);
            assertEquals(-linearProblem.infinity(), constraint.lb(), linearProblem.infinity() * 1e-3);
        });
    }

    @Test
    void testVariationVariableAndConstraintsApproxPsts() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeAction(state, 1);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            true,
            network,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        rangeActionsPerState.get(state).forEach(ra -> {
            OpenRaoMPVariable binary = linearProblem.getRangeActionVariationBinary(ra, state);
            OpenRaoMPConstraint constraint = linearProblem.getIsVariationConstraint(ra, state);

            assertNotNull(binary);
            assertNotNull(constraint);

            OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(ra, state, LinearProblem.VariationDirectionExtension.UPWARD);
            OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(ra, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
            double initialSetpoint = prePerimeterRangeActionActivationResult.getOptimizedSetpoint(ra, state);

            assertEquals(1, constraint.getCoefficient(upwardVariationVariable), DOUBLE_TOLERANCE);
            assertEquals(1, constraint.getCoefficient(downwardVariationVariable), DOUBLE_TOLERANCE);
            assertEquals(-(ra.getMaxAdmissibleSetpoint(initialSetpoint) + RANGE_ACTION_SETPOINT_EPSILON - ra.getMinAdmissibleSetpoint(initialSetpoint)), constraint.getCoefficient(binary), DOUBLE_TOLERANCE);
            assertEquals(-linearProblem.infinity(), constraint.lb(), linearProblem.infinity() * 1e-3);
        });
    }

    @Test
    void testSkipConstraints1() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeAction(state, 5);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
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
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
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
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);

        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        OpenRaoMPConstraint constraint = linearProblem.getMaxRaConstraint(state);
        assertNotNull(constraint);
        assertEquals(0, constraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(4, constraint.ub(), DOUBLE_TOLERANCE);
        rangeActionsPerState.get(state).forEach(ra ->
            assertEquals(1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(ra, state)), DOUBLE_TOLERANCE));
    }

    @Test
    void testSkipLargeMaxRa1() {
        // maxRa = 5 but there are only 5 RangeActions, so skip the constraint
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeAction(state, 5);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);

        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        Exception exception = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxRaConstraint(state));
        assertEquals("Constraint maxra_preventive_constraint has not been created yet", exception.getMessage());
    }

    @Test
    void testSkipLargeMaxRa2() {
        // maxRa = 6 but there are only 5 RangeActions, so skip the constraint
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeAction(state, 6);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);

        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        Exception exception = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxRaConstraint(state));
        assertEquals("Constraint maxra_preventive_constraint has not been created yet", exception.getMessage());
    }

    private void checkTsoToRaConstraint(String tso, RangeAction<?> ra) {
        OpenRaoMPConstraint constraint = linearProblem.getTsoRaUsedConstraint(tso, ra, state);
        assertNotNull(constraint);
        assertEquals(0, constraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), constraint.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable(tso, state)), DOUBLE_TOLERANCE);
        assertEquals(-1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(ra, state)), DOUBLE_TOLERANCE);
    }

    @Test
    void testMaxTso() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxTso(state, 2);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        OpenRaoMPConstraint constraint = linearProblem.getMaxTsoConstraint(state);
        assertNotNull(constraint);
        assertEquals(0, constraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(2, constraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opA", state)), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opB", state)), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opC", state)), DOUBLE_TOLERANCE);

        checkTsoToRaConstraint("opA", pst1);
        checkTsoToRaConstraint("opA", pst2);
        checkTsoToRaConstraint("opB", pst3);
        checkTsoToRaConstraint("opA", hvdc);
        checkTsoToRaConstraint("opC", injection);
    }

    @Test
    void testSkipLargeMaxTso1() {
        // maxTso = 3 but there are only 3 TSOs, so no need to set the constraint
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxTso(state, 3);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxTsoConstraint(state));
        assertEquals("Constraint maxtso_preventive_constraint has not been created yet", e.getMessage());
    }

    @Test
    void testSkipLargeMaxTso2() {
        // maxTso = 4 but there are only 3 TSOs, so no need to set the constraint
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxTso(state, 4);
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxTsoConstraint(state));
        assertEquals("Constraint maxtso_preventive_constraint has not been created yet", e.getMessage());
    }

    @Test
    void testMaxTsoWithExclusion() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxTso(state, 1);
        raLimitationParameters.setMaxTsoExclusion(state, Set.of("opC"));
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        OpenRaoMPConstraint constraint = linearProblem.getMaxTsoConstraint(state);
        assertNotNull(constraint);
        assertEquals(0, constraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opA", state)), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opB", state)), DOUBLE_TOLERANCE);
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getTsoRaUsedCumulativeVariable("opC", state));
        assertEquals("Variable tsorausedcumulative_opC_preventive_variable has not been created yet", e.getMessage());
    }

    @Test
    void testMaxRaPerTso() {
        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeActionPerTso(state, Map.of("opA", 2, "opC", 0));
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);

        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
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
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);

        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
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

    @Test
    void testMaxElementaryActionsPerTsoConstraint() {
        when(prePerimeterRangeActionSetpointResult.getTap(pst1)).thenReturn(1);
        when(prePerimeterRangeActionSetpointResult.getTap(pst2)).thenReturn(1);
        when(pst1.getCurrentTapPosition(network)).thenReturn(-1);
        when(pst2.getCurrentTapPosition(network)).thenReturn(0);

        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxElementaryActionsPerTso(state, Map.of("opA", 14));
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerState,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            true,
            network,
            false);

        Map<State, Set<PstRangeAction>> pstRangeActionsPerState = new HashMap<>();
        rangeActionsPerState.forEach((s, rangeActionSet) -> rangeActionSet.stream().filter(PstRangeAction.class::isInstance).map(PstRangeAction.class::cast).forEach(pstRangeAction -> pstRangeActionsPerState.computeIfAbsent(s, e -> new HashSet<>()).add(pstRangeAction)));

        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        when(optimizationPerimeter.getMainOptimizationState()).thenReturn(state);
        when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActionsPerState);

        DiscretePstTapFiller discretePstTapFiller = new DiscretePstTapFiller(optimizationPerimeter, pstRangeActionsPerState, prePerimeterRangeActionSetpointResult, new RangeActionsOptimizationParameters(), false);

        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(discretePstTapFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
            .withInitialRangeActionActivationResult(prePerimeterRangeActionActivationResult)
            .build();

        linearProblem.fill(flowResult, sensitivityResult);

        // PST 1
        OpenRaoMPVariable pst1AbsoluteVariationFromInitialTapVariable = linearProblem.getPstAbsoluteVariationFromInitialTapVariable(pst1, state);
        assertEquals("pstabsolutevariationfrominitialtap_pst1_preventive_variable", pst1AbsoluteVariationFromInitialTapVariable.name());
        assertEquals(0, pst1AbsoluteVariationFromInitialTapVariable.lb());
        assertEquals(linearProblem.infinity(), pst1AbsoluteVariationFromInitialTapVariable.ub(), linearProblem.infinity() * 1e-3);

        OpenRaoMPVariable pst1TapVariationUpwardVariable = linearProblem.getPstTapVariationVariable(pst1, state, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable pst1TapVariationDownwardVariable = linearProblem.getPstTapVariationVariable(pst1, state, LinearProblem.VariationDirectionExtension.DOWNWARD);

        OpenRaoMPConstraint pst1AbsoluteVariationFromInitialTapConstraintPositive = linearProblem.getPstAbsoluteVariationFromInitialTapConstraint(pst1, state, LinearProblem.AbsExtension.POSITIVE);
        assertEquals("pstabsolutevariationfrominitialtap_pst1_preventive_constraint_POSITIVE", pst1AbsoluteVariationFromInitialTapConstraintPositive.name());
        assertEquals(-2, pst1AbsoluteVariationFromInitialTapConstraintPositive.lb());
        assertEquals(linearProblem.infinity(), pst1AbsoluteVariationFromInitialTapConstraintPositive.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(1d, pst1AbsoluteVariationFromInitialTapConstraintPositive.getCoefficient(pst1AbsoluteVariationFromInitialTapVariable));
        assertEquals(-1d, pst1AbsoluteVariationFromInitialTapConstraintPositive.getCoefficient(pst1TapVariationUpwardVariable));
        assertEquals(1d, pst1AbsoluteVariationFromInitialTapConstraintPositive.getCoefficient(pst1TapVariationDownwardVariable));

        OpenRaoMPConstraint pst1AbsoluteVariationFromInitialTapConstraintNegative = linearProblem.getPstAbsoluteVariationFromInitialTapConstraint(pst1, state, LinearProblem.AbsExtension.NEGATIVE);
        assertEquals("pstabsolutevariationfrominitialtap_pst1_preventive_constraint_NEGATIVE", pst1AbsoluteVariationFromInitialTapConstraintNegative.name());
        assertEquals(2, pst1AbsoluteVariationFromInitialTapConstraintNegative.lb());
        assertEquals(linearProblem.infinity(), pst1AbsoluteVariationFromInitialTapConstraintNegative.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(1d, pst1AbsoluteVariationFromInitialTapConstraintNegative.getCoefficient(pst1AbsoluteVariationFromInitialTapVariable));
        assertEquals(1d, pst1AbsoluteVariationFromInitialTapConstraintNegative.getCoefficient(pst1TapVariationUpwardVariable));
        assertEquals(-1d, pst1AbsoluteVariationFromInitialTapConstraintNegative.getCoefficient(pst1TapVariationDownwardVariable));

        // PST 2
        OpenRaoMPVariable pst2AbsoluteVariationFromInitialTapVariable = linearProblem.getPstAbsoluteVariationFromInitialTapVariable(pst2, state);
        assertEquals("pstabsolutevariationfrominitialtap_pst2_preventive_variable", pst2AbsoluteVariationFromInitialTapVariable.name());
        assertEquals(0, pst2AbsoluteVariationFromInitialTapVariable.lb());
        assertEquals(linearProblem.infinity(), pst2AbsoluteVariationFromInitialTapVariable.ub(), linearProblem.infinity() * 1e-3);

        OpenRaoMPVariable pst2TapVariationUpwardVariable = linearProblem.getPstTapVariationVariable(pst2, state, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable pst2TapVariationDownwardVariable = linearProblem.getPstTapVariationVariable(pst2, state, LinearProblem.VariationDirectionExtension.DOWNWARD);

        OpenRaoMPConstraint pst2AbsoluteVariationFromInitialTapConstraintPositive = linearProblem.getPstAbsoluteVariationFromInitialTapConstraint(pst2, state, LinearProblem.AbsExtension.POSITIVE);
        assertEquals("pstabsolutevariationfrominitialtap_pst2_preventive_constraint_POSITIVE", pst2AbsoluteVariationFromInitialTapConstraintPositive.name());
        assertEquals(-1, pst2AbsoluteVariationFromInitialTapConstraintPositive.lb());
        assertEquals(linearProblem.infinity(), pst2AbsoluteVariationFromInitialTapConstraintPositive.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(1d, pst2AbsoluteVariationFromInitialTapConstraintPositive.getCoefficient(pst2AbsoluteVariationFromInitialTapVariable));
        assertEquals(-1d, pst2AbsoluteVariationFromInitialTapConstraintPositive.getCoefficient(pst2TapVariationUpwardVariable));
        assertEquals(1d, pst2AbsoluteVariationFromInitialTapConstraintPositive.getCoefficient(pst2TapVariationDownwardVariable));

        OpenRaoMPConstraint pst2AbsoluteVariationFromInitialTapConstraintNegative = linearProblem.getPstAbsoluteVariationFromInitialTapConstraint(pst2, state, LinearProblem.AbsExtension.NEGATIVE);
        assertEquals("pstabsolutevariationfrominitialtap_pst2_preventive_constraint_NEGATIVE", pst2AbsoluteVariationFromInitialTapConstraintNegative.name());
        assertEquals(1, pst2AbsoluteVariationFromInitialTapConstraintNegative.lb());
        assertEquals(linearProblem.infinity(), pst2AbsoluteVariationFromInitialTapConstraintNegative.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(1d, pst2AbsoluteVariationFromInitialTapConstraintNegative.getCoefficient(pst2AbsoluteVariationFromInitialTapVariable));
        assertEquals(1d, pst2AbsoluteVariationFromInitialTapConstraintNegative.getCoefficient(pst2TapVariationUpwardVariable));
        assertEquals(-1d, pst2AbsoluteVariationFromInitialTapConstraintNegative.getCoefficient(pst2TapVariationDownwardVariable));

        // PST 3 -> no constraint for TSO
        assertThrows(OpenRaoException.class, () -> linearProblem.getPstAbsoluteVariationFromInitialTapVariable(pst3, state));

        // TSO max elementary actions constraint
        OpenRaoMPConstraint maxElementaryActionsConstraint = linearProblem.getTsoMaxElementaryActionsConstraint("opA", state);
        assertEquals("maxelementaryactionspertso_opA_preventive_constraint", maxElementaryActionsConstraint.name());
        assertEquals(0, maxElementaryActionsConstraint.lb());
        assertEquals(14, maxElementaryActionsConstraint.ub());
        assertEquals(1d, maxElementaryActionsConstraint.getCoefficient(pst1AbsoluteVariationFromInitialTapVariable));
        assertEquals(1d, maxElementaryActionsConstraint.getCoefficient(pst2AbsoluteVariationFromInitialTapVariable));
    }

    @Test
    void testGetAllRangeActionsAvailableForAllPreviousCurativeStates() {
        setUpMultiCurativeIn2P();

        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();

        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerStateMultiCurative,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);

        Map<State, Set<RangeAction<?>>> rangeActionsPerStateBeforeCo1Curative1 = raUsageLimitsFiller.getAllRangeActionsAvailableForAllPreviousCurativeStates(co1Curative1);
        assertEquals(Map.of(co1Curative1, Set.of(pst1, hvdc, injection)), rangeActionsPerStateBeforeCo1Curative1);
        Map<State, Set<RangeAction<?>>> rangeActionsPerStateBeforeCo2Curative1 = raUsageLimitsFiller.getAllRangeActionsAvailableForAllPreviousCurativeStates(co2Curative2);
        assertEquals(Map.of(co2Curative2, Set.of(pst2, pst3)), rangeActionsPerStateBeforeCo2Curative1);
        Map<State, Set<RangeAction<?>>> rangeActionsPerStateBeforePreventive = raUsageLimitsFiller.getAllRangeActionsAvailableForAllPreviousCurativeStates(preventiveState);
        assertEquals(Map.of(preventiveState, Set.of(injection)), rangeActionsPerStateBeforePreventive);

        // look for all the range action available for state co1Curative2 + all the curative state defined on the same contingency that come before.
        // The range actions available for the preventive state should not be included
        // co2curative1 is not included either since it's defined on another contingency != co1Curative2's contingency
        Map<State, Set<RangeAction<?>>> rangeActionsPerStateBeforeCo1Curative2 = raUsageLimitsFiller.getAllRangeActionsAvailableForAllPreviousCurativeStates(co1Curative2);
        assertEquals(Map.of(co1Curative1, Set.of(pst1, hvdc, injection), co1Curative2, Set.of(pst1, pst2, pst3)), rangeActionsPerStateBeforeCo1Curative2);
    }

    @Test
    void testMaxRaUsageLimitMultiCurativeSecondPreventive() {
        // Check that the Max RA Usage Limit is correctly defined in multi curative scenarios
        // ie. take into account in a cumulative way all the range actions from previous and current curative state sharing (same contingencies !)
        // when defining the max ra usage limit constraint

        setUpMultiCurativeIn2P();

        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxRangeAction(co1Curative1, 1);
        raLimitationParameters.setMaxRangeAction(co1Curative2, 2);
        raLimitationParameters.setMaxRangeAction(co2Curative2, 1);
        raLimitationParameters.setMaxRangeAction(preventiveState, 2); // not constraint (only 1 RA is available)
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerStateMultiCurative,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);

        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        // Check the constraint for all states
        // co1Curative1
        OpenRaoMPConstraint constraint = linearProblem.getMaxRaConstraint(co1Curative1);
        assertNotNull(constraint);
        assertEquals(0, constraint.lb());
        assertEquals(1, constraint.ub());
        assertEquals(1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1, co1Curative1)));
        assertEquals(1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(hvdc, co1Curative1)));
        assertEquals(1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(injection, co1Curative1)));
        // co1Curative2 should take into account co1Curative1's range action
        constraint = linearProblem.getMaxRaConstraint(co1Curative2);
        assertNotNull(constraint);
        assertEquals(0, constraint.lb());
        assertEquals(2, constraint.ub());
        assertEquals(1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1, co1Curative1)));
        assertEquals(1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(hvdc, co1Curative1)));
        assertEquals(1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(injection, co1Curative1)));
        assertEquals(1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1, co1Curative2)));
        assertEquals(1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(pst2, co1Curative2)));
        assertEquals(1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(pst3, co1Curative2)));
        // co2Curative2
        constraint = linearProblem.getMaxRaConstraint(co2Curative2);
        assertNotNull(constraint);
        assertEquals(0, constraint.lb());
        assertEquals(1, constraint.ub());
        assertEquals(0, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1, co1Curative1)));
        assertEquals(0, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(hvdc, co1Curative1)));
        assertEquals(1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(pst2, co2Curative2)));
        assertEquals(1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(pst3, co2Curative2)));
        // preventiveState
        Exception exception = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxRaConstraint(preventiveState));
        assertEquals("Constraint maxra_preventiveState_constraint has not been created yet", exception.getMessage());
    }

    @Test
    void testMaxTsoUsageLimitMultiCurativeSecondPreventive() {
        // Test cumulative effect for multi curative 2nd prev
        // In total we have 3 TSO : opA, opB, opC.
        // Check that maxTso constraints are correctly created and filled
        // Check that cumulative binary variables are correctly created and constraint.

        setUpMultiCurativeIn2P();

        RangeActionLimitationParameters raLimitationParameters = new RangeActionLimitationParameters();
        raLimitationParameters.setMaxTso(co1Curative1, 1);
        raLimitationParameters.setMaxTso(co1Curative2, 1);

        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
            rangeActionsPerStateMultiCurative,
            prePerimeterRangeActionSetpointResult,
            raLimitationParameters,
            false,
            network,
            false);

        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(raUsageLimitsFiller)
            .withSolver(Solver.SCIP)
            .build();

        linearProblem.fill(flowResult, sensitivityResult);

        // Check constraint for state co1Curative1

        // 1. Check maxTsoConstraint sum_tso(delta_cumulative_tso_co1Curative1) <= TSOmax
        OpenRaoMPConstraint constraintCo1Curative1   = linearProblem.getMaxTsoConstraint(co1Curative1);
        assertNotNull(constraintCo1Curative1);
        assertEquals(0, constraintCo1Curative1.lb());
        assertEquals(1, constraintCo1Curative1.ub());
        assertEquals(1, constraintCo1Curative1.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opA", co1Curative1)));
        Exception exception = assertThrows(OpenRaoException.class, () -> linearProblem.getTsoRaUsedCumulativeVariable("opB", co1Curative1));
        assertEquals("Variable tsorausedcumulative_opB_co1Curative1_variable has not been created yet", exception.getMessage());
        assertEquals(1, constraintCo1Curative1.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opC", co1Curative1)));
        // check that variable from other state not used
        assertEquals(0, constraintCo1Curative1.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opA", co1Curative2)));
        assertEquals(0, constraintCo1Curative1.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opB", co1Curative2)));
        assertEquals(0, constraintCo1Curative1.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opC", co1Curative2)));

        // 2. check TsoRaUsedCumulativeConstraint

        OpenRaoMPConstraint constraintOpACo1Curative1   = linearProblem.getTsoRaUsedCumulativeConstraint("opA", co1Curative1);
        assertEquals(0, constraintOpACo1Curative1.lb());
        assertEquals(linearProblem.infinity(), constraintOpACo1Curative1.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(1, constraintOpACo1Curative1.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opA", co1Curative1)));
        assertEquals(-1, constraintOpACo1Curative1.getCoefficient(linearProblem.getTsoRaUsedVariable("opA", co1Curative1)));
        assertEquals(0, constraintOpACo1Curative1.getCoefficient(linearProblem.getTsoRaUsedVariable("opA", co1Curative2)));

        OpenRaoMPConstraint constraintOpCCo1Curative1   = linearProblem.getTsoRaUsedCumulativeConstraint("opC", co1Curative1);
        assertEquals(0, constraintOpCCo1Curative1.lb());
        assertEquals(linearProblem.infinity(), constraintOpCCo1Curative1.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(1, constraintOpCCo1Curative1.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opC", co1Curative1)));
        assertEquals(-1, constraintOpCCo1Curative1.getCoefficient(linearProblem.getTsoRaUsedVariable("opC", co1Curative1)));

        // Check constraint for state co1Curative2

        // 1. Check maxTsoConstraint sum_tso(delta_cumulative_tso_co1Curative1) <= TSOmax
        OpenRaoMPConstraint constraintCo1Curative2 = linearProblem.getMaxTsoConstraint(co1Curative2);
        assertNotNull(constraintCo1Curative2);
        assertEquals(0, constraintCo1Curative2.lb());
        assertEquals(1, constraintCo1Curative2.ub());
        assertEquals(1, constraintCo1Curative2.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opA", co1Curative2)));
        assertEquals(1, constraintCo1Curative2.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opB", co1Curative2)));
        assertEquals(1, constraintCo1Curative2.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opC", co1Curative2)));
        // check that variable from other state not used
        assertEquals(0, constraintCo1Curative2.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opA", co1Curative1)));
        assertEquals(0, constraintCo1Curative2.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opC", co1Curative1)));

        // 2. check TsoRaUsedCumulativeConstraint
        OpenRaoMPConstraint constraintOpACo1Curative2   = linearProblem.getTsoRaUsedCumulativeConstraint("opA", co1Curative2);
        assertEquals(0, constraintOpACo1Curative2.lb());
        assertEquals(linearProblem.infinity(), constraintOpACo1Curative2.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(1, constraintOpACo1Curative2.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opA", co1Curative2)));
        assertEquals(-1, constraintOpACo1Curative2.getCoefficient(linearProblem.getTsoRaUsedVariable("opA", co1Curative1)));
        assertEquals(-1, constraintOpACo1Curative2.getCoefficient(linearProblem.getTsoRaUsedVariable("opA", co1Curative2)));

        OpenRaoMPConstraint constraintOpBCo1Curative2   = linearProblem.getTsoRaUsedCumulativeConstraint("opB", co1Curative2);
        assertEquals(0, constraintOpBCo1Curative2.lb());
        assertEquals(linearProblem.infinity(), constraintOpBCo1Curative2.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(1, constraintOpBCo1Curative2.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opB", co1Curative2)));
        assertEquals(-1, constraintOpBCo1Curative2.getCoefficient(linearProblem.getTsoRaUsedVariable("opB", co1Curative2))); // co1Curative1 has no range action from opB

        OpenRaoMPConstraint constraintOpCCo1Curative2   = linearProblem.getTsoRaUsedCumulativeConstraint("opC", co1Curative2);
        assertEquals(0, constraintOpCCo1Curative2.lb());
        assertEquals(linearProblem.infinity(), constraintOpCCo1Curative2.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(1, constraintOpCCo1Curative2.getCoefficient(linearProblem.getTsoRaUsedCumulativeVariable("opC", co1Curative2)));
        assertEquals(-1, constraintOpCCo1Curative2.getCoefficient(linearProblem.getTsoRaUsedVariable("opC", co1Curative1)));  // co1Curative2 has no range action from opC
    }

}
