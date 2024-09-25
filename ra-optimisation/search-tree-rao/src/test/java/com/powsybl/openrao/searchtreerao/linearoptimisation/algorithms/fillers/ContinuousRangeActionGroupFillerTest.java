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
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class ContinuousRangeActionGroupFillerTest extends AbstractFillerTest {

    @Test
    void testFillAndUpdateMethods() throws IOException {

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

        CoreProblemFiller coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            (new RaoParameters()).getRangeActionsOptimizationParameters(),
            null,
            Unit.MEGAWATT,
            false, RangeActionsOptimizationParameters.PstModel.CONTINUOUS);

        ContinuousRangeActionGroupFiller continuousRangeActionGroupFiller = new ContinuousRangeActionGroupFiller(
            rangeActions);

        LinearProblem linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(continuousRangeActionGroupFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();

        // fill problem
        linearProblem.fill(flowResult, sensitivityResult);

        // check that all constraints and variables relate to discrete Pst Group filler exists
        OpenRaoMPVariable groupSetpointV = linearProblem.getRangeActionGroupSetpointVariable(groupId, state);
        OpenRaoMPVariable setpoint1V = linearProblem.getRangeActionSetpointVariable(pstRa1, state);
        OpenRaoMPVariable setpoint2V = linearProblem.getRangeActionSetpointVariable(pstRa2, state);

        OpenRaoMPConstraint groupTap1C = linearProblem.getRangeActionGroupSetpointConstraint(pstRa1, state);
        OpenRaoMPConstraint groupTap2C = linearProblem.getRangeActionGroupSetpointConstraint(pstRa2, state);

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
