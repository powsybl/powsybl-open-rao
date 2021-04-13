/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *//*


package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.rao_api.RaoParameters.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

*/
/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 *//*

@RunWith(PowerMockRunner.class)
public class UnoptimizedCnecFillerTest extends AbstractFillerTest {

    private MaxMinMarginFiller maxMinMarginFiller;
    private MaxMinRelativeMarginFiller maxMinRelativeMarginFiller;
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

        coreProblemFiller = new CoreProblemFiller(linearProblem, network, Set.of(cnecNl, cnecFr), Collections.emptyMap());

        coreProblemFiller.fill(sensitivityAndLoopflowResults);
    }

    @Test
    public void testGetLargestCnecThreshold1() {
        assertEquals(1000, UnoptimizedCnecFiller.getLargestCnecThreshold(Set.of(cnecFr, cnecNl)), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetLargestCnecThreshold2() {
        crac.newBranchCnec().setId("Pure MNEC")
                .newNetworkElement().setId("DDE2AA1  NNL3AA1  1").add()
                .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(3000.0).setMin(-3000.).setUnit(Unit.MEGAWATT).add()
                .monitored()
                .setInstant(Instant.PREVENTIVE)
                .add();
        crac.newBranchCnec().setId("CNEC MNEC")
                .newNetworkElement().setId("DDE2AA1  NNL3AA1  1").add()
                .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(2000.0).setMin(-2500.).setUnit(Unit.MEGAWATT).add()
                .monitored()
                .optimized()
                .setInstant(Instant.PREVENTIVE)
                .add();
        assertEquals(2500,
            UnoptimizedCnecFiller.getLargestCnecThreshold(crac.getBranchCnecs().stream().filter(Cnec::isOptimized).collect(Collectors.toSet())),
            DOUBLE_TOLERANCE);
    }

    @Test
    public void testCnecsNotToOptimizeBinaryVar() {
        maxMinMarginFiller = new MaxMinMarginFiller(linearProblem, Set.of(cnecNl, cnecFr), Set.of(rangeAction), MEGAWATT, DEFAULT_PST_PENALTY_COST);
        unoptimizedCnecFiller = new UnoptimizedCnecFiller(linearProblem, Map.of(cnecNl, 400.), Set.of(cnecFr, cnecNl));
        maxMinMarginFiller.fill(sensitivityAndLoopflowResults);
        unoptimizedCnecFiller.fill(sensitivityAndLoopflowResults);

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
        assertEquals(linearProblem.infinity(), marginDecreaseConstraintMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1000 + (800 - 400), marginDecreaseConstraintMin.lb(), DOUBLE_TOLERANCE);
        assertEquals(1.0, marginDecreaseConstraintMin.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000, marginDecreaseConstraintMin.getCoefficient(binaryVar), DOUBLE_TOLERANCE); // 1000 being the largest cnec threshold

        MPConstraint marginDecreaseConstraintMax = linearProblem.getMarginDecreaseConstraint(cnecNl, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(marginDecreaseConstraintMax);
        assertEquals(linearProblem.infinity(), marginDecreaseConstraintMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(-800 + (800 - 400), marginDecreaseConstraintMax.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1.0, marginDecreaseConstraintMax.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000, marginDecreaseConstraintMax.getCoefficient(binaryVar), DOUBLE_TOLERANCE); // 1000 being the largest cnec threshold
    }

    @Test
    public void testExcludeCnecsNotToOptimizeInMinMargin() {
        maxMinMarginFiller = new MaxMinMarginFiller(linearProblem, Set.of(cnecNl, cnecFr), Set.of(rangeAction), MEGAWATT, DEFAULT_PST_PENALTY_COST);
        unoptimizedCnecFiller = new UnoptimizedCnecFiller(linearProblem, Map.of(cnecNl, 400.), Set.of(cnecFr, cnecNl));
        maxMinMarginFiller.fill(sensitivityAndLoopflowResults);
        unoptimizedCnecFiller.fill(sensitivityAndLoopflowResults);

        double maxAbsThreshold = 1000;

        // Test that cnecFr's constraint does not have a bigM
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);

        // Test that cnecNl's constraint does have a bigM
        MPVariable marginDecreaseVariable = linearProblem.getMarginDecreaseBinaryVariable(cnecNl);
        MPConstraint minMarginDefMin = linearProblem.getMinimumMarginConstraint(cnecNl, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertEquals(1000 + 2 * maxAbsThreshold, minMarginDefMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * maxAbsThreshold, minMarginDefMin.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
        MPConstraint minMarginDefMax = linearProblem.getMinimumMarginConstraint(cnecNl, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertEquals(800 + 2 * maxAbsThreshold, minMarginDefMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * maxAbsThreshold, minMarginDefMax.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
    }

    @Test
    public void testCnecsNotToOptimizeBinaryVarRelative() {
        maxMinMarginFiller = new MaxMinMarginFiller(linearProblem, Set.of(cnecNl, cnecFr), Set.of(rangeAction), MEGAWATT, DEFAULT_PST_PENALTY_COST);
        unoptimizedCnecFiller = new UnoptimizedCnecFiller(linearProblem, Map.of(cnecNl, 400.), Set.of(cnecFr, cnecNl));
        maxMinMarginFiller.fill(sensitivityAndLoopflowResults);
        unoptimizedCnecFiller.fill(sensitivityAndLoopflowResults);

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
        assertEquals(linearProblem.infinity(), marginDecreaseConstraintMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1000 + (800 - 400), marginDecreaseConstraintMin.lb(), DOUBLE_TOLERANCE);
        assertEquals(1.0, marginDecreaseConstraintMin.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000, marginDecreaseConstraintMin.getCoefficient(binaryVar), DOUBLE_TOLERANCE); // 1000 being the largest cnec threshold

        MPConstraint marginDecreaseConstraintMax = linearProblem.getMarginDecreaseConstraint(cnecNl, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(marginDecreaseConstraintMax);
        assertEquals(linearProblem.infinity(), marginDecreaseConstraintMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(-800 + (800 - 400), marginDecreaseConstraintMax.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1.0, marginDecreaseConstraintMax.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000, marginDecreaseConstraintMax.getCoefficient(binaryVar), DOUBLE_TOLERANCE); // 1000 being the largest cnec threshold
    }

    @Test
    public void testExcludeCnecsNotToOptimizeInMinMarginRelative() {
        maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(
            linearProblem,
            Map.of(cnecFr, 600., cnecNl, 400.),
            Collections.emptySet(),
            MEGAWATT,
            DEFAULT_PST_PENALTY_COST,
            DEFAULT_NEGATIVE_MARGIN_OBJECTIVE_COEFFICIENT,
            DEFAULT_PTDF_SUM_LOWER_BOUND);

        unoptimizedCnecFiller = new UnoptimizedCnecFiller(linearProblem, Map.of(cnecNl, 400.), Set.of(cnecFr, cnecNl));
        maxMinRelativeMarginFiller.fill(sensitivityAndLoopflowResults);
        unoptimizedCnecFiller.fill(sensitivityAndLoopflowResults);

        double maxAbsThreshold = 1000;

        // Test that cnecFr's constraint does not have a bigM
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumRelativeMarginConstraint(cnecFr, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumRelativeMarginConstraint(cnecFr, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);

        // Test that cnecNl's constraint does have a bigM
        MPVariable marginDecreaseVariable = linearProblem.getMarginDecreaseBinaryVariable(cnecNl);
        MPConstraint minMarginDefMin = linearProblem.getMinimumMarginConstraint(cnecNl, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertEquals(1000 + 2 * maxAbsThreshold, minMarginDefMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * maxAbsThreshold, minMarginDefMin.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
        MPConstraint minMarginDefMax = linearProblem.getMinimumMarginConstraint(cnecNl, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertEquals(800 + 2 * maxAbsThreshold, minMarginDefMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * maxAbsThreshold, minMarginDefMax.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
    }
}
*/
