/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionResult;
import com.powsybl.openrao.searchtreerao.result.impl.MultiStateRemedialActionResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionResultImpl;
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
class DiscretePstGroupFillerTest extends AbstractFillerTest {

    @Test
    void testFillAndUpdateMethods() {

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

        RangeActionResult initialRangeActionResult = RangeActionResultImpl.buildWithSetpointsFromNetwork(network, Set.of(pstRa1, pstRa2));
        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);

        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        rangeActions.put(state, Set.of(pstRa1, pstRa2));
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);

        RangeActionsOptimizationParameters rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(new RaoParameters());

        CoreProblemFiller coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionResult,
            new MultiStateRemedialActionResultImpl(flowAndSensiResult, optimizationPerimeter),
            rangeActionParameters,
            Unit.MEGAWATT,
            false);

        Map<State, Set<PstRangeAction>> pstRangeActions = new HashMap<>();
        pstRangeActions.put(state, Set.of(pstRa1, pstRa2));
        DiscretePstTapFiller discretePstTapFiller = new DiscretePstTapFiller(
            network,
            state,
            pstRangeActions,
            initialRangeActionResult);

        DiscretePstGroupFiller discretePstGroupFiller = new DiscretePstGroupFiller(
            network,
            state,
            pstRangeActions);

        LinearProblem linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(discretePstTapFiller)
            .withProblemFiller(discretePstGroupFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();

        // fill problem
        linearProblem.fill(flowAndSensiResult);

        // check that all constraints and variables relate to discrete Pst Group filler exists
        OpenRaoMPVariable groupTapV = linearProblem.getPstGroupTapVariable(groupId, state);
        OpenRaoMPVariable variationUp1V = linearProblem.getPstTapVariationVariable(pstRa1, state, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable variationDown1V = linearProblem.getPstTapVariationVariable(pstRa1, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable variationUp2V = linearProblem.getPstTapVariationVariable(pstRa2, state, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable variationDown2V = linearProblem.getPstTapVariationVariable(pstRa2, state, LinearProblem.VariationDirectionExtension.DOWNWARD);

        OpenRaoMPConstraint groupTap1C = linearProblem.getPstGroupTapConstraint(pstRa1, state);
        OpenRaoMPConstraint groupTap2C = linearProblem.getPstGroupTapConstraint(pstRa2, state);

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

        MultiStateRemedialActionResultImpl updatedRangeActionResult = new MultiStateRemedialActionResultImpl(flowAndSensiResult, optimizationPerimeter);
        updatedRangeActionResult.activate(pstRa1, crac.getPreventiveState(), newAlpha);
        updatedRangeActionResult.activate(pstRa2, crac.getPreventiveState(), newAlpha);
        discretePstTapFiller.updateBetweenSensiIteration(linearProblem, flowAndSensiResult, updatedRangeActionResult);
        discretePstGroupFiller.updateBetweenSensiIteration(linearProblem, flowAndSensiResult, updatedRangeActionResult);

        // check constraints
        assertEquals(-10, groupTap1C.lb(), 1e-6);
        assertEquals(-10, groupTap1C.ub(), 1e-6);

        assertEquals(-10, groupTap2C.lb(), 1e-6);
        assertEquals(-10, groupTap2C.ub(), 1e-6);
    }
}
