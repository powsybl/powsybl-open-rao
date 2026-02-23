/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.crac.api.range.TapRangeAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class TapRangeArrayDeserializer {
    private TapRangeArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, PstRangeActionAdder ownerAdder) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            TapRangeAdder adder = ownerAdder.newTapRange();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.currentName()) {
                    case JsonSerializationConstants.MIN:
                        adder.withMinTap(jsonParser.nextIntValue(Integer.MIN_VALUE));
                        break;
                    case JsonSerializationConstants.MAX:
                        adder.withMaxTap(jsonParser.nextIntValue(Integer.MAX_VALUE));
                        break;
                    case JsonSerializationConstants.RANGE_TYPE:
                        adder.withRangeType(JsonSerializationConstants.deserializeRangeType(jsonParser.nextTextValue()));
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in TapRange: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
