/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_commons.result_api.LinearProblemStatus;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearProblemResultTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private LinearProblem linearProblem;
    private LinearProblemResult linearProblemResult;
    private PstRangeAction activatedPstRangeAction;
    private PstRangeAction notActivatedPstRangeAction;
    private PstRangeAction unoptimizedPstRangeAction;
    private RangeAction activatedRangeAction;
    private RangeAction notActivatedRangeAction;
    private RangeAction unoptimizedRangeAction;

    @Before
    public void setUp() {
        activatedPstRangeAction = Mockito.mock(PstRangeAction.class);
        notActivatedPstRangeAction = Mockito.mock(PstRangeAction.class);
        unoptimizedPstRangeAction = Mockito.mock(PstRangeAction.class);
        activatedRangeAction = Mockito.mock(RangeAction.class);
        notActivatedRangeAction = Mockito.mock(RangeAction.class);
        unoptimizedRangeAction = Mockito.mock(RangeAction.class);

        linearProblem = Mockito.mock(LinearProblem.class);
        Mockito.when(linearProblem.getStatus()).thenReturn(LinearProblemStatus.OPTIMAL);
        Set<RangeAction> rangeActions = Set.of(activatedPstRangeAction, notActivatedPstRangeAction, activatedRangeAction, notActivatedRangeAction);
        Mockito.when(linearProblem.getRangeActions()).thenReturn(rangeActions);

        MPVariable activatedPstSetPointVariable = Mockito.mock(MPVariable.class);
        MPVariable activatedPstVariationSetPointVariable = Mockito.mock(MPVariable.class);
        MPVariable notActivatedPstSetPointVariable = Mockito.mock(MPVariable.class);
        MPVariable notActivatedPstVariationSetPointVariable = Mockito.mock(MPVariable.class);
        MPVariable activatedRangeActionSetPointVariable = Mockito.mock(MPVariable.class);
        MPVariable activatedRangeActionVariationSetPointVariable = Mockito.mock(MPVariable.class);
        MPVariable notActivatedRangeActionSetPointVariable = Mockito.mock(MPVariable.class);
        MPVariable notActivatedRangeActionVariationSetPointVariable = Mockito.mock(MPVariable.class);

        Map<RangeAction, MPVariable> setPointVariablePerRangeAction = Map.of(
                activatedPstRangeAction, activatedPstSetPointVariable,
                notActivatedPstRangeAction, notActivatedPstSetPointVariable,
                activatedRangeAction, activatedRangeActionSetPointVariable,
                notActivatedRangeAction, notActivatedRangeActionSetPointVariable
        );

        Map<RangeAction, MPVariable> setPointVariationVariablePerRangeAction = Map.of(
                activatedPstRangeAction, activatedPstVariationSetPointVariable,
                notActivatedPstRangeAction, notActivatedPstVariationSetPointVariable,
                activatedRangeAction, activatedRangeActionVariationSetPointVariable,
                notActivatedRangeAction, notActivatedRangeActionVariationSetPointVariable
        );

        Map<RangeAction, Double> setPointPerRangeAction = Map.of(
                activatedPstRangeAction, 1.5,
                notActivatedPstRangeAction, 5.4,
                activatedRangeAction, 600.,
                notActivatedRangeAction, -200.
        );

        Map<RangeAction, Double> setPointVariationPerRangeAction = Map.of(
                activatedPstRangeAction, 2.3,
                notActivatedPstRangeAction, 0.,
                activatedRangeAction, 200.,
                notActivatedRangeAction, 0.
        );

        rangeActions.forEach(rangeAction -> {
            MPVariable setPointVariable = setPointVariablePerRangeAction.get(rangeAction);
            Mockito.when(linearProblem.getRangeActionSetPointVariable(rangeAction)).thenReturn(setPointVariable);
            Mockito.when(setPointVariable.solutionValue()).thenReturn(setPointPerRangeAction.get(rangeAction));

            MPVariable setPointVariationVariable = setPointVariationVariablePerRangeAction.get(rangeAction);
            Mockito.when(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction)).thenReturn(setPointVariationVariable);
            Mockito.when(setPointVariationVariable.solutionValue()).thenReturn(setPointVariationPerRangeAction.get(rangeAction));
        });

        Mockito.when(activatedPstRangeAction.convertAngleToTap(1.5)).thenReturn(3);
        Mockito.when(notActivatedPstRangeAction.convertAngleToTap(5.4)).thenReturn(10);
    }

    @Test(expected = FaraoException.class)
    public void testBuildingResultWithNonOptimalLinearProblem() {
        Mockito.when(linearProblem.getStatus()).thenReturn(LinearProblemStatus.ABNORMAL);
        linearProblemResult = new LinearProblemResult(linearProblem);
    }

    @Test
    public void testGetOptimizedSetPoint() {
        linearProblemResult = new LinearProblemResult(linearProblem);
        assertEquals(1.5, linearProblemResult.getOptimizedSetPoint(activatedPstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(5.4, linearProblemResult.getOptimizedSetPoint(notActivatedPstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(600., linearProblemResult.getOptimizedSetPoint(activatedRangeAction), DOUBLE_TOLERANCE);
        assertEquals(-200., linearProblemResult.getOptimizedSetPoint(notActivatedRangeAction), DOUBLE_TOLERANCE);
    }

    @Test(expected = FaraoException.class)
    public void testGetOptimizedSetPointFailsWithUnoptimizedRangeAction() {
        linearProblemResult = new LinearProblemResult(linearProblem);
        linearProblemResult.getOptimizedSetPoint(unoptimizedRangeAction);
    }

    @Test
    public void testGetOptimizedTap() {
        linearProblemResult = new LinearProblemResult(linearProblem);
        assertEquals(3, linearProblemResult.getOptimizedTap(activatedPstRangeAction));
        assertEquals(10, linearProblemResult.getOptimizedTap(notActivatedPstRangeAction));
    }

    @Test(expected = FaraoException.class)
    public void testGetOptimizedTapFailsWithUnoptimizedPstRangeAction() {
        linearProblemResult = new LinearProblemResult(linearProblem);
        linearProblemResult.getOptimizedTap(unoptimizedPstRangeAction);
    }

    @Test
    public void testGetOptimizedTaps() {
        linearProblemResult = new LinearProblemResult(linearProblem);
        Map<PstRangeAction, Integer> map = linearProblemResult.getOptimizedTaps();
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(3), map.get(activatedPstRangeAction));
        assertEquals(Integer.valueOf(10), map.get(notActivatedPstRangeAction));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetOptimizedTapsReturnsUnmodifiableMap() {
        linearProblemResult = new LinearProblemResult(linearProblem);
        Map<PstRangeAction, Integer> map = linearProblemResult.getOptimizedTaps();
        map.put(unoptimizedPstRangeAction, 4);
    }

    @Test
    public void testGetOptimizedSetPoints() {
        linearProblemResult = new LinearProblemResult(linearProblem);
        Map<RangeAction, Double> map = linearProblemResult.getOptimizedSetPoints();
        assertEquals(4, map.size());
        assertEquals(Double.valueOf(1.5), map.get(activatedPstRangeAction));
        assertEquals(Double.valueOf(5.4), map.get(notActivatedPstRangeAction));
        assertEquals(Double.valueOf(600.), map.get(activatedRangeAction));
        assertEquals(Double.valueOf(-200.), map.get(notActivatedRangeAction));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetOptimizedSetPointsReturnsUnmodifiableMap() {
        linearProblemResult = new LinearProblemResult(linearProblem);
        Map<RangeAction, Double> map = linearProblemResult.getOptimizedSetPoints();
        map.put(unoptimizedRangeAction, 300.);
    }
}
