/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.INSTANT;
import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.MAX_ELEMENTARY_ACTIONS_PER_TSO;
import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.MAX_RA;
import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.MAX_TSO;
import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.MAX_TOPO_PER_TSO;
import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.MAX_PST_PER_TSO;
import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.MAX_RA_PER_TSO;
import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.RA_USAGE_LIMITS_PER_INSTANT;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
public final class RaUsageLimitsDeserializer {
    private RaUsageLimitsDeserializer() {
        // should not be used
    }

    public static void deserialize(JsonParser jsonParser, Crac crac) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            Pair<String, RaUsageLimits> raUsageLimitsPair = deserializeRaUsageLimits(jsonParser);
            RaUsageLimits raUsageLimits = raUsageLimitsPair.getRight();
            crac.newRaUsageLimits(raUsageLimitsPair.getLeft())
                .withMaxRa(raUsageLimits.getMaxRa())
                .withMaxTso(raUsageLimits.getMaxTso())
                .withMaxRaPerTso(raUsageLimits.getMaxRaPerTso())
                .withMaxPstPerTso(raUsageLimits.getMaxPstPerTso())
                .withMaxTopoPerTso(raUsageLimits.getMaxTopoPerTso())
                .withMaxElementaryActionPerTso(raUsageLimits.getMaxElementaryActionsPerTso())
                .add();
        }
    }

    private static Map<String, Integer> readStringToPositiveIntMap(JsonParser jsonParser) throws IOException {
        HashMap<String, Integer> map = jsonParser.readValueAs(HashMap.class);
        // Check types
        map.forEach((Object o, Object o2) -> {
            if (!(o instanceof String) || !(o2 instanceof Integer)) {
                throw new OpenRaoException("Unexpected key or value type in a Map<String, Integer> parameter!");
            }
            if ((int) o2 < 0) {
                throw new OpenRaoException("Unexpected negative integer!");
            }
        });
        return map;
    }

    public static Pair<String, RaUsageLimits> deserializeRaUsageLimits(JsonParser jsonParser) throws IOException {
        RaUsageLimits raUsageLimits = new RaUsageLimits();
        String instant = null;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case INSTANT -> {
                    jsonParser.nextToken();
                    instant = jsonParser.getValueAsString();
                }
                case MAX_RA -> {
                    jsonParser.nextToken();
                    raUsageLimits.setMaxRa(jsonParser.getIntValue());
                }
                case MAX_TSO -> {
                    jsonParser.nextToken();
                    raUsageLimits.setMaxTso(jsonParser.getIntValue());
                }
                case MAX_TOPO_PER_TSO -> {
                    jsonParser.nextToken();
                    raUsageLimits.setMaxTopoPerTso(readStringToPositiveIntMap(jsonParser));
                }
                case MAX_PST_PER_TSO -> {
                    jsonParser.nextToken();
                    raUsageLimits.setMaxPstPerTso(readStringToPositiveIntMap(jsonParser));
                }
                case MAX_RA_PER_TSO -> {
                    jsonParser.nextToken();
                    raUsageLimits.setMaxRaPerTso(readStringToPositiveIntMap(jsonParser));
                }
                case MAX_ELEMENTARY_ACTIONS_PER_TSO -> {
                    jsonParser.nextToken();
                    raUsageLimits.setMaxElementaryActionsPerTso(readStringToPositiveIntMap(jsonParser));
                }
                default ->
                    throw new OpenRaoException(String.format("Cannot deserialize ra-usage-limits-per-instant parameters: unexpected field in %s (%s)", RA_USAGE_LIMITS_PER_INSTANT, jsonParser.getCurrentName()));
            }
        }
        return Pair.of(instant, raUsageLimits);
    }
}
