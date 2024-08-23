/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ValidationException;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
public final class BestTapFinderMultiTS {

    private BestTapFinderMultiTS() {
        // Should not be instantiated
    }

    /**
     * This function computes the best tap positions for PstRangeActions that were optimized in the linear problem.
     * It is a little smarter than just rounding the optimal angle to the closest tap position:
     * if the optimal angle is close to the limit between two tap positions, it will chose the one that maximizes the
     * minimum margin on the 10 most limiting elements (pre-optim)
     * If virtual costs are an important part of the optimization, it is highly recommended to use APPROXIMATED_INTEGERS
     * taps in the linear optimization, rather than relying on the best tap finder to round the taps.
     *
     * @return a map containing the best tap position for every PstRangeAction that was optimized in the linear problem
     */
    public static RangeActionActivationResult round(RangeActionActivationResult linearProblemResult,
                                                    List<Network> networks,
                                                    List<OptimizationPerimeter> optimizationPerimeters,
                                                    RangeActionSetpointResult prePerimeterSetpoint,
                                                    LinearOptimizationResult linearOptimizationResult,
                                                    Unit unit) {

        RangeActionActivationResultImpl roundedResult = new RangeActionActivationResultImpl(prePerimeterSetpoint);
        findBestTapOfPstRangeActions(linearProblemResult, networks, optimizationPerimeters, linearOptimizationResult, roundedResult, unit);
        roundOtherRa(linearProblemResult, optimizationPerimeters, roundedResult);
        return roundedResult;
    }

    private static void findBestTapOfPstRangeActions(RangeActionActivationResult linearProblemResult,
                                                     List<Network> networks,
                                                     List<OptimizationPerimeter> optimizationPerimeters,
                                                     LinearOptimizationResult linearOptimizationResult,
                                                     RangeActionActivationResultImpl roundedResult,
                                                     Unit unit) {

        for (int i = 0; i < optimizationPerimeters.size(); i++) {
            OptimizationPerimeter optimizationPerimeter = optimizationPerimeters.get(i);
            for (State state : optimizationPerimeter.getRangeActionOptimizationStates()) {

                Map<PstRangeAction, Map<Integer, Double>> minMarginPerTap = new HashMap<>();

                int finalI = i;
                optimizationPerimeter.getRangeActionsPerState().get(state).stream()
                    .filter(PstRangeAction.class::isInstance)
                    .map(PstRangeAction.class::cast)
                    .forEach(pstRangeAction -> minMarginPerTap.put(pstRangeAction, computeMinMarginsForBestTaps(networks.get(finalI), pstRangeAction, linearProblemResult.getOptimizedSetpoint(pstRangeAction, state), linearOptimizationResult, unit)));

                Map<String, Integer> bestTapPerPstGroup = computeBestTapPerPstGroup(minMarginPerTap);

                for (RangeAction<?> rangeAction : optimizationPerimeter.getRangeActionsPerState().get(state)) {
                    if (rangeAction instanceof PstRangeAction pstRangeAction && linearProblemResult.getActivatedRangeActions(state).contains(rangeAction)) {
                        Optional<String> optGroupId = pstRangeAction.getGroupId();
                        if (optGroupId.isPresent()) {
                            roundedResult.activate(pstRangeAction, state, pstRangeAction.convertTapToAngle(bestTapPerPstGroup.get(optGroupId.get())));
                        } else {
                            int bestTap = minMarginPerTap.get(pstRangeAction).entrySet().stream().max(Comparator.comparing(Map.Entry<Integer, Double>::getValue)).orElseThrow().getKey();
                            roundedResult.activate(pstRangeAction, state, pstRangeAction.convertTapToAngle(bestTap));
                        }
                    }
                }
            }
        }
    }

