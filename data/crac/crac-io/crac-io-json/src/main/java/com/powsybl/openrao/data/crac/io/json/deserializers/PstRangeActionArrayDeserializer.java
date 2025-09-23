/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.deserializeVariationDirection;
import static com.powsybl.openrao.data.crac.io.json.deserializers.CracDeserializer.LOGGER;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class PstRangeActionArrayDeserializer {
    private PstRangeActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, String version, Crac crac, Map<String, String> networkElementsNamesPerId, Network network) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", JsonSerializationConstants.PST_RANGE_ACTIONS, JsonSerializationConstants.NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case JsonSerializationConstants.ID:
                        pstRangeActionAdder.withId(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.NAME:
                        pstRangeActionAdder.withName(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.OPERATOR:
                        pstRangeActionAdder.withOperator(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.ON_INSTANT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnInstantArrayDeserializer.deserialize(jsonParser, pstRangeActionAdder);
                        break;
                    case JsonSerializationConstants.FREE_TO_USE_USAGE_RULES:
                        deserializeFreeToUseUsageRules(jsonParser, version, pstRangeActionAdder);
                        break;
                    case JsonSerializationConstants.ON_CONTINGENCY_STATE_USAGE_RULES:
                        jsonParser.nextToken();
                        OnStateArrayDeserializer.deserialize(jsonParser, pstRangeActionAdder);
                        break;
                    case JsonSerializationConstants.ON_STATE_USAGE_RULES:
                        deserializeOnStateUsageRules(jsonParser, version, pstRangeActionAdder);
                        break;
                    case JsonSerializationConstants.ON_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnConstraintArrayDeserializer.deserialize(jsonParser, pstRangeActionAdder, version);
                        break;
                    case JsonSerializationConstants.ON_FLOW_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        deserializeOlderOnConstraintUsageRules(jsonParser, JsonSerializationConstants.ON_FLOW_CONSTRAINT_USAGE_RULES, version, pstRangeActionAdder);
                        break;
                    case JsonSerializationConstants.ON_ANGLE_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        deserializeOlderOnConstraintUsageRules(jsonParser, JsonSerializationConstants.ON_ANGLE_CONSTRAINT_USAGE_RULES, version, pstRangeActionAdder);
                        break;
                    case JsonSerializationConstants.ON_VOLTAGE_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        deserializeOlderOnConstraintUsageRules(jsonParser, JsonSerializationConstants.ON_VOLTAGE_CONSTRAINT_USAGE_RULES, version, pstRangeActionAdder);
                        break;
                    case JsonSerializationConstants.ON_FLOW_CONSTRAINT_IN_COUNTRY_USAGE_RULES:
                        jsonParser.nextToken();
                        OnFlowConstraintInCountryArrayDeserializer.deserialize(jsonParser, pstRangeActionAdder, version);
                        break;
                    case JsonSerializationConstants.NETWORK_ELEMENT_ID:
                        deserializeNetworkElementId(jsonParser, networkElementsNamesPerId, pstRangeActionAdder, network);
                        break;
                    case JsonSerializationConstants.GROUP_ID:
                        pstRangeActionAdder.withGroupId(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.INITIAL_TAP:
                        jsonParser.nextToken();
                        if (JsonSerializationConstants.getPrimaryVersionNumber(version) <= 1 ||
                            JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) <= 6) {
                            LOGGER.warn("The initial tap is now read from the network so the value in the crac will not be read");
                        }
                        break;
                    case JsonSerializationConstants.TAP_TO_ANGLE_CONVERSION_MAP:
                        jsonParser.nextToken();
                        readIntToDoubleMap(jsonParser);
                        if (JsonSerializationConstants.getPrimaryVersionNumber(version) <= 1 ||
                            JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) <= 6) {
                            LOGGER.warn("The tap to angle conversion map is now read from the network so the value in the crac will not be read");
                        }
                        break;
                    case JsonSerializationConstants.RANGES:
                        jsonParser.nextToken();
                        TapRangeArrayDeserializer.deserialize(jsonParser, pstRangeActionAdder);
                        break;
                    case JsonSerializationConstants.EXTENSIONS:
                        throw new OpenRaoException("Extensions are deprecated since CRAC version 1.7");
                    case JsonSerializationConstants.SPEED:
                        jsonParser.nextToken();
                        pstRangeActionAdder.withSpeed(jsonParser.getIntValue());
                        break;
                    case JsonSerializationConstants.ACTIVATION_COST:
                        jsonParser.nextToken();
                        pstRangeActionAdder.withActivationCost(jsonParser.getDoubleValue());
                        break;
                    case JsonSerializationConstants.VARIATION_COSTS:
                        jsonParser.nextToken();
                        deserializeVariationCosts(pstRangeActionAdder, jsonParser);
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in PstRangeAction: " + jsonParser.getCurrentName());
                }
            }
            pstRangeActionAdder.add();
        }
    }

    private static void deserializeNetworkElementId(JsonParser jsonParser, Map<String, String> networkElementsNamesPerId, PstRangeActionAdder pstRangeActionAdder, Network network) throws IOException {
        String networkElementId = jsonParser.nextTextValue();
        if (networkElementsNamesPerId.containsKey(networkElementId)) {
            pstRangeActionAdder.withNetworkElement(networkElementId, networkElementsNamesPerId.get(networkElementId));
        } else {
            pstRangeActionAdder.withNetworkElement(networkElementId);
        }
        PhaseTapChanger phaseTapChanger = getPhaseTapChanger(network, networkElementId);
        pstRangeActionAdder.withInitialTap(phaseTapChanger.getTapPosition());
        Map<Integer, Double> tapToAngleConversionMap = new HashMap<>();
        phaseTapChanger.getAllSteps().forEach((tap, ptcStep) -> tapToAngleConversionMap.put(tap, ptcStep.getAlpha()));
        pstRangeActionAdder.withTapToAngleConversionMap(tapToAngleConversionMap);
    }

    private static PhaseTapChanger getPhaseTapChanger(Network network, String networkElementId) {
        // here for three winding transfo as RA support ?
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(networkElementId);
        if (transformer == null) {
            throw new OpenRaoException(String.format("PST %s does not exist in the current network", networkElementId));
        }
        PhaseTapChanger phaseTapChangerFromNetwork = transformer.getPhaseTapChanger();
        if (phaseTapChangerFromNetwork == null) {
            throw new OpenRaoException(String.format("Transformer %s is not a PST but is defined as a TapRange", networkElementId));
        }
        return phaseTapChangerFromNetwork;
    }

    private static void deserializeOnStateUsageRules(JsonParser jsonParser, String version, PstRangeActionAdder pstRangeActionAdder) throws IOException {
        if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 1 || JsonSerializationConstants.getSubVersionNumber(version) > 5) {
            throw new OpenRaoException("OnState has been renamed to OnContingencyState since CRAC version 1.6");
        } else {
            jsonParser.nextToken();
            OnStateArrayDeserializer.deserialize(jsonParser, pstRangeActionAdder);
        }
    }

    private static void deserializeFreeToUseUsageRules(JsonParser jsonParser, String version, PstRangeActionAdder pstRangeActionAdder) throws IOException {
        if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 1 || JsonSerializationConstants.getSubVersionNumber(version) > 5) {
            throw new OpenRaoException("FreeToUse has been renamed to OnInstant since CRAC version 1.6");
        } else {
            jsonParser.nextToken();
            OnInstantArrayDeserializer.deserialize(jsonParser, pstRangeActionAdder);
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

    private static void deserializeOlderOnConstraintUsageRules(JsonParser jsonParser, String keyword, String version, PstRangeActionAdder pstRangeActionAdder) throws IOException {
        if (JsonSerializationConstants.getPrimaryVersionNumber(version) < 2 || JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) < 4) {
            OnConstraintArrayDeserializer.deserialize(jsonParser, pstRangeActionAdder, version);
        } else {
            throw new OpenRaoException("Unsupported field %s in CRAC version >= 2.4".formatted(keyword));
        }
    }

    private static void deserializeVariationCosts(PstRangeActionAdder pstRangeActionAdder, JsonParser jsonParser) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            jsonParser.nextToken();
            pstRangeActionAdder.withVariationCost(jsonParser.getDoubleValue(), deserializeVariationDirection(jsonParser.getCurrentName()));
        }
    }
}
