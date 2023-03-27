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
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPConstraint;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPVariable;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemBuilder;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionActivationResultImpl;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class DiscretePstGroupFillerTest extends AbstractFillerTest {

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

        Map<State, Set<PstRangeAction>> pstRangeActions = new HashMap<>();
        pstRangeActions.put(state, Set.of(pstRa1, pstRa2));
        DiscretePstTapFiller discretePstTapFiller = new DiscretePstTapFiller(
            network,
            state,
            pstRangeActions,
            initialRangeActionSetpointResult);

        DiscretePstGroupFiller discretePstGroupFiller = new DiscretePstGroupFiller(
            network,
            state,
            pstRangeActions);

        LinearProblem linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(discretePstTapFiller)
            .withProblemFiller(discretePstGroupFiller)
            .withSolver(mpSolver)
            .build();

        // fill problem
        linearProblem.fill(flowResult, sensitivityResult);

        // check that all constraints and variables relate to discrete Pst Group filler exists
        FaraoMPVariable groupTapV = linearProblem.getPstGroupTapVariable(groupId, state);
        FaraoMPVariable variationUp1V = linearProblem.getPstTapVariationVariable(pstRa1, state, LinearProblem.VariationDirectionExtension.UPWARD);
        FaraoMPVariable variationDown1V = linearProblem.getPstTapVariationVariable(pstRa1, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        FaraoMPVariable variationUp2V = linearProblem.getPstTapVariationVariable(pstRa2, state, LinearProblem.VariationDirectionExtension.UPWARD);
        FaraoMPVariable variationDown2V = linearProblem.getPstTapVariationVariable(pstRa2, state, LinearProblem.VariationDirectionExtension.DOWNWARD);

        FaraoMPConstraint groupTap1C = linearProblem.getPstGroupTapConstraint(pstRa1, state);
        FaraoMPConstraint groupTap2C = linearProblem.getPstGroupTapConstraint(pstRa2, state);

        assertNotNull(groupTapV);
        assertNotNull(groupTap1C);
        assertNotNull(groupTap2C);

        // check constraints
        assertEquals(initialAlpha, groupTap1C.lb(), 1e-6);
        assertEquals(initialAlpha, groupTap1C.ub(), 1e-6);
        assertEquals(1, groupTap1C.getCoefficient(groupTapV), 1e-6);
        assertEquals(-1, groupTap1C.getCoefficient(variationUp1V), 1e-6);
        assertEquals(1, groupTap1C.getCoefficient(variationDown1V), 1e-6);

        assertEquals(initialAlpha, groupTap2C.lb(), 1e-6);
        assertEquals(initialAlpha, groupTap2C.ub(), 1e-6);
        assertEquals(1, groupTap2C.getCoefficient(groupTapV), 1e-6);
        assertEquals(-1, groupTap2C.getCoefficient(variationUp2V), 1e-6);
        assertEquals(1, groupTap2C.getCoefficient(variationDown2V), 1e-6);

        // update with a tap of -10
        double newAlpha = tapToAngle.get(-10);
        RangeActionActivationResult updatedRangeActionActivationResult = new RangeActionActivationResultImpl(new RangeActionSetpointResultImpl(Map.of(pstRa1, newAlpha, pstRa2, newAlpha)));
        discretePstTapFiller.updateBetweenSensiIteration(linearProblem, flowResult, sensitivityResult, updatedRangeActionActivationResult);
        discretePstGroupFiller.updateBetweenSensiIteration(linearProblem, flowResult, sensitivityResult, updatedRangeActionActivationResult);

        // check constraints
        assertEquals(-10, groupTap1C.lb(), 1e-6);
        assertEquals(-10, groupTap1C.ub(), 1e-6);

        assertEquals(-10, groupTap2C.lb(), 1e-6);
        assertEquals(-10, groupTap2C.ub(), 1e-6);
    }
}
