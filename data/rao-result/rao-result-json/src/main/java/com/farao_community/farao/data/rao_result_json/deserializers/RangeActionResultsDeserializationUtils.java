/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.farao_community.farao.data.rao_result_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.jgrapht.alg.util.Pair;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * Common functions for StandardRangeActionResultArrayDeserializer & PstRangeActionResultArrayDeserializer
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class RangeActionResultsDeserializationUtils {
    private RangeActionResultsDeserializationUtils() {
        // utility class
    }

    static Map<State, Pair<Integer, Double>> deserializeTapAndSetpointPerState(JsonParser jsonParser, Crac crac, boolean isTapRequired, String objectName) throws IOException {
        Instant instant = null;
        String contingencyId = null;
        Double setpoint = null;
        Integer tap = null;
        Map<State, Pair<Integer, Double>> stateToTapAndSetpoint = new HashMap<>();
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
                        throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", objectName, jsonParser.getCurrentName()));
                }
            }

            if (setpoint == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: setpoint is required in %s", objectName));
            }
            if (isTapRequired && tap == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: tap is required in %s", objectName));
            }
            stateToTapAndSetpoint.put(StateDeserializer.getState(instant, contingencyId, crac, objectName), Pair.of(tap, setpoint));
        }
        return stateToTapAndSetpoint;
    }
}
