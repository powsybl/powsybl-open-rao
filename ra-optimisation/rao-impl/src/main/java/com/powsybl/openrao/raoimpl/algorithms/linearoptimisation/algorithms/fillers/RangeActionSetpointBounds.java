/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.range.TapRange;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.StandardRangeAction;
import com.powsybl.openrao.raoimpl.commons.RaoUtil;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.raoimpl.result.api.RangeActionSetpointResult;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

/**
 * Bounds of the set-point variables of range actions in the linear problem.
 * Shared by the core problem fillers and by anything that needs to reason on the exact same bounds.
 */
public final class RangeActionSetpointBounds {
    public static final double RANGE_ACTION_SETPOINT_EPSILON = 1e-5;

    private RangeActionSetpointBounds() {
    }

    public record Interval(double min, double max) {
    }

    /**
     * Lower and upper bound of the set-point variable S[rangeAction, state]
     */
    public static Interval getSetpointVariableBounds(OptimizationPerimeter perimeter, RangeAction<?> rangeAction, State state, RangeActionSetpointResult prePerimeterSetpoints, double infinity) {
        Pair<RangeAction<?>, State> lastAvailableRangeAction = RaoUtil.getLastAvailableRangeActionOnSameNetworkElement(perimeter, rangeAction, state);
        if (lastAvailableRangeAction == null) {
            // rangeAction could not have been activated in a previous instant
            double prePerimeterSetPoint = prePerimeterSetpoints.getSetpoint(rangeAction);
            double minSetPoint = rangeAction.getMinAdmissibleSetpoint(prePerimeterSetPoint);
            double maxSetPoint = rangeAction.getMaxAdmissibleSetpoint(prePerimeterSetPoint);
            return new Interval(minSetPoint - RANGE_ACTION_SETPOINT_EPSILON, maxSetPoint + RANGE_ACTION_SETPOINT_EPSILON);
        }
        // absolute range
        List<Double> minAndMaxAbsoluteAndRelativeSetpoints = getMinAndMaxAbsoluteAndRelativeSetpoints(rangeAction, infinity);
        return new Interval(minAndMaxAbsoluteAndRelativeSetpoints.get(0) - RANGE_ACTION_SETPOINT_EPSILON, minAndMaxAbsoluteAndRelativeSetpoints.get(1) + RANGE_ACTION_SETPOINT_EPSILON);
    }

    public static List<Double> getMinAndMaxAbsoluteAndRelativeSetpoints(RangeAction<?> rangeAction, double infinity) {

        // if relative to previous instant range
        double minAbsoluteSetpoint = -infinity;
        double maxAbsoluteSetpoint = infinity;
        double minRelativeSetpoint = -infinity;
        double maxRelativeSetpoint = infinity;
        if (rangeAction instanceof PstRangeAction pstRangeAction) {
            Map<Integer, Double> tapToAngleMap = pstRangeAction.getTapToAngleConversionMap();
            List<TapRange> ranges = pstRangeAction.getRanges();

            int minAbsoluteTap = tapToAngleMap.keySet().stream().mapToInt(k -> k).min().orElseThrow();
            int maxAbsoluteTap = tapToAngleMap.keySet().stream().mapToInt(k -> k).max().orElseThrow();
            double minRelativeTap = -infinity;
            double maxRelativeTap = infinity;
            for (TapRange range : ranges) {
                RangeType rangeType = range.getRangeType();
                switch (rangeType) {
                    case ABSOLUTE -> {
                        minAbsoluteTap = Math.max(minAbsoluteTap, range.getMinTap());
                        maxAbsoluteTap = Math.min(maxAbsoluteTap, range.getMaxTap());
                    }
                    case RELATIVE_TO_INITIAL_NETWORK -> {
                        minAbsoluteTap = Math.max(minAbsoluteTap, pstRangeAction.getInitialTap() + range.getMinTap());
                        maxAbsoluteTap = Math.min(maxAbsoluteTap, pstRangeAction.getInitialTap() + range.getMaxTap());
                    }
                    case RELATIVE_TO_PREVIOUS_INSTANT -> {
                        minRelativeTap = Math.max(minRelativeTap, range.getMinTap());
                        maxRelativeTap = Math.min(maxRelativeTap, range.getMaxTap());
                    }
                    default -> throw new OpenRaoException(String.format("Unsupported range type %s", rangeType));
                }
            }
            // The taps are not necessarily in order of increasing angle.
            double setPointMinAbsoluteTap = tapToAngleMap.get(minAbsoluteTap);
            double setPointMaxAbsoluteTap = tapToAngleMap.get(maxAbsoluteTap);
            minAbsoluteSetpoint = Math.min(setPointMinAbsoluteTap, setPointMaxAbsoluteTap);
            maxAbsoluteSetpoint = Math.max(setPointMinAbsoluteTap, setPointMaxAbsoluteTap);
            // Make sure we stay in the range by multiplying the relative tap by the smallest angle between taps.
            // (As long as minRelativeTap is negative (or zero) and maxRelativeTap is positive (or zero).)
            minRelativeSetpoint = minRelativeTap * pstRangeAction.getSmallestAngleStep();
            maxRelativeSetpoint = maxRelativeTap * pstRangeAction.getSmallestAngleStep();
        } else if (rangeAction instanceof StandardRangeAction<?> standardRangeAction) {
            List<StandardRange> ranges = standardRangeAction.getRanges();
            for (StandardRange range : ranges) {
                RangeType rangeType = range.getRangeType();
                switch (rangeType) {
                    case ABSOLUTE -> {
                        minAbsoluteSetpoint = Math.max(minAbsoluteSetpoint, range.getMin());
                        maxAbsoluteSetpoint = Math.min(maxAbsoluteSetpoint, range.getMax());
                    }
                    case RELATIVE_TO_INITIAL_NETWORK -> {
                        minAbsoluteSetpoint = Math.max(minAbsoluteSetpoint, standardRangeAction.getInitialSetpoint() + range.getMin());
                        maxAbsoluteSetpoint = Math.min(maxAbsoluteSetpoint, standardRangeAction.getInitialSetpoint() + range.getMax());
                    }
                    case RELATIVE_TO_PREVIOUS_INSTANT -> {
                        minRelativeSetpoint = Math.max(minRelativeSetpoint, range.getMin());
                        maxRelativeSetpoint = Math.min(maxRelativeSetpoint, range.getMax());
                    }
                    default -> throw new OpenRaoException(String.format("Unsupported range type %s", rangeType));
                }
            }
        } else {
            throw new NotImplementedException("range action type is not supported yet");
        }

        return List.of(minAbsoluteSetpoint, maxAbsoluteSetpoint, minRelativeSetpoint, maxRelativeSetpoint);
    }
}
