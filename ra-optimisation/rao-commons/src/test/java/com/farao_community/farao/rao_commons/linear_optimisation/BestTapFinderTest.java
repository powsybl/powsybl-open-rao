/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.RangeActionResult;
import com.farao_community.farao.rao_api.results.SensitivityResult;
import com.farao_community.farao.rao_commons.result.RangeActionResultImpl;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ValidationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class BestTapFinderTest {
    private static final double DOUBLE_TOLERANCE = 0.01;
    private static final double INITIAL_PST_SET_POINT = 1.2;
    private static final double REF_FLOW_1 = 100;
    private static final double REF_FLOW_2 = -400;
    private static final double SENSI_1 = 10;
    private static final double SENSI_2 = -40;

    private Network network;
    private RangeActionResult rangeActionResult;
    private BranchResult branchResult;
    private SensitivityResult sensitivityResult;
    private BranchCnec cnec1;
    private BranchCnec cnec2;
    private PstRangeAction pstRangeAction;

    @Before
    public void setUp() {
        sensitivityResult = Mockito.mock(SensitivityResult.class);
        cnec1 = Mockito.mock(BranchCnec.class);
        cnec2 = Mockito.mock(BranchCnec.class);
        network = Mockito.mock(Network.class);

        branchResult = Mockito.mock(BranchResult.class);
        when(branchResult.getFlow(cnec1, Unit.MEGAWATT)).thenReturn(REF_FLOW_1);
        when(branchResult.getFlow(cnec2, Unit.MEGAWATT)).thenReturn(REF_FLOW_2);

        rangeActionResult = Mockito.mock(RangeActionResult.class);
        pstRangeAction = createPst();
        when(rangeActionResult.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 0.));
    }

    private void setSensitivityValues(PstRangeAction pstRangeAction) {
        when(sensitivityResult.getSensitivityValue(cnec1, pstRangeAction, Unit.MEGAWATT)).thenReturn(SENSI_1);
        when(sensitivityResult.getSensitivityValue(cnec2, pstRangeAction, Unit.MEGAWATT)).thenReturn(SENSI_2);
    }

    private void mockPstRangeAction(PstRangeAction pstRangeAction) {
        when(pstRangeAction.convertTapToAngle(-3)).thenThrow(new ValidationException(() -> "header", "Out of bound"));
        when(pstRangeAction.convertTapToAngle(-2)).thenReturn(-2.5);
        when(pstRangeAction.convertTapToAngle(-1)).thenReturn(-0.75);
        when(pstRangeAction.convertTapToAngle(0)).thenReturn(0.);
        when(pstRangeAction.convertTapToAngle(1)).thenReturn(0.75);
        when(pstRangeAction.convertTapToAngle(2)).thenReturn(2.5);
        when(pstRangeAction.convertTapToAngle(3)).thenThrow(new ValidationException(() -> "header", "Out of bound"));
    }

    private void setClosestTapPosition(PstRangeAction pstRangeAction, double setPoint, int tapPosition) {
        when(pstRangeAction.convertAngleToTap(setPoint)).thenReturn(tapPosition);
    }

    private void setMarginsForTap(PstRangeAction pstRangeAction, int tap, double marginForCnec1, double marginForCnec2) {
        mockMarginOnCnec1(pstRangeAction, tap, marginForCnec1);
        mockMarginOnCnec2(pstRangeAction, tap, marginForCnec2);
    }

    private void mockMarginOnCnec1(PstRangeAction pstRangeAction, int tap, double margin) {
        double flow = REF_FLOW_1 + (pstRangeAction.convertTapToAngle(tap) - INITIAL_PST_SET_POINT) * SENSI_1;
        when(cnec1.computeMargin(flow, Side.LEFT, Unit.MEGAWATT)).thenReturn(margin);
    }

    private void mockMarginOnCnec2(PstRangeAction pstRangeAction, int tap, double margin) {
        double flow = REF_FLOW_2 + (pstRangeAction.convertTapToAngle(tap) - INITIAL_PST_SET_POINT) * SENSI_2;
        when(cnec2.computeMargin(flow, Side.LEFT, Unit.MEGAWATT)).thenReturn(margin);
    }

    private Map<Integer, Double> computeMinMarginsForBestTaps(double startingSetPoint) {
        return BestTapFinder.computeMinMarginsForBestTaps(
                network,
                pstRangeAction,
                startingSetPoint,
                List.of(cnec1, cnec2),
                branchResult,
                sensitivityResult
        );
    }

    private RangeActionResult computeUpdatedRangeActionResult() {
        return BestTapFinder.find(
                rangeActionResult,
                network,
                List.of(cnec1, cnec2),
                branchResult,
                sensitivityResult
        );
    }

    private PstRangeAction createPstWithGroupId(String groupId) {
        PstRangeAction pst = createPst();
        when(pst.getGroupId()).thenReturn(Optional.of(groupId));
        return pst;
    }

    private PstRangeAction createPst() {
        PstRangeAction pst = Mockito.mock(PstRangeAction.class);
        when(pst.getCurrentSetpoint(network)).thenReturn(INITIAL_PST_SET_POINT);
        mockPstRangeAction(pst);
        setSensitivityValues(pst);
        return pst;
    }

    @Test
    public void testMarginWhenTheSetPointIsTooFarFromTheMiddle() {
        // Set point is really close to tap 1, so there is no computation and margin is considered the best for tap 1
        double startingSetPoint = 0.8;
        setClosestTapPosition(pstRangeAction, startingSetPoint, 1);
        setMarginsForTap(pstRangeAction, 1, 100, 120);
        setMarginsForTap(pstRangeAction, 2, 150, 50);

        Map<Integer, Double> marginsForBestTaps = computeMinMarginsForBestTaps(startingSetPoint);

        assertEquals(1, marginsForBestTaps.size());
        assertEquals(Double.MAX_VALUE, marginsForBestTaps.get(1), DOUBLE_TOLERANCE);
    }

    @Test
    public void testMarginsWithOtherTapDecreasingTheMinMargin() {
        // Set point is close enough to the middle of the range between tap 1 and 2, so we consider the two taps
        // The closest tap is still 1, and the next tap worsen the margin so it is not considered
        double startingSetPoint = 1.5;
        setClosestTapPosition(pstRangeAction, startingSetPoint, 1);
        setMarginsForTap(pstRangeAction, 1, 100, 120);
        setMarginsForTap(pstRangeAction, 2, 150, 50);

        Map<Integer, Double> marginsForBestTaps = computeMinMarginsForBestTaps(startingSetPoint);

        assertEquals(1, marginsForBestTaps.size());
        assertEquals(Double.MAX_VALUE, marginsForBestTaps.get(1), DOUBLE_TOLERANCE);
    }

    @Test
    public void testMarginsWithOtherTapNotIncreasingEnoughTheMinMargin() {
        // Set point is close enough to the middle of the range between tap 1 and 2, so we consider the two taps
        // The closest tap is still 1, and the next tap increase the margin but not enough (>10%) so it is not considered
        double startingSetPoint = 1.5;
        setClosestTapPosition(pstRangeAction, startingSetPoint, 1);
        setMarginsForTap(pstRangeAction, 1, 100, 120);
        setMarginsForTap(pstRangeAction, 2, 150, 110);

        Map<Integer, Double> marginsForBestTaps = computeMinMarginsForBestTaps(startingSetPoint);

        assertEquals(1, marginsForBestTaps.size());
        assertEquals(Double.MAX_VALUE, marginsForBestTaps.get(1), DOUBLE_TOLERANCE);
    }

    @Test
    public void testMarginsWithOtherTapIncreasingEnoughTheMinMargin() {
        // Set point is close enough to the middle of the range between tap 1 and 2, so we consider the two taps
        // The closest tap is still 1, and the other tap increases the margin enough (>10%) so it is considered
        double startingSetPoint = 1.5;
        setClosestTapPosition(pstRangeAction, startingSetPoint, 1);
        setMarginsForTap(pstRangeAction, 1, 100, 120);
        setMarginsForTap(pstRangeAction, 2, 150, 120);

        Map<Integer, Double> marginsForBestTaps = computeMinMarginsForBestTaps(startingSetPoint);

        assertEquals(2, marginsForBestTaps.size());
        assertEquals(100, marginsForBestTaps.get(1), DOUBLE_TOLERANCE);
        assertEquals(120, marginsForBestTaps.get(2), DOUBLE_TOLERANCE);
    }

    @Test
    public void testMarginsWithOtherTapIncreasingEnoughTheMinMarginWithNegativeMargins() {
        // Set point is close enough to the middle of the range between tap 1 and 2, so we consider the two taps
        // The closest tap is still 1, and the next tap increase the margin but not enough (>10%) so it is not considered
        double startingSetPoint = 1.5;
        setClosestTapPosition(pstRangeAction, startingSetPoint, 1);
        setMarginsForTap(pstRangeAction, 1, -200, -250);
        setMarginsForTap(pstRangeAction, 2, 100, -120);

        Map<Integer, Double> marginsForBestTaps = computeMinMarginsForBestTaps(startingSetPoint);

        assertEquals(2, marginsForBestTaps.size());
        assertEquals(-250, marginsForBestTaps.get(1), DOUBLE_TOLERANCE);
        assertEquals(-120, marginsForBestTaps.get(2), DOUBLE_TOLERANCE);
    }

    @Test
    public void testMarginsWithOtherTapIncreasingEnoughTheMinMarginOnUpperBound() {
        // Set point is close enough to the middle of the range between tap 1 and 2, so we consider the two taps
        // The closest tap is 2 which is the upper bound, and the other tap increases the margin enough (>10%) so it is considered
        double startingSetPoint = 1.7;
        setClosestTapPosition(pstRangeAction, startingSetPoint, 2);
        setMarginsForTap(pstRangeAction, 1, 140, 150);
        setMarginsForTap(pstRangeAction, 2, 150, 120);

        Map<Integer, Double> marginsForBestTaps = computeMinMarginsForBestTaps(startingSetPoint);

        assertEquals(2, marginsForBestTaps.size());
        assertEquals(140, marginsForBestTaps.get(1), DOUBLE_TOLERANCE);
        assertEquals(120, marginsForBestTaps.get(2), DOUBLE_TOLERANCE);
    }

    @Test
    public void testMarginsWithOtherTapIncreasingEnoughTheMinMarginOnLowerBound() {
        // Set point is close enough to the middle of the range between tap -1 and -2, so we consider the two taps
        // The closest tap is -2 which is the lower bound, and the other tap increases the margin enough (>10%) so it is considered
        double startingSetPoint = -1.7;
        setClosestTapPosition(pstRangeAction, startingSetPoint, -2);
        setMarginsForTap(pstRangeAction, -1, 140, 150);
        setMarginsForTap(pstRangeAction, -2, 150, 120);

        Map<Integer, Double> marginsForBestTaps = computeMinMarginsForBestTaps(startingSetPoint);

        assertEquals(2, marginsForBestTaps.size());
        assertEquals(140, marginsForBestTaps.get(-1), DOUBLE_TOLERANCE);
        assertEquals(120, marginsForBestTaps.get(-2), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeBestTapPerPstGroup() {
        PstRangeAction pst1 = createPst();
        PstRangeAction pst2 = createPstWithGroupId("group1");
        PstRangeAction pst3 = createPstWithGroupId("group1");
        PstRangeAction pst4 = createPstWithGroupId("group2");
        PstRangeAction pst5 = createPstWithGroupId("group2");
        PstRangeAction pst6 = createPstWithGroupId("group2");
        PstRangeAction pst7 = createPstWithGroupId("group2");

        Map<PstRangeAction, Map<Integer, Double>> minMarginPerTap = new HashMap<>();
        minMarginPerTap.put(pst1, Map.of(3, 100., 4, 500.));

        minMarginPerTap.put(pst2, Map.of(3, 100., 4, 500.));
        minMarginPerTap.put(pst3, Map.of(3, 110., 4, 50.));

        minMarginPerTap.put(pst4, Map.of(-10, -30., -11, -80.));
        minMarginPerTap.put(pst5, Map.of(-10, -40., -11, -20.));
        minMarginPerTap.put(pst6, Map.of(-10, -70., -11, 200.));
        minMarginPerTap.put(pst7, Map.of(-11, Double.MAX_VALUE));

        Map<String, Integer> bestTapPerPstGroup = BestTapFinder.computeBestTapPerPstGroup(minMarginPerTap);
        assertEquals(2, bestTapPerPstGroup.size());
        assertEquals(3, bestTapPerPstGroup.get("group1").intValue());
        assertEquals(-10, bestTapPerPstGroup.get("group2").intValue());
    }

    @Test
    public void testUpdatedRangeActionResultWithOtherTapSelected() {
        double startingSetPoint = 1.7;
        setClosestTapPosition(pstRangeAction, startingSetPoint, 2);
        setMarginsForTap(pstRangeAction, 1, 140, 150); // Tap 1 should be selected because min margin is 140
        setMarginsForTap(pstRangeAction, 2, 150, 120);

        RangeAction activatedRangeActionOtherThanPst = Mockito.mock(RangeAction.class);
        rangeActionResult = new RangeActionResultImpl(
                Map.of(
                    pstRangeAction, startingSetPoint,
                    activatedRangeActionOtherThanPst, 200.
                ));

        RangeActionResult updatedRangeActionResult = computeUpdatedRangeActionResult();

        assertEquals(0.75, updatedRangeActionResult.getOptimizedSetPoint(pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(200., updatedRangeActionResult.getOptimizedSetPoint(activatedRangeActionOtherThanPst), DOUBLE_TOLERANCE);
    }

    @Test
    public void testUpdatedRangeActionResultWithClosestTapSelected() {
        double startingSetPoint = 1.7;
        setClosestTapPosition(pstRangeAction, startingSetPoint, 2);
        setMarginsForTap(pstRangeAction, 1, 120, 150);
        setMarginsForTap(pstRangeAction, 2, 150, 140); // Tap 2 should be selected because min margin is 140

        RangeAction activatedRangeActionOtherThanPst = Mockito.mock(RangeAction.class);
        rangeActionResult = new RangeActionResultImpl(
                Map.of(
                        pstRangeAction, startingSetPoint,
                        activatedRangeActionOtherThanPst, 200.
                ));

        RangeActionResult updatedRangeActionResult = computeUpdatedRangeActionResult();

        assertEquals(2.5,  updatedRangeActionResult.getOptimizedSetPoint(pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(200., updatedRangeActionResult.getOptimizedSetPoint(activatedRangeActionOtherThanPst), DOUBLE_TOLERANCE);
    }

    @Test
    public void testUpdatedRangeActionResultNoOptimizationOfTheTap() {
        double startingSetPoint = 0.8;
        // Starting point is really close to set point of tap 1 so it will be set to tap 1
        setClosestTapPosition(pstRangeAction, startingSetPoint, 1);
        setMarginsForTap(pstRangeAction, 1, 120, 150);
        setMarginsForTap(pstRangeAction, 2, 150, 140); // Tap 2 would be ignored even if result is better

        RangeAction activatedRangeActionOtherThanPst = Mockito.mock(RangeAction.class);
        rangeActionResult = new RangeActionResultImpl(
                Map.of(
                        pstRangeAction, startingSetPoint,
                        activatedRangeActionOtherThanPst, 200.
                ));

        RangeActionResult updatedRangeActionResult = computeUpdatedRangeActionResult();

        assertEquals(0.75,  updatedRangeActionResult.getOptimizedSetPoint(pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(200., updatedRangeActionResult.getOptimizedSetPoint(activatedRangeActionOtherThanPst), DOUBLE_TOLERANCE);
    }

    @Test
    public void testUpdatedRangeActionResultWithGroups() {
        double startingSetPoint = 1.7;
        setClosestTapPosition(pstRangeAction, startingSetPoint, 2);
        setMarginsForTap(pstRangeAction, 1, 120, 150);
        setMarginsForTap(pstRangeAction, 2, 150, 140); // Tap 2 should be selected because min margin is 140

        PstRangeAction pstGroup1 = createPstWithGroupId("group1");
        PstRangeAction pstGroup2 = createPstWithGroupId("group1");
        double groupStartingSetPoint = -0.4;
        setClosestTapPosition(pstGroup1, groupStartingSetPoint, -1);
        setMarginsForTap(pstGroup1, -1, 120, 150);
        setMarginsForTap(pstGroup1, 0, 150, 140); // Tap 0 should be selected because min margin is 140
        setClosestTapPosition(pstGroup2, groupStartingSetPoint, -1);
        setMarginsForTap(pstGroup2, -1, 120, 150);
        setMarginsForTap(pstGroup2, 0, 150, 140); // Tap 0 should be selected because min margin is 140

        rangeActionResult = new RangeActionResultImpl(
                Map.of(
                        pstRangeAction, startingSetPoint,
                        pstGroup1, groupStartingSetPoint,
                        pstGroup2, groupStartingSetPoint
                ));

        RangeActionResult updatedRangeActionResult = computeUpdatedRangeActionResult();

        assertEquals(2.5,  updatedRangeActionResult.getOptimizedSetPoint(pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(0, updatedRangeActionResult.getOptimizedSetPoint(pstGroup1), DOUBLE_TOLERANCE);
        assertEquals(0, updatedRangeActionResult.getOptimizedSetPoint(pstGroup2), DOUBLE_TOLERANCE);
    }
}
