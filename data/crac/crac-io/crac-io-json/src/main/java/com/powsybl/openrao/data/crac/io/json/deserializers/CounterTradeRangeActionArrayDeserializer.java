/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.BorderRangeAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.ConnectedAreaAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public final class CounterTradeRangeActionArrayDeserializer {
    private CounterTradeRangeActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, String version, Crac crac) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction();

            while (!jsonParser.nextToken().isStructEnd()) {
                addElement(counterTradeRangeActionAdder, jsonParser, version);
            }
            counterTradeRangeActionAdder.withInitialSetpoint(0.0);
            counterTradeRangeActionAdder.add();
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
            case JsonSerializationConstants.CONNECTED_AREAS:
                jsonParser.nextToken();
                deserializeConnectedAreas(jsonParser, counterTradeRangeActionAdder);
                break;
            case JsonSerializationConstants.EXPORTING_AREA, JsonSerializationConstants.EXPORTING_COUNTRY:
                deserializeLegacyArea(jsonParser, version, jsonParser.currentName(), counterTradeRangeActionAdder::withArea);
                break;
            case JsonSerializationConstants.IMPORTING_AREA, JsonSerializationConstants.IMPORTING_COUNTRY:
                deserializeLegacyArea(jsonParser, version, jsonParser.currentName(), importingArea -> counterTradeRangeActionAdder.newConnectedArea().withArea(importingArea).add());
                break;
            default:
                throw new OpenRaoException("Unexpected field in CounterTradeRangeAction: " + jsonParser.currentName());
        }
    }

    /**
     * exportingArea/importingArea (and their legacy aliases exportingCountry/importingCountry) were replaced by
     * area/connectedAreas in CRAC version 2.12. They are still read for older versions, for retrocompatibility with
     * CRAC files written before that version.
     */
    private static void deserializeLegacyArea(JsonParser jsonParser, String version, String fieldName, Consumer<String> consumer) throws IOException {
        if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 2
            || JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) >= 12) {
            throw new OpenRaoException("Unsupported field %s in CRAC version >= 2.12".formatted(fieldName));
        }
        consumer.accept(jsonParser.nextTextValue());
    }

    private static void deserializeConnectedAreas(JsonParser jsonParser, CounterTradeRangeActionAdder counterTradeRangeActionAdder) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            ConnectedAreaAdder connectedAreaAdder = counterTradeRangeActionAdder.newConnectedArea();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.currentName()) {
                    case JsonSerializationConstants.AREA:
                        connectedAreaAdder.withArea(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.BORDER_RANGES:
                        jsonParser.nextToken();
                        deserializeBorderRanges(jsonParser, connectedAreaAdder);
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in ConnectedArea: " + jsonParser.currentName());
                }
            }
            connectedAreaAdder.add();
        }
    }

    private static void deserializeBorderRanges(JsonParser jsonParser, ConnectedAreaAdder connectedAreaAdder) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            BorderRangeAdder borderRangeAdder = connectedAreaAdder.newBorderRange();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.currentName()) {
                    case JsonSerializationConstants.MIN:
                        jsonParser.nextToken();
                        borderRangeAdder.withMin(jsonParser.getDoubleValue());
                        break;
                    case JsonSerializationConstants.MAX:
                        jsonParser.nextToken();
                        borderRangeAdder.withMax(jsonParser.getDoubleValue());
                        break;
                    case JsonSerializationConstants.RANGE_TYPE:
                        borderRangeAdder.withRangeType(JsonSerializationConstants.deserializeRangeType(jsonParser.nextTextValue()));
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in BorderRange: " + jsonParser.currentName());
                }
            }
            borderRangeAdder.add();
        }
    }
}
