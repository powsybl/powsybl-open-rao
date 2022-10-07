/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.MaxMinRelativeMarginParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.RangeActionParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.UnoptimizedCnecParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemBuilder;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionActivationResultImpl;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionSetpointResultImpl;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class UnoptimizedCnecFillerPstLimitationRuleTest extends AbstractFillerTest {
    private static final double MAX_ABS_THRESHOLD = 1000;

    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;
    private UnoptimizedCnecFiller unoptimizedCnecFiller;
    private FlowCnec cnecInSeries;
    private FlowCnec classicCnec;
    private PstRangeAction pstRangeActionInSeries;
    private OptimizationPerimeter optimizationPerimeter;
    private Map<FlowCnec, PstRangeAction> flowCnecPstRangeActionMap = new HashMap<>();
    private double constraintCoeff;

    @Before
    public void setUp() {
        init();

        // Add a cnec
        crac.newFlowCnec()
            .withId("cnecId")
            .withNetworkElement("neId")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(800.0).withMin(-1000.).withUnit(Unit.MEGAWATT).add()
            .withOptimized(true)
            .withInstant(Instant.PREVENTIVE)
            .withOperator("NL")
            .add();
        crac.newPstRangeAction().withId("pstRangeActionInSeries")
                .withName("pstRange1Name")
                .withOperator("RTE")
                .withNetworkElement("pst")
                .withInitialTap(1)
                .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
                .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
                .newFreeToUseUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
                .add();

        // Set initial margins on both preventive CNECs
        cnecInSeries = crac.getFlowCnec("cnecId");
        classicCnec = crac.getFlowCnec("Tieline BE FR - N - preventive");
        pstRangeActionInSeries = crac.getPstRangeAction("pstRangeActionInSeries");

        flowCnecPstRangeActionMap.put(cnecInSeries, pstRangeActionInSeries);

        RangeActionSetpointResult initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(pstRangeActionInSeries, 0.5));

        optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(classicCnec, cnecInSeries));
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(Map.of(crac.getPreventiveState(), Set.of(pstRangeActionInSeries)));

        RaoParameters raoParameters = new RaoParameters();
        raoParameters.setPstPenaltyCost(0.01);
        raoParameters.setHvdcPenaltyCost(0.01);
        raoParameters.setInjectionRaPenaltyCost(0.01);
        RangeActionParameters rangeActionParameters = RangeActionParameters.buildFromRaoParameters(raoParameters);

        coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            new RangeActionActivationResultImpl(initialRangeActionSetpointResult),
            rangeActionParameters,
            MEGAWATT);
    }

    private void buildLinearProblemWithMaxMinMarginAndPositiveSensitivityValue() {
        UnoptimizedCnecParameters unoptimizedCnecParameters = new UnoptimizedCnecParameters(null, flowCnecPstRangeActionMap);
        MaxMinMarginFiller maxMinMarginFiller = new MaxMinMarginFiller(Set.of(classicCnec, cnecInSeries), Unit.MEGAWATT);
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getMargin(classicCnec, Unit.MEGAWATT)).thenReturn(600.);
        when(initialFlowResult.getMargin(cnecInSeries, Unit.MEGAWATT)).thenReturn(400.);
        when(sensitivityResult.getSensitivityValue(cnecInSeries, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(5.);
        unoptimizedCnecFiller = new UnoptimizedCnecFiller(
                optimizationPerimeter,
                Set.of(classicCnec, cnecInSeries),
                initialFlowResult,
                unoptimizedCnecParameters
        );
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(maxMinMarginFiller)
            .withProblemFiller(unoptimizedCnecFiller)
            .withSolver(mpSolver)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void buildLinearProblemWithMaxMinMarginAndNegativeSensitivityValue() {
        UnoptimizedCnecParameters unoptimizedCnecParameters = new UnoptimizedCnecParameters(null, flowCnecPstRangeActionMap);
        MaxMinMarginFiller maxMinMarginFiller = new MaxMinMarginFiller(Set.of(classicCnec, cnecInSeries), Unit.MEGAWATT);
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getMargin(classicCnec, Unit.MEGAWATT)).thenReturn(400.);
        when(initialFlowResult.getMargin(cnecInSeries, Unit.MEGAWATT)).thenReturn(600.);
        when(sensitivityResult.getSensitivityValue(cnecInSeries, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(-4.);
        unoptimizedCnecFiller = new UnoptimizedCnecFiller(
                optimizationPerimeter,
                Set.of(classicCnec, cnecInSeries),
                initialFlowResult,
                unoptimizedCnecParameters
        );
        linearProblem = new LinearProblemBuilder()
                .withProblemFiller(coreProblemFiller)
                .withProblemFiller(maxMinMarginFiller)
                .withProblemFiller(unoptimizedCnecFiller)
                .withSolver(mpSolver)
                .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void buildLinearProblemWithMaxMinRelativeMarginAndPositiveSensi() {
        MaxMinRelativeMarginParameters maxMinRelativeMarginParameters = new MaxMinRelativeMarginParameters(0.01);
        UnoptimizedCnecParameters unoptimizedCnecParameters = new UnoptimizedCnecParameters(null, flowCnecPstRangeActionMap);
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getMargin(classicCnec, Unit.MEGAWATT)).thenReturn(400.);
        when(initialFlowResult.getMargin(cnecInSeries, Unit.MEGAWATT)).thenReturn(600.);
        when(initialFlowResult.getPtdfZonalSum(cnecInSeries)).thenReturn(0.5);
        when(initialFlowResult.getPtdfZonalSum(classicCnec)).thenReturn(2.6);
        when(sensitivityResult.getSensitivityValue(cnecInSeries, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(5.);
        MaxMinRelativeMarginFiller maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(
                Set.of(classicCnec, cnecInSeries),
                initialFlowResult,
                Unit.MEGAWATT,
                maxMinRelativeMarginParameters
        );
        double relMarginCoef = Math.max(initialFlowResult.getPtdfZonalSum(classicCnec), maxMinRelativeMarginParameters.getPtdfSumLowerBound());
        double unitConversionCoefficient = RaoUtil.getFlowUnitMultiplier(classicCnec, Side.LEFT, MEGAWATT, MEGAWATT);
        constraintCoeff =  5 * RaoUtil.getLargestCnecThreshold(Set.of(cnecInSeries, classicCnec)) / maxMinRelativeMarginParameters.getPtdfSumLowerBound() * unitConversionCoefficient * relMarginCoef;

        unoptimizedCnecFiller = new UnoptimizedCnecFiller(
                optimizationPerimeter,
                Set.of(cnecInSeries, classicCnec),
                initialFlowResult,
                unoptimizedCnecParameters
        );
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(maxMinRelativeMarginFiller)
            .withProblemFiller(unoptimizedCnecFiller)
            .withSolver(mpSolver)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void buildLinearProblemWithMaxMinRelativeMarginAndNegativeSensi() {
        MaxMinRelativeMarginParameters maxMinRelativeMarginParameters = new MaxMinRelativeMarginParameters(0.01);
        UnoptimizedCnecParameters unoptimizedCnecParameters = new UnoptimizedCnecParameters(null, flowCnecPstRangeActionMap);
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getMargin(classicCnec, Unit.MEGAWATT)).thenReturn(400.);
        when(initialFlowResult.getMargin(cnecInSeries, Unit.MEGAWATT)).thenReturn(600.);
        when(initialFlowResult.getPtdfZonalSum(cnecInSeries)).thenReturn(0.5);
        when(initialFlowResult.getPtdfZonalSum(classicCnec)).thenReturn(2.6);
        when(sensitivityResult.getSensitivityValue(cnecInSeries, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(-4.);
        MaxMinRelativeMarginFiller maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(
                Set.of(classicCnec, cnecInSeries),
                initialFlowResult,
                Unit.MEGAWATT,
                maxMinRelativeMarginParameters
        );
        double relMarginCoef = Math.max(initialFlowResult.getPtdfZonalSum(classicCnec), maxMinRelativeMarginParameters.getPtdfSumLowerBound());
        double unitConversionCoefficient = RaoUtil.getFlowUnitMultiplier(classicCnec, Side.LEFT, MEGAWATT, MEGAWATT);
        constraintCoeff =  5 * RaoUtil.getLargestCnecThreshold(Set.of(cnecInSeries, classicCnec)) / maxMinRelativeMarginParameters.getPtdfSumLowerBound() * unitConversionCoefficient * relMarginCoef;

        unoptimizedCnecFiller = new UnoptimizedCnecFiller(
                optimizationPerimeter,
                Set.of(cnecInSeries, classicCnec),
                initialFlowResult,
                unoptimizedCnecParameters
        );
        linearProblem = new LinearProblemBuilder()
                .withProblemFiller(coreProblemFiller)
                .withProblemFiller(maxMinRelativeMarginFiller)
                .withProblemFiller(unoptimizedCnecFiller)
                .withSolver(mpSolver)
                .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    @Test
    public void testCnecsNotToOptimizePositiveSensiBinaryVar() {
        buildLinearProblemWithMaxMinMarginAndPositiveSensitivityValue();

        // Verify existence of optimize_cnec binary variable
        assertNull(linearProblem.getOptimizeCnecBinaryVariable(classicCnec));
        MPVariable binaryVar = linearProblem.getOptimizeCnecBinaryVariable(cnecInSeries);
        assertNotNull(binaryVar);

        // Get variables
        MPVariable flowVar = linearProblem.getFlowVariable(cnecInSeries);
        MPVariable setPointVar = linearProblem.getRangeActionSetpointVariable(pstRangeActionInSeries, crac.getPreventiveState());

        // Verify existence of optimize_cnec definition constraints
        assertNull(linearProblem.getOptimizeCnecConstraint(classicCnec, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertNull(linearProblem.getOptimizeCnecConstraint(classicCnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD));

        MPConstraint optimizeCnecConstraintBelowThreshold = linearProblem.getOptimizeCnecConstraint(cnecInSeries, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(optimizeCnecConstraintBelowThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintBelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(5*0.5 - 1000, optimizeCnecConstraintBelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(1., optimizeCnecConstraintBelowThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(5., optimizeCnecConstraintBelowThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(2.5*5., optimizeCnecConstraintBelowThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);

        MPConstraint optimizeCnecConstraintAboveThreshold = linearProblem.getOptimizeCnecConstraint(cnecInSeries, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(optimizeCnecConstraintAboveThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintAboveThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-5*3 - 800, optimizeCnecConstraintAboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1., optimizeCnecConstraintAboveThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(-5., optimizeCnecConstraintAboveThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(2.5*5., optimizeCnecConstraintAboveThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);
    }

    @Test
    public void testCnecsNotToOptimizeNegativeSensiBinaryVar() {
        buildLinearProblemWithMaxMinMarginAndNegativeSensitivityValue();

        // Verify existence of optimize_cnec binary variable
        assertNull(linearProblem.getOptimizeCnecBinaryVariable(classicCnec));
        MPVariable binaryVar = linearProblem.getOptimizeCnecBinaryVariable(cnecInSeries);
        assertNotNull(binaryVar);

        // Get variables
        MPVariable flowVar = linearProblem.getFlowVariable(cnecInSeries);
        MPVariable setPointVar = linearProblem.getRangeActionSetpointVariable(pstRangeActionInSeries, crac.getPreventiveState());

        // Verify existence of optimize_cnec definition constraints
        assertNull(linearProblem.getOptimizeCnecConstraint(classicCnec, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertNull(linearProblem.getOptimizeCnecConstraint(classicCnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD));

        MPConstraint optimizeCnecConstraintBelowThreshold = linearProblem.getOptimizeCnecConstraint(cnecInSeries, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(optimizeCnecConstraintBelowThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintBelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-4*3 - 1000, optimizeCnecConstraintBelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(1., optimizeCnecConstraintBelowThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(-4., optimizeCnecConstraintBelowThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(-2.5*-4., optimizeCnecConstraintBelowThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);

        MPConstraint optimizeCnecConstraintAboveThreshold = linearProblem.getOptimizeCnecConstraint(cnecInSeries, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(optimizeCnecConstraintAboveThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintAboveThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(4*0.5 - 800, optimizeCnecConstraintAboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1., optimizeCnecConstraintAboveThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(4., optimizeCnecConstraintAboveThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(-2.5*-4, optimizeCnecConstraintAboveThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);
    }

    @Test
    public void testCnecsNotToOptimizePositiveSensiBinaryVarRelative() {
        buildLinearProblemWithMaxMinRelativeMarginAndPositiveSensi();

        // Verify existence of margin_decrease binary variable
        assertNull(linearProblem.getOptimizeCnecBinaryVariable(classicCnec));
        MPVariable binaryVar = linearProblem.getOptimizeCnecBinaryVariable(cnecInSeries);
        assertNotNull(binaryVar);

        // Get variables
        MPVariable flowVar = linearProblem.getFlowVariable(cnecInSeries);
        MPVariable setPointVar = linearProblem.getRangeActionSetpointVariable(pstRangeActionInSeries, crac.getPreventiveState());


        // Verify existence of margin_decrease definition constraints
        assertNull(linearProblem.getOptimizeCnecConstraint(classicCnec, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertNull(linearProblem.getOptimizeCnecConstraint(classicCnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD));

        MPConstraint optimizeCnecConstraintBelowThreshold = linearProblem.getOptimizeCnecConstraint(cnecInSeries, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(optimizeCnecConstraintBelowThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintBelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(5*0.5 - 1000, optimizeCnecConstraintBelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(1., optimizeCnecConstraintBelowThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(5., optimizeCnecConstraintBelowThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(2.5*5., optimizeCnecConstraintBelowThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);

        MPConstraint optimizeCnecConstraintAboveThreshold = linearProblem.getOptimizeCnecConstraint(cnecInSeries, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(optimizeCnecConstraintAboveThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintAboveThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-5*3 - 800, optimizeCnecConstraintAboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1., optimizeCnecConstraintAboveThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(-5., optimizeCnecConstraintAboveThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(2.5*5., optimizeCnecConstraintAboveThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);
    }

    @Test
    public void testCnecsNotToOptimizeNegativeSensiBinaryVarRelative() {
        buildLinearProblemWithMaxMinRelativeMarginAndNegativeSensi();

        // Verify existence of margin_decrease binary variable
        assertNull(linearProblem.getOptimizeCnecBinaryVariable(classicCnec));
        MPVariable binaryVar = linearProblem.getOptimizeCnecBinaryVariable(cnecInSeries);
        assertNotNull(binaryVar);

        // Get variables
        MPVariable flowVar = linearProblem.getFlowVariable(cnecInSeries);
        MPVariable setPointVar = linearProblem.getRangeActionSetpointVariable(pstRangeActionInSeries, crac.getPreventiveState());


        // Verify existence of margin_decrease definition constraints
        assertNull(linearProblem.getOptimizeCnecConstraint(classicCnec, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertNull(linearProblem.getOptimizeCnecConstraint(classicCnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD));

        MPConstraint optimizeCnecConstraintBelowThreshold = linearProblem.getOptimizeCnecConstraint(cnecInSeries, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(optimizeCnecConstraintBelowThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintBelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-4*3 - 1000, optimizeCnecConstraintBelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(1., optimizeCnecConstraintBelowThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(-4., optimizeCnecConstraintBelowThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(-2.5*-4., optimizeCnecConstraintBelowThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);

        MPConstraint optimizeCnecConstraintAboveThreshold = linearProblem.getOptimizeCnecConstraint(cnecInSeries, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(optimizeCnecConstraintAboveThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintAboveThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(4*0.5 - 800, optimizeCnecConstraintAboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1., optimizeCnecConstraintAboveThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(4., optimizeCnecConstraintAboveThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(-2.5*-4, optimizeCnecConstraintAboveThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);
    }

    @Test
    public void testExcludeCnecsNotToOptimizeInMinMargin() {
        buildLinearProblemWithMaxMinMarginAndPositiveSensitivityValue();

        // Test that classicCnec's constraint does not have a bigM
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(classicCnec, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(classicCnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);

        // Test that cnecInSeries's constraint does have a bigM
        MPVariable optimizeCnecBinaryVariable = linearProblem.getOptimizeCnecBinaryVariable(cnecInSeries);
        MPConstraint minMarginDefMin = linearProblem.getMinimumMarginConstraint(cnecInSeries, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertEquals(1000 + 2 * MAX_ABS_THRESHOLD, minMarginDefMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMin.getCoefficient(optimizeCnecBinaryVariable), DOUBLE_TOLERANCE);
        MPConstraint minMarginDefMax = linearProblem.getMinimumMarginConstraint(cnecInSeries, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertEquals(800 + 2 * MAX_ABS_THRESHOLD, minMarginDefMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMax.getCoefficient(optimizeCnecBinaryVariable), DOUBLE_TOLERANCE);
    }

    @Test
    public void testExcludeCnecsNotToOptimizeInMinMarginRelative() {
        buildLinearProblemWithMaxMinRelativeMarginAndPositiveSensi();

        // Test that classicCnec's constraint does not have a bigM
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(classicCnec, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(classicCnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        // All cnecs now have fmax + maxNegativeRelativeRam * unitConversionCoefficient * relMarginCoef OR - fmin + maxNegativeRelativeRam * unitConversionCoefficient * relMarginCoef as mMRM ub
        assertEquals(750.0 + constraintCoeff, linearProblem.getMinimumRelativeMarginConstraint(classicCnec, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0 + constraintCoeff, linearProblem.getMinimumRelativeMarginConstraint(classicCnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);

        // Test that cnecInSeries's constraint does have a bigM
        MPVariable marginDecreaseVariable = linearProblem.getOptimizeCnecBinaryVariable(cnecInSeries);
        MPConstraint minMarginDefMin = linearProblem.getMinimumMarginConstraint(cnecInSeries, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertEquals(1000 + 2 * MAX_ABS_THRESHOLD, minMarginDefMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMin.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
        MPConstraint minMarginDefMax = linearProblem.getMinimumMarginConstraint(cnecInSeries, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertEquals(800 + 2 * MAX_ABS_THRESHOLD, minMarginDefMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMax.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
    }
}
