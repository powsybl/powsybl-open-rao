/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresultjson.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresultimpl.NetworkActionResult;
import com.powsybl.openrao.data.raoresultimpl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.powsybl.openrao.data.raoresultjson.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class NetworkActionResultArrayDeserializer {

    private NetworkActionResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac) throws IOException {

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(NETWORKACTION_ID)) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", NETWORKACTION_RESULTS, NETWORKACTION_ID));
            }

            String networkActionId = jsonParser.nextTextValue();
            NetworkAction networkAction = crac.getNetworkAction(networkActionId);

            if (networkAction == null) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: cannot deserialize RaoResult: networkAction with id %s does not exist in the Crac", networkActionId));
            }

            NetworkActionResult networkActionResult = raoResult.getAndCreateIfAbsentNetworkActionResult(networkAction);
            while (!jsonParser.nextToken().isStructEnd()) {
                if (jsonParser.getCurrentName().equals(STATES_ACTIVATED)) {
                    jsonParser.nextToken();
                    deserializeStates(jsonParser, networkActionResult, crac);
                } else {
                    throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", NETWORKACTION_RESULTS, jsonParser.getCurrentName()));
                }
            }
        }
    }

    private static void deserializeStates(JsonParser jsonParser, NetworkActionResult networkActionResult, Crac crac) throws IOException {
        String instantId = null;
        String contingencyId = null;
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case INSTANT:
                        String stringValue = jsonParser.nextTextValue();
                        instantId = stringValue;
                        break;
                    case CONTINGENCY_ID:
                        contingencyId = jsonParser.nextTextValue();
                        break;
                    default:
                        throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", NETWORKACTION_RESULTS, jsonParser.getCurrentName()));
                }
            }
            networkActionResult.addActivationForState(StateDeserializer.getState(instantId, contingencyId, crac, NETWORKACTION_RESULTS));
        }
    }
}
