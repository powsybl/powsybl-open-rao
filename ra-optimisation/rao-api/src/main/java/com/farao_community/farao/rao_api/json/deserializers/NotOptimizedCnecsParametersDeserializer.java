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
public final class NotOptimizedCnecsParametersDeserializer {

    private NotOptimizedCnecsParametersDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case DO_NOT_OPTIMIZE_CURATIVE_CNECS:
                    jsonParser.nextToken();
                    raoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(jsonParser.getBooleanValue());
                    break;
                case DO_NOT_OPTIMIZE_CNECS_SECURED_BY_ITS_PST:
                    jsonParser.nextToken();
                    raoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCnecsSecuredByTheirPst(readStringToStringMap(jsonParser));
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize not optimized cnecs parameters: unexpected field in %s (%s)", NOT_OPTIMIZED_CNECS, jsonParser.getCurrentName()));
            }
        }
    }

    private static Map<String, String> readStringToStringMap(JsonParser jsonParser) throws IOException {
        HashMap<String, String> map = jsonParser.readValueAs(HashMap.class);
        // Check types
        map.forEach((Object o, Object o2) -> {
            if (!(o instanceof String) || !(o2 instanceof String)) {
                throw new FaraoException("Unexpected key or value type in a Map<String, String> parameter!");
            }
        });
        return map;
    }
}
