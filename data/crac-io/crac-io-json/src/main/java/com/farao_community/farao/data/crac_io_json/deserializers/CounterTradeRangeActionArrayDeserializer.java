/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.range_action.CounterTradeRangeActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.iidm.network.Country;

import java.io.IOException;
import java.util.Map;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public final class CounterTradeRangeActionArrayDeserializer {
    private CounterTradeRangeActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new FaraoException(String.format("Cannot deserialize %s before %s", COUNTER_TRADE_RANGE_ACTIONS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction();

            while (!jsonParser.nextToken().isStructEnd()) {
                addElement(counterTradeRangeActionAdder, jsonParser, version);
            }
            if (getPrimaryVersionNumber(version) <= 1 && getSubVersionNumber(version) < 3) {
                // initial setpoint was not exported then, set default value to 0 to avoid errors
                counterTradeRangeActionAdder.withInitialSetpoint(0);
            }
            counterTradeRangeActionAdder.add();
        }
    }

    private static void addElement(CounterTradeRangeActionAdder counterTradeRangeActionAdder, JsonParser jsonParser, String version) throws IOException {
        switch (jsonParser.getCurrentName()) {
            case ID:
                counterTradeRangeActionAdder.withId(jsonParser.nextTextValue());
                break;
            case NAME:
                counterTradeRangeActionAdder.withName(jsonParser.nextTextValue());
                break;
            case OPERATOR:
                counterTradeRangeActionAdder.withOperator(jsonParser.nextTextValue());
                break;
            case ON_INSTANT_USAGE_RULES:
                jsonParser.nextToken();
                OnInstantArrayDeserializer.deserialize(jsonParser, version, counterTradeRangeActionAdder);
                break;
            case FREE_TO_USE_USAGE_RULES:
                if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 5) {
                    throw new FaraoException("FreeToUse has been renamed to OnInstant since CRAC version 1.6");
                } else {
                    jsonParser.nextToken();
                    OnInstantArrayDeserializer.deserialize(jsonParser, version, counterTradeRangeActionAdder);
                }
                break;
            case ON_CONTINGENCY_STATE_USAGE_RULES:
                jsonParser.nextToken();
                OnStateArrayDeserializer.deserialize(jsonParser, version, counterTradeRangeActionAdder);
                break;
            case ON_STATE_USAGE_RULES:
                if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 5) {
                    throw new FaraoException("OnState has been renamed to OnContingencyState since CRAC version 1.6");
                } else {
                    jsonParser.nextToken();
                    OnStateArrayDeserializer.deserialize(jsonParser, version, counterTradeRangeActionAdder);
                }
                break;
            case ON_FLOW_CONSTRAINT_USAGE_RULES:
                jsonParser.nextToken();
                OnFlowConstraintArrayDeserializer.deserialize(jsonParser, counterTradeRangeActionAdder);
                break;
            case ON_ANGLE_CONSTRAINT_USAGE_RULES:
                jsonParser.nextToken();
                OnAngleConstraintArrayDeserializer.deserialize(jsonParser, counterTradeRangeActionAdder);
                break;
            case ON_VOLTAGE_CONSTRAINT_USAGE_RULES:
                jsonParser.nextToken();
                OnVoltageConstraintArrayDeserializer.deserialize(jsonParser, counterTradeRangeActionAdder);
                break;
            case ON_FLOW_CONSTRAINT_IN_COUNTRY_USAGE_RULES:
                jsonParser.nextToken();
                OnFlowConstraintInCountryArrayDeserializer.deserialize(jsonParser, counterTradeRangeActionAdder);
                break;
            case EXPORTING_COUNTRY:
                counterTradeRangeActionAdder.withExportingCountry(Country.valueOf(jsonParser.nextTextValue()));
                break;
            case IMPORTING_COUNTRY:
                counterTradeRangeActionAdder.withImportingCountry(Country.valueOf(jsonParser.nextTextValue()));
                break;
            case GROUP_ID:
                counterTradeRangeActionAdder.withGroupId(jsonParser.nextTextValue());
                break;
            case INITIAL_SETPOINT:
                jsonParser.nextToken();
                counterTradeRangeActionAdder.withInitialSetpoint(jsonParser.getDoubleValue());
                break;
            case RANGES:
                jsonParser.nextToken();
                StandardRangeArrayDeserializer.deserialize(jsonParser, counterTradeRangeActionAdder);
                break;
            case EXTENSIONS:
                throw new FaraoException("Extensions are deprecated since CRAC version 1.7");
            case SPEED:
                jsonParser.nextToken();
                counterTradeRangeActionAdder.withSpeed(jsonParser.getIntValue());
                break;
            default:
                throw new FaraoException("Unexpected field in InjectionRangeAction: " + jsonParser.getCurrentName());
        }
    }
}
