/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.LinearProblem;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionResult;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionResultImpl;
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
public class DiscretePstGroupFillerTest extends AbstractFillerTest {

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
                0.,
                false
        );

        DiscretePstTapFiller discretePstTapFiller = new DiscretePstTapFiller(
                network,
                Set.of(pstRa1, pstRa2),
                initialRangeActionResult);

        DiscretePstGroupFiller discretePstGroupFiller = new DiscretePstGroupFiller(
                network,
                Set.of(pstRa1, pstRa2)
                );

        LinearProblem linearProblem = new LinearProblem(List.of(coreProblemFiller, discretePstTapFiller, discretePstGroupFiller), mpSolver);

        // fill problem
        coreProblemFiller.fill(linearProblem, flowResult, sensitivityResult);
        discretePstTapFiller.fill(linearProblem, flowResult, sensitivityResult);
        discretePstGroupFiller.fill(linearProblem, flowResult, sensitivityResult);

        // check that all constraints and variables relate to discrete Pst Group filler exists
        MPVariable groupTapV = linearProblem.getPstGroupTapVariable(groupId);
        MPVariable variationUp1V = linearProblem.getPstTapVariationVariable(pstRa1, LinearProblem.VariationDirectionExtension.UPWARD);
        MPVariable variationDown1V = linearProblem.getPstTapVariationVariable(pstRa1, LinearProblem.VariationDirectionExtension.DOWNWARD);
        MPVariable variationUp2V = linearProblem.getPstTapVariationVariable(pstRa2, LinearProblem.VariationDirectionExtension.UPWARD);
        MPVariable variationDown2V = linearProblem.getPstTapVariationVariable(pstRa2, LinearProblem.VariationDirectionExtension.DOWNWARD);

        MPConstraint groupTap1C = linearProblem.getPstGroupTapConstraint(pstRa1);
        MPConstraint groupTap2C = linearProblem.getPstGroupTapConstraint(pstRa2);

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
        RangeActionResult updatedRangeActionResult = new RangeActionResultImpl(Map.of(pstRa1, newAlpha, pstRa2, newAlpha));
        discretePstTapFiller.update(linearProblem, flowResult, sensitivityResult, updatedRangeActionResult);
        discretePstGroupFiller.update(linearProblem, flowResult, sensitivityResult, updatedRangeActionResult);

        // check constraints
        assertEquals(-10, groupTap1C.lb(), 1e-6);
        assertEquals(-10, groupTap1C.ub(), 1e-6);

        assertEquals(-10, groupTap2C.lb(), 1e-6);
        assertEquals(-10, groupTap2C.ub(), 1e-6);
    }
}