    /**
     * This function computes, for every group of PSTs, the common tap position that maximizes the minimum margin
     *
     * @param minMarginPerTap: a map containing for each PstRangeAction, a map with tap positions and resulting minimum margin
     * @return a map containing for each group ID, the best common tap position for the PSTs
     */
    static Map<String, Integer> computeBestTapPerPstGroup(Map<PstRangeAction, Map<Integer, Double>> minMarginPerTap) {
        Map<String, Integer> bestTapPerPstGroup = new HashMap<>();
        Set<PstRangeAction> pstRangeActions = minMarginPerTap.keySet();
        Set<String> pstGroups = pstRangeActions.stream().map(PstRangeAction::getGroupId).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
        for (String pstGroup : pstGroups) {
            Set<PstRangeAction> pstsOfGroup = pstRangeActions.stream()
                .filter(pstRangeAction -> pstRangeAction.getGroupId().isPresent() && pstRangeAction.getGroupId().get().equals(pstGroup))
                .collect(Collectors.toSet());
            Map<Integer, Double> groupMinMarginPerTap = new HashMap<>();
            for (PstRangeAction pstRangeAction : pstsOfGroup) {
                Map<Integer, Double> pstMinMarginPerTap = minMarginPerTap.get(pstRangeAction);
                for (Map.Entry<Integer, Double> entry : pstMinMarginPerTap.entrySet()) {
                    int tap = entry.getKey();
                    if (groupMinMarginPerTap.containsKey(tap)) {
                        groupMinMarginPerTap.put(tap, Math.min(entry.getValue(), groupMinMarginPerTap.get(tap)));
                    } else {
                        groupMinMarginPerTap.put(tap, entry.getValue());
                    }
                }
            }
            int bestGroupTap = groupMinMarginPerTap.entrySet().stream().max(Comparator.comparing(Map.Entry<Integer, Double>::getValue)).orElseThrow().getKey();
            bestTapPerPstGroup.put(pstGroup, bestGroupTap);
        }
        return bestTapPerPstGroup;
    }

    /**
     * This function computes the best tap positions for an optimized PST range action, using the optimal angle
     * computed by the linear problem
     * It first chooses the closest tap position to the angle, then the second closest one, if the angle is close enough
     * (15% threshold) to the limit between two tap positions
     * It computes the minimum margin among the most limiting cnecs for both tap positions and returns them in a map
     * Exceptions:
     * - if the closest tap position is at a min or max limit, and the angle is close to the angle limit, then only
     * the closest tap is returned. The margin is not computed but replaced with Double.MAX_VALUE
     * - if the angle is not close enough to the limit between two tap positions, only the closest tap is returned
     * with a Double.MAX_VALUE margin
     *
     * @param pstRangeAction:           the PstRangeAction for which we need the best taps and margins
     * @param angle:                    the optimal angle computed by the linear problem
     * @param linearOptimizationResult: allows to get flow & sensitivity values, as well as most limiting flow CNECs
     * @param unit:                     the unit of the evaluators (MW or A)
     * @return a map containing the minimum margin for each best tap position (one or two taps)
     */
    static Map<Integer, Double> computeMinMarginsForBestTaps(Network network,
                                                             PstRangeAction pstRangeAction,
                                                             double angle,
                                                             LinearOptimizationResult linearOptimizationResult,
                                                             Unit unit) {
        int closestTap = pstRangeAction.convertAngleToTap(angle);
        double closestAngle = pstRangeAction.convertTapToAngle(closestTap);

        Integer otherTap = findOtherTap(pstRangeAction, angle, closestTap, closestAngle);

        // Default case
        if (otherTap == null) {
            return Map.of(closestTap, Double.MAX_VALUE);
        }

        double otherAngle = pstRangeAction.convertTapToAngle(otherTap);
        double approxLimitAngle = 0.5 * (closestAngle + otherAngle);
        if (Math.abs(angle - approxLimitAngle) / Math.abs(closestAngle - otherAngle) < 0.15) {
            // Angle is too close to the limit between two tap positions
            // Chose the tap that maximizes the margin on the most limiting element
            Pair<Double, Double> margins = computeMinMargins(network, pstRangeAction, closestAngle, otherAngle, linearOptimizationResult, unit);
            if (margins.getRight() > margins.getLeft()) {
                return Map.of(closestTap, margins.getLeft(), otherTap, margins.getRight());
            }
        }

        // Default case
        return Map.of(closestTap, Double.MAX_VALUE);
    }

