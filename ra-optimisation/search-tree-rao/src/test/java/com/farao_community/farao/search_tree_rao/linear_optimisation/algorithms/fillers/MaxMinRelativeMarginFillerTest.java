/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.MaxMinRelativeMarginParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.RangeActionParameters;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class MaxMinRelativeMarginFillerTest extends AbstractFillerTest {
    private static final double PRECISE_DOUBLE_TOLERANCE = 1e-10;

    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;
    private MaxMinRelativeMarginFiller maxMinRelativeMarginFiller;
    private MaxMinRelativeMarginParameters parameters;

    @Before
    public void setUp() {
        init();
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        double initialAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();
        RangeActionSetpointResult initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(pstRangeAction, initialAlpha));

        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(cnec1));

        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        rangeActions.put(cnec1.getState(), Set.of(pstRangeAction));
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);

        RaoParameters raoParameters = new RaoParameters();
        raoParameters.setPstPenaltyCost(0.01);
        raoParameters.setHvdcPenaltyCost(0.01);
        raoParameters.setInjectionRaPenaltyCost(0.01);
        raoParameters.setPtdfSumLowerBound(0.01);
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        RangeActionParameters rangeActionParameters = RangeActionParameters.buildFromRaoParameters(raoParameters);
        parameters = MaxMinRelativeMarginParameters.buildFromRaoParameters(raoParameters);

        coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            new RangeActionActivationResultImpl(initialRangeActionSetpointResult),
            rangeActionParameters,
            MEGAWATT);
        // TODO: test with AMPERE unit
    }

    private void createMaxMinRelativeMarginFiller(Unit unit, double cnecInitialAbsolutePtdfSum) {
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getPtdfZonalSum(cnec1)).thenReturn(cnecInitialAbsolutePtdfSum);
        maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(
                Set.of(cnec1),
                initialFlowResult,
                unit,
                parameters
        );
    }

    private void buildLinearProblem() {
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(maxMinRelativeMarginFiller)
            .withSolver(mpSolver)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    @Test
    public void fillWithMaxMinRelativeMarginInMegawatt() {
        createMaxMinRelativeMarginFiller(MEGAWATT, 0.9);
        buildLinearProblem();

        MPVariable flowCnec1 = linearProblem.getFlowVariable(cnec1);
        MPVariable absoluteVariation = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction, cnec1.getState());

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
        assertEquals(-LinearProblem.infinity(), cnec1BelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-MIN_FLOW_1, cnec1BelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-LinearProblem.infinity(), cnec1AboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(MAX_FLOW_1, cnec1AboveThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1, cnec1BelowThreshold.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(-1, cnec1BelowThresholdRelative.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThresholdRelative.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1 * 0.9, cnec1BelowThresholdRelative.getCoefficient(minimumRelativeMargin), DOUBLE_TOLERANCE);
        assertEquals(1 * 0.9, cnec1AboveThresholdRelative.getCoefficient(minimumRelativeMargin), DOUBLE_TOLERANCE);

        // check objective
        assertEquals(0.01, linearProblem.getObjective().getCoefficient(absoluteVariation), DOUBLE_TOLERANCE);
        assertEquals(-1.0, linearProblem.getObjective().getCoefficient(minimumMargin), DOUBLE_TOLERANCE);
        assertEquals(-1.0, linearProblem.getObjective().getCoefficient(minimumRelativeMargin), DOUBLE_TOLERANCE);
        assertTrue(linearProblem.getObjective().minimization());

        // check the number of variables and constraints
        assertEquals(6, linearProblem.numVariables());
        assertEquals(9, linearProblem.numConstraints());
    }

    @Test
    public void fillWithMaxMinRelativeMarginInAmpere() {
        createMaxMinRelativeMarginFiller(AMPERE, 0.005);
        buildLinearProblem();

        MPVariable flowCnec1 = linearProblem.getFlowVariable(cnec1);
        MPVariable absoluteVariation = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction, cnec1.getState());

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
        assertEquals(-LinearProblem.infinity(), cnec1BelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-MIN_FLOW_1, cnec1BelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-LinearProblem.infinity(), cnec1AboveThreshold.lb(), DOUBLE_TOLERANCE);
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
        assertEquals(-1.0, linearProblem.getObjective().getCoefficient(minimumMargin), DOUBLE_TOLERANCE);
        assertEquals(-1.0, linearProblem.getObjective().getCoefficient(minimumRelativeMargin), DOUBLE_TOLERANCE);
        assertTrue(linearProblem.getObjective().minimization());

        // check the number of variables and constraints
        assertEquals(6, linearProblem.numVariables());
        assertEquals(9, linearProblem.numConstraints());
    }
}
