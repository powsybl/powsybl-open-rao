/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.json.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.rangeaction.CounterTradeRangeActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.data.cracio.json.JsonSerializationConstants;

import java.io.IOException;
import java.util.Map;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public final class CounterTradeRangeActionArrayDeserializer {
    private CounterTradeRangeActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", JsonSerializationConstants.COUNTER_TRADE_RANGE_ACTIONS, JsonSerializationConstants.NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction();

            while (!jsonParser.nextToken().isStructEnd()) {
                addElement(counterTradeRangeActionAdder, jsonParser, version);
            }
            if (JsonSerializationConstants.getPrimaryVersionNumber(version) <= 1 && JsonSerializationConstants.getSubVersionNumber(version) < 3) {
                // initial setpoint was not exported then, set default value to 0 to avoid errors
                counterTradeRangeActionAdder.withInitialSetpoint(0);
            }
            counterTradeRangeActionAdder.add();
        }
    }

    private static void addElement(CounterTradeRangeActionAdder counterTradeRangeActionAdder, JsonParser jsonParser, String version) throws IOException {
        if (StandardRangeActionDeserializer.addCommonElement(counterTradeRangeActionAdder, jsonParser, version)) {
            return;
        }
        switch (jsonParser.getCurrentName()) {
            case JsonSerializationConstants.EXPORTING_COUNTRY:
                counterTradeRangeActionAdder.withExportingCountry(Country.valueOf(jsonParser.nextTextValue()));
                break;
            case JsonSerializationConstants.IMPORTING_COUNTRY:
                counterTradeRangeActionAdder.withImportingCountry(Country.valueOf(jsonParser.nextTextValue()));
                break;
            default:
                throw new OpenRaoException("Unexpected field in InjectionRangeAction: " + jsonParser.getCurrentName());
        }
    }
}
