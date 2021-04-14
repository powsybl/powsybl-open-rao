/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.MaxMinRelativeMarginParameters;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */

@RunWith(PowerMockRunner.class)
public class MaxMinRelativeMarginFillerTest extends AbstractFillerTest {
    private static final double PST_SENSITIVITY_THRESHOLD = 0.0;
    private static final double PST_PENALTY_COST = 0.01;
    private static final double NEGATIVE_MARGIN_OBJECTIVE_COEFFICIENT = 1000;
    private static final double PTDF_SUM_LOWER_BOUND = 0.01;
    private static final double PRECISE_DOUBLE_TOLERANCE = 1e-10;

    @Before
    public void setUp() {
        init();
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        double initialAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();
        coreProblemFiller = new CoreProblemFiller(
            linearProblem,
            network,
            Set.of(cnec1),
            Map.of(rangeAction, initialAlpha),
            PST_SENSITIVITY_THRESHOLD);
        coreProblemFiller.fill(sensitivityAndLoopflowResults);
    }

    private void createProblem(Unit unit, double cnecInitialAbsolutePtdfSum) {
        MaxMinRelativeMarginFiller maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(
            linearProblem,
            Map.of(cnec1, cnecInitialAbsolutePtdfSum),
            Set.of(rangeAction),
            new MaxMinRelativeMarginParameters(unit, PST_PENALTY_COST, NEGATIVE_MARGIN_OBJECTIVE_COEFFICIENT, PTDF_SUM_LOWER_BOUND));
        maxMinRelativeMarginFiller.fill(sensitivityAndLoopflowResults);
    }

    @Test
    public void fillWithMaxMinRelativeMarginInMegawatt() {
        createProblem(MEGAWATT, 0.9);

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
        assertEquals(-1, cnec1BelowThresholdRelative.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThresholdRelative.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1 * 0.9, cnec1BelowThresholdRelative.getCoefficient(minimumRelativeMargin), DOUBLE_TOLERANCE);
        assertEquals(1 * 0.9, cnec1AboveThresholdRelative.getCoefficient(minimumRelativeMargin), DOUBLE_TOLERANCE);

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
        createProblem(AMPERE, 0.005);

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
        assertEquals(-1, cnec1BelowThresholdRelative.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThresholdRelative.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(380.0 * Math.sqrt(3) / 1000 * 0.01, cnec1AboveThresholdRelative.getCoefficient(minimumRelativeMargin), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(380.0 * Math.sqrt(3) / 1000 * 0.01, cnec1BelowThresholdRelative.getCoefficient(minimumRelativeMargin), PRECISE_DOUBLE_TOLERANCE);

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
