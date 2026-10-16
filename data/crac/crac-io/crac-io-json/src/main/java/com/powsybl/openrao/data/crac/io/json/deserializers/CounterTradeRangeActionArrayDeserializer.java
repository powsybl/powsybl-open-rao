/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;

import static com.powsybl.openrao.data.crac.io.json.deserializers.CracDeserializer.LOGGER;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public final class CounterTradeRangeActionArrayDeserializer {
    private CounterTradeRangeActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, String version, Crac crac, Network network) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction().withConnectedAreas(network);
            if (isBeforeV2Point12(version)) {
                // before v2.12, the exporting area is the area and the importing area is a connected area
                // the initial net position did not exist so a placeholder value is used
                counterTradeRangeActionAdder.withInitialNetPosition(0.0);
            }

            while (!jsonParser.nextToken().isStructEnd()) {
                addElement(counterTradeRangeActionAdder, jsonParser, version);
            }
            counterTradeRangeActionAdder.withInitialSetpoint(0.0);
            CounterTradeRangeAction counterTradeRangeAction = counterTradeRangeActionAdder.add();
            if (isBeforeV2Point12(version)) {
                LOGGER.warn("The initial net position of CounterTradeRangeAction {} is not defined before CRAC version 2.12, 0.0 is used as a placeholder", counterTradeRangeAction.getId());
            }
        }
    }

    private static boolean isBeforeV2Point12(String version) {
        return JsonSerializationConstants.getPrimaryVersionNumber(version) < 2
            || JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) < 12;
    }

    private static void checkFieldRemovedInV2Point12(JsonParser jsonParser, String version) throws IOException {
        if (!isBeforeV2Point12(version)) {
            throw new OpenRaoException("%s field is no longer used since CRAC version 2.12, it is replaced by area and connectedAreas".formatted(jsonParser.currentName()));
        }
    }

    private static void addElement(CounterTradeRangeActionAdder counterTradeRangeActionAdder, JsonParser jsonParser, String version) throws IOException {
        if (StandardRangeActionDeserializer.addCommonElement(counterTradeRangeActionAdder, jsonParser, version)) {
            return;
        }
        switch (jsonParser.currentName()) {
            case JsonSerializationConstants.AREA:
                counterTradeRangeActionAdder.withArea(jsonParser.nextTextValue());
                break;
            case JsonSerializationConstants.INITIAL_NET_POSITION:
                jsonParser.nextToken();
                counterTradeRangeActionAdder.withInitialNetPosition(jsonParser.getDoubleValue());
                break;
            case JsonSerializationConstants.EXPORTING_AREA, JsonSerializationConstants.EXPORTING_COUNTRY:
                checkFieldRemovedInV2Point12(jsonParser, version);
                counterTradeRangeActionAdder.withArea(jsonParser.nextTextValue());
                break;
            case JsonSerializationConstants.IMPORTING_AREA, JsonSerializationConstants.IMPORTING_COUNTRY:
                checkFieldRemovedInV2Point12(jsonParser, version);
                counterTradeRangeActionAdder.newConnectedArea().withArea(jsonParser.nextTextValue()).add();
                break;
            case JsonSerializationConstants.CONNECTED_AREAS:
                jsonParser.nextToken();
                ConnectedAreaArrayDeserializer.deserialize(jsonParser, counterTradeRangeActionAdder);
                break;
            default:
                throw new OpenRaoException("Unexpected field in InjectionRangeAction: " + jsonParser.currentName());
        }
    }
}
