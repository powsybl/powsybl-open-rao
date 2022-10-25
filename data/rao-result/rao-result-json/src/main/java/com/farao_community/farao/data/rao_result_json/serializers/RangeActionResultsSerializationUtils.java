/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_json.serializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;
import org.jgrapht.alg.util.Pair;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * Common functions for StandardRangeActionResultArraySerializer & PstRangeActionResultArraySerializer
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class RangeActionResultsSerializationUtils {
    private RangeActionResultsSerializationUtils() {
        // utility class
    }

    static boolean isRangeActionPreventive(RangeAction<?> rangeAction, Crac crac) {
        return isRangeActionAvailableInState(rangeAction, crac.getPreventiveState(), crac);
    }

    static boolean isRangeActionAuto(RangeAction<?> rangeAction, Crac crac) {
        return crac.getStates().stream()
                .filter(state -> state.getInstant().equals(Instant.AUTO))
                .anyMatch(state -> isRangeActionAvailableInState(rangeAction, state, crac));
    }

    static boolean isRangeActionCurative(RangeAction<?> rangeAction, Crac crac) {
        return crac.getStates().stream()
                .filter(state -> state.getInstant().equals(Instant.CURATIVE))
                .anyMatch(state -> isRangeActionAvailableInState(rangeAction, state, crac));
    }

    private static boolean isRangeActionAvailableInState(RangeAction<?> rangeAction, State state, Crac crac) {
        Set<RangeAction<?>> rangeActionsForState = crac.getRangeActions(state, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED);
        return rangeActionsForState.contains(rangeAction);
    }

    static Double safeGetPreOptimizedSetpoint(RaoResult raoResult, State state, RangeAction<?> rangeAction) {
        if (rangeAction == null) {
            return Double.NaN;
        }
        try {
            return raoResult.getPreOptimizationSetPointOnState(state, rangeAction);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    static Double safeGetOptimizedSetpoint(RaoResult raoResult, State state, RangeAction<?> rangeAction) {
        if (rangeAction == null) {
            return Double.NaN;
        }
        try {
            return raoResult.getOptimizedSetPointOnState(state, rangeAction);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    static void writeStateToTapAndSetpointArray(JsonGenerator jsonGenerator, Map<State, Pair<Integer, Double>> stateToTapAndSetpoint, String arrayName) throws IOException {
        jsonGenerator.writeArrayFieldStart(arrayName);
        for (Map.Entry<State, Pair<Integer, Double>> entry : stateToTapAndSetpoint.entrySet()) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(INSTANT, serializeInstant(entry.getKey().getInstant()));

            Optional<Contingency> optContingency = entry.getKey().getContingency();
            if (optContingency.isPresent()) {
                jsonGenerator.writeStringField(CONTINGENCY_ID, optContingency.get().getId());
            }

            Integer tap = entry.getValue().getFirst();
            Double setpoint = entry.getValue().getSecond();
            if (Objects.nonNull(tap)) {
                jsonGenerator.writeNumberField(TAP, tap);
            }
            if (!Double.isNaN(setpoint)) {
                jsonGenerator.writeNumberField(SETPOINT, setpoint);
            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }
}
