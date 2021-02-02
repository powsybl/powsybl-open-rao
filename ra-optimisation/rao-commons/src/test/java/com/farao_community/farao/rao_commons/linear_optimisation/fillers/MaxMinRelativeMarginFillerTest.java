/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.rao_api.RaoParameters.DEFAULT_PST_PENALTY_COST;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class MaxMinRelativeMarginFillerTest extends AbstractFillerTest {

    private MaxMinRelativeMarginFiller maxMinRelativeMarginFiller;
    static final double PRECISE_DOUBLE_TOLERANCE = 1e-10;

    @Before
    public void setUp() {
        init();
        coreProblemFiller = new CoreProblemFiller();
        maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(MEGAWATT, DEFAULT_PST_PENALTY_COST, 1000, 0.01);
        initRaoData(crac.getPreventiveState());
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
}
