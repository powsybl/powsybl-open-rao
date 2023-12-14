/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.ID;
import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.INSTANT_KIND;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */

public final class InstantArrayDeserializer {
    private InstantArrayDeserializer() {
        // should not be used
    }

    public static void deserialize(JsonParser jsonParser, Crac crac) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String instantId = null;
            InstantKind instantKind = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case ID:
                        instantId = jsonParser.nextTextValue();
                        break;
                    case INSTANT_KIND:
                        instantKind = InstantKind.valueOf(jsonParser.nextTextValue());
                        break;
                    default:
                        throw new FaraoException("Unexpected field in Instant: " + jsonParser.getCurrentName());
                }
            }
            crac.newInstant(instantId, instantKind);
        }
    }
}
