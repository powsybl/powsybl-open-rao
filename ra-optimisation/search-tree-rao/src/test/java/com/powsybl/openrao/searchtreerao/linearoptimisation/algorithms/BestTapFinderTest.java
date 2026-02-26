/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms;

import com.powsybl.iidm.network.DefaultMessageHeader;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.Validable;
import com.powsybl.iidm.network.ValidationException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.IteratingLinearOptimizer.roundOtherRas;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class BestTapFinderTest {
    private static final double DOUBLE_TOLERANCE = 0.01;
    private static final double INITIAL_PST_SET_POINT = 1.2;
    private static final double REF_FLOW_1 = 100;
    private static final double REF_FLOW_2 = -400;
    private static final double SENSI_1 = 10;
    private static final double SENSI_2 = -40;
    private static int pstCounter = 0;

    private Network network;
    private RangeActionActivationResult rangeActionActivationResult;
    private LinearOptimizationResult linearOptimizationResult;
    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private PstRangeAction pstRangeAction;
    private State optimizedState;
    private OptimizationPerimeter optimizationPerimeter;
    private RangeActionSetpointResult rangeActionSetpointResult;

    @BeforeEach
    public void setUp() {
        cnec1 = Mockito.mock(FlowCnec.class);
        when(cnec1.getMonitoredSides()).thenReturn(Collections.singleton(TwoSides.ONE));
        cnec2 = Mockito.mock(FlowCnec.class);
        when(cnec2.getMonitoredSides()).thenReturn(Collections.singleton(TwoSides.TWO));
        network = Mockito.mock(Network.class);

        linearOptimizationResult = mock(LinearOptimizationResult.class);
        when(linearOptimizationResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2));

        when(linearOptimizationResult.getFlow(cnec1, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(REF_FLOW_1);
        when(linearOptimizationResult.getFlow(cnec2, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(REF_FLOW_2);

        rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        pstRangeAction = createPst();
        optimizedState = Mockito.mock(State.class);
        when(optimizedState.getContingency()).thenReturn(Optional.empty());
        Instant preventiveInstant = Mockito.mock(Instant.class);
        when(optimizedState.getInstant()).thenReturn(preventiveInstant);
        optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        when(optimizationPerimeter.getMainOptimizationState()).thenReturn(optimizedState);
        when(optimizationPerimeter.getRangeActionOptimizationStates()).thenReturn(Set.of(optimizedState));
        rangeActionSetpointResult = Mockito.mock(RangeActionSetpointResult.class);
        when(rangeActionActivationResult.getOptimizedSetpointsOnState(optimizedState)).thenReturn(Map.of(pstRangeAction, 0.));
    }

    private void setSensitivityValues(PstRangeAction pstRangeAction) {
        when(linearOptimizationResult.getSensitivityValue(cnec1, TwoSides.ONE, pstRangeAction, Unit.MEGAWATT)).thenReturn(SENSI_1);
        when(linearOptimizationResult.getSensitivityValue(cnec2, TwoSides.TWO, pstRangeAction, Unit.MEGAWATT)).thenReturn(SENSI_2);
    }

    private void mockPstRangeAction(PstRangeAction pstRangeAction) {
        Validable.MessageHeader messageHeader = new DefaultMessageHeader("PST", "1");
        when(pstRangeAction.convertTapToAngle(-3)).thenThrow(new ValidationException(() -> messageHeader, "Out of bound"));
        when(pstRangeAction.convertTapToAngle(-2)).thenReturn(-2.5);
        when(pstRangeAction.convertTapToAngle(-1)).thenReturn(-0.75);
        when(pstRangeAction.convertTapToAngle(0)).thenReturn(0.);
        when(pstRangeAction.convertTapToAngle(1)).thenReturn(0.75);
        when(pstRangeAction.convertTapToAngle(2)).thenReturn(2.5);
        when(pstRangeAction.convertTapToAngle(3)).thenThrow(new ValidationException(() -> messageHeader, "Out of bound"));
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
        when(cnec1.computeMargin(flow, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(margin);
    }

    private void mockMarginOnCnec2(PstRangeAction pstRangeAction, int tap, double margin) {
        double flow = REF_FLOW_2 + (pstRangeAction.convertTapToAngle(tap) - INITIAL_PST_SET_POINT) * SENSI_2;
        when(cnec2.computeMargin(flow, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(margin);
    }

    private Map<Integer, Double> computeMinMarginsForBestTaps(double startingSetPoint) {
        return BestTapFinder.computeMinMarginsForBestTaps(
            network,
            pstRangeAction,
            startingSetPoint,
            linearOptimizationResult,
            Unit.MEGAWATT
        );
    }

    private RangeActionActivationResult computeUpdatedRangeActionResult() {
        RangeActionActivationResultImpl roundedResult = BestTapFinder.round(
            rangeActionActivationResult,
            network,
            optimizationPerimeter,
            rangeActionSetpointResult,
            linearOptimizationResult,
            Unit.MEGAWATT
        );
        roundOtherRas(rangeActionActivationResult, optimizationPerimeter, roundedResult);
        return roundedResult;
    }

    private PstRangeAction createPstWithGroupId(String groupId) {
        PstRangeAction pst = createPst();
        when(pst.getGroupId()).thenReturn(Optional.of(groupId));
        return pst;
    }

    private PstRangeAction createPst() {
        PstRangeAction pst = Mockito.mock(PstRangeAction.class);
        when(pst.getCurrentSetpoint(network)).thenReturn(INITIAL_PST_SET_POINT);
        when(pst.getId()).thenReturn("pst" + pstCounter);
        NetworkElement networkElement = Mockito.mock(NetworkElement.class);
        when(networkElement.getId()).thenReturn("pstNE" + pstCounter++);
        when(pst.getNetworkElements()).thenReturn(Set.of(networkElement));
        mockPstRangeAction(pst);
        setSensitivityValues(pst);
        return pst;
    }

    @Test
    void testMarginWhenTheSetPointIsTooFarFromTheMiddle() {
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
    void testMarginsWithOtherTapDecreasingTheMinMargin() {
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
    void testMarginsWithOtherTapIncreasingTheMinMargin() {
        // Set point is close enough to the middle of the range between tap 1 and 2, so we consider the two taps
        // The closest tap is still 1, and the other tap increases the margin so it is considered
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
    void testMarginsWithOtherTapIncreasingTheMinMarginWithNegativeMargins() {
        // Set point is close enough to the middle of the range between tap 1 and 2, so we consider the two taps
        // The closest tap is still 1, and the next tap increase the margin so it is considered
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
    void testMarginsWithOtherTapIncreasingTheMinMarginOnUpperBound() {
        // Set point is close enough to the middle of the range between tap 1 and 2, so we consider the two taps
        // The closest tap is 2 which is the upper bound, and the other tap increases the margin so it is considered
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
    void testMarginsWithOtherTapIncreasingTheMinMarginOnLowerBound() {
        // Set point is close enough to the middle of the range between tap -1 and -2, so we consider the two taps
        // The closest tap is -2 which is the lower bound, and the other tap increases the margin so it is considered
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
    void testComputeBestTapPerPstGroup() {
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
    void testUpdatedRangeActionResultWithOtherTapSelected() {
        double startingSetPoint = 0.;
        double notRoundedSetpoint = 1.7;
        setClosestTapPosition(pstRangeAction, notRoundedSetpoint, 2);
        setMarginsForTap(pstRangeAction, 1, 140, 150); // Tap 1 should be selected because min margin is 140
        setMarginsForTap(pstRangeAction, 2, 150, 120);

        RangeAction<?> activatedRangeActionOtherThanPst = Mockito.mock(RangeAction.class);
        when(activatedRangeActionOtherThanPst.getId()).thenReturn("notPst");
        NetworkElement networkElementOtherThanPst = Mockito.mock(NetworkElement.class);
        when(networkElementOtherThanPst.getId()).thenReturn("notPstNE");
        when(activatedRangeActionOtherThanPst.getNetworkElements()).thenReturn(Set.of(networkElementOtherThanPst));

        rangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(
            pstRangeAction, startingSetPoint,
            activatedRangeActionOtherThanPst, startingSetPoint
        ));
        rangeActionActivationResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        ((RangeActionActivationResultImpl) rangeActionActivationResult).putResult(pstRangeAction, optimizedState, notRoundedSetpoint);
        ((RangeActionActivationResultImpl) rangeActionActivationResult).putResult(activatedRangeActionOtherThanPst, optimizedState, 200.);
        when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(Map.of(
            optimizedState, Set.of(pstRangeAction, activatedRangeActionOtherThanPst)
        ));

        RangeActionActivationResult updatedRangeActionActivationResult = computeUpdatedRangeActionResult();

        assertEquals(0.75, updatedRangeActionActivationResult.getOptimizedSetpoint(pstRangeAction, optimizedState), DOUBLE_TOLERANCE);
        assertEquals(200., updatedRangeActionActivationResult.getOptimizedSetpoint(activatedRangeActionOtherThanPst, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    void testUpdatedRangeActionResultWithClosestTapSelected() {
        double startingSetPoint = 0.;
        double notRoundedSetpoint = 1.7;
        setClosestTapPosition(pstRangeAction, notRoundedSetpoint, 2);
        setMarginsForTap(pstRangeAction, 1, 120, 150);
        setMarginsForTap(pstRangeAction, 2, 150, 140); // Tap 2 should be selected because min margin is 140

        RangeAction<?> activatedRangeActionOtherThanPst = Mockito.mock(RangeAction.class);
        when(activatedRangeActionOtherThanPst.getId()).thenReturn("notPst");
        NetworkElement networkElementOtherThanPst = Mockito.mock(NetworkElement.class);
        when(networkElementOtherThanPst.getId()).thenReturn("notPstNE");
        when(activatedRangeActionOtherThanPst.getNetworkElements()).thenReturn(Set.of(networkElementOtherThanPst));

        rangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(
            pstRangeAction, startingSetPoint,
            activatedRangeActionOtherThanPst, startingSetPoint
        ));
        rangeActionActivationResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        ((RangeActionActivationResultImpl) rangeActionActivationResult).putResult(pstRangeAction, optimizedState, notRoundedSetpoint);
        ((RangeActionActivationResultImpl) rangeActionActivationResult).putResult(activatedRangeActionOtherThanPst, optimizedState, 200.);
        when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(Map.of(
            optimizedState, Set.of(pstRangeAction, activatedRangeActionOtherThanPst)
        ));

        RangeActionActivationResult updatedRangeActionActivationResult = computeUpdatedRangeActionResult();

        assertEquals(2.5, updatedRangeActionActivationResult.getOptimizedSetpoint(pstRangeAction, optimizedState), DOUBLE_TOLERANCE);
        assertEquals(200., updatedRangeActionActivationResult.getOptimizedSetpoint(activatedRangeActionOtherThanPst, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    void testUpdatedRangeActionResultNoOptimizationOfTheTap() {
        double startingSetPoint = 0.;
        double notRoundedSetpoint = 0.8;
        // Starting point is really close to set point of tap 1 so it will be set to tap 1
        setClosestTapPosition(pstRangeAction, notRoundedSetpoint, 1);
        setMarginsForTap(pstRangeAction, 1, 120, 150);
        setMarginsForTap(pstRangeAction, 2, 150, 140); // Tap 2 would be ignored even if result is better

        RangeAction<?> activatedRangeActionOtherThanPst = Mockito.mock(RangeAction.class);
        when(activatedRangeActionOtherThanPst.getId()).thenReturn("notPst");
        NetworkElement networkElementOtherThanPst = Mockito.mock(NetworkElement.class);
        when(networkElementOtherThanPst.getId()).thenReturn("notPstNE");
        when(activatedRangeActionOtherThanPst.getNetworkElements()).thenReturn(Set.of(networkElementOtherThanPst));

        rangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(
            pstRangeAction, startingSetPoint,
            activatedRangeActionOtherThanPst, startingSetPoint
        ));
        rangeActionActivationResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        ((RangeActionActivationResultImpl) rangeActionActivationResult).putResult(pstRangeAction, optimizedState, notRoundedSetpoint);
        ((RangeActionActivationResultImpl) rangeActionActivationResult).putResult(activatedRangeActionOtherThanPst, optimizedState, 200.);
        when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(Map.of(
            optimizedState, Set.of(pstRangeAction, activatedRangeActionOtherThanPst)
        ));

        RangeActionActivationResult updatedRangeActionActivationResult = computeUpdatedRangeActionResult();

        assertEquals(0.75, updatedRangeActionActivationResult.getOptimizedSetpoint(pstRangeAction, optimizedState), DOUBLE_TOLERANCE);
        assertEquals(200., updatedRangeActionActivationResult.getOptimizedSetpoint(activatedRangeActionOtherThanPst, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    void testUpdatedRangeActionResultWithGroups() {
        double startingSetPoint = 0.;
        double notRoundedSetpoint = 0.8;
        setClosestTapPosition(pstRangeAction, notRoundedSetpoint, 2);
        setMarginsForTap(pstRangeAction, 1, 120, 150);
        setMarginsForTap(pstRangeAction, 2, 150, 140); // Tap 2 should be selected because min margin is 140

        PstRangeAction pstGroup1 = createPstWithGroupId("group1");
        PstRangeAction pstGroup2 = createPstWithGroupId("group1");
        double groupNotRoundedSetpoint = -0.4;
        setClosestTapPosition(pstGroup1, groupNotRoundedSetpoint, -1);
        setMarginsForTap(pstGroup1, -1, 120, 150);
        setMarginsForTap(pstGroup1, 0, 150, 140); // Tap 0 should be selected because min margin is 140
        setClosestTapPosition(pstGroup2, groupNotRoundedSetpoint, -1);
        setMarginsForTap(pstGroup2, -1, 120, 150);
        setMarginsForTap(pstGroup2, 0, 150, 140); // Tap 0 should be selected because min margin is 140

        rangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(
            pstRangeAction, startingSetPoint,
            pstGroup1, startingSetPoint,
            pstGroup2, startingSetPoint
        ));
        rangeActionActivationResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        ((RangeActionActivationResultImpl) rangeActionActivationResult).putResult(pstRangeAction, optimizedState, notRoundedSetpoint);
        ((RangeActionActivationResultImpl) rangeActionActivationResult).putResult(pstGroup1, optimizedState, groupNotRoundedSetpoint);
        ((RangeActionActivationResultImpl) rangeActionActivationResult).putResult(pstGroup2, optimizedState, groupNotRoundedSetpoint);
        when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(Map.of(
            optimizedState, Set.of(pstRangeAction, pstGroup1, pstGroup2)
        ));

        RangeActionActivationResult updatedRangeActionActivationResult = computeUpdatedRangeActionResult();

        assertEquals(2.5, updatedRangeActionActivationResult.getOptimizedSetpoint(pstRangeAction, optimizedState), DOUBLE_TOLERANCE);
        assertEquals(0, updatedRangeActionActivationResult.getOptimizedSetpoint(pstGroup1, optimizedState), DOUBLE_TOLERANCE);
        assertEquals(0, updatedRangeActionActivationResult.getOptimizedSetpoint(pstGroup2, optimizedState), DOUBLE_TOLERANCE);
    }
}
