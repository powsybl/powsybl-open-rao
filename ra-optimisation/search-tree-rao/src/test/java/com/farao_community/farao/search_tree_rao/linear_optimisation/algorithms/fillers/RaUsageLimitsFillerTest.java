/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.LinearProblem;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;
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
    private Set<RangeAction<?>> rangeActions;
    private RangeActionResult prePerimeterRangeActionResult;

    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;

    @Before
    public void setup() {
        init();

        pst1 = mock(PstRangeAction.class);
        when(pst1.getId()).thenReturn("pst1");
        when(pst1.getOperator()).thenReturn("opA");

        pst2 = mock(PstRangeAction.class);
        when(pst2.getId()).thenReturn("pst2");
        when(pst2.getOperator()).thenReturn("opA");

        pst3 = mock(PstRangeAction.class);
        when(pst3.getId()).thenReturn("pst3");
        when(pst3.getOperator()).thenReturn("opB");

        hvdc = mock(HvdcRangeAction.class);
        when(hvdc.getId()).thenReturn("hvdc");
        when(hvdc.getOperator()).thenReturn("opA");

        injection = mock(InjectionRangeAction.class);
        when(injection.getId()).thenReturn("injection");
        when(injection.getOperator()).thenReturn("opC");

        rangeActions = Set.of(pst1, pst2, pst3, hvdc, injection);

        prePerimeterRangeActionResult = mock(RangeActionResult.class);

        when(prePerimeterRangeActionResult.getOptimizedSetPoint(pst1)).thenReturn(1.);
        when(prePerimeterRangeActionResult.getOptimizedSetPoint(pst2)).thenReturn(2.);
        when(prePerimeterRangeActionResult.getOptimizedSetPoint(pst3)).thenReturn(3.);
        when(prePerimeterRangeActionResult.getOptimizedSetPoint(hvdc)).thenReturn(4.);
        when(prePerimeterRangeActionResult.getOptimizedSetPoint(injection)).thenReturn(5.);

        rangeActions.forEach(ra -> {
            double min = -10 * prePerimeterRangeActionResult.getOptimizedSetPoint(ra);
            double max = 20 * prePerimeterRangeActionResult.getOptimizedSetPoint(ra);
            when(ra.getMinAdmissibleSetpoint(anyDouble())).thenReturn(min);
            when(ra.getMaxAdmissibleSetpoint(anyDouble())).thenReturn(max);
        });

        // fill the problem : the core filler is required
        coreProblemFiller = new CoreProblemFiller(
            network,
            Set.of(),
            rangeActions,
            prePerimeterRangeActionResult,
            0.,
            0.,
            0.,
            false
        );
    }

    @Test
    public void testSkipFiller() {
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(rangeActions, prePerimeterRangeActionResult, null, null, null, null, null);
        linearProblem = new LinearProblem(List.of(coreProblemFiller, raUsageLimitsFiller), mpSolver);
        linearProblem.fill(flowResult, sensitivityResult);

        rangeActions.forEach(ra -> assertNull(linearProblem.getRangeActionVariationBinary(ra)));
    }

    @Test
    public void testVariationVariableAndConstraints() {
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(rangeActions, prePerimeterRangeActionResult, 1, null, null, null, null);
        linearProblem = new LinearProblem(List.of(coreProblemFiller, raUsageLimitsFiller), mpSolver);
        linearProblem.fill(flowResult, sensitivityResult);

        rangeActions.forEach(ra -> {
            MPVariable binary = linearProblem.getRangeActionVariationBinary(ra);
            MPConstraint constraintUp = linearProblem.getIsVariationInDirectionConstraint(ra, LinearProblem.VariationReferenceExtension.PREPERIMETER, LinearProblem.VariationDirectionExtension.UPWARD);
            MPConstraint constraintDown = linearProblem.getIsVariationInDirectionConstraint(ra, LinearProblem.VariationReferenceExtension.PREPERIMETER, LinearProblem.VariationDirectionExtension.DOWNWARD);

            assertNotNull(binary);
            assertNotNull(constraintUp);
            assertNotNull(constraintDown);

            MPVariable setpointVariable = linearProblem.getRangeActionSetpointVariable(ra);
            double initialSetpoint = prePerimeterRangeActionResult.getOptimizedSetPoint(ra);

            assertEquals(1, constraintUp.getCoefficient(setpointVariable), DOUBLE_TOLERANCE);
            assertEquals(-(ra.getMaxAdmissibleSetpoint(initialSetpoint) - initialSetpoint), constraintUp.getCoefficient(binary), DOUBLE_TOLERANCE);
            assertEquals(-LinearProblem.infinity(), constraintUp.lb(), DOUBLE_TOLERANCE);
            assertEquals(initialSetpoint, constraintUp.ub(), DOUBLE_TOLERANCE);

            assertEquals(1, constraintDown.getCoefficient(setpointVariable), DOUBLE_TOLERANCE);
            assertEquals(initialSetpoint - ra.getMinAdmissibleSetpoint(initialSetpoint), constraintDown.getCoefficient(binary), DOUBLE_TOLERANCE);
            assertEquals(initialSetpoint, constraintDown.lb(), DOUBLE_TOLERANCE);
            assertEquals(LinearProblem.infinity(), constraintDown.ub(), DOUBLE_TOLERANCE);
        });
    }

    @Test
    public void testSkipConstraints1() {
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(rangeActions, prePerimeterRangeActionResult, 5, null, null, null, null);
        linearProblem = new LinearProblem(List.of(coreProblemFiller, raUsageLimitsFiller), mpSolver);
        linearProblem.fill(flowResult, sensitivityResult);
        assertNull(linearProblem.getMaxTsoConstraint());
        assertNull(linearProblem.getMaxPstPerTsoConstraint("opA"));
        assertNull(linearProblem.getMaxPstPerTsoConstraint("opB"));
        assertNull(linearProblem.getMaxPstPerTsoConstraint("opC"));
        assertNull(linearProblem.getMaxRaPerTsoConstraint("opA"));
        assertNull(linearProblem.getMaxRaPerTsoConstraint("opB"));
        assertNull(linearProblem.getMaxRaPerTsoConstraint("opC"));
    }

    @Test
    public void testSkipConstraints2() {
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(rangeActions, prePerimeterRangeActionResult, null, 3, null, null, null);
        linearProblem = new LinearProblem(List.of(coreProblemFiller, raUsageLimitsFiller), mpSolver);
        linearProblem.fill(flowResult, sensitivityResult);
        assertNull(linearProblem.getMaxRaConstraint());
    }

    @Test
    public void testMaxRa() {
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(rangeActions, prePerimeterRangeActionResult, 5, null, null, null, null);
        linearProblem = new LinearProblem(List.of(coreProblemFiller, raUsageLimitsFiller), mpSolver);
        linearProblem.fill(flowResult, sensitivityResult);

        MPConstraint constraint = linearProblem.getMaxRaConstraint();
        assertNotNull(constraint);
        assertEquals(0, constraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(5, constraint.ub(), DOUBLE_TOLERANCE);
        rangeActions.forEach(ra -> assertEquals(1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(ra)), DOUBLE_TOLERANCE));
    }

    private void checkTsoToRaConstraint(String tso, RangeAction<?> ra) {
        MPConstraint constraint = linearProblem.getTsoRaUsedConstraint(tso, ra);
        assertNotNull(constraint);
        assertEquals(0, constraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), constraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable(tso)), DOUBLE_TOLERANCE);
        assertEquals(-1, constraint.getCoefficient(linearProblem.getRangeActionVariationBinary(ra)), DOUBLE_TOLERANCE);
    }

    @Test
    public void testMaxTso() {
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(rangeActions, prePerimeterRangeActionResult, null, 2, null, null, null);
        linearProblem = new LinearProblem(List.of(coreProblemFiller, raUsageLimitsFiller), mpSolver);
        linearProblem.fill(flowResult, sensitivityResult);

        MPConstraint constraint = linearProblem.getMaxTsoConstraint();
        assertNotNull(constraint);
        assertEquals(0, constraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(2, constraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable("opA")), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable("opB")), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable("opC")), DOUBLE_TOLERANCE);

        checkTsoToRaConstraint("opA", pst1);
        checkTsoToRaConstraint("opA", pst2);
        checkTsoToRaConstraint("opB", pst3);
        checkTsoToRaConstraint("opA", hvdc);
        checkTsoToRaConstraint("opC", injection);
    }

    @Test
    public void testMaxTsoWithExclusion() {
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(rangeActions, prePerimeterRangeActionResult, null, 1, Set.of("opC"), null, null);
        linearProblem = new LinearProblem(List.of(coreProblemFiller, raUsageLimitsFiller), mpSolver);
        linearProblem.fill(flowResult, sensitivityResult);

        MPConstraint constraint = linearProblem.getMaxTsoConstraint();
        assertNotNull(constraint);
        assertEquals(0, constraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable("opA")), DOUBLE_TOLERANCE);
        assertEquals(1, constraint.getCoefficient(linearProblem.getTsoRaUsedVariable("opB")), DOUBLE_TOLERANCE);
        assertNull(linearProblem.getTsoRaUsedVariable("opC"));
    }

    @Test
    public void testMaxRaPerTso() {
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(rangeActions, prePerimeterRangeActionResult, null, null, null, null, Map.of("opA", 2, "opC", 0));
        linearProblem = new LinearProblem(List.of(coreProblemFiller, raUsageLimitsFiller), mpSolver);
        linearProblem.fill(flowResult, sensitivityResult);

        MPConstraint constraintA = linearProblem.getMaxRaPerTsoConstraint("opA");
        assertNotNull(constraintA);
        assertEquals(0, constraintA.lb(), DOUBLE_TOLERANCE);
        assertEquals(2, constraintA.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1)), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst2)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst3)), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(hvdc)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(injection)), DOUBLE_TOLERANCE);

        MPConstraint constraintC = linearProblem.getMaxRaPerTsoConstraint("opC");
        assertNotNull(constraintC);
        assertEquals(0, constraintC.lb(), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.ub(), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst2)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst3)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(hvdc)), DOUBLE_TOLERANCE);
        assertEquals(1, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(injection)), DOUBLE_TOLERANCE);

        assertNull(linearProblem.getMaxPstPerTsoConstraint("opB"));
    }

    @Test
    public void testMaxPstPerTso() {
        RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(rangeActions, prePerimeterRangeActionResult, null, null, null, Map.of("opA", 1, "opC", 3), null);
        linearProblem = new LinearProblem(List.of(coreProblemFiller, raUsageLimitsFiller), mpSolver);
        linearProblem.fill(flowResult, sensitivityResult);

        MPConstraint constraintA = linearProblem.getMaxPstPerTsoConstraint("opA");
        assertNotNull(constraintA);
        assertEquals(0, constraintA.lb(), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1)), DOUBLE_TOLERANCE);
        assertEquals(1, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst2)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(pst3)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(hvdc)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintA.getCoefficient(linearProblem.getRangeActionVariationBinary(injection)), DOUBLE_TOLERANCE);

        MPConstraint constraintC = linearProblem.getMaxPstPerTsoConstraint("opC");
        assertNotNull(constraintC);
        assertEquals(0, constraintC.lb(), DOUBLE_TOLERANCE);
        assertEquals(3, constraintC.ub(), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst1)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst2)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(pst3)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(hvdc)), DOUBLE_TOLERANCE);
        assertEquals(0, constraintC.getCoefficient(linearProblem.getRangeActionVariationBinary(injection)), DOUBLE_TOLERANCE);

        assertNull(linearProblem.getMaxPstPerTsoConstraint("opB"));
    }
}
