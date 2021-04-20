/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.rao_api.RaoParameters.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class OperatorsNotToOptimizeFillerTest extends AbstractFillerTest {

    private MaxMinMarginFiller maxMinMarginFiller;
    private MaxMinRelativeMarginFiller maxMinRelativeMarginFiller;
    private OperatorsNotToOptimizeFiller operatorsNotToOptimizeFiller;
    private FlowCnec cnecNl;
    private FlowCnec cnecFr;

    @Before
    public void setUp() {
        init();
        coreProblemFiller = new CoreProblemFiller();
        maxMinMarginFiller = new MaxMinMarginFiller(MEGAWATT, DEFAULT_PST_PENALTY_COST);
        maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(MEGAWATT, DEFAULT_PST_PENALTY_COST, DEFAULT_NEGATIVE_MARGIN_OBJECTIVE_COEFFICIENT, DEFAULT_PTDF_SUM_LOWER_BOUND);
        operatorsNotToOptimizeFiller = new OperatorsNotToOptimizeFiller(null);
    }

    private void fillProblemWithCoreFiller() {
        // arrange some additional data
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        raoData.getCracResultManager().applyRangeActionResultsOnNetwork();

        // fill the problem : the core filler is required
        coreProblemFiller.fill(raoData, linearProblem);
    }

    private void setupOperatorsNotToOptimize() {
        // Add a cnec
        cnecNl = crac.newFlowCnec()
            .withId("Line NL - N - preventive")
            .withNetworkElement("NNL1AA1  NNL2AA1  1")
            .newThreshold()
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMax(800.0)
                .withMin(-1000.)
                .withUnit(Unit.MEGAWATT)
                .add()
            .withOptimized()
            .withInstant(Instant.PREVENTIVE)
            .withOperator("NL")
            .add();

        // Set initial margins on both preventive CNECs
        cnecFr = crac.getFlowCnec("Tieline BE FR - N - preventive");

        // Create filler with new operatorsNotToOptimize and fill
        operatorsNotToOptimizeFiller = new OperatorsNotToOptimizeFiller(Collections.singleton("NL"));
    }

    @Test
    public void testGetCnecsForOperatorsNoToOptimize1() {
        initRaoData(crac.getPreventiveState());
        assertEquals(0, operatorsNotToOptimizeFiller.getCnecsForOperatorsNotToOptimize(raoData).count());
    }

    @Test
    public void testGetCnecsForOperatorsNoToOptimize2() {
        setupOperatorsNotToOptimize();
        initRaoData(crac.getPreventiveState());
        assertEquals(1, operatorsNotToOptimizeFiller.getCnecsForOperatorsNotToOptimize(raoData).count());
        assertSame(cnecNl, operatorsNotToOptimizeFiller.getCnecsForOperatorsNotToOptimize(raoData).collect(Collectors.toList()).get(0));
    }

    @Test
    public void testGetMinPossibleMarginOnPerimeter() {
        setupOperatorsNotToOptimize();
        initRaoData(crac.getPreventiveState());
        cnecFr.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(600); // wit a threshold of 750
        cnecNl.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(500); // wit a threshold of 800
        assertEquals(150.0, operatorsNotToOptimizeFiller.getMinPossibleMarginOnPerimeter(raoData), DOUBLE_TOLERANCE);

        String newVariant = raoData.getCracVariantManager().cloneWorkingVariant();
        raoData.getCrac().getExtension(ResultVariantManager.class).setPrePerimeterVariantId(newVariant);
        cnecFr.getExtension(CnecResultExtension.class).getVariant(newVariant).setFlowInMW(650); // wit a threshold of 750
        cnecNl.getExtension(CnecResultExtension.class).getVariant(newVariant).setFlowInMW(-950); // wit a threshold of -1000
        assertEquals(50.0, operatorsNotToOptimizeFiller.getMinPossibleMarginOnPerimeter(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetLargestCnecThreshold1() {
        setupOperatorsNotToOptimize();
        initRaoData(crac.getPreventiveState());
        assertEquals(1000, operatorsNotToOptimizeFiller.getLargestCnecThreshold(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetLargestCnecThreshold2() {
        setupOperatorsNotToOptimize();

        crac.newFlowCnec()
            .withId("Pure MNEC")
            .withNetworkElement("DDE2AA1  NNL3AA1  1")
            .newThreshold()
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMax(3000.0)
                .withMin(-3000.)
                .withUnit(Unit.MEGAWATT)
                .add()
            .withMonitored()
            .withInstant(Instant.PREVENTIVE)
            .add();

        crac.newFlowCnec()
            .withId("CNEC MNEC")
            .withNetworkElement("DDE2AA1  NNL3AA1  1")
            .newThreshold()
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMax(2000.0)
                .withMin(-2500.)
                .withUnit(Unit.MEGAWATT)
                .add()
            .withMonitored()
            .withOptimized()
            .withInstant(Instant.PREVENTIVE)
            .add();

        initRaoData(crac.getPreventiveState());
        assertEquals(2500, operatorsNotToOptimizeFiller.getLargestCnecThreshold(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testCnecsNotToOptimizeBinaryVar() {
        setupOperatorsNotToOptimize();
        initRaoData(crac.getPreventiveState());
        cnecFr.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(600); // wit a threshold of +750/-750
        cnecNl.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(400); // wit a threshold of +800/-1000
        double worstMarginDecreaseOnCnecNl = (800 - 400) - (750 - 600);

        fillProblemWithCoreFiller();
        maxMinMarginFiller.fill(raoData, linearProblem);
        operatorsNotToOptimizeFiller.fill(raoData, linearProblem);

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
        assertEquals(worstMarginDecreaseOnCnecNl, marginDecreaseConstraintMin.getCoefficient(binaryVar), DOUBLE_TOLERANCE);

        MPConstraint marginDecreaseConstraintMax = linearProblem.getMarginDecreaseConstraint(cnecNl, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(marginDecreaseConstraintMax);
        assertEquals(linearProblem.infinity(), marginDecreaseConstraintMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(-800 + (800 - 400), marginDecreaseConstraintMax.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1.0, marginDecreaseConstraintMax.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(worstMarginDecreaseOnCnecNl, marginDecreaseConstraintMax.getCoefficient(binaryVar), DOUBLE_TOLERANCE);
    }

    @Test
    public void testExcludeCnecsNotToOptimizeInMinMargin() {
        setupOperatorsNotToOptimize();
        initRaoData(crac.getPreventiveState());
        cnecFr.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(600); // wit a threshold of +750/-750
        cnecNl.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(400); // wit a threshold of +800/-1000
        double maxAbsThreshold = 1000;
        fillProblemWithCoreFiller();
        maxMinMarginFiller.fill(raoData, linearProblem);
        operatorsNotToOptimizeFiller.fill(raoData, linearProblem);

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
        setupOperatorsNotToOptimize();
        initRaoData(crac.getPreventiveState());
        cnecFr.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(600); // with a threshold of +750/-750
        cnecNl.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(400); // with a threshold of +800/-1000
        double worstMarginDecreaseOnCnecNl = (800 - 400) - (750 - 600);

        fillProblemWithCoreFiller();
        maxMinRelativeMarginFiller.fill(raoData, linearProblem);
        operatorsNotToOptimizeFiller.fill(raoData, linearProblem);

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
        assertEquals(worstMarginDecreaseOnCnecNl, marginDecreaseConstraintMin.getCoefficient(binaryVar), DOUBLE_TOLERANCE);

        MPConstraint marginDecreaseConstraintMax = linearProblem.getMarginDecreaseConstraint(cnecNl, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(marginDecreaseConstraintMax);
        assertEquals(linearProblem.infinity(), marginDecreaseConstraintMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(-800 + (800 - 400), marginDecreaseConstraintMax.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1.0, marginDecreaseConstraintMax.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(worstMarginDecreaseOnCnecNl, marginDecreaseConstraintMax.getCoefficient(binaryVar), DOUBLE_TOLERANCE);
    }

    @Test
    public void testExcludeCnecsNotToOptimizeInMinMarginRelative() {
        setupOperatorsNotToOptimize();
        initRaoData(crac.getPreventiveState());
        cnecFr.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(600); // with a threshold of +750/-750
        cnecNl.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(400); // with a threshold of +800/-1000
        double maxAbsThreshold = 1000;
        fillProblemWithCoreFiller();
        maxMinRelativeMarginFiller.fill(raoData, linearProblem);
        operatorsNotToOptimizeFiller.fill(raoData, linearProblem);

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
