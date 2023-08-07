/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeActionAdder;
import com.farao_community.farao.data.crac_io_json.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class HvdcRangeActionArrayDeserializer {
    private HvdcRangeActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new FaraoException(String.format("Cannot deserialize %s before %s", HVDC_RANGE_ACTIONS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction();
            List<Extension<HvdcRangeAction>> extensions = new ArrayList<>();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case ID:
                        hvdcRangeActionAdder.withId(jsonParser.nextTextValue());
                        break;
                    case NAME:
                        hvdcRangeActionAdder.withName(jsonParser.nextTextValue());
                        break;
                    case OPERATOR:
                        hvdcRangeActionAdder.withOperator(jsonParser.nextTextValue());
                        break;
                    case ON_INSTANT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnInstantArrayDeserializer.deserialize(jsonParser, version, hvdcRangeActionAdder);
                        break;
                    case FREE_TO_USE_USAGE_RULES:
                        if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 5) {
                            throw new FaraoException("FreeToUse has been renamed to OnInstant since CRAC version 1.6");
                        } else {
                            jsonParser.nextToken();
                            OnInstantArrayDeserializer.deserialize(jsonParser, version, hvdcRangeActionAdder);
                        }
                        break;
                    case ON_CONTINGENCY_STATE_USAGE_RULES:
                        jsonParser.nextToken();
                        OnStateArrayDeserializer.deserialize(jsonParser, version, hvdcRangeActionAdder);
                        break;
                    case ON_STATE_USAGE_RULES:
                        if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 5) {
                            throw new FaraoException("OnState has been renamed to OnContingencyState since CRAC version 1.6");
                        } else {
                            jsonParser.nextToken();
                            OnStateArrayDeserializer.deserialize(jsonParser, version, hvdcRangeActionAdder);
                        }
                        break;
                    case ON_FLOW_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnFlowConstraintArrayDeserializer.deserialize(jsonParser, hvdcRangeActionAdder);
                        break;
                    case ON_ANGLE_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnAngleConstraintArrayDeserializer.deserialize(jsonParser, hvdcRangeActionAdder);
                        break;
                    case ON_VOLTAGE_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnVoltageConstraintArrayDeserializer.deserialize(jsonParser, hvdcRangeActionAdder);
                        break;
                    case ON_FLOW_CONSTRAINT_IN_COUNTRY_USAGE_RULES:
                        jsonParser.nextToken();
                        OnFlowConstraintInCountryArrayDeserializer.deserialize(jsonParser, hvdcRangeActionAdder);
                        break;
                    case NETWORK_ELEMENT_ID:
                        readNetworkElementId(jsonParser, networkElementsNamesPerId, hvdcRangeActionAdder);
                        break;
                    case GROUP_ID:
                        hvdcRangeActionAdder.withGroupId(jsonParser.nextTextValue());
                        break;
                    case INITIAL_SETPOINT:
                        jsonParser.nextToken();
                        hvdcRangeActionAdder.withInitialSetpoint(jsonParser.getDoubleValue());
                        break;
                    case RANGES:
                        jsonParser.nextToken();
                        StandardRangeArrayDeserializer.deserialize(jsonParser, hvdcRangeActionAdder);
                        break;
                    case EXTENSIONS:
                        jsonParser.nextToken();
                        extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                        break;
                    case SPEED:
                        jsonParser.nextToken();
                        hvdcRangeActionAdder.withSpeed(jsonParser.getIntValue());
                        break;
                    default:
                        throw new FaraoException("Unexpected field in HvdcRangeAction: " + jsonParser.getCurrentName());
                }
            }
            if (getPrimaryVersionNumber(version) <= 1 && getSubVersionNumber(version) < 3) {
                // initial setpoint was not exported then, set default value to 0 to avoid errors
                hvdcRangeActionAdder.withInitialSetpoint(0);
            }
            HvdcRangeAction hvdcRangeAction = hvdcRangeActionAdder.add();
            if (!extensions.isEmpty()) {
                ExtensionsHandler.getExtensionsSerializers().addExtensions(hvdcRangeAction, extensions);
            }
        }
    }

    private static void readNetworkElementId(JsonParser jsonParser, Map<String, String> networkElementsNamesPerId, HvdcRangeActionAdder hvdcRangeActionAdder) throws IOException {
        String networkElementId = jsonParser.nextTextValue();
        if (networkElementsNamesPerId.containsKey(networkElementId)) {
            hvdcRangeActionAdder.withNetworkElement(networkElementId, networkElementsNamesPerId.get(networkElementId));
        } else {
            hvdcRangeActionAdder.withNetworkElement(networkElementId);
        }
    }
}
