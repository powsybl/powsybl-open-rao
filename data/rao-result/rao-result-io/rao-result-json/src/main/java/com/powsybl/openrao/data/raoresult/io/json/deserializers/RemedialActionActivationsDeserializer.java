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
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case INSTANT -> instant = crac.getInstant(jsonParser.nextTextValue());
                    case CONTINGENCY_ID -> contingency = crac.getContingency(jsonParser.nextTextValue());
                    case TIMESTAMP -> jsonParser.nextToken(); // TODO: use this when a CRAC can be defined on several timestamps
                    case ACTIVATED_REMEDIAL_ACTIONS -> {
                        State state = contingency == null ? crac.getPreventiveState() : crac.getState(contingency, instant);
                        if (state == null) {
                            throw new JsonParseException(jsonParser, "Unknown state.");
                        }
                        jsonParser.nextToken();
                        deserializeActivatedRemedialActionsForState(jsonParser, state, raoResult, crac);
                    }
                    default ->
                        throw new JsonParseException(jsonParser, "Unexpected field in remedialActionActivations: " + jsonParser.currentName());
                }
            }
        }
    }

    private static void deserializeActivatedRemedialActionsForState(JsonParser jsonParser, State state, RaoResultImpl raoResult, Crac crac) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            RemedialAction<?> remedialAction = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case ID -> remedialAction = crac.getRemedialAction(jsonParser.nextTextValue());
                    case SET_POINT -> {
                        if (remedialAction == null) {
                            throw new JsonParseException(jsonParser, "Set-point defined without remedialAction in remedialActionActivations.");
                        } else if (remedialAction instanceof StandardRangeAction<?> standardRangeAction) {
                            jsonParser.nextToken();
                            raoResult.getAndCreateIfAbsentRangeActionResult(standardRangeAction).addActivationForState(state, jsonParser.getDoubleValue());
                        } else {
                            throw new JsonParseException(jsonParser, "Cannot define a set-point for remedial action '%s' because is not a standard range action.".formatted(remedialAction.getId()));
                        }
                    }
                    case TAP -> {
                        if (remedialAction == null) {
                            throw new JsonParseException(jsonParser, "Tap defined without remedialAction in remedialActionActivations.");
                        } else if (remedialAction instanceof PstRangeAction pstRangeAction) {
                            jsonParser.nextToken();
                            raoResult.getAndCreateIfAbsentRangeActionResult(pstRangeAction)
                                .addActivationForState(state, pstRangeAction.convertTapToAngle(jsonParser.getIntValue()));
                        } else {
                            throw new JsonParseException(jsonParser, "Cannot define a tap for remedial action '%s' because is not a PST range action.".formatted(remedialAction.getId()));
                        }
                    }
                    default ->
                        throw new JsonParseException(jsonParser, "Unexpected field in remedialActionActivations: " + jsonParser.currentName());
                }
            }
            if (remedialAction instanceof NetworkAction networkAction) {
                raoResult.getAndCreateIfAbsentNetworkActionResult(networkAction).addActivationForState(state);
            }
        }
    }
}
