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
public class DiscretePstTapFillerTest extends AbstractFillerTest {

    @Test
    public void testFillAndUpdateMethods() {

        // prepare data
        init();
        PstRangeAction pstRangeAction = crac.getPstRangeAction(RANGE_ACTION_ID);
        Map<Integer, Double> tapToAngle = pstRangeAction.getTapToAngleConversionMap();
        double initialAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();
        RangeActionResult initialRangeActionResult = new RangeActionResultImpl(Map.of(this.pstRangeAction, initialAlpha));

        CoreProblemFiller coreProblemFiller = new CoreProblemFiller(
                network,
                Set.of(cnec1),
                Set.of(pstRangeAction),
                initialRangeActionResult,
                0.,
                0.,
                0.,
                false);

        DiscretePstTapFiller discretePstTapFiller = new DiscretePstTapFiller(
                network,
                Set.of(pstRangeAction),
                initialRangeActionResult);

        LinearProblem linearProblem = new LinearProblem(List.of(coreProblemFiller, discretePstTapFiller), mpSolver);

        // fill linear problem
        coreProblemFiller.fill(linearProblem, flowResult, sensitivityResult);
        discretePstTapFiller.fill(linearProblem, flowResult, sensitivityResult);

        // check that all constraints and variables exists
        MPVariable setpointV = linearProblem.getRangeActionSetpointVariable(pstRangeAction);
        MPVariable variationUpV = linearProblem.getPstTapVariationVariable(pstRangeAction, LinearProblem.VariationExtension.UPWARD);
        MPVariable variationDownV = linearProblem.getPstTapVariationVariable(pstRangeAction, LinearProblem.VariationExtension.DOWNWARD);
        MPVariable binaryUpV = linearProblem.getPstTapVariationBinary(pstRangeAction, LinearProblem.VariationExtension.UPWARD);
        MPVariable binaryDownV = linearProblem.getPstTapVariationBinary(pstRangeAction, LinearProblem.VariationExtension.DOWNWARD);
        MPConstraint tapToAngleConversionC = linearProblem.getTapToAngleConversionConstraint(pstRangeAction);
        MPConstraint upOrDownC = linearProblem.getUpOrDownPstVariationConstraint(pstRangeAction);
        MPConstraint upVariationC = linearProblem.getIsVariationInDirectionConstraint(pstRangeAction, LinearProblem.VariationExtension.UPWARD);
        MPConstraint downVariationC = linearProblem.getIsVariationInDirectionConstraint(pstRangeAction, LinearProblem.VariationExtension.DOWNWARD);

        assertNotNull(setpointV);
        assertNotNull(variationUpV);
        assertNotNull(variationDownV);
        assertNotNull(binaryUpV);
        assertNotNull(binaryDownV);
        assertNotNull(tapToAngleConversionC);
        assertNotNull(upOrDownC);
        assertNotNull(upVariationC);
        assertNotNull(downVariationC);

        // check variable bounds
        assertEquals(0, variationUpV.lb(), 1e-6);
        assertEquals(0, variationDownV.lb(), 1e-6);
        assertEquals(0, binaryUpV.lb(), 1e-6);
        assertEquals(1, binaryUpV.ub(), 1e-6);
        assertEquals(0, binaryDownV.lb(), 1e-6);
        assertEquals(1, binaryDownV.ub(), 1e-6);

        // check tap to angle conversion constraints
        assertEquals(initialAlpha, tapToAngleConversionC.lb(), 1e-6);
        assertEquals(initialAlpha, tapToAngleConversionC.ub(), 1e-6);
        assertEquals(1, tapToAngleConversionC.getCoefficient(setpointV), 1e-6);
        assertEquals(-(tapToAngle.get(15) - tapToAngle.get(0)) / 15, tapToAngleConversionC.getCoefficient(variationUpV), 1e-6);
        assertEquals(-(tapToAngle.get(-15) - tapToAngle.get(0)) / 15, tapToAngleConversionC.getCoefficient(variationDownV), 1e-6);

        // check other constraints
        assertEquals(1, upOrDownC.ub(), 1e-6);
        assertEquals(1, upOrDownC.getCoefficient(binaryUpV), 1e-6);
        assertEquals(1, upOrDownC.getCoefficient(binaryDownV), 1e-6);

        assertEquals(0, upVariationC.ub(), 1e-6);
        assertEquals(1, upVariationC.getCoefficient(variationUpV), 1e-6);
        assertEquals(-15, upVariationC.getCoefficient(binaryUpV), 1e-6);

        assertEquals(0, downVariationC.ub(), 1e-6);
        assertEquals(1, downVariationC.getCoefficient(variationDownV), 1e-6);
        assertEquals(-15, downVariationC.getCoefficient(binaryDownV), 1e-6);

        // update linear problem, with a new PST tap equal to -4
        double alphaBeforeUpdate = tapToAngle.get(-4);
        RangeActionResult rangeActionResultBeforeUpdate = new RangeActionResultImpl(Map.of(this.pstRangeAction, alphaBeforeUpdate));
        discretePstTapFiller.update(linearProblem, flowResult, sensitivityResult, rangeActionResultBeforeUpdate);

        // check tap to angle conversion constraints
        assertEquals(alphaBeforeUpdate, tapToAngleConversionC.lb(), 1e-6);
        assertEquals(alphaBeforeUpdate, tapToAngleConversionC.ub(), 1e-6);
        assertEquals(1, tapToAngleConversionC.getCoefficient(setpointV), 1e-6);
        assertEquals(-(tapToAngle.get(-3) - tapToAngle.get(-4)), tapToAngleConversionC.getCoefficient(variationUpV), 1e-6);
        assertEquals(-(tapToAngle.get(-5) - tapToAngle.get(-4)), tapToAngleConversionC.getCoefficient(variationDownV), 1e-6);

        // check other constraints
        assertEquals(0, upVariationC.ub(), 1e-6);
        assertEquals(1, upVariationC.getCoefficient(variationUpV), 1e-6);
        assertEquals(-19, upVariationC.getCoefficient(binaryUpV), 1e-6);

        assertEquals(0, downVariationC.ub(), 1e-6);
        assertEquals(1, downVariationC.getCoefficient(variationDownV), 1e-6);
        assertEquals(-11, downVariationC.getCoefficient(binaryDownV), 1e-6);
    }
}
