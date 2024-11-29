/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class UnoptimizedCnecFillerMarginDecreaseRuleTest extends AbstractFillerTest {
    private static final double MAX_ABS_THRESHOLD = 1000;

    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;
    private UnoptimizedCnecFiller unoptimizedCnecFiller;
    private FlowCnec cnecNl;
    private FlowCnec cnecFr;
    private double constraintCoeff;

    @BeforeEach
    public void setUp() throws IOException {
        init();

        // Add a cnec
        cnecNl = crac.newFlowCnec()
                .withId("Line NL - N - preventive")
                .withNetworkElement("NNL1AA1  NNL2AA1  1")
                .newThreshold().withSide(TwoSides.TWO).withMax(800.0).withMin(-1000.).withUnit(Unit.MEGAWATT).add()
                .withOptimized(true)
                .withInstant(PREVENTIVE_INSTANT_ID)
                .withOperator("NL")
                .add();

        // Set initial margins on both preventive CNECs
        cnecFr = crac.getFlowCnec("Tieline BE FR - N - preventive");

        RangeActionSetpointResult initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(Collections.emptyMap());

        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(cnecNl, cnecFr));

        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        rangeActions.put(cnec1.getState(), Collections.emptySet());
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);

        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getRangeActionsOptimizationParameters().setPstRAMinImpactThreshold(0.01);
        raoParameters.getRangeActionsOptimizationParameters().setHvdcRAMinImpactThreshold(0.01);
        raoParameters.getRangeActionsOptimizationParameters().setInjectionRAMinImpactThreshold(0.01);

        coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            raoParameters.getRangeActionsOptimizationParameters(),
            null,
            MEGAWATT,
            false, RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
    }

    private void buildLinearProblemWithMaxMinMargin() {
        buildLinearProblemWithMaxMinMargin(false);
    }

    private void buildLinearProblemWithMaxMinMargin(boolean initialFlowsAreNan) {
        UnoptimizedCnecParameters unoptimizedCnecParameters = new UnoptimizedCnecParameters(Set.of("NL"));
        MaxMinMarginFiller maxMinMarginFiller = new MaxMinMarginFiller(Set.of(cnecNl, cnecFr), Unit.MEGAWATT);
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getMargin(cnecNl, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(400.);
        when(initialFlowResult.getMargin(cnecFr, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(600.);
        if (initialFlowsAreNan) {
            when(initialFlowResult.getFlow(cnecNl, TwoSides.TWO, MEGAWATT)).thenReturn(Double.NaN);
            when(initialFlowResult.getFlow(cnecFr, TwoSides.ONE, MEGAWATT)).thenReturn(Double.NaN);
        }
        unoptimizedCnecFiller = new UnoptimizedCnecFiller(
                Set.of(cnecNl, cnecFr),
                initialFlowResult,
                unoptimizedCnecParameters
        );
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(maxMinMarginFiller)
            .withProblemFiller(unoptimizedCnecFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void buildLinearProblemWithMaxMinRelativeMargin() {
        RelativeMarginsParameters maxMinRelativeMarginParameters = new RelativeMarginsParameters();

        UnoptimizedCnecParameters unoptimizedCnecParameters = new UnoptimizedCnecParameters(Set.of("NL"));
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getMargin(cnecNl, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(400.);
        when(initialFlowResult.getMargin(cnecFr, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(600.);
        when(initialFlowResult.getPtdfZonalSum(cnecNl, TwoSides.TWO)).thenReturn(0.5);
        when(initialFlowResult.getPtdfZonalSum(cnecFr, TwoSides.ONE)).thenReturn(2.6);
        MaxMinRelativeMarginFiller maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(
                Set.of(cnecNl, cnecFr),
                initialFlowResult,
                Unit.MEGAWATT,
                maxMinRelativeMarginParameters
        );
        double relMarginCoef = Math.max(initialFlowResult.getPtdfZonalSum(cnecFr, TwoSides.ONE), maxMinRelativeMarginParameters.getPtdfSumLowerBound());
        double unitConversionCoefficient = RaoUtil.getFlowUnitMultiplier(cnecFr, TwoSides.ONE, MEGAWATT, MEGAWATT);
        constraintCoeff = 5 * RaoUtil.getLargestCnecThreshold(Set.of(cnecNl, cnecFr), MEGAWATT) / maxMinRelativeMarginParameters.getPtdfSumLowerBound() * unitConversionCoefficient * relMarginCoef;

        unoptimizedCnecFiller = new UnoptimizedCnecFiller(
                Set.of(cnecNl, cnecFr),
                initialFlowResult,
                unoptimizedCnecParameters
        );
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(maxMinRelativeMarginFiller)
            .withProblemFiller(unoptimizedCnecFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    @Test
    void testCnecsNotToOptimizeBinaryVar() {
        buildLinearProblemWithMaxMinMargin();

        // Verify existence of margin_decrease binary variable
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getOptimizeCnecBinaryVariable(cnecFr, TwoSides.ONE));
        assertEquals("Variable Tieline BE FR - N - preventive_one_optimizecnec_variable has not been created yet", e.getMessage());
        OpenRaoMPVariable binaryVar = linearProblem.getOptimizeCnecBinaryVariable(cnecNl, TwoSides.TWO);
        assertNotNull(binaryVar);

        // Get flow variable
        OpenRaoMPVariable flowVar = linearProblem.getFlowVariable(cnecNl, TwoSides.TWO);

        // Verify existence of margin_decrease definition constraints
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getDontOptimizeCnecConstraint(cnecFr, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertEquals("Constraint Tieline BE FR - N - preventive_one_optimizecnecbelow_threshold_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getDontOptimizeCnecConstraint(cnecFr, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD));
        assertEquals("Constraint Tieline BE FR - N - preventive_one_optimizecnecabove_threshold_constraint has not been created yet", e.getMessage());

        OpenRaoMPConstraint marginDecreaseConstraintMin = linearProblem.getDontOptimizeCnecConstraint(cnecNl, TwoSides.TWO, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(marginDecreaseConstraintMin);
        assertEquals(linearProblem.infinity(), marginDecreaseConstraintMin.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(-1000 + (800 - 400), marginDecreaseConstraintMin.lb(), DOUBLE_TOLERANCE);
        assertEquals(1.0, marginDecreaseConstraintMin.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000, marginDecreaseConstraintMin.getCoefficient(binaryVar), DOUBLE_TOLERANCE); // 1000 being the largest cnec threshold

        OpenRaoMPConstraint marginDecreaseConstraintMax = linearProblem.getDontOptimizeCnecConstraint(cnecNl, TwoSides.TWO, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(marginDecreaseConstraintMax);
        assertEquals(linearProblem.infinity(), marginDecreaseConstraintMax.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(-800 + (800 - 400), marginDecreaseConstraintMax.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1.0, marginDecreaseConstraintMax.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000, marginDecreaseConstraintMax.getCoefficient(binaryVar), DOUBLE_TOLERANCE); // 1000 being the largest cnec threshold
    }

    @Test
    void testExcludeCnecsNotToOptimizeInMinMargin() {
        buildLinearProblemWithMaxMinMargin();

        // Test that cnecFr's constraint does not have a bigM
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);

        // Test that cnecNl's constraint does have a bigM
        OpenRaoMPVariable marginDecreaseVariable = linearProblem.getOptimizeCnecBinaryVariable(cnecNl, TwoSides.TWO);
        OpenRaoMPConstraint minMarginDefMin = linearProblem.getMinimumMarginConstraint(cnecNl, TwoSides.TWO, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertEquals(1000 + 2 * MAX_ABS_THRESHOLD, minMarginDefMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMin.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
        OpenRaoMPConstraint minMarginDefMax = linearProblem.getMinimumMarginConstraint(cnecNl, TwoSides.TWO, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertEquals(800 + 2 * MAX_ABS_THRESHOLD, minMarginDefMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMax.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
    }

    @Test
    void testCnecsNotToOptimizeBinaryVarRelative() {
        buildLinearProblemWithMaxMinRelativeMargin();

        // Verify existence of margin_decrease binary variable
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getOptimizeCnecBinaryVariable(cnecFr, TwoSides.ONE));
        assertEquals("Variable Tieline BE FR - N - preventive_one_optimizecnec_variable has not been created yet", e.getMessage());
        OpenRaoMPVariable binaryVar = linearProblem.getOptimizeCnecBinaryVariable(cnecNl, TwoSides.TWO);
        assertNotNull(binaryVar);

        // Get flow variable
        OpenRaoMPVariable flowVar = linearProblem.getFlowVariable(cnecNl, TwoSides.TWO);

        // Verify existence of margin_decrease definition constraints
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getDontOptimizeCnecConstraint(cnecFr, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertEquals("Constraint Tieline BE FR - N - preventive_one_optimizecnecbelow_threshold_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getDontOptimizeCnecConstraint(cnecFr, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD));
        assertEquals("Constraint Tieline BE FR - N - preventive_one_optimizecnecabove_threshold_constraint has not been created yet", e.getMessage());

        OpenRaoMPConstraint marginDecreaseConstraintMin = linearProblem.getDontOptimizeCnecConstraint(cnecNl, TwoSides.TWO, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(marginDecreaseConstraintMin);
        assertEquals(linearProblem.infinity(), marginDecreaseConstraintMin.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(-1000 + (800 - 400), marginDecreaseConstraintMin.lb(), DOUBLE_TOLERANCE);
        assertEquals(1.0, marginDecreaseConstraintMin.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000, marginDecreaseConstraintMin.getCoefficient(binaryVar), DOUBLE_TOLERANCE); // 1000 being the largest cnec threshold

        OpenRaoMPConstraint marginDecreaseConstraintMax = linearProblem.getDontOptimizeCnecConstraint(cnecNl, TwoSides.TWO, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(marginDecreaseConstraintMax);
        assertEquals(linearProblem.infinity(), marginDecreaseConstraintMax.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(-800 + (800 - 400), marginDecreaseConstraintMax.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1.0, marginDecreaseConstraintMax.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000, marginDecreaseConstraintMax.getCoefficient(binaryVar), DOUBLE_TOLERANCE); // 1000 being the largest cnec threshold
    }

    @Test
    void testExcludeCnecsNotToOptimizeInMinMarginRelative() {
        buildLinearProblemWithMaxMinRelativeMargin();

        // Test that cnecFr's constraint does not have a bigM
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        // All cnecs now have fmax + maxNegativeRelativeRam * unitConversionCoefficient * relMarginCoef OR - fmin + maxNegativeRelativeRam * unitConversionCoefficient * relMarginCoef as mMRM ub
        assertEquals(750.0 + constraintCoeff, linearProblem.getMinimumRelativeMarginConstraint(cnecFr, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0 + constraintCoeff, linearProblem.getMinimumRelativeMarginConstraint(cnecFr, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);

        // Test that cnecNl's constraint does have a bigM
        OpenRaoMPVariable marginDecreaseVariable = linearProblem.getOptimizeCnecBinaryVariable(cnecNl, TwoSides.TWO);
        OpenRaoMPConstraint minMarginDefMin = linearProblem.getMinimumMarginConstraint(cnecNl, TwoSides.TWO, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertEquals(1000 + 2 * MAX_ABS_THRESHOLD, minMarginDefMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMin.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
        OpenRaoMPConstraint minMarginDefMax = linearProblem.getMinimumMarginConstraint(cnecNl, TwoSides.TWO, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertEquals(800 + 2 * MAX_ABS_THRESHOLD, minMarginDefMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMax.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
    }

    @Test
    void testFilterCnecWithInitialNanFlow() {
        buildLinearProblemWithMaxMinMargin(true);

        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getOptimizeCnecBinaryVariable(cnecFr, TwoSides.TWO));
        assertEquals("Variable Tieline BE FR - N - preventive_two_optimizecnec_variable has not been created yet", e.getMessage());

        e = assertThrows(OpenRaoException.class, () -> linearProblem.getOptimizeCnecBinaryVariable(cnecNl, TwoSides.ONE));
        assertEquals("Variable Line NL - N - preventive_one_optimizecnec_variable has not been created yet", e.getMessage());
    }
}
