/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.ACTIVATED_REMEDIAL_ACTIONS;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.CONTINGENCY_ID;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.ID;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.INSTANT;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.REMEDIAL_ACTION_ACTIVATIONS;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.SET_POINT;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.TAP;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.TIMESTAMP;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
final class RemedialActionActivationsSerializer {
    private RemedialActionActivationsSerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {
        Map<State, List<RemedialAction<?>>> activatedRemedialActionsPerState = getActivatedRemedialActionsPerState(raoResult, crac);
        if (activatedRemedialActionsPerState.isEmpty()) {
            return;
        }
        jsonGenerator.writeArrayFieldStart(REMEDIAL_ACTION_ACTIVATIONS);
        for (State state : sort(activatedRemedialActionsPerState.keySet())) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(INSTANT, state.getInstant().getId());
            Optional<Contingency> contingency = state.getContingency();
            if (contingency.isPresent()) {
                jsonGenerator.writeStringField(CONTINGENCY_ID, contingency.get().getId());
            }
            Optional<OffsetDateTime> timestamp = state.getTimestamp();
            if (timestamp.isPresent()) {
                jsonGenerator.writeStringField(TIMESTAMP, DateTimeFormatter.ISO_DATE_TIME.format(timestamp.get()));
            }
            jsonGenerator.writeArrayFieldStart(ACTIVATED_REMEDIAL_ACTIONS);
            for (RemedialAction<?> remedialAction : activatedRemedialActionsPerState.get(state)) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField(ID, remedialAction.getId());
                if (remedialAction instanceof PstRangeAction pstRangeAction) {
                    jsonGenerator.writeNumberField(TAP, raoResult.getOptimizedTapOnState(state, pstRangeAction));
                } else if (remedialAction instanceof RangeAction<?> rangeAction) {
                    jsonGenerator.writeNumberField(SET_POINT, raoResult.getOptimizedSetPointOnState(state, rangeAction));
                }
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    private static Map<State, List<RemedialAction<?>>> getActivatedRemedialActionsPerState(RaoResult raoResult, Crac crac) {
        Map<State, List<RemedialAction<?>>> activatedRemedialActionsPerState = new HashMap<>();
        for (State state : crac.getStates()) {
            Set<RemedialAction<?>> activatedRemedialActions = new HashSet<>(raoResult.getActivatedNetworkActionsDuringState(state));
            activatedRemedialActions.addAll(raoResult.getActivatedRangeActionsDuringState(state));
            if (!activatedRemedialActions.isEmpty()) {
                activatedRemedialActionsPerState.put(state, activatedRemedialActions.stream().sorted(Comparator.comparing(RemedialAction::getId)).toList());
            }
        }
        return activatedRemedialActionsPerState;
    }

    private static List<State> sort(Set<State> states) {
        return states.stream()
            .sorted(RemedialActionActivationsSerializer::compare)
            .toList();
    }

    private static int compare(State state1, State state2) {
        if (areTimestampsEqual(state1, state2)) {
            return compareForSameTimestamp(state1, state2);
        }
        OffsetDateTime timestamp1 = state1.getTimestamp().orElseThrow(); // timestamp is necessarily present
        OffsetDateTime timestamp2 = state2.getTimestamp().orElseThrow(); // timestamp is necessarily present
        return timestamp1.compareTo(timestamp2);
    }

    private static boolean areTimestampsEqual(State state1, State state2) {
        Optional<OffsetDateTime> timestamp1 = state1.getTimestamp();
        Optional<OffsetDateTime> timestamp2 = state2.getTimestamp();
        if (timestamp1.isPresent() && timestamp2.isPresent()) {
            return timestamp1.get().equals(timestamp2.get());
        } else if (timestamp1.isEmpty() && timestamp2.isEmpty()) {
            return true;
        }
        throw new OpenRaoException("Cannot compare a timestamped state with a non-timestamped state");
    }

    private static int compareForSameTimestamp(State state1, State state2) {
        Instant instant1 = state1.getInstant();
        Instant instant2 = state2.getInstant();
        if (instant1.equals(instant2)) {
            Optional<Contingency> contingency1 = state1.getContingency();
            Optional<Contingency> contingency2 = state2.getContingency();
            if (contingency1.isPresent() && contingency2.isPresent()) {
                return contingency1.get().getId().compareTo(contingency2.get().getId());
            }
            return 0;
        }
        return instant1.compareTo(instant2);
    }
}
