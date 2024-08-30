/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.range.StandardRangeAdder;
import com.powsybl.openrao.data.cracapi.rangeaction.StandardRangeActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

/**
 *  @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class StandardRangeArrayDeserializer {
    private StandardRangeArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, StandardRangeActionAdder<?> ownerAdder) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            StandardRangeAdder<?> adder = ownerAdder.newRange();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case MIN:
                        jsonParser.nextToken();
                        adder.withMin(jsonParser.getDoubleValue());
                        break;
                    case MAX:
                        jsonParser.nextToken();
                        adder.withMax(jsonParser.getDoubleValue());
                        break;
                    case RANGE_TYPE:
                        jsonParser.nextToken();
                        adder.withRangeType(deserializeRangeType(jsonParser.getText()));
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in StandardRange: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
