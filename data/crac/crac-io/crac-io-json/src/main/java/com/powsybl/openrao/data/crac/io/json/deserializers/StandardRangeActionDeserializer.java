/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.rangeaction.StandardRangeActionAdder;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.deserializeVariationDirection;
import static com.powsybl.openrao.data.crac.io.json.deserializers.CracDeserializer.LOGGER;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public final class StandardRangeActionDeserializer {

    private StandardRangeActionDeserializer() {
    }

    /**
     * De-serializes common elements in StandardRangeAction implementations
     * @return true if the element was found
     * @throws IOException
     */
    public static boolean addCommonElement(StandardRangeActionAdder<?> standardRangeActionAdder, JsonParser jsonParser, String version) throws IOException {
        switch (jsonParser.getCurrentName()) {
            case JsonSerializationConstants.ID:
                standardRangeActionAdder.withId(jsonParser.nextTextValue());
                break;
            case JsonSerializationConstants.NAME:
                standardRangeActionAdder.withName(jsonParser.nextTextValue());
                break;
            case JsonSerializationConstants.OPERATOR:
                standardRangeActionAdder.withOperator(jsonParser.nextTextValue());
                break;
            case JsonSerializationConstants.ON_INSTANT_USAGE_RULES:
                jsonParser.nextToken();
                OnInstantArrayDeserializer.deserialize(jsonParser, standardRangeActionAdder, version);
                break;
            case JsonSerializationConstants.FREE_TO_USE_USAGE_RULES:
                if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 1 || JsonSerializationConstants.getSubVersionNumber(version) > 5) {
                    throw new OpenRaoException("FreeToUse has been renamed to OnInstant since CRAC version 1.6");
                } else {
                    jsonParser.nextToken();
                    OnInstantArrayDeserializer.deserialize(jsonParser, standardRangeActionAdder, version);
                }
                break;
            case JsonSerializationConstants.ON_CONTINGENCY_STATE_USAGE_RULES:
                jsonParser.nextToken();
                OnStateArrayDeserializer.deserialize(jsonParser, standardRangeActionAdder, version);
                break;
            case JsonSerializationConstants.ON_STATE_USAGE_RULES:
                if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 1 || JsonSerializationConstants.getSubVersionNumber(version) > 5) {
                    throw new OpenRaoException("OnState has been renamed to OnContingencyState since CRAC version 1.6");
                } else {
                    jsonParser.nextToken();
                    OnStateArrayDeserializer.deserialize(jsonParser, standardRangeActionAdder, version);
                }
                break;
            case JsonSerializationConstants.ON_CONSTRAINT_USAGE_RULES:
                jsonParser.nextToken();
                OnConstraintArrayDeserializer.deserialize(jsonParser, standardRangeActionAdder, version);
                break;
            case JsonSerializationConstants.ON_FLOW_CONSTRAINT_USAGE_RULES:
                jsonParser.nextToken();
                deserializeOlderOnConstraintUsageRules(jsonParser, JsonSerializationConstants.ON_FLOW_CONSTRAINT_USAGE_RULES, version, standardRangeActionAdder);
                break;
            case JsonSerializationConstants.ON_ANGLE_CONSTRAINT_USAGE_RULES:
                jsonParser.nextToken();
                deserializeOlderOnConstraintUsageRules(jsonParser, JsonSerializationConstants.ON_ANGLE_CONSTRAINT_USAGE_RULES, version, standardRangeActionAdder);
                break;
            case JsonSerializationConstants.ON_VOLTAGE_CONSTRAINT_USAGE_RULES:
                jsonParser.nextToken();
                deserializeOlderOnConstraintUsageRules(jsonParser, JsonSerializationConstants.ON_VOLTAGE_CONSTRAINT_USAGE_RULES, version, standardRangeActionAdder);
                break;
            case JsonSerializationConstants.ON_FLOW_CONSTRAINT_IN_COUNTRY_USAGE_RULES:
                jsonParser.nextToken();
                OnFlowConstraintInCountryArrayDeserializer.deserialize(jsonParser, standardRangeActionAdder, version);
                break;
            case JsonSerializationConstants.GROUP_ID:
                standardRangeActionAdder.withGroupId(jsonParser.nextTextValue());
                break;
            case JsonSerializationConstants.INITIAL_SETPOINT:
                if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 2 || JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) > 7) {
                    throw new OpenRaoException("initialSetpoint field is no longer used since CRAC version 2.8, the value is now directly determined from the network");
                } else {
                    jsonParser.nextToken();
                    LOGGER.warn("The initial setpoint is now read from the network so the value in the crac will not be read");
                    break;
                }
            case JsonSerializationConstants.RANGES:
                jsonParser.nextToken();
                StandardRangeArrayDeserializer.deserialize(jsonParser, standardRangeActionAdder);
                break;
            case JsonSerializationConstants.EXTENSIONS:
                throw new OpenRaoException("Extensions are deprecated since CRAC version 1.7");
            case JsonSerializationConstants.SPEED:
                jsonParser.nextToken();
                standardRangeActionAdder.withSpeed(jsonParser.getIntValue());
                break;
            case JsonSerializationConstants.ACTIVATION_COST:
                jsonParser.nextToken();
                standardRangeActionAdder.withActivationCost(jsonParser.getDoubleValue());
                break;
            case JsonSerializationConstants.VARIATION_COSTS:
                jsonParser.nextToken();
                deserializeVariationCosts(standardRangeActionAdder, jsonParser);
                break;
            default:
                return false;
        }
        return true;
    }

    private static void deserializeOlderOnConstraintUsageRules(JsonParser jsonParser, String keyword, String version, StandardRangeActionAdder<?> standardRangeActionAdder) throws IOException {
        if (JsonSerializationConstants.getPrimaryVersionNumber(version) < 2 || JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) < 4) {
            OnConstraintArrayDeserializer.deserialize(jsonParser, standardRangeActionAdder, version);
        } else {
            throw new OpenRaoException("Unsupported field %s in CRAC version >= 2.4".formatted(keyword));
        }
    }

    private static void deserializeVariationCosts(StandardRangeActionAdder<?> standardRangeActionAdder, JsonParser jsonParser) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            jsonParser.nextToken();
            standardRangeActionAdder.withVariationCost(jsonParser.getDoubleValue(), deserializeVariationDirection(jsonParser.getCurrentName()));
        }
    }

}
