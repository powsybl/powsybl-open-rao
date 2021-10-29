/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.result.RangeActionResultImpl;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;
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

        PstRangeAction pstRa1 = crac.getPstRangeAction("pst1-group1");
        PstRangeAction pstRa2 = crac.getPstRangeAction("pst2-group1");
        String groupId = "group1";
        Map<Integer, Double> tapToAngle = pstRa1.getTapToAngleConversionMap(); // both PSTs have the same map
        double initialAlpha = tapToAngle.get(0);
        RangeActionResult initialRangeActionResult = new RangeActionResultImpl(Map.of(pstRa1, initialAlpha, pstRa2, initialAlpha));

        CoreProblemFiller coreProblemFiller = new CoreProblemFiller(
                network,
                Set.of(cnec1),
                Set.of(pstRa1, pstRa2),
                initialRangeActionResult,
                0.,
                0.,
                false
        );

        ContinuousRangeActionGroupFiller continuousRangeActionGroupFiller = new ContinuousRangeActionGroupFiller(
                Set.of(pstRa1, pstRa2)
                );

        LinearProblem linearProblem = new LinearProblem(List.of(coreProblemFiller, continuousRangeActionGroupFiller), mpSolver);

        // fill problem
        coreProblemFiller.fill(linearProblem, flowResult, sensitivityResult);
        continuousRangeActionGroupFiller.fill(linearProblem, flowResult, sensitivityResult);

        // check that all constraints and variables relate to discrete Pst Group filler exists
        MPVariable groupSetpointV = linearProblem.getRangeActionGroupSetpointVariable(groupId);
        MPVariable setpoint1V = linearProblem.getRangeActionSetpointVariable(pstRa1);
        MPVariable setpoint2V = linearProblem.getRangeActionSetpointVariable(pstRa2);

        MPConstraint groupTap1C = linearProblem.getRangeActionGroupSetpointConstraint(pstRa1);
        MPConstraint groupTap2C = linearProblem.getRangeActionGroupSetpointConstraint(pstRa2);

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
