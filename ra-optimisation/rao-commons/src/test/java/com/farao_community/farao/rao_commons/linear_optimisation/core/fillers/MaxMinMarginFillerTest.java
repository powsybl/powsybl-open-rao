/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.core.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_commons.linear_optimisation.core.LinearProblem;
import com.farao_community.farao.rao_commons.linear_optimisation.core.LinearProblemParameters;
import com.google.ortools.linearsolver.MPConstraint;

import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

/**
 * @author Joris Mancini{@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class MaxMinMarginFillerTest extends AbstractFillerTest {

    private MaxMinMarginFiller maxMinMarginFiller;

    @Before
    public void setUp() {
        init();
        coreProblemFiller = new CoreProblemFiller();
        maxMinMarginFiller = new MaxMinMarginFiller();
    }

    private void fillProblemWithFiller() {
        // arrange some additional data
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);

        // fill the problem : the core filler is required
        coreProblemFiller.fill(raoData, linearProblem, linearProblemParameters);
        maxMinMarginFiller.fill(raoData, linearProblem, linearProblemParameters);
    }

    @Test
    public void fillWithMaxMinMarginInMegawatt() {
        linearProblemParameters.setObjectiveFunction(LinearProblemParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT);
        fillProblemWithFiller();

        MPVariable flowCnec1 = linearProblem.getFlowVariable(cnec1);
        MPVariable absoluteVariation = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

        // check minimum margin variable
        MPVariable minimumMargin = linearProblem.getMinimumMarginVariable();
        assertNotNull(minimumMargin);

        // check minimum margin constraints
        MPConstraint cnec1AboveThreshold = linearProblem.getMinimumMarginConstraint(cnec1, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        MPConstraint cnec1BelowThreshold = linearProblem.getMinimumMarginConstraint(cnec1, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(cnec1AboveThreshold);
        assertNotNull(cnec1BelowThreshold);
        assertEquals(-Double.POSITIVE_INFINITY, cnec1BelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-MIN_FLOW_1, cnec1BelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-Double.POSITIVE_INFINITY, cnec1AboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(MAX_FLOW_1, cnec1AboveThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1, cnec1BelowThreshold.getCoefficient(flowCnec1), DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(flowCnec1), DOUBLE_TOLERANCE);
        assertEquals(1, cnec1BelowThreshold.getCoefficient(minimumMargin), DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(minimumMargin), DOUBLE_TOLERANCE);

        // check objective
        assertNotEquals(0, linearProblem.getObjective().getCoefficient(absoluteVariation)); // penalty cost
        assertEquals(-1, linearProblem.getObjective().getCoefficient(minimumMargin), DOUBLE_TOLERANCE); // penalty cost
        assertTrue(linearProblem.getObjective().minimization());

        // check the number of variables and constraints
        // total number of variables 5 :
        //      - 4 due to CoreFiller
        //      - minimum margin variable
        // total number of constraints 8 :
        //      - 4 due to CoreFiller
        //      - 2 per CNEC (min margin constraints)
        assertEquals(5, linearProblem.getSolver().numVariables());
        assertEquals(8, linearProblem.getSolver().numConstraints());
    }

    @Test
    public void fillWithMaxMinMarginInAmpere() {
        linearProblemParameters.setObjectiveFunction(LinearProblemParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        fillProblemWithFiller();

        MPVariable flowCnec1 = linearProblem.getFlowVariable(cnec1);
        MPVariable absoluteVariation = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

        // check minimum margin variable
        MPVariable minimumMargin = linearProblem.getMinimumMarginVariable();
        assertNotNull(minimumMargin);

        // check minimum margin constraints
        MPConstraint cnec1AboveThreshold = linearProblem.getMinimumMarginConstraint(cnec1, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        MPConstraint cnec1BelowThreshold = linearProblem.getMinimumMarginConstraint(cnec1, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(cnec1AboveThreshold);
        assertNotNull(cnec1BelowThreshold);
        assertEquals(-Double.POSITIVE_INFINITY, cnec1BelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-MIN_FLOW_1, cnec1BelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-Double.POSITIVE_INFINITY, cnec1AboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(MAX_FLOW_1, cnec1AboveThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1, cnec1BelowThreshold.getCoefficient(flowCnec1), DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(flowCnec1), DOUBLE_TOLERANCE);

        assertEquals(380.0 * Math.sqrt(3) / 1000, cnec1BelowThreshold.getCoefficient(minimumMargin), DOUBLE_TOLERANCE);
        assertEquals(380.0 * Math.sqrt(3) / 1000, cnec1AboveThreshold.getCoefficient(minimumMargin), DOUBLE_TOLERANCE);

        // check objective
        assertNotEquals(0, linearProblem.getObjective().getCoefficient(absoluteVariation)); // penalty cost
        assertEquals(-1, linearProblem.getObjective().getCoefficient(minimumMargin), DOUBLE_TOLERANCE); // penalty cost
        assertTrue(linearProblem.getObjective().minimization());

        // check the number of variables and constraints
        assertEquals(5, linearProblem.getSolver().numVariables());
        assertEquals(8, linearProblem.getSolver().numConstraints());
    }

    @Test
    public void fillWithMissingFlowVariables() {
        // AbsoluteRangeActionVariables present, but no the FlowVariables
        linearProblem.addAbsoluteRangeActionVariationVariable(0.0, 0.0, rangeAction);
        try {
            maxMinMarginFiller.fill(raoData, linearProblem, linearProblemParameters);
            fail();
        } catch (FaraoException e) {
            assertTrue(e.getMessage().contains("Flow variable"));
        }
    }

    @Test
    public void fillWithMissingRangeActionVariables() {
        // FlowVariables present , but not the absoluteRangeActionVariables present,
        linearProblem.addFlowVariable(0.0, 0.0, cnec1);
        linearProblem.addFlowVariable(0.0, 0.0, cnec2);
        try {
            maxMinMarginFiller.fill(raoData, linearProblem, linearProblemParameters);
            fail();
        } catch (FaraoException e) {
            assertTrue(e.getMessage().contains("Range action variable"));
        }
    }
}

