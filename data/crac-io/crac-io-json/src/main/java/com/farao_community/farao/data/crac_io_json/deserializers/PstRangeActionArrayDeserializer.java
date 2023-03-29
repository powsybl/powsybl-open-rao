/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_io_json.ExtensionsHandler;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class PstRangeActionArrayDeserializer {
    private PstRangeActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new FaraoException(String.format("Cannot deserialize %s before %s", PST_RANGE_ACTIONS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction();
            List<Extension<PstRangeAction>> extensions = new ArrayList<>();
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
                    case ON_INSTANT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnInstantArrayDeserializer.deserialize(jsonParser, pstRangeActionAdder);
                        break;
                    case ON_STATE_USAGE_RULES:
                        jsonParser.nextToken();
                        OnStateArrayDeserializer.deserialize(jsonParser, pstRangeActionAdder);
                        break;
                    case ON_FLOW_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnFlowConstraintArrayDeserializer.deserialize(jsonParser, pstRangeActionAdder);
                        break;
                    case ON_ANGLE_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnAngleConstraintArrayDeserializer.deserialize(jsonParser, pstRangeActionAdder);
                        break;
                    case ON_FLOW_CONSTRAINT_IN_COUNTRY_USAGE_RULES:
                        jsonParser.nextToken();
                        OnFlowConstraintInCountryArrayDeserializer.deserialize(jsonParser, pstRangeActionAdder);
                        break;
                    case NETWORK_ELEMENT_ID:
                        String networkElementId = jsonParser.nextTextValue();
                        if (networkElementsNamesPerId.containsKey(networkElementId)) {
                            pstRangeActionAdder.withNetworkElement(networkElementId, networkElementsNamesPerId.get(networkElementId));
                        } else {
                            pstRangeActionAdder.withNetworkElement(networkElementId);
                        }
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
                        jsonParser.nextToken();
                        extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                        break;
                    case SPEED:
                        jsonParser.nextToken();
                        pstRangeActionAdder.withSpeed(jsonParser.getIntValue());
                        break;
                    default:
                        throw new FaraoException("Unexpected field in PstRangeAction: " + jsonParser.getCurrentName());
                }
            }
            PstRangeAction pstRangeAction = pstRangeActionAdder.add();
            if (!extensions.isEmpty()) {
                ExtensionsHandler.getExtensionsSerializers().addExtensions(pstRangeAction, extensions);
            }
        }
    }

    private static Map<Integer, Double> readIntToDoubleMap(JsonParser jsonParser) throws IOException {
        HashMap<Integer, Double> map = jsonParser.readValueAs(new TypeReference<Map<Integer, Double>>() {
        });
        // Check types
        map.forEach((Object o, Object o2) -> {
            if (!(o instanceof Integer) || !(o2 instanceof Double)) {
                throw new FaraoException("Unexpected key or value type in a Map<Integer, Double> parameter!");
            }
        });
        return map;
    }
}
