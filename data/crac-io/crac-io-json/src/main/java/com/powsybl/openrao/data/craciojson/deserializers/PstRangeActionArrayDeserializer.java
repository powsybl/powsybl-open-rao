/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class PstRangeActionArrayDeserializer {
    private PstRangeActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", PST_RANGE_ACTIONS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case ID:
                        pstRangeActionAdder.withId(jsonParser.nextTextValue());
                        break;
                    case NAME:
                        pstRangeActionAdder.withName(jsonParser.nextTextValue());
                        break;
                    case OPERATOR:
                        pstRangeActionAdder.withOperator(jsonParser.nextTextValue());
                        break;
                    case TRIGGER_CONDITIONS:
                        jsonParser.nextToken();
                        TriggerConditionDeserializer.deserialize(jsonParser, pstRangeActionAdder, version);
                        break;
                    case ON_INSTANT_USAGE_RULES, FREE_TO_USE_USAGE_RULES, ON_CONTINGENCY_STATE_USAGE_RULES, ON_STATE_USAGE_RULES, ON_CONSTRAINT_USAGE_RULES, ON_FLOW_CONSTRAINT_USAGE_RULES, ON_ANGLE_CONSTRAINT_USAGE_RULES, ON_VOLTAGE_CONSTRAINT_USAGE_RULES, ON_FLOW_CONSTRAINT_IN_COUNTRY_USAGE_RULES:
                        deserializeUsageRules(jsonParser, version, pstRangeActionAdder, FREE_TO_USE_USAGE_RULES.equals(jsonParser.getCurrentName()));
                        break;
                    case NETWORK_ELEMENT_ID:
                        deserializeNetworkElementId(jsonParser, networkElementsNamesPerId, pstRangeActionAdder);
                        break;
                    case GROUP_ID:
                        pstRangeActionAdder.withGroupId(jsonParser.nextTextValue());
                        break;
                    case INITIAL_TAP:
                        jsonParser.nextToken();
                        pstRangeActionAdder.withInitialTap(jsonParser.getIntValue());
                        break;
                    case TAP_TO_ANGLE_CONVERSION_MAP:
                        jsonParser.nextToken();
                        pstRangeActionAdder.withTapToAngleConversionMap(readIntToDoubleMap(jsonParser));
                        break;
                    case RANGES:
                        jsonParser.nextToken();
                        TapRangeArrayDeserializer.deserialize(jsonParser, pstRangeActionAdder);
                        break;
                    case EXTENSIONS:
                        throw new OpenRaoException("Extensions are deprecated since CRAC version 1.7");
                    case SPEED:
                        jsonParser.nextToken();
                        pstRangeActionAdder.withSpeed(jsonParser.getIntValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in PstRangeAction: " + jsonParser.getCurrentName());
                }
            }
            pstRangeActionAdder.add();
        }
    }

    private static void deserializeNetworkElementId(JsonParser jsonParser, Map<String, String> networkElementsNamesPerId, PstRangeActionAdder pstRangeActionAdder) throws IOException {
        String networkElementId = jsonParser.nextTextValue();
        if (networkElementsNamesPerId.containsKey(networkElementId)) {
            pstRangeActionAdder.withNetworkElement(networkElementId, networkElementsNamesPerId.get(networkElementId));
        } else {
            pstRangeActionAdder.withNetworkElement(networkElementId);
        }
    }

    private static Map<Integer, Double> readIntToDoubleMap(JsonParser jsonParser) throws IOException {
        HashMap<Integer, Double> map = jsonParser.readValueAs(new TypeReference<Map<Integer, Double>>() {
        });
        // Check types
        map.forEach((Object o, Object o2) -> {
            if (!(o instanceof Integer) || !(o2 instanceof Double)) {
                throw new OpenRaoException("Unexpected key or value type in a Map<Integer, Double> parameter!");
            }
        });
        return map;
    }
}
