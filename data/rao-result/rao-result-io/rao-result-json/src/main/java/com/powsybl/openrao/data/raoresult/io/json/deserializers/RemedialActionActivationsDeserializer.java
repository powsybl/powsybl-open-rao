/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.StandardRangeAction;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.ACTIVATED_REMEDIAL_ACTIONS;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.CONTINGENCY_ID;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.ID;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.INSTANT;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.SET_POINT;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.TAP;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.TIMESTAMP;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
final class RemedialActionActivationsDeserializer {
    private RemedialActionActivationsDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            Instant instant = null;
            Contingency contingency = null;
            Map<PstRangeAction, Integer> activatedPstRangeActions = new HashMap<>();
            Map<StandardRangeAction<?>, Double> activatedStandardRangeActions = new HashMap<>();
            Set<NetworkAction> activatedNetworkActions = new HashSet<>();
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case INSTANT -> instant = crac.getInstant(jsonParser.nextTextValue());
                    case CONTINGENCY_ID -> contingency = crac.getContingency(jsonParser.nextTextValue());
                    case TIMESTAMP ->
                        jsonParser.nextToken(); // TODO: use this when a CRAC can be defined on several timestamps
                    case ACTIVATED_REMEDIAL_ACTIONS -> {
                        jsonParser.nextToken();
                        deserializeActivatedRemedialActionsForState(jsonParser, crac, activatedPstRangeActions, activatedStandardRangeActions, activatedNetworkActions);
                    }
                    default ->
                        throw new JsonParseException(jsonParser, "Unexpected field in remedialActionActivations: " + jsonParser.currentName());
                }
            }
            State state = contingency == null ? crac.getPreventiveState() : crac.getState(contingency, instant);
            if (state == null) {
                throw new JsonParseException(jsonParser, "Unknown state.");
            }
            activatedPstRangeActions.forEach(((pstRangeAction, tap) ->
                raoResult.getAndCreateIfAbsentRangeActionResult(pstRangeAction).addActivationForState(state, pstRangeAction.convertTapToAngle(tap))));
            activatedStandardRangeActions.forEach(((standardRangeAction, setPoint) ->
                raoResult.getAndCreateIfAbsentRangeActionResult(standardRangeAction).addActivationForState(state, setPoint)));
            activatedNetworkActions.forEach(networkAction ->
                raoResult.getAndCreateIfAbsentNetworkActionResult(networkAction).addActivationForState(state));
        }
    }

    private static void deserializeActivatedRemedialActionsForState(JsonParser jsonParser,
                                                                    Crac crac,
                                                                    Map<PstRangeAction, Integer> activatedPstRangeActions,
                                                                    Map<StandardRangeAction<?>, Double> activatedStandardRangeActions,
                                                                    Set<NetworkAction> activatedNetworkActions) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            RemedialAction<?> remedialAction = null;
            Integer tap = null;
            Double setPoint = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case ID -> remedialAction = crac.getRemedialAction(jsonParser.nextTextValue());
                    case SET_POINT -> {
                        jsonParser.nextToken();
                        setPoint = jsonParser.getDoubleValue();
                    }
                    case TAP -> {
                        jsonParser.nextToken();
                        tap = jsonParser.getIntValue();
                    }
                    default ->
                        throw new JsonParseException(jsonParser, "Unexpected field in remedialActionActivations: " + jsonParser.currentName());
                }
            }
            switch (remedialAction) {
                case null -> throw new JsonParseException(jsonParser, "Missing or unknown remedial.");
                case NetworkAction networkAction -> {
                    checkNoTap(remedialAction, tap, jsonParser);
                    checkNoSetPoint(remedialAction, setPoint, jsonParser);
                    activatedNetworkActions.add(networkAction);
                }
                case PstRangeAction pstRangeAction -> {
                    if (tap == null) {
                        throw new JsonParseException(jsonParser, "Missing tap for PST range action '%s'.".formatted(remedialAction.getId()));
                    }
                    checkNoSetPoint(remedialAction, setPoint, jsonParser);
                    activatedPstRangeActions.put(pstRangeAction, tap);
                }
                case StandardRangeAction<?> standardRangeAction -> {
                    checkNoTap(remedialAction, tap, jsonParser);
                    if (setPoint == null) {
                        throw new JsonParseException(jsonParser, "Missing set-point for standard range action '%s'.".formatted(remedialAction.getId()));
                    }
                    activatedStandardRangeActions.put(standardRangeAction, setPoint);
                }
                default -> {
                }
            }
        }
    }

    private static void checkNoTap(RemedialAction<?> remedialAction, Integer tap, JsonParser jsonParser) throws IOException {
        if (tap != null) {
            throw new JsonParseException(jsonParser, "Cannot define a tap for remedial action '%s' because is not a PST range action.".formatted(remedialAction.getId()));
        }
    }

    private static void checkNoSetPoint(RemedialAction<?> remedialAction, Double setPoint, JsonParser jsonParser) throws IOException {
        if (setPoint != null) {
            throw new JsonParseException(jsonParser, "Cannot define a set-point for remedial action '%s' because is not a standard range action.".formatted(remedialAction.getId()));
        }
    }
}
