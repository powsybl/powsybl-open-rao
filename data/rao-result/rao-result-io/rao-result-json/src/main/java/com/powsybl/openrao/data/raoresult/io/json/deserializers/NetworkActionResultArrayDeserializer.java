/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresult.impl.NetworkActionResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants;

import java.io.IOException;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class NetworkActionResultArrayDeserializer {

    private NetworkActionResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac) throws IOException {

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(RaoResultJsonConstants.NETWORKACTION_ID)) {
                throw new OpenRaoException(String.format(
                    "Cannot deserialize RaoResult: each %s must start with an %s field",
                    RaoResultJsonConstants.NETWORKACTION_RESULTS,
                    RaoResultJsonConstants.NETWORKACTION_ID
                ));
            }

            String networkActionId = jsonParser.nextTextValue();
            NetworkAction networkAction = crac.getNetworkAction(networkActionId);

            if (networkAction == null) {
                throw new OpenRaoException(String.format(
                    "Cannot deserialize RaoResult: cannot deserialize RaoResult: networkAction with id %s does not exist in the Crac",
                    networkActionId
                ));
            }

            NetworkActionResult networkActionResult = raoResult.getAndCreateIfAbsentNetworkActionResult(networkAction);
            while (!jsonParser.nextToken().isStructEnd()) {
                if (jsonParser.getCurrentName().equals(RaoResultJsonConstants.STATES_ACTIVATED)) {
                    jsonParser.nextToken();
                    deserializeStates(jsonParser, networkActionResult, crac);
                } else {
                    throw new OpenRaoException(String.format(
                        "Cannot deserialize RaoResult: unexpected field in %s (%s)",
                        RaoResultJsonConstants.NETWORKACTION_RESULTS,
                        jsonParser.getCurrentName()
                    ));
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
                    case RaoResultJsonConstants.INSTANT:
                        String stringValue = jsonParser.nextTextValue();
                        instantId = stringValue;
                        break;
                    case RaoResultJsonConstants.CONTINGENCY_ID:
                        contingencyId = jsonParser.nextTextValue();
                        break;
                    default:
                        throw new OpenRaoException(String.format(
                            "Cannot deserialize RaoResult: unexpected field in %s (%s)",
                            RaoResultJsonConstants.NETWORKACTION_RESULTS,
                            jsonParser.getCurrentName()
                        ));
                }
            }
            networkActionResult.addActivationForState(StateDeserializer.getState(instantId, contingencyId, crac, RaoResultJsonConstants.NETWORKACTION_RESULTS));
        }
    }
}
