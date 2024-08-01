/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

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
                        instantKind = deseralizeInstantKind(jsonParser.nextTextValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in Instant: " + jsonParser.getCurrentName());
                }
            }
            crac.newInstant(instantId, instantKind);
        }
    }
}
