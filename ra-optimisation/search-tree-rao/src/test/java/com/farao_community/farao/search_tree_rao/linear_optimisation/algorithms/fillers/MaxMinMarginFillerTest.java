/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.rao_api.parameters.MaxMinMarginParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.LinearProblem;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionResult;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionResultImpl;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

/**
 * @author Joris Mancini{@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class MaxMinMarginFillerTest extends AbstractFillerTest {
    private static final double MAX_ABS_THRESHOLD = 1000;

    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;
    private MaxMinMarginFiller maxMinMarginFiller;
    private MaxMinMarginParameters maxMinMarginParameters;

    @Before
    public void setUp() {
        init();
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        double initialAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();
        RangeActionResult initialRangeActionResult = new RangeActionResultImpl(Map.of(pstRangeAction, initialAlpha));
        coreProblemFiller = new CoreProblemFiller(
                network,
                Set.of(cnec1),
                Set.of(pstRangeAction),
                initialRangeActionResult,
                0.,
                0.,
                0.,
                false
        );
        maxMinMarginParameters = new MaxMinMarginParameters(0.01, 0.01, 0.01, MAX_ABS_THRESHOLD);
    }

    private void createMaxMinMarginFiller(Unit unit) {
        maxMinMarginFiller = new MaxMinMarginFiller(
                Set.of(cnec1),
                Set.of(pstRangeAction),
                unit,
                maxMinMarginParameters
        );
    }

    private void buildLinearProblem() {
        linearProblem = new LinearProblem(List.of(coreProblemFiller, maxMinMarginFiller), mpSolver);
        linearProblem.fill(flowResult, sensitivityResult);
    }

    @Test
    public void fillWithMaxMinMarginInMegawatt() {
        createMaxMinMarginFiller(Unit.MEGAWATT);
        buildLinearProblem();

        MPVariable flowCnec1 = linearProblem.getFlowVariable(cnec1);
        MPVariable absoluteVariation = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction);

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
        assertEquals(0.01, linearProblem.getObjective().getCoefficient(absoluteVariation), DOUBLE_TOLERANCE); // penalty cost
        assertEquals(-1.0, linearProblem.getObjective().getCoefficient(minimumMargin), DOUBLE_TOLERANCE); // penalty cost
        assertTrue(linearProblem.getObjective().minimization());

        // check the number of variables and constraints
        // total number of variables 5 :
        //      - 3 due to CoreFiller
        //      - minimum margin variable
        //      - minimum relative margin sign binary variable
        // total number of constraints 6 :
        //      - 3 due to CoreFiller
        //      - 2 per CNEC (min margin constraints)
        //      - 1 due to minimumRelMarginSignDefinition
        assertEquals(5, linearProblem.numVariables());
        assertEquals(6, linearProblem.numConstraints());
    }

    @Test
    public void fillWithMaxMinMarginInAmpere() {
        createMaxMinMarginFiller(Unit.AMPERE);
        buildLinearProblem();

        MPVariable flowCnec1 = linearProblem.getFlowVariable(cnec1);
        MPVariable absoluteVariation = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction);

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
        assertEquals(0.01, linearProblem.getObjective().getCoefficient(absoluteVariation), DOUBLE_TOLERANCE); // penalty cost
        assertEquals(-1.0, linearProblem.getObjective().getCoefficient(minimumMargin), DOUBLE_TOLERANCE); // penalty cost
        assertTrue(linearProblem.getObjective().minimization());

        // check the number of variables and constraints
        assertEquals(5, linearProblem.numVariables());
        assertEquals(6, linearProblem.numConstraints());
    }

    @Test
    public void fillWithMissingFlowVariables() {
        createMaxMinMarginFiller(Unit.MEGAWATT);
        linearProblem = new LinearProblem(List.of(maxMinMarginFiller), mpSolver);

        // AbsoluteRangeActionVariables present, but no the FlowVariables
        linearProblem.addAbsoluteRangeActionVariationVariable(0.0, 0.0, pstRangeAction);
        try {
            linearProblem.fill(flowResult, sensitivityResult);
            fail();
        } catch (FaraoException e) {
            assertTrue(e.getMessage().contains("Flow variable"));
        }
    }

    @Test(expected = Test.None.class) // no exception expected
    public void fillWithMissingRangeActionVariables() {
        createMaxMinMarginFiller(Unit.MEGAWATT);
        linearProblem = new LinearProblem(List.of(maxMinMarginFiller), mpSolver);

        // FlowVariables present , but not the absoluteRangeActionVariables present,
        // This should work since range actions can be filtered out by the CoreProblemFiller if their number
        // exceeds the max-pst-per-tso parameter
        linearProblem.addFlowVariable(0.0, 0.0, cnec1);
        linearProblem.addFlowVariable(0.0, 0.0, cnec2);
        linearProblem.fill(flowResult, sensitivityResult);
    }
}

