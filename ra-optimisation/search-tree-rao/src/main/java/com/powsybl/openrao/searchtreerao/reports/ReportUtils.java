/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.MeasurementRounding.roundValueBasedOnMargin;
import static java.lang.String.format;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class ReportUtils {
    private ReportUtils() {
        // Utility class should not be instantiated
    }

    public static String formatDoubleBasedOnMargin(final double value, final double margin) {
        if (value >= Double.MAX_VALUE) {
            return "+infinity";
        } else if (value <= -Double.MAX_VALUE) {
            return "-infinity";
        } else {
            // Double.toString, similarly to String formatting with Locale.English ensures doubles are written with "." rather than ","
            return Double.toString(roundValueBasedOnMargin(value, margin, 2).doubleValue());
        }
    }

    public static List<FlowCnec> getMostLimitingElements(final ObjectiveFunctionResult objectiveFunctionResult,
                                                         final Set<State> states,
                                                         final int maxNumberOfElements) {
        if (states == null) {
            return objectiveFunctionResult.getMostLimitingElements(maxNumberOfElements);
        } else {
            final List<FlowCnec> cnecs = objectiveFunctionResult.getMostLimitingElements(Integer.MAX_VALUE)
                .stream()
                .filter(cnec -> states.contains(cnec.getState()))
                .toList();
            return cnecs.subList(0, Math.min(cnecs.size(), maxNumberOfElements));
        }
    }

    public static Map<FlowCnec, Double> getMostLimitingElementsAndMargins(final OptimizationResult optimizationResult,
                                                                          final Set<State> states,
                                                                          final Unit unit,
                                                                          final boolean relativePositiveMargins,
                                                                          final int maxNumberOfElements) {
        final Map<FlowCnec, Double> mostLimitingElementsAndMargins = new HashMap<>();
        final List<FlowCnec> cnecs = getMostLimitingElements(optimizationResult, states, maxNumberOfElements);
        cnecs.forEach(cnec -> {
            final double cnecMargin = relativePositiveMargins ? optimizationResult.getRelativeMargin(cnec, unit) : optimizationResult.getMargin(cnec, unit);
            mostLimitingElementsAndMargins.put(cnec, cnecMargin);
        });
        return mostLimitingElementsAndMargins;
    }

    public static String getRaResult(final Set<NetworkAction> networkActions, final Map<RangeAction<?>, java.lang.Double> rangeActions) {
        final long activatedNetworkActions = networkActions.size();
        final long activatedRangeActions = rangeActions.size();
        final String networkActionsNames = StringUtils.join(networkActions.stream().map(Identifiable::getName).collect(Collectors.toSet()), ", ");

        final Set<String> rangeActionsSet = new HashSet<>();
        rangeActions.forEach((key, value) -> rangeActionsSet.add(format("%s: %.0f", key.getName(), value)));
        final String rangeActionsNames = StringUtils.join(rangeActionsSet, ", ");

        if (activatedNetworkActions + activatedRangeActions == 0) {
            return "no remedial actions activated";
        } else if (activatedNetworkActions > 0 && activatedRangeActions == 0) {
            return String.format("%s network action(s) activated : %s", activatedNetworkActions, networkActionsNames);
        } else if (activatedRangeActions > 0 && activatedNetworkActions == 0) {
            return String.format("%s range action(s) activated : %s", activatedRangeActions, rangeActionsNames);
        } else {
            return String.format("%s network action(s) and %s range action(s) activated : %s and %s",
                activatedNetworkActions, activatedRangeActions, networkActionsNames, rangeActionsNames);
        }
    }

    public static String getScenarioName(final State state) {
        return state.getContingency()
            .map(contingency -> contingency.getName().orElse(contingency.getId()))
            .orElse("preventive");
    }
}
