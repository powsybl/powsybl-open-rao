/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class RaUsageLimitsPerContingencyParametersDeserializer {

    private RaUsageLimitsPerContingencyParametersDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case MAX_CURATIVE_RA:
                    jsonParser.nextToken();
                    raoParameters.getRaUsageLimitsPerContingencyParameters().setMaxCurativeRa(jsonParser.getIntValue());
                    break;
                case MAX_CURATIVE_TSO:
                    jsonParser.nextToken();
                    raoParameters.getRaUsageLimitsPerContingencyParameters().setMaxCurativeTso(jsonParser.getIntValue());
                    break;
                case MAX_CURATIVE_TOPO_PER_TSO:
                    jsonParser.nextToken();
                    raoParameters.getRaUsageLimitsPerContingencyParameters().setMaxCurativeTopoPerTso(readStringToPositiveIntMap(jsonParser));
                    break;
                case MAX_CURATIVE_PST_PER_TSO:
                    jsonParser.nextToken();
                    raoParameters.getRaUsageLimitsPerContingencyParameters().setMaxCurativePstPerTso(readStringToPositiveIntMap(jsonParser));
                    break;
                case MAX_CURATIVE_RA_PER_TSO:
                    jsonParser.nextToken();
                    raoParameters.getRaUsageLimitsPerContingencyParameters().setMaxCurativeRaPerTso(readStringToPositiveIntMap(jsonParser));
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize ra usage limits per contingency parameters: unexpected field in %s (%s)", RA_USAGE_LIMITS_PER_CONTINGENCY, jsonParser.getCurrentName()));
            }
        }
    }

    private static Map<String, Integer> readStringToPositiveIntMap(JsonParser jsonParser) throws IOException {
        HashMap<String, Integer> map = jsonParser.readValueAs(HashMap.class);
        // Check types
        map.forEach((Object o, Object o2) -> {
            if (!(o instanceof String) || !(o2 instanceof Integer)) {
                throw new FaraoException("Unexpected key or value type in a Map<String, Integer> parameter!");
            }
            if ((int) o2 < 0) {
                throw new FaraoException("Unexpected negative integer!");
            }
        });
        return map;
    }
}
