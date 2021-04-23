/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_api.range_action.TapRangeAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

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
                switch (jsonParser.getCurrentName()) {
                    case MIN:
                        adder.withMinTap(jsonParser.nextIntValue(Integer.MIN_VALUE));
                        break;
                    case MAX:
                        adder.withMaxTap(jsonParser.nextIntValue(Integer.MAX_VALUE));
                        break;
                    case RANGE_TYPE:
                        adder.withRangeType(deserializeRangeType(jsonParser.nextTextValue()));
                        break;
                    case TAP_CONVENTION:
                        adder.withTapConvention(deserializeTapConvention(jsonParser.nextTextValue()));
                        break;
                    default:
                        throw new FaraoException("Unexpected field in TapRange: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
