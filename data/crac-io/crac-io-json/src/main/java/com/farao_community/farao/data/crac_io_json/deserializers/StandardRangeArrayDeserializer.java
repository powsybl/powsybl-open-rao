/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.range.StandardRangeAdder;
import com.farao_community.farao.data.crac_api.range_action.StandardRangeActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 *  @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class StandardRangeArrayDeserializer {
    private StandardRangeArrayDeserializer() {
    }

    //a Standard range is implicitly of type "ABSOLUTE" : no RANGE_TYPE
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
                    default:
                        throw new FaraoException("Unexpected field in StandardRange: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
