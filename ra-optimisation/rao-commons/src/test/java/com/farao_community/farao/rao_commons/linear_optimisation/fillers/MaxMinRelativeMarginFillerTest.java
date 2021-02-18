/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.rao_api.RaoParameters.DEFAULT_PST_PENALTY_COST;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class MaxMinRelativeMarginFillerTest extends AbstractFillerTest {

    private MaxMinRelativeMarginFiller maxMinRelativeMarginFiller;
    static final double PRECISE_DOUBLE_TOLERANCE = 1e-10;
    BranchCnec cnecNl;
    BranchCnec cnecFr;

    @Before
    public void setUp() {
        init();
        coreProblemFiller = new CoreProblemFiller();
        maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(MEGAWATT, DEFAULT_PST_PENALTY_COST, null, 1000, 0.01);
    }

    private void fillProblemWithCoreFiller() {
        // arrange some additional data
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        raoData.getCracResultManager().applyRangeActionResultsOnNetwork();

        // fill the problem : the core filler is required
        coreProblemFiller.fill(raoData, linearProblem);
    }

    @Test
    public void fillWithMaxMinRelativeMarginInMegawatt() {
        initRaoData(crac.getPreventiveState());
        // this is almost a copy of fillWithMaxMinMarginInMegawatt()
        // only the coefficients in the MinMargin constraint should be different
        fillProblemWithCoreFiller();
        cnec1.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setAbsolutePtdfSum(0.9);
        cnec2.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setAbsolutePtdfSum(0.7);
        maxMinRelativeMarginFiller.fill(raoData, linearProblem);

        MPVariable flowCnec1 = linearProblem.getFlowVariable(cnec1);
        MPVariable absoluteVariation = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

        // check minimum margin variable
        MPVariable minimumMargin = linearProblem.getMinimumMarginVariable();
        assertNotNull(minimumMargin);
        assertEquals(0.0, minimumMargin.ub(), PRECISE_DOUBLE_TOLERANCE);
        MPVariable minimumRelativeMargin = linearProblem.getMinimumRelativeMarginVariable();
        assertNotNull(minimumRelativeMargin);

        // check minimum margin constraints
        MPConstraint cnec1AboveThreshold = linearProblem.getMinimumMarginConstraint(cnec1, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        MPConstraint cnec1BelowThreshold = linearProblem.getMinimumMarginConstraint(cnec1, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        MPConstraint cnec1AboveThresholdRelative = linearProblem.getMinimumRelativeMarginConstraint(cnec1, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        MPConstraint cnec1BelowThresholdRelative = linearProblem.getMinimumRelativeMarginConstraint(cnec1, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(cnec1AboveThreshold);
        assertNotNull(cnec1BelowThreshold);
        assertEquals(-Double.POSITIVE_INFINITY, cnec1BelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-MIN_FLOW_1, cnec1BelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-Double.POSITIVE_INFINITY, cnec1AboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(MAX_FLOW_1, cnec1AboveThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1, cnec1BelowThreshold.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(-1 / 0.9, cnec1BelowThresholdRelative.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1 / 0.9, cnec1AboveThresholdRelative.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(minimumMargin), DOUBLE_TOLERANCE);

        // check objective
        assertEquals(0.01, linearProblem.getObjective().getCoefficient(absoluteVariation), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, linearProblem.getObjective().getCoefficient(minimumMargin), DOUBLE_TOLERANCE);
        assertEquals(-1.0, linearProblem.getObjective().getCoefficient(minimumRelativeMargin), DOUBLE_TOLERANCE);
        assertTrue(linearProblem.getObjective().minimization());

        // check the number of variables and constraints
        assertEquals(5, linearProblem.getSolver().numVariables());
        assertEquals(7, linearProblem.getSolver().numConstraints());
    }

    @Test
    public void fillWithMaxMinRelativeMarginInAmpere() {
        initRaoData(crac.getPreventiveState());
        // this is almost a copy of fillWithMaxMinMarginInAmpere()
        // only the objective function should be different
        fillProblemWithCoreFiller();
        cnec1.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setAbsolutePtdfSum(0.005);
        cnec2.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setAbsolutePtdfSum(0.1);
        maxMinRelativeMarginFiller.setUnit(AMPERE);
        maxMinRelativeMarginFiller.fill(raoData, linearProblem);

        MPVariable flowCnec1 = linearProblem.getFlowVariable(cnec1);
        MPVariable absoluteVariation = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

        // check minimum margin variable
        MPVariable minimumMargin = linearProblem.getMinimumMarginVariable();
        assertNotNull(minimumMargin);
        assertEquals(0.0, minimumMargin.ub(), PRECISE_DOUBLE_TOLERANCE);
        MPVariable minimumRelativeMargin = linearProblem.getMinimumRelativeMarginVariable();
        assertNotNull(minimumRelativeMargin);

        // check minimum margin constraints
        MPConstraint cnec1AboveThreshold = linearProblem.getMinimumMarginConstraint(cnec1, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        MPConstraint cnec1BelowThreshold = linearProblem.getMinimumMarginConstraint(cnec1, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        MPConstraint cnec1AboveThresholdRelative = linearProblem.getMinimumRelativeMarginConstraint(cnec1, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        MPConstraint cnec1BelowThresholdRelative = linearProblem.getMinimumRelativeMarginConstraint(cnec1, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(cnec1AboveThreshold);
        assertNotNull(cnec1BelowThreshold);
        assertEquals(-Double.POSITIVE_INFINITY, cnec1BelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-MIN_FLOW_1, cnec1BelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-Double.POSITIVE_INFINITY, cnec1AboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(MAX_FLOW_1, cnec1AboveThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1, cnec1BelowThreshold.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(-1 / 0.01, cnec1BelowThresholdRelative.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1 / 0.01, cnec1AboveThresholdRelative.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);

        assertEquals(380.0 * Math.sqrt(3) / 1000, cnec1BelowThreshold.getCoefficient(minimumMargin), DOUBLE_TOLERANCE);
        assertEquals(380.0 * Math.sqrt(3) / 1000, cnec1AboveThreshold.getCoefficient(minimumMargin), DOUBLE_TOLERANCE);

        // check objective
        assertEquals(0.01, linearProblem.getObjective().getCoefficient(absoluteVariation), DOUBLE_TOLERANCE); // penalty cost
        assertEquals(-1000.0, linearProblem.getObjective().getCoefficient(minimumMargin), DOUBLE_TOLERANCE);
        assertEquals(-1.0, linearProblem.getObjective().getCoefficient(minimumRelativeMargin), DOUBLE_TOLERANCE);
        assertTrue(linearProblem.getObjective().minimization());

        // check the number of variables and constraints
        assertEquals(5, linearProblem.getSolver().numVariables());
        assertEquals(7, linearProblem.getSolver().numConstraints());
    }

    private void setupOperatorsNotSharingRas() {
        // Add a cnec
        crac.newBranchCnec().setId("Line NL - N - preventive")
                .newNetworkElement().setId("NNL1AA1  NNL2AA1  1").add()
                .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(800.0).setMin(-1000.).setUnit(Unit.MEGAWATT).add()
                .optimized()
                .setInstant(crac.getInstant("N"))
                .setOperator("NL")
                .add();
        // Set initial margins on both preventive CNECs
        cnecNl = crac.getBranchCnec("Line NL - N - preventive");
        cnecFr = crac.getBranchCnec("Tieline BE FR - N - preventive");

        // Create filler with new operatorsNotSharingRas and fill
        maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(MEGAWATT, DEFAULT_PST_PENALTY_COST, Collections.singleton("NL"), 1000, 0.01);
    }

    @Test
    public void testExcludeCnecsNotSharingRasBinaryVar() {
        setupOperatorsNotSharingRas();
        initRaoData(crac.getPreventiveState());
        cnecFr.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(600); // wit a threshold of +750/-750
        cnecNl.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(400); // wit a threshold of +800/-1000
        double worstMarginDecreaseOnCnecNl = (800 - 400) - (750 - 600);

        fillProblemWithCoreFiller();
        maxMinRelativeMarginFiller.fill(raoData, linearProblem);

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
    public void testExcludeCnecsNotSharingInMinMargin() {
        setupOperatorsNotSharingRas();
        initRaoData(crac.getPreventiveState());
        cnecFr.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(600); // wit a threshold of +750/-750
        cnecNl.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(400); // wit a threshold of +800/-1000
        double maxAbsRelativeThreshold = 1000;
        fillProblemWithCoreFiller();
        maxMinRelativeMarginFiller.fill(raoData, linearProblem);

        // Test that cnecFr's constraint does not have a bigM
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumRelativeMarginConstraint(cnecFr, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumRelativeMarginConstraint(cnecFr, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);

        // Test that cnecNl's constraint does have a bigM
        MPVariable marginDecreaseVariable = linearProblem.getMarginDecreaseBinaryVariable(cnecNl);
        MPConstraint minMarginDefMin = linearProblem.getMinimumMarginConstraint(cnecNl, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertEquals(1000 + 2 * maxAbsRelativeThreshold, minMarginDefMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * maxAbsRelativeThreshold, minMarginDefMin.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
        MPConstraint minMarginDefMax = linearProblem.getMinimumMarginConstraint(cnecNl, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertEquals(800 + 2 * maxAbsRelativeThreshold, minMarginDefMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * maxAbsRelativeThreshold, minMarginDefMax.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
    }
}
