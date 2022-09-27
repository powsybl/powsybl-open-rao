/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.RangeActionParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemBuilder;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionActivationResultImpl;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionSetpointResultImpl;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class ContinuousRangeActionGroupFillerTest extends AbstractFillerTest {

    @Test
    public void testFillAndUpdateMethods() {

        // prepare data
        init();
        addPstGroupInCrac();
        useNetworkWithTwoPsts();
        State state = crac.getPreventiveState();

        PstRangeAction pstRa1 = crac.getPstRangeAction("pst1-group1");
        PstRangeAction pstRa2 = crac.getPstRangeAction("pst2-group1");
        String groupId = "group1";
        Map<Integer, Double> tapToAngle = pstRa1.getTapToAngleConversionMap(); // both PSTs have the same map
        double initialAlpha = tapToAngle.get(0);
        RangeActionSetpointResult initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(pstRa1, initialAlpha, pstRa2, initialAlpha));
        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);

        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        rangeActions.put(state, Set.of(pstRa1, pstRa2));
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);

        RangeActionParameters rangeActionParameters = RangeActionParameters.buildFromRaoParameters(new RaoParameters());

        CoreProblemFiller coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            new RangeActionActivationResultImpl(initialRangeActionSetpointResult),
            rangeActionParameters,
            Unit.MEGAWATT);

        ContinuousRangeActionGroupFiller continuousRangeActionGroupFiller = new ContinuousRangeActionGroupFiller(
            rangeActions);

        LinearProblem linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(continuousRangeActionGroupFiller)
            .withSolver(mpSolver)
            .build();

        // fill problem
        linearProblem.fill(flowResult, sensitivityResult);

        // check that all constraints and variables relate to discrete Pst Group filler exists
        MPVariable groupSetpointV = linearProblem.getRangeActionGroupSetpointVariable(groupId, state);
        MPVariable setpoint1V = linearProblem.getRangeActionSetpointVariable(pstRa1, state);
        MPVariable setpoint2V = linearProblem.getRangeActionSetpointVariable(pstRa2, state);

        MPConstraint groupTap1C = linearProblem.getRangeActionGroupSetpointConstraint(pstRa1, state);
        MPConstraint groupTap2C = linearProblem.getRangeActionGroupSetpointConstraint(pstRa2, state);

        assertNotNull(groupSetpointV);
        assertNotNull(groupTap1C);
        assertNotNull(groupTap2C);

        // check constraints
        assertEquals(0, groupTap1C.lb(), 1e-6);
        assertEquals(0, groupTap1C.ub(), 1e-6);
        assertEquals(-1, groupTap1C.getCoefficient(groupSetpointV), 1e-6);
        assertEquals(1, groupTap1C.getCoefficient(setpoint1V), 1e-6);

        assertEquals(0, groupTap2C.lb(), 1e-6);
        assertEquals(0, groupTap2C.ub(), 1e-6);
        assertEquals(-1, groupTap2C.getCoefficient(groupSetpointV), 1e-6);
        assertEquals(1, groupTap2C.getCoefficient(setpoint2V), 1e-6);
    }
}
