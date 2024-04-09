/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresultjson.serializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.raoresultjson.RaoResultJsonConstants;
import com.fasterxml.jackson.core.JsonGenerator;
import org.jgrapht.alg.util.Pair;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.raoresultjson.RaoResultJsonConstants.*;

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

        jsonGenerator.writeArrayFieldStart(RANGEACTION_RESULTS);
        for (RangeAction<?> rangeAction : sortedListOfRangeActions) {
            serializeRangeActionResult(rangeAction, raoResult, crac, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeRangeActionResult(RangeAction<?> rangeAction, RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(RANGEACTION_ID, rangeAction.getId());

        Double initialSetpoint = safeGetPreOptimizedSetpoint(raoResult, crac.getPreventiveState(), rangeAction);
        if (!Double.isNaN(initialSetpoint)) {
            jsonGenerator.writeNumberField(INITIAL_SETPOINT, initialSetpoint);
        }

        List<State> statesWhenRangeActionIsActivated = crac.getStates().stream()
                .filter(state -> safeIsActivatedDuringState(raoResult, state, rangeAction))
                .sorted(STATE_COMPARATOR)
                .toList();

        Map<State, Pair<Integer, Double>> activatedSetpoints = statesWhenRangeActionIsActivated.stream().collect(Collectors.toMap(
                Function.identity(), state -> Pair.of(safeGetOptimizedTap(raoResult, state, rangeAction), safeGetOptimizedSetpoint(raoResult, state, rangeAction))
        ));
        writeStateToTapAndSetpointArray(jsonGenerator, activatedSetpoints, RaoResultJsonConstants.STATES_ACTIVATED);

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

    static void writeStateToTapAndSetpointArray(JsonGenerator jsonGenerator, Map<State, Pair<Integer, Double>> stateToTapAndSetpoint, String arrayName) throws IOException {
        if (stateToTapAndSetpoint.isEmpty()) {
            return;
        }
        jsonGenerator.writeArrayFieldStart(arrayName);
        for (Map.Entry<State, Pair<Integer, Double>> entry : stateToTapAndSetpoint.entrySet()) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(INSTANT, serializeInstantId(entry.getKey().getInstant()));

            Optional<Contingency> optContingency = entry.getKey().getContingency();
            if (optContingency.isPresent()) {
                jsonGenerator.writeStringField(CONTINGENCY_ID, optContingency.get().getId());
            }

            Double setpoint = entry.getValue().getSecond();
            if (!Double.isNaN(setpoint)) {
                jsonGenerator.writeNumberField(SETPOINT, setpoint);
            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }
}
