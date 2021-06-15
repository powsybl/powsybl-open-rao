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
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.rao_result_impl.NetworkActionResult;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class NetworkActionResultArrayDeserializer {

    private NetworkActionResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac) throws IOException {

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(NETWORKACTION_ID)) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", NETWORKACTION_RESULTS, NETWORKACTION_ID));
            }

            String networkActionId = jsonParser.nextTextValue();
            NetworkAction networkAction = crac.getNetworkAction(networkActionId);

            if (networkAction == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: cannot deserialize RaoResult: networkAction with id %s does not exist in the Crac", networkActionId));
            }

            NetworkActionResult networkActionResult = raoResult.getAndCreateIfAbsentNetworkActionResult(networkAction);
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case STATES_ACTIVATED_NETWORKACTION:
                        jsonParser.nextToken();
                        deserializeStates(jsonParser, networkActionResult, crac);
                        break;
                    default:
                        throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", NETWORKACTION_RESULTS, jsonParser.getCurrentName()));
                }
            }
        }
    }

    private static void deserializeStates(JsonParser jsonParser, NetworkActionResult networkActionResult, Crac crac) throws IOException {

        Instant instant = null;
        String contingencyId = null;
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

                    default:
                        throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", NETWORKACTION_RESULTS, jsonParser.getCurrentName()));
                }
            }

            if (instant == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: no instant defined in activated states of %s", NETWORKACTION_RESULTS));
            }

            if (instant == Instant.PREVENTIVE) {
                state = crac.getPreventiveState();
            } else {
                if (contingencyId == null) {
                    throw new FaraoException(String.format("Cannot deserialize RaoResult: no contingency defined in N-k activated states of %s", NETWORKACTION_RESULTS));
                }
                state = crac.getState(contingencyId, instant);
                if (state == null) {
                    throw new FaraoException(String.format("Cannot deserialize RaoResult: State at instant %s with contingency %s not found in Crac", instant, contingencyId));
                }
            }
            networkActionResult.addActivationForState(state);

        }
    }
}
