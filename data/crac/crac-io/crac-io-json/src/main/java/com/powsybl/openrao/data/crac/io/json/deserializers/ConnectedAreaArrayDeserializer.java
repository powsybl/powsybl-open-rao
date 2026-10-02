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
import com.powsybl.openrao.data.crac.api.range.ConnectedAreaAdder;
import com.powsybl.openrao.data.crac.api.range.ConnectedAreaBorderRangeAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;

/**
 * @author Pedro Tobarra {@literal <pedro.tobarra at artelys.com>}
 */
public final class ConnectedAreaArrayDeserializer {
    private ConnectedAreaArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, CounterTradeRangeActionAdder ownerAdder) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            ConnectedAreaAdder adder = ownerAdder.newConnectedArea();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.currentName()) {
                    case JsonSerializationConstants.AREA:
                        adder.withArea(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.BORDER_RANGES:
                        jsonParser.nextToken();
                        deserializeBorderRanges(jsonParser, adder);
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in ConnectedArea: " + jsonParser.currentName());
                }
            }
            adder.add();
        }
    }

    private static void deserializeBorderRanges(JsonParser jsonParser, ConnectedAreaAdder ownerAdder) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            ConnectedAreaBorderRangeAdder adder = ownerAdder.newBorderRange();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.currentName()) {
                    case JsonSerializationConstants.MIN:
                        jsonParser.nextToken();
                        adder.withMin(jsonParser.getDoubleValue());
                        break;
                    case JsonSerializationConstants.MAX:
                        jsonParser.nextToken();
                        adder.withMax(jsonParser.getDoubleValue());
                        break;
                    case JsonSerializationConstants.RANGE_TYPE:
                        adder.withRangeType(JsonSerializationConstants.deserializeRangeType(jsonParser.nextTextValue()));
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in ConnectedAreaBorderRange: " + jsonParser.currentName());
                }
            }
            adder.add();
        }
    }
}