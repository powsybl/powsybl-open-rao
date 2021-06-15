/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.rao_result_impl.PstRangeActionResult;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class PstRangeActionResultArrayDeserializer {

    private PstRangeActionResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac) throws IOException {

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(PSTRANGEACTION_ID)) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", PSTRANGEACTION_RESULTS, NETWORKACTION_ID));
            }

            String pstRangeActionId = jsonParser.nextTextValue();
            PstRangeAction pstRangeAction = crac.getPstRangeAction(pstRangeActionId);

            if (pstRangeAction == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: cannot deserialize RaoResult: pstRangeAction with id %s does not exist in the Crac", pstRangeActionId));
            }

            PstRangeActionResult pstRangeActionResult = raoResult.getAndCreateIfAbsentPstRangeActionResult(pstRangeAction);
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    case PST_NETWORKELEMENT_ID:
                        pstRangeActionResult.setPstNetworkElementId(jsonParser.nextTextValue());
                        break;

                    case INITIAL_TAP:
                        jsonParser.nextToken();
                        pstRangeActionResult.setInitialTap(jsonParser.getIntValue());
                        break;

                    case INITIAL_SETPOINT:
                        jsonParser.nextToken();
                        pstRangeActionResult.setInitialSetPoint(jsonParser.getDoubleValue());
                        break;

                    case STATES_ACTIVATED_NETWORKACTION:
                        jsonParser.nextToken();
                        deserializeResultsPerStates(jsonParser, pstRangeActionResult, crac);
                        break;

                    default:
                        throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", PSTRANGEACTION_RESULTS, jsonParser.getCurrentName()));
                }
            }
        }
    }

    private static void deserializeResultsPerStates(JsonParser jsonParser, PstRangeActionResult pstRangeActionResult, Crac crac) throws IOException {

        Instant instant = null;
        String contingencyId = null;
        Double setpoint = null;
        Integer tap = null;
        State state;

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    case INSTANT:
                        instant = deserializeInstant(jsonParser.nextTextValue());
                        break;

                    case CONTINGENCY_ID:
                        contingencyId = jsonParser.nextTextValue();
                        break;

                    case TAP:
                        jsonParser.nextToken();
                        tap = jsonParser.getIntValue();
                        break;

                    case SETPOINT:
                        jsonParser.nextToken();
                        setpoint = jsonParser.getDoubleValue();
                        break;

                    default:
                        throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", PSTRANGEACTION_RESULTS, jsonParser.getCurrentName()));
                }
            }

            if (instant == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: no instant defined in activated states of %s", PSTRANGEACTION_RESULTS));
            }

            if (instant == Instant.PREVENTIVE) {
                state = crac.getPreventiveState();
            } else {
                if (contingencyId == null) {
                    throw new FaraoException(String.format("Cannot deserialize RaoResult: no contingency defined in N-k activated states of %s", PSTRANGEACTION_RESULTS));
                }
                state = crac.getState(contingencyId, instant);
                if (state == null) {
                    throw new FaraoException(String.format("Cannot deserialize RaoResult: State at instant %s with contingency %s not found in Crac", instant, contingencyId));
                }
            }

            if (setpoint == null || tap == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: tap and setpoint are required in %s", PSTRANGEACTION_RESULTS));
            }
            pstRangeActionResult.addActivationForState(state, tap, setpoint);

        }
    }
}
