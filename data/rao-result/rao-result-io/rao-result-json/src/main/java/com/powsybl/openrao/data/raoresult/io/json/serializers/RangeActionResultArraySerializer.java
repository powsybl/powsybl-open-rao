/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresult.io.json.serializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants;
import com.fasterxml.jackson.core.JsonGenerator;
import org.jgrapht.alg.util.Pair;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
final class RangeActionResultArraySerializer {

    private RangeActionResultArraySerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        List<RangeAction<?>> sortedListOfRangeActions = crac.getRangeActions().stream()
            .sorted(Comparator.comparing(RangeAction::getId))
            .toList();

        jsonGenerator.writeArrayFieldStart(RaoResultJsonConstants.RANGEACTION_RESULTS);
        for (RangeAction<?> rangeAction : sortedListOfRangeActions) {
            serializeRangeActionResult(rangeAction, raoResult, crac, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeRangeActionResult(RangeAction<?> rangeAction, RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(RaoResultJsonConstants.RANGEACTION_ID, rangeAction.getId());

        if (rangeAction instanceof PstRangeAction pstRangeAction) {
            jsonGenerator.writeNumberField(RaoResultJsonConstants.INITIAL_TAP, pstRangeAction.getInitialTap());
        } else {
            Double initialSetpoint = safeGetPreOptimizedSetpoint(raoResult, crac.getPreventiveState(), rangeAction);
            if (!Double.isNaN(initialSetpoint)) {
                jsonGenerator.writeNumberField(RaoResultJsonConstants.INITIAL_SETPOINT, initialSetpoint);
            }
        }

        List<State> statesWhenRangeActionIsActivated = crac.getStates().stream()
            .filter(state -> safeIsActivatedDuringState(raoResult, state, rangeAction))
            .sorted(RaoResultJsonConstants.STATE_COMPARATOR)
            .toList();

        Map<State, Pair<Integer, Double>> activatedSetpoints = statesWhenRangeActionIsActivated.stream()
            .collect(Collectors.toMap(
                Function.identity(), state -> Pair.of(safeGetOptimizedTap(raoResult, state, rangeAction),
                    safeGetOptimizedSetpoint(raoResult, state, rangeAction)),
                (x, y) -> x,
                LinkedHashMap::new));
        writeStateToTapAndSetpointArray(jsonGenerator, activatedSetpoints, RaoResultJsonConstants.STATES_ACTIVATED, rangeAction instanceof PstRangeAction);

        jsonGenerator.writeEndObject();
    }

    private static boolean safeIsActivatedDuringState(RaoResult raoResult, State state, RangeAction<?> rangeAction) {
        // isActivatedDuringState might throw an exception, for instance if the RAO was run on one state only, and the
        // state in argument of this method is not the same state.
        try {
            return raoResult.isActivatedDuringState(state, rangeAction);
        } catch (OpenRaoException e) {
            return false;
        }
    }

    private static Integer safeGetOptimizedTap(RaoResult raoResult, State state, RangeAction<?> rangeAction) {
        if (!(rangeAction instanceof PstRangeAction)) {
            return null;
        }
        try {
            return raoResult.getOptimizedTapOnState(state, (PstRangeAction) rangeAction);
        } catch (OpenRaoException e) {
            return null;
        }
    }

    static Double safeGetPreOptimizedSetpoint(RaoResult raoResult, State state, RangeAction<?> rangeAction) {
        if (rangeAction == null) {
            return Double.NaN;
        }
        try {
            return raoResult.getPreOptimizationSetPointOnState(state, rangeAction);
        } catch (OpenRaoException e) {
            return Double.NaN;
        }
    }

    static Double safeGetOptimizedSetpoint(RaoResult raoResult, State state, RangeAction<?> rangeAction) {
        if (rangeAction == null) {
            return Double.NaN;
        }
        try {
            return raoResult.getOptimizedSetPointOnState(state, rangeAction);
        } catch (OpenRaoException e) {
            return Double.NaN;
        }
    }

    static void writeStateToTapAndSetpointArray(JsonGenerator jsonGenerator, Map<State, Pair<Integer, Double>> stateToTapAndSetpoint, String arrayName, boolean isPstRangeAction) throws IOException {
        if (stateToTapAndSetpoint.isEmpty()) {
            return;
        }
        jsonGenerator.writeArrayFieldStart(arrayName);
        for (Map.Entry<State, Pair<Integer, Double>> entry : stateToTapAndSetpoint.entrySet()) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(RaoResultJsonConstants.INSTANT, RaoResultJsonConstants.serializeInstantId(entry.getKey().getInstant()));

            Optional<Contingency> optContingency = entry.getKey().getContingency();
            if (optContingency.isPresent()) {
                jsonGenerator.writeStringField(RaoResultJsonConstants.CONTINGENCY_ID, optContingency.get().getId());
            }

            if (isPstRangeAction) {
                Integer tap = entry.getValue().getFirst();
                if (tap != null) {
                    jsonGenerator.writeNumberField(RaoResultJsonConstants.TAP, tap);
                }
            } else {
                Double setPoint = entry.getValue().getSecond();
                if (!Double.isNaN(setPoint)) {
                    jsonGenerator.writeNumberField(RaoResultJsonConstants.SETPOINT, setPoint);
                }
            }

            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }
}
