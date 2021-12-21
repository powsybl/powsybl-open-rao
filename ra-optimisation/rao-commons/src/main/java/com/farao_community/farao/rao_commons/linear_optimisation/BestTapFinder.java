/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.rao_commons.result.RangeActionResultImpl;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.farao_community.farao.rao_commons.result_api.SensitivityResult;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ValidationException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class BestTapFinder {

    private BestTapFinder() {
        // Should not be instantiated
    }

    /**
     * This function computes the best tap positions for PstRangeActions that were optimized in the linear problem.
     * It is a little smarter than just rounding the optimal angle to the closest tap position:
     * if the optimal angle is close to the limit between two tap positions, it will chose the one that maximizes the
     * minimum margin on the 10 most limiting elements (pre-optim)
     * Exception: if choosing the tap that is not the closest one to the optimal angle does not improve the margin
     * enough (current threshold of 10%), then the closest tap is kept
     *
     * @return a map containing the best tap position for every PstRangeAction that was optimized in the linear problem
     */
    public static RangeActionResult find(Map<RangeAction<?>, Double> optimizedSetPoints,
                                         Network network,
                                         List<FlowCnec> mostLimitingCnecs,
                                         FlowResult flowResult,
                                         SensitivityResult sensitivityResult) {
        // TODO: network could be replaced by the previous RangeActionResult
        Map<PstRangeAction, Integer> bestTaps = new HashMap<>();
        Map<PstRangeAction, Map<Integer, Double>> minMarginPerTap = new HashMap<>();

        Set<PstRangeAction> pstRangeActions = optimizedSetPoints.keySet().stream().filter(PstRangeAction.class::isInstance).map(PstRangeAction.class::cast).collect(Collectors.toSet());

        pstRangeActions.forEach(pstRangeAction ->
                minMarginPerTap.put(
                        pstRangeAction,
                        computeMinMarginsForBestTaps(
                                network,
                                pstRangeAction,
                                optimizedSetPoints.get(pstRangeAction),
                                mostLimitingCnecs,
                                flowResult,
                                sensitivityResult)));

        Map<String, Integer> bestTapPerPstGroup = computeBestTapPerPstGroup(minMarginPerTap);

        for (PstRangeAction pstRangeAction : pstRangeActions) {
            Optional<String> optGroupId = pstRangeAction.getGroupId();
            if (optGroupId.isPresent()) {
                bestTaps.put(pstRangeAction, bestTapPerPstGroup.get(optGroupId.get()));
            } else {
                int bestTap = minMarginPerTap.get(pstRangeAction).entrySet().stream().max(Comparator.comparing(Map.Entry<Integer, Double>::getValue)).orElseThrow().getKey();
                bestTaps.put(pstRangeAction, bestTap);
            }
        }

        Map<RangeAction<?>, Double> roundedSetPoints = new HashMap<>(optimizedSetPoints);
        for (var entry : bestTaps.entrySet()) {
            roundedSetPoints.put(entry.getKey(), entry.getKey().convertTapToAngle(entry.getValue()));
        }
        return new RangeActionResultImpl(roundedSetPoints);
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
     * - if the second closest tap position does not improve the margin enough (10% threshold), then only the closest
     * tap is returned with a Double.MAX_VALUE margin
     *
     * @param pstRangeAction:    the PstRangeAction for which we need the best taps and margins
     * @param angle:             the optimal angle computed by the linear problem
     * @param mostLimitingCnecs: the cnecs upon which we compute the minimum margin
     * @return a map containing the minimum margin for each best tap position (one or two taps)
     */
    static Map<Integer, Double> computeMinMarginsForBestTaps(Network network,
                                                             PstRangeAction pstRangeAction,
                                                             double angle,
                                                             List<FlowCnec> mostLimitingCnecs,
                                                             FlowResult flowResult,
                                                             SensitivityResult sensitivityResult) {
        int closestTap = pstRangeAction.convertAngleToTap(angle);
        double closestAngle = pstRangeAction.convertTapToAngle(closestTap);

        Integer otherTap = null;

        // We don't have access to min and max tap positions directly
        // We have access to min and max angles, but angles and taps do not necessarily increase/decrese in the same direction
        // So we have to try/catch in order to know if we're at the tap limits
        boolean testTapPlus1 = true;
        try {
            pstRangeAction.convertTapToAngle(closestTap + 1);
        } catch (FaraoException | ValidationException e) {
            testTapPlus1 = false;
        }
        boolean testTapMinus1 = true;
        try {
            pstRangeAction.convertTapToAngle(closestTap - 1);
        } catch (FaraoException | ValidationException e) {
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

        // Default case
        if (otherTap == null) {
            return Map.of(closestTap, Double.MAX_VALUE);
        }

        double otherAngle = pstRangeAction.convertTapToAngle(otherTap);
        double approxLimitAngle = 0.5 * (closestAngle + otherAngle);
        if (Math.abs(angle - approxLimitAngle) / Math.abs(closestAngle - otherAngle) < 0.15) {
            // Angle is too close to the limit between two tap positions
            // Chose the tap that maximizes the margin on the most limiting element
            Pair<Double, Double> margins = computeMinMargins(network, pstRangeAction, mostLimitingCnecs, closestAngle, otherAngle, flowResult, sensitivityResult);
            // Exception: if choosing the tap that is not the closest one to the optimal angle does not improve the margin
            // enough (current threshold of 10%), then only the closest tap is kept
            // This is actually a workaround that mitigates adverse effects of this rounding on virtual costs
            // TODO : we can remove it when we use cost evaluators directly here
            if (margins.getRight() > margins.getLeft() + 0.1 * Math.abs(margins.getLeft())) {
                return Map.of(closestTap, margins.getLeft(), otherTap, margins.getRight());
            }
        }

        // Default case
        return Map.of(closestTap, Double.MAX_VALUE);
    }

    /**
     * This method estimates the minimum margin upon a given set of cnecs, for two angles of a given PST
     *
     * @param pstRangeAction: the PstRangeAction that we should test on two angles
     * @param flowCnecs:          the set of cnecs to compute the minimum margin
     * @param angle1:         the first angle for the PST
     * @param angle2:         the second angle for the PST
     * @return a pair of two minimum margins (margin for angle1, margin for angle2)
     */
    static Pair<Double, Double> computeMinMargins(Network network,
                                                  PstRangeAction pstRangeAction,
                                                  List<FlowCnec> flowCnecs,
                                                  double angle1,
                                                  double angle2,
                                                  FlowResult flowResult,
                                                  SensitivityResult sensitivityResult) {
        double minMargin1 = Double.MAX_VALUE;
        double minMargin2 = Double.MAX_VALUE;
        for (FlowCnec flowCnec : flowCnecs) {
            double sensitivity = sensitivityResult.getSensitivityValue(flowCnec, pstRangeAction, MEGAWATT);
            double currentSetPoint = pstRangeAction.getCurrentSetpoint(network);
            double referenceFlow = flowResult.getFlow(flowCnec, MEGAWATT);

            double flow1 = sensitivity * (angle1 - currentSetPoint) + referenceFlow;
            double flow2 = sensitivity * (angle2 - currentSetPoint) + referenceFlow;

            minMargin1 = Math.min(minMargin1, flowCnec.computeMargin(flow1, Side.LEFT, MEGAWATT));
            minMargin2 = Math.min(minMargin2, flowCnec.computeMargin(flow2, Side.LEFT, MEGAWATT));
        }
        return Pair.of(minMargin1, minMargin2);
    }
}
