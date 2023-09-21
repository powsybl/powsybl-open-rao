/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Map;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class InjectionRangeActionArrayDeserializer {
    private InjectionRangeActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new FaraoException(String.format("Cannot deserialize %s before %s", INJECTION_RANGE_ACTIONS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction();

            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case ID:
                        injectionRangeActionAdder.withId(jsonParser.nextTextValue());
                        break;
                    case NAME:
                        injectionRangeActionAdder.withName(jsonParser.nextTextValue());
                        break;
                    case OPERATOR:
                        injectionRangeActionAdder.withOperator(jsonParser.nextTextValue());
                        break;
                    case ON_INSTANT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnInstantArrayDeserializer.deserialize(jsonParser, version, injectionRangeActionAdder);
                        break;
                    case FREE_TO_USE_USAGE_RULES:
                        if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 5) {
                            throw new FaraoException("FreeToUse has been renamed to OnInstant since CRAC version 1.6");
                        } else {
                            jsonParser.nextToken();
                            OnInstantArrayDeserializer.deserialize(jsonParser, version, injectionRangeActionAdder);
                        }
                        break;
                    case ON_CONTINGENCY_STATE_USAGE_RULES:
                        jsonParser.nextToken();
                        OnStateArrayDeserializer.deserialize(jsonParser, version, injectionRangeActionAdder);
                        break;
                    case ON_STATE_USAGE_RULES:
                        if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 5) {
                            throw new FaraoException("OnState has been renamed to OnContingencyState since CRAC version 1.6");
                        } else {
                            jsonParser.nextToken();
                            OnStateArrayDeserializer.deserialize(jsonParser, version, injectionRangeActionAdder);
                        }
                        break;
                    case ON_FLOW_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnFlowConstraintArrayDeserializer.deserialize(jsonParser, injectionRangeActionAdder);
                        break;
                    case ON_ANGLE_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnAngleConstraintArrayDeserializer.deserialize(jsonParser, injectionRangeActionAdder);
                        break;
                    case ON_VOLTAGE_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnVoltageConstraintArrayDeserializer.deserialize(jsonParser, injectionRangeActionAdder);
                        break;
                    case ON_FLOW_CONSTRAINT_IN_COUNTRY_USAGE_RULES:
                        jsonParser.nextToken();
                        OnFlowConstraintInCountryArrayDeserializer.deserialize(jsonParser, injectionRangeActionAdder);
                        break;
                    case NETWORK_ELEMENT_IDS_AND_KEYS:
                        jsonParser.nextToken();
                        deserializeInjectionDistributionKeys(jsonParser, injectionRangeActionAdder, networkElementsNamesPerId);
                        break;
                    case GROUP_ID:
                        injectionRangeActionAdder.withGroupId(jsonParser.nextTextValue());
                        break;
                    case INITIAL_SETPOINT:
                        jsonParser.nextToken();
                        injectionRangeActionAdder.withInitialSetpoint(jsonParser.getDoubleValue());
                        break;
                    case RANGES:
                        jsonParser.nextToken();
                        StandardRangeArrayDeserializer.deserialize(jsonParser, injectionRangeActionAdder);
                        break;
                    case EXTENSIONS:
                        throw new FaraoException("Extensions are deprecated since CRAC version 1.7");
                    case SPEED:
                        jsonParser.nextToken();
                        injectionRangeActionAdder.withSpeed(jsonParser.getIntValue());
                        break;
                    default:
                        throw new FaraoException("Unexpected field in InjectionRangeAction: " + jsonParser.getCurrentName());
                }
            }
            if (getPrimaryVersionNumber(version) <= 1 && getSubVersionNumber(version) < 3) {
                // initial setpoint was not exported then, set default value to 0 to avoid errors
                injectionRangeActionAdder.withInitialSetpoint(0);
            }
            injectionRangeActionAdder.add();
        }
    }

    private static void deserializeInjectionDistributionKeys(JsonParser jsonParser, InjectionRangeActionAdder adder, Map<String, String> networkElementsNamesPerId) throws IOException {

        while (!jsonParser.nextToken().isStructEnd()) {
            String networkElementId = jsonParser.getCurrentName();
            jsonParser.nextToken();
            double key = jsonParser.getDoubleValue();
            if (networkElementsNamesPerId.containsKey(networkElementId)) {
                adder.withNetworkElementAndKey(key, networkElementId, networkElementsNamesPerId.get(networkElementId));
            } else {
                adder.withNetworkElementAndKey(key, networkElementId);
            }
        }
    }
}
