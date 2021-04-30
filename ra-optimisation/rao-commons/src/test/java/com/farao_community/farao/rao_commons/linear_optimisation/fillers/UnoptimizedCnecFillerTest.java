/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_api.parameters.MaxMinMarginParameters;
import com.farao_community.farao.rao_api.parameters.MaxMinRelativeMarginParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.UnoptimizedCnecParameters;
import com.farao_community.farao.rao_commons.result.BranchResultImpl;
import com.farao_community.farao.rao_commons.result.RangeActionResultImpl;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class UnoptimizedCnecFillerTest extends AbstractFillerTest {
    private static final double MAX_ABS_THRESHOLD = 1000;

    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;
    private UnoptimizedCnecFiller unoptimizedCnecFiller;
    BranchCnec cnecNl;
    BranchCnec cnecFr;

    @Before
    public void setUp() {
        init();

        // Add a cnec
        crac.newBranchCnec().setId("Line NL - N - preventive")
                .newNetworkElement().setId("NNL1AA1  NNL2AA1  1").add()
                .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(800.0).setMin(-1000.).setUnit(Unit.MEGAWATT).add()
                .optimized()
                .setInstant(Instant.PREVENTIVE)
                .setOperator("NL")
                .add();
        // Set initial margins on both preventive CNECs
        cnecNl = crac.getBranchCnec("Line NL - N - preventive");
        cnecFr = crac.getBranchCnec("Tieline BE FR - N - preventive");

        coreProblemFiller = new CoreProblemFiller(
                network,
                Set.of(cnecNl, cnecFr),
                Collections.emptySet(),
                new RangeActionResultImpl(Collections.emptyMap()),
                0.
        );
    }

    private void buildLinearProblemWithMaxMinMargin() {
        MaxMinMarginParameters maxMinMarginParameters = new MaxMinMarginParameters(0.01);
        UnoptimizedCnecParameters unoptimizedCnecParameters = new UnoptimizedCnecParameters(Set.of("NL"), MAX_ABS_THRESHOLD);
        MaxMinMarginFiller maxMinMarginFiller = new MaxMinMarginFiller(
                Set.of(cnecNl, cnecFr),
                Set.of(rangeAction),
                Unit.MEGAWATT,
                maxMinMarginParameters
        );
        BranchResult initialBranchResult = Mockito.mock(BranchResult.class);
        when(initialBranchResult.getMargin(cnecNl, Unit.MEGAWATT)).thenReturn(400.);
        when(initialBranchResult.getMargin(cnecFr, Unit.MEGAWATT)).thenReturn(600.);
        unoptimizedCnecFiller = new UnoptimizedCnecFiller(
                Set.of(cnecNl, cnecFr),
                initialBranchResult,
                unoptimizedCnecParameters
        );
        linearProblem = new LinearProblem(List.of(coreProblemFiller, maxMinMarginFiller, unoptimizedCnecFiller), mpSolver);
        linearProblem.fill(branchResult, sensitivityResult);
    }

    private void buildLinearProblemWithMaxMinRelativeMargin() {
        MaxMinRelativeMarginParameters maxMinRelativeMarginParameters = new MaxMinRelativeMarginParameters(
                0.01, 1000, 0.01);
        UnoptimizedCnecParameters unoptimizedCnecParameters = new UnoptimizedCnecParameters(Set.of("NL"), MAX_ABS_THRESHOLD);
        BranchResult initialBranchResult = Mockito.mock(BranchResult.class);
        when(initialBranchResult.getMargin(cnecNl, Unit.MEGAWATT)).thenReturn(400.);
        when(initialBranchResult.getMargin(cnecFr, Unit.MEGAWATT)).thenReturn(600.);
        when(initialBranchResult.getPtdfZonalSum(cnecNl)).thenReturn(0.5);
        when(initialBranchResult.getPtdfZonalSum(cnecFr)).thenReturn(2.6);
        MaxMinRelativeMarginFiller maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(
                Set.of(cnecNl, cnecFr),
                initialBranchResult,
                Set.of(rangeAction),
                Unit.MEGAWATT,
                maxMinRelativeMarginParameters
        );
        unoptimizedCnecFiller = new UnoptimizedCnecFiller(
                Set.of(cnecNl, cnecFr),
                initialBranchResult,
                unoptimizedCnecParameters
        );
        linearProblem = new LinearProblem(List.of(coreProblemFiller, maxMinRelativeMarginFiller, unoptimizedCnecFiller), mpSolver);
        linearProblem.fill(branchResult, sensitivityResult);
    }

    @Test
    public void testCnecsNotToOptimizeBinaryVar() {
        buildLinearProblemWithMaxMinMargin();

        // Verify existence of margin_decrease binary variable
        assertNull(linearProblem.getMarginDecreaseBinaryVariable(cnecFr));
        MPVariable binaryVar = linearProblem.getMarginDecreaseBinaryVariable(cnecNl);
        assertNotNull(binaryVar);

        // Get flow variable
        MPVariable flowVar = linearProblem.getFlowVariable(cnecNl);

        // Verify existence of margin_decrease definition constraints
        assertNull(linearProblem.getMarginDecreaseConstraint(cnecFr, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertNull(linearProblem.getMarginDecreaseConstraint(cnecFr, LinearProblem.MarginExtension.ABOVE_THRESHOLD));

        MPConstraint marginDecreaseConstraintMin = linearProblem.getMarginDecreaseConstraint(cnecNl, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(marginDecreaseConstraintMin);
        assertEquals(LinearProblem.infinity(), marginDecreaseConstraintMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1000 + (800 - 400), marginDecreaseConstraintMin.lb(), DOUBLE_TOLERANCE);
        assertEquals(1.0, marginDecreaseConstraintMin.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000, marginDecreaseConstraintMin.getCoefficient(binaryVar), DOUBLE_TOLERANCE); // 1000 being the largest cnec threshold

        MPConstraint marginDecreaseConstraintMax = linearProblem.getMarginDecreaseConstraint(cnecNl, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(marginDecreaseConstraintMax);
        assertEquals(LinearProblem.infinity(), marginDecreaseConstraintMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(-800 + (800 - 400), marginDecreaseConstraintMax.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1.0, marginDecreaseConstraintMax.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000, marginDecreaseConstraintMax.getCoefficient(binaryVar), DOUBLE_TOLERANCE); // 1000 being the largest cnec threshold
    }

    @Test
    public void testExcludeCnecsNotToOptimizeInMinMargin() {
        buildLinearProblemWithMaxMinMargin();

        // Test that cnecFr's constraint does not have a bigM
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);

        // Test that cnecNl's constraint does have a bigM
        MPVariable marginDecreaseVariable = linearProblem.getMarginDecreaseBinaryVariable(cnecNl);
        MPConstraint minMarginDefMin = linearProblem.getMinimumMarginConstraint(cnecNl, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertEquals(1000 + 2 * MAX_ABS_THRESHOLD, minMarginDefMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMin.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
        MPConstraint minMarginDefMax = linearProblem.getMinimumMarginConstraint(cnecNl, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertEquals(800 + 2 * MAX_ABS_THRESHOLD, minMarginDefMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMax.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
    }

    @Test
    public void testCnecsNotToOptimizeBinaryVarRelative() {
        buildLinearProblemWithMaxMinRelativeMargin();

        // Verify existence of margin_decrease binary variable
        assertNull(linearProblem.getMarginDecreaseBinaryVariable(cnecFr));
        MPVariable binaryVar = linearProblem.getMarginDecreaseBinaryVariable(cnecNl);
        assertNotNull(binaryVar);

        // Get flow variable
        MPVariable flowVar = linearProblem.getFlowVariable(cnecNl);

        // Verify existence of margin_decrease definition constraints
        assertNull(linearProblem.getMarginDecreaseConstraint(cnecFr, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertNull(linearProblem.getMarginDecreaseConstraint(cnecFr, LinearProblem.MarginExtension.ABOVE_THRESHOLD));

        MPConstraint marginDecreaseConstraintMin = linearProblem.getMarginDecreaseConstraint(cnecNl, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(marginDecreaseConstraintMin);
        assertEquals(LinearProblem.infinity(), marginDecreaseConstraintMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1000 + (800 - 400), marginDecreaseConstraintMin.lb(), DOUBLE_TOLERANCE);
        assertEquals(1.0, marginDecreaseConstraintMin.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000, marginDecreaseConstraintMin.getCoefficient(binaryVar), DOUBLE_TOLERANCE); // 1000 being the largest cnec threshold

        MPConstraint marginDecreaseConstraintMax = linearProblem.getMarginDecreaseConstraint(cnecNl, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(marginDecreaseConstraintMax);
        assertEquals(LinearProblem.infinity(), marginDecreaseConstraintMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(-800 + (800 - 400), marginDecreaseConstraintMax.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1.0, marginDecreaseConstraintMax.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000, marginDecreaseConstraintMax.getCoefficient(binaryVar), DOUBLE_TOLERANCE); // 1000 being the largest cnec threshold
    }

    @Test
    public void testExcludeCnecsNotToOptimizeInMinMarginRelative() {
        buildLinearProblemWithMaxMinRelativeMargin();

        // Test that cnecFr's constraint does not have a bigM
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumRelativeMarginConstraint(cnecFr, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumRelativeMarginConstraint(cnecFr, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);

        // Test that cnecNl's constraint does have a bigM
        MPVariable marginDecreaseVariable = linearProblem.getMarginDecreaseBinaryVariable(cnecNl);
        MPConstraint minMarginDefMin = linearProblem.getMinimumMarginConstraint(cnecNl, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertEquals(1000 + 2 * MAX_ABS_THRESHOLD, minMarginDefMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMin.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
        MPConstraint minMarginDefMax = linearProblem.getMinimumMarginConstraint(cnecNl, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertEquals(800 + 2 * MAX_ABS_THRESHOLD, minMarginDefMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMax.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
    }
}