    /**
     * Given a PST, an angle (not equal to a whole tap) and the closest whole tap & angle, this method finds
     * the other closest tap (+1 or -1) that is also close to the angle
     */
    private static Integer findOtherTap(PstRangeAction pstRangeAction, double angle, int closestTap, double closestAngle) {
        Integer otherTap = null;

        // We don't have access to min and max tap positions directly
        // We have access to min and max angles, but angles and taps do not necessarily increase/decrese in the same direction
        // So we have to try/catch in order to know if we're at the tap limits
        boolean testTapPlus1 = true;
        try {
            pstRangeAction.convertTapToAngle(closestTap + 1);
        } catch (OpenRaoException | ValidationException e) {
            testTapPlus1 = false;
        }
        boolean testTapMinus1 = true;
        try {
            pstRangeAction.convertTapToAngle(closestTap - 1);
        } catch (OpenRaoException | ValidationException e) {
            testTapMinus1 = false;
        }

        if (testTapPlus1 && testTapMinus1) {
            // We can test tap+1 and tap-1
            double angleOfTapPlus1 = pstRangeAction.convertTapToAngle(closestTap + 1);
            otherTap = (Math.signum(angleOfTapPlus1 - closestAngle) * Math.signum(angle - closestAngle) > 0) ? closestTap + 1 : closestTap - 1;
        } else if (testTapPlus1) {
            // We can only test tap+1, if the optimal angle is between the closest angle and the angle of tap+1
            double angleOfTapPlus1 = pstRangeAction.convertTapToAngle(closestTap + 1);
            if (Math.signum(angleOfTapPlus1 - closestAngle) * Math.signum(angle - closestAngle) > 0) {
                otherTap = closestTap + 1;
            }
        } else if (testTapMinus1) {
            // We can only test tap-1, if the optimal angle is between the closest angle and the angle of tap-1
            double angleOfTapMinus1 = pstRangeAction.convertTapToAngle(closestTap - 1);
            if (Math.signum(angleOfTapMinus1 - closestAngle) * Math.signum(angle - closestAngle) > 0) {
                otherTap = closestTap - 1;
            }
        }
        return otherTap;
    }

    /**
     * This method estimates the minimum margin upon a given set of cnecs, for two angles of a given PST
     *
     * @param pstRangeAction:           the PstRangeAction that we should test on two angles
     * @param angle1:                   the first angle for the PST
     * @param angle2:                   the second angle for the PST
     * @param linearOptimizationResult: allows to get flow & sensitivity values, as well as most limiting flow CNECs
     * @param unit:                     the unit of the evalutors (MW or A)
     * @return a pair of two minimum margins (margin for angle1, margin for angle2)
     */
    static Pair<Double, Double> computeMinMargins(Network network,
                                                  PstRangeAction pstRangeAction,
                                                  double angle1,
                                                  double angle2,
                                                  LinearOptimizationResult linearOptimizationResult,
                                                  Unit unit) {
        double minMargin1 = Double.MAX_VALUE;
        double minMargin2 = Double.MAX_VALUE;
        for (FlowCnec flowCnec : linearOptimizationResult.getMostLimitingElements(10)) {
            for (Side side : flowCnec.getMonitoredSides()) {
                double sensitivity = linearOptimizationResult.getSensitivityValue(flowCnec, side, pstRangeAction, MEGAWATT);
                double currentSetPoint = pstRangeAction.getCurrentSetpoint(network);
                double referenceFlow = linearOptimizationResult.getFlow(flowCnec, side, unit) * RaoUtil.getFlowUnitMultiplier(flowCnec, side, unit, MEGAWATT);

                double flow1 = sensitivity * (angle1 - currentSetPoint) + referenceFlow;
                double flow2 = sensitivity * (angle2 - currentSetPoint) + referenceFlow;

                minMargin1 = Math.min(minMargin1, flowCnec.computeMargin(flow1, side, MEGAWATT));
                minMargin2 = Math.min(minMargin2, flowCnec.computeMargin(flow2, side, MEGAWATT));
            }
        }
        return Pair.of(minMargin1, minMargin2);
    }

    private static void roundOtherRa(RangeActionActivationResult linearProblemResult,
                                     List<OptimizationPerimeter> optimizationPerimeters,
                                     RangeActionActivationResultImpl roundedResult) {

        optimizationPerimeters.forEach(optimizationPerimeter -> optimizationPerimeter.getRangeActionsPerState().forEach((state, rangeActions) ->
            rangeActions.stream()
                .filter(ra -> !(ra instanceof PstRangeAction))
                .filter(ra -> linearProblemResult.getActivatedRangeActions(state).contains(ra))
                .forEach(ra -> roundedResult.activate(ra, state, Math.round(linearProblemResult.getOptimizedSetpoint(ra, state))))));
    }
}
